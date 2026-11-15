package com.github.flowtrip.data.remote

import android.content.Context
import com.github.flowtrip.BuildConfig
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.LocationRepository
import com.github.flowtrip.domain.usecase.RecommendationRules
import com.github.flowtrip.domain.usecase.isFresh
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

/** The existing backend must forward the two documented Kakao endpoints and inject its REST key.
 * No Kakao credential is stored in the application. No implicit mock fallback. */
class KakaoPlaceSearch @Inject constructor(
    @ApplicationContext context: Context,
    private val location: LocationRepository,
) {
    private val history = context.getSharedPreferences("flowtrip_visits", Context.MODE_PRIVATE)
    fun recordVisit(place: Place) {
        if (place.provider != DataSource.KAKAO) return
        history.edit().putStringSet("ids", (history.getStringSet("ids", emptySet()).orEmpty() + place.id).toList().takeLast(500).toSet()).apply()
        val destination = place.destination ?: return
        val doc = JSONObject().put("id", place.id.removePrefix("kakao:")).put("place_name", place.name)
            .put("category_name", place.category).put("address_name", destination.address)
            .put("phone", place.phone).put("place_url", place.detailUrl)
            .put("x", destination.coordinates.longitude.toString()).put("y", destination.coordinates.latitude.toString())
            .put("stage", place.stage.name)
        val previous = runCatching { JSONArray(history.getString("recent", "[]")) }.getOrDefault(JSONArray())
        val updated = JSONArray().put(doc)
        for (i in 0 until previous.length()) {
            val old = previous.optJSONObject(i) ?: continue
            if (old.optString("id") != doc.getString("id") && updated.length() < 20) updated.put(old)
        }
        history.edit().putString("recent", updated.toString()).apply()
    }

    fun recentVisits(): List<Place> = runCatching {
        val docs = JSONArray(history.getString("recent", "[]"))
        (0 until docs.length()).mapNotNull { i ->
            val doc = docs.getJSONObject(i)
            runCatching { parse(doc, RecommendationStage.valueOf(doc.getString("stage"))) }.getOrNull()
        }.take(4)
    }.getOrDefault(emptyList())

    suspend fun search(request: RecommendationRequest): List<Place> = withContext(Dispatchers.IO) {
        check(BuildConfig.PLACE_PROXY_URL.isNotBlank()) { "실제 장소 검색 프록시 주소(placeProxyUrl)가 설정되지 않았습니다." }
        val fix = location.state.value.let { state -> state.location?.takeIf {
            state.status == LocationStatus.AVAILABLE && it.source == DataSource.PLEOS && it.isFresh(System.currentTimeMillis())
        } }
        val center = if (request.mode == TravelMode.DATE) {
            val coordinates = checkNotNull(fix) { "최신 차량 위치가 필요합니다. 위치 권한과 FusedLocation 연결을 확인해 주세요." }.coordinates
            check(RecommendationRules.isDomesticSearchArea(coordinates)) {
                "현재 차량 위치가 국내 장소 검색 범위 밖입니다. GPS 위치를 확인해 주세요." +
                    if (BuildConfig.DEBUG) " Pleos 에뮬레이터는 국내 시험 위치로 설정해야 합니다." else ""
            }
            coordinates
        } else {
            val docs = get("address.json", mapOf("query" to request.destination.replace(" 전체", ""))).getJSONArray("documents")
            check(docs.length() > 0) { "선택한 여행 지역의 좌표를 찾을 수 없습니다." }
            docs.getJSONObject(0).let { Coordinates(it.getString("y").toDouble(), it.getString("x").toDouble()) }
        }
        var origin = fix?.coordinates ?: center
        val budget = request.remainingMinutes ?: RecommendationRules.activityBudget(request.availableMinutes, request.dayInput)
        check(budget > 0) { "활동 가능 시간이 끝났습니다. 새 코스를 시작해 주세요." }
        val visited = history.getStringSet("ids", emptySet()).orEmpty().toSet()
        val used = request.excludedIds.toMutableSet()
        var plannedMinutes = budget
        request.mode.stages.drop(request.startStage).flatMap { stage ->
            if (plannedMinutes <= 0) return@flatMap emptyList<Place>()
            var responseCount = 0
            var parsedCount = 0
            val documents = queries(stage, request.preference).flatMap { keyword ->
                coroutineContext.ensureActive()
                val regionQuery = request.destination.replace(" 전체", "")
                val params = mutableMapOf("query" to if (request.mode == TravelMode.TRIP) "$regionQuery $keyword" else keyword,
                    "x" to center.longitude.toString(), "y" to center.latitude.toString(), "radius" to "20000", "size" to "15", "sort" to "distance")
                when (stage) {
                    RecommendationStage.RESTAURANT, RecommendationStage.LOCAL_FOOD -> params["category_group_code"] = "FD6"
                    RecommendationStage.DESSERT, RecommendationStage.CAFE -> params["category_group_code"] = "CE7"
                    else -> Unit
                }
                val docs = get("keyword.json", params).getJSONArray("documents")
                responseCount += docs.length()
                (0 until docs.length()).mapNotNull { i -> parse(docs.getJSONObject(i), stage)?.also { parsedCount++ } }.filter { place ->
                    RecommendationRules.distance(center, place.destination!!.coordinates) <= 20.0 &&
                        (request.mode != TravelMode.TRIP || inRegion(place.destination.address, request.destination))
                }
            }
            val assessment = RecommendationRules.assess(documents, origin, plannedMinutes, used, visited, request.preferNewPlaces)
            if (stage == request.mode.stages.getOrNull(request.startStage) && assessment.places.isEmpty()) {
                error(when {
                    responseCount == 0 -> "검색 위치 주변 20km에서 ${stage.title} 검색 결과가 없습니다. 위치나 관심 유형을 변경해 주세요."
                    parsedCount == 0 -> "검색 응답에서 유효한 장소 좌표를 읽지 못했습니다. 다시 시도해 주세요."
                    documents.isEmpty() -> "선택한 지역과 검색 위치 주변 20km 안에 ${stage.title} 후보가 없습니다. 위치나 지역을 확인해 주세요."
                    else -> assessment.emptyMessage(plannedMinutes)
                })
            }
            val ranked = assessment.places.take(3)
                .map { it.copy(reason = it.reason + if (fix == null) " · 여행 지역 중심에서 출발 가정" else " · 차량 위치 기준") }
            used.addAll(ranked.map { it.id })
            ranked.firstOrNull()?.let { chosen ->
                plannedMinutes -= chosen.travelMinutes + chosen.visitMinutes
                origin = chosen.destination!!.coordinates
            }
            ranked
        }
    }

    private fun inRegion(address: String, region: String): Boolean {
        fun normalize(value: String) = value.replace("특별자치도", "").replace("특별자치시", "")
            .replace("특별시", "").replace("광역시", "").replace("전체", "").replace(" ", "")
            .removeSuffix("시").removeSuffix("도")
        val parts = region.split(' ').filter { it.isNotBlank() && it != "전체" }
        return parts.all { normalize(it) in normalize(address) }
    }

    private fun queries(stage: RecommendationStage, preference: String): List<String> = when (stage) {
        RecommendationStage.RESTAURANT, RecommendationStage.LOCAL_FOOD -> listOf("음식점")
        RecommendationStage.DESSERT, RecommendationStage.CAFE -> listOf("카페")
        else -> buildList {
            if ("자연" in preference || "힐링" in preference) add("공원")
            if ("야경" in preference || "로맨틱" in preference) add("전망대")
            if ("역사" in preference) add("박물관")
            if ("문화" in preference || "예술" in preference || "전시" in preference) add("미술관")
            if ("쇼핑" in preference) add("쇼핑몰")
            if ("액티비티" in preference || "활동적" in preference || "특별한 경험" in preference) add("체험")
            if ("드라이브" in preference) add("관광명소")
        }.ifEmpty { listOf(if (stage == RecommendationStage.NIGHT_VIEW) "전망대" else "관광명소") }.distinct()
    }

    private fun parse(doc: JSONObject, stage: RecommendationStage): Place? = runCatching {
        val id = "kakao:${doc.getString("id")}"
        val name = doc.getString("place_name")
        require(name.isNotBlank() && id != "kakao:")
        val category = doc.getString("category_name")
        val address = doc.optString("road_address_name").ifBlank { doc.getString("address_name") }
        val phone = doc.optString("phone")
        val url = doc.optString("place_url").takeIf { it.startsWith("https://place.map.kakao.com/") || it.startsWith("http://place.map.kakao.com/") }.orEmpty()
        Place(id, stage, name, category, "선택한 지역·관심 유형 검색 · 직선거리 기반 이동·체류 추정, 실제 도로 ETA 아님",
            0.0, 0, destination = Destination(id, name,
                Coordinates(doc.getString("y").toDouble(), doc.getString("x").toDouble()), DataSource.KAKAO, address),
            visitMinutes = when (stage) {
                RecommendationStage.RESTAURANT, RecommendationStage.LOCAL_FOOD -> 60
                RecommendationStage.ATTRACTION -> 60
                else -> 40
            }, expectedSpend = "정보 미제공", parkingSummary = "정보 미제공",
            details = listOf(PlaceDetail("주소", address), PlaceDetail("카테고리", category),
                PlaceDetail("연락처", phone.ifBlank { "정보 미제공" }), PlaceDetail("상세 페이지", url.ifBlank { "정보 미제공" }),
                PlaceDetail("체류시간", "유형별 추정치 · 영업시간 미확인")),
            provider = DataSource.KAKAO, category = category, phone = phone, detailUrl = url)
    }.getOrNull()

    private fun get(endpoint: String, params: Map<String, String>): JSONObject {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val url = URI("${BuildConfig.PLACE_PROXY_URL.trimEnd('/')}/v2/local/search/$endpoint?$query").toURL()
        require(url.protocol == "https" || (BuildConfig.DEBUG && BuildConfig.ALLOW_LOCAL_PLACE_PROXY &&
            url.protocol == "http" && url.host in setOf("10.0.2.2", "127.0.0.1", "localhost")))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            val status = connection.responseCode
            if (status != 200) {
                val code = runCatching {
                    connection.errorStream?.use { JSONObject(it.readBytesLimited(16_384).toString(Charsets.UTF_8)).optString("code") }
                }.getOrNull()
                error(when {
                    code == "KAKAO_SERVICE_DISABLED" -> "카카오 지도·로컬 서비스가 비활성화되어 있습니다. 카카오디벨로퍼스의 FlowTrip 앱 → 카카오맵 → 사용 설정을 ON으로 변경해 주세요."
                    status == 401 -> "카카오 REST API 인증에 실패했습니다. 프록시 서버의 키 설정을 확인해 주세요."
                    status == 429 -> "카카오 검색 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."
                    else -> "장소 검색 서버 응답 오류 ($status). 설정·인증·쿼터를 확인해 주세요."
                })
            }
            val bytes = connection.inputStream.use { it.readBytesLimited(1_048_576) }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } catch (error: java.io.IOException) {
            if (BuildConfig.DEBUG && BuildConfig.ALLOW_LOCAL_PLACE_PROXY &&
                url.host in setOf("10.0.2.2", "127.0.0.1", "localhost")) {
                throw IllegalStateException("PC 장소 검색 서버에 연결하지 못했습니다. PC에서 tools/place-proxy/start.ps1을 실행하고 앱을 다시 실행해 주세요. 에뮬레이터 재시작 후에는 adb 포트 연결도 다시 설정해야 합니다.", error)
            }
            throw error
        } finally { connection.disconnect() }
    }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val size = read(buffer)
        if (size < 0) break
        check(out.size() + size <= limit) { "장소 검색 응답이 너무 큽니다." }
        out.write(buffer, 0, size)
    }
    return out.toByteArray()
}
