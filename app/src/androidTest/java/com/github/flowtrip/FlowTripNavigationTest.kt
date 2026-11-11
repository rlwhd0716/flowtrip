package com.github.flowtrip

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.flowtrip.data.repository.MockUxRestrictionsRepository
import com.github.flowtrip.domain.model.UxAccess
import org.junit.Rule
import org.junit.Test

class FlowTripNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dateStart() {
        compose.onNodeWithText("02  데이트 모드").performClick()
        compose.onNodeWithText("데이트 시작").performScrollTo().performClick()
        waitFor("골목의 작은 식탁")
    }
    private fun waitFor(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }
    @Test fun mockStandaloneHasControlPanelWithoutMapAndCyclesThreeCandidates() {
        dateStart()
        compose.onNodeWithText("내비게이션 화면 요청").performScrollTo().assertIsNotEnabled()
        listOf("온기 키친", "초록 테이블", "골목의 작은 식탁").forEach {
            compose.onNodeWithText("다음 추천 경로").performScrollTo().performClick()
            waitFor(it)
        }
        compose.onNodeWithText("현재 코스 1/3 · 처음 가보는 식당").performScrollTo().assertIsDisplayed()
    }
    @Test fun tripInputBackNavigationAndRecreation() {
        compose.onNodeWithText("01  여행 모드").performClick()
        compose.onNodeWithText("코스 추천 시작").assertIsNotEnabled()
        compose.onNodeWithText("여행 목적지").performTextInput("강릉 & 바다/카페?")
        compose.onNodeWithText("코스 추천 시작").performScrollTo().performClick()
        waitFor("골목의 작은 식탁")
        compose.onNodeWithText("다음 추천 경로").performScrollTo().performClick()
        waitFor("온기 키친")
        compose.activityRule.scenario.recreate()
        waitFor("온기 키친")
        compose.onNodeWithText("‹  뒤로").performScrollTo().performClick()
        compose.onNodeWithText("강릉 & 바다/카페?").assertIsDisplayed()
    }
    @Test fun routeDoesNotAdvanceStageAndNextCourseAndSkipRemainSeparate() {
        dateStart()
        compose.onNodeWithText("이 장소로 길안내").performScrollTo().performClick()
        waitFor("현재 목적지: 골목의 작은 식탁")
        compose.onNodeWithText("현재 코스 1/3 · 처음 가보는 식당").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다음 코스").performScrollTo().performClick()
        waitFor("달빛 전망길")
        compose.onNodeWithText("이 단계 건너뛰기").performScrollTo().performClick()
        waitFor("느린 오후")
        compose.onNodeWithText("코스 완료").performScrollTo().performClick()
        waitFor("오늘의 코스를 살펴보세요")
        compose.onNodeWithText("선택 2곳 · 건너뛰기 1곳").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈으로").performScrollTo().performClick()
        compose.onNodeWithText("FlowTrip").assertIsDisplayed()
    }
    @Test fun restrictedUxBlocksInputsAndRestoresFormAfterRestrictionEnds() {
        compose.onNodeWithText("01  여행 모드").performClick()
        compose.onNodeWithText("여행 목적지").performTextInput("강릉")
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.RESTRICTED) }
        compose.onNodeWithText("정차 후 사용해 주세요").assertIsDisplayed()
        compose.onNodeWithText("여행 목적지").assertDoesNotExist()
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.SIMULATED) }
        compose.onNodeWithText("강릉").assertIsDisplayed()
    }
}
