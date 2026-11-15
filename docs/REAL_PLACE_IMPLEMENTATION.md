# 1차 실제 장소 추천 연결 결과

작성: 2026-11-15, 기준 브랜치 `main`, 시작 HEAD `2f116f0`. 커밋·배포·기기 설치는 하지 않았다.

**후속 연결 작업:** 전달받은 REST 키는 Git 제외 `secrets.properties`에만 저장했고, Node 기본 모듈을 사용하는 PC 로컬 프록시와 Debug 전용 연결을 추가했다. 최초 비활성화 403 이후 카카오 서비스 활성화로 주소/장소 검색 HTTP 200을 확인했다. 이후 앱용 프록시 미실행에 따른 Pleos 연결 시간 초과를 수정하고, 에뮬레이터 내부에서 두 로컬 경로 모두 HTTP 200/장소 JSON을 확인했다. 아래 본문의 ‘프록시 없음’은 최초 구현 시점 기록이다. 최신 실행·설정·검증 범위는 [프록시 안내](../tools/place-proxy/README.md)를 참고한다.

## A. 기존 구현 확인

- 시작 작업 트리는 깨끗했다. `69875e7` 이후 `184a830`(분할화면 수정), `2f116f0`(화면 디자인·리소스)이 있다. 현재 소스를 우선했다.
- README, PROGRESS, PLEOS_SDK_RESEARCH, DEVELOPMENT_ROUTING 및 화면/분할 관련 문서와 현재 소스를 대조했다. 저장소와 상위 경로에서 적용할 AGENTS.md는 발견되지 않았다.
- 연결된 Codex 기록 `docs 사진 2개 확인` (`01a1d58f-7fc9-7202-8db3-a4cba19891a1`), `구현 방식 결정하기` (`01a1c10a-aaa1-7262-b526-e6b9faf9ad3f`)의 최근 각각 4개 턴을 읽었다. 화면 입력·아이콘 적용, 주입 생성자 수정, 개발용 샘플 목적지 연결, 자동 분할 실험 종료 내용을 확인했다. 전체 과거 세션을 열람한 것은 아니다. 읽은 결과는 Git/현재 소스와 일치했다.
- 현행은 단일 app / Kotlin / Compose / Hilt / MVVM / Repository, 일반·Pleos 조건부 소스셋이다. 기존 5개 화면 구성과 디자인을 유지했다.
- 여행은 시·도/시·군·구, 시간/일, 관심사 최대 3개. 데이트는 기존 위치 스트림, 시간/일, 분위기 최대 2개, 장소 유형, 새로운 장소 옵션을 갖고 있다.
- 여행과 데이트는 각각 3단계, 각 단계 3개 Mock 후보. `chooseCandidate`/`recommendAnother`는 탐색만 하고 경로를 전송하지 않는다. `nextCourse`는 선택 기록을 남기고 단계를 진행한다.
- 추천 로딩/오류/요청 중 여부는 기존 UiState 필드, SDK 경로는 `IDLE/SEARCHING/GUIDING/ARRIVED/FAILED`를 사용한다. 요청서의 `RECOMMENDING/READY/ROUTING/NAVIGATING/ERROR`라는 별도 통합 enum은 없으며 추가하지 않았다.
- SavedStateHandle이 후보 스냅샷, 단계, 선택/제외 ID, 모드별 스냅샷, 중단 요청과 재시도를 복원한다. 복원 중 경로를 자동 재전송하지 않는다.
- FusedLocation의 initialize/last-known/callback/해제, 60초 freshness 판단 및 권한 처리를 재사용했다. 시각 없는 NaviHelper 대체 위치는 최신 위치로 인정하지 않는다.
- NaviHelper는 요청 직후 성공 처리하지 않는다. 완료 이벤트 후 목적지 좌표·POI/경유 순서·안내 모드를 대조하고 최대 25초 기다리는 기존 로직을 유지한다.
- 자동 두 앱 배치는 이전 실험에서 실패/미해결이었다. 현재 Activity의 START/STOP, 분할 상태에서 pause 허용, 구성 변경 시 어댑터 유지 정책을 변경하지 않았다.
- 기존 최근 장소는 선택 ID 중심이었다. 실제 방문 확정과 실제 공급자 장소의 영구 상세 저장은 없었다. HTTP/장소 API/서버 프로젝트도 저장소에서 발견하지 못했다.

| 기능 | 분류 | 처리 |
| --- | --- | --- |
| 화면/입력/후보 선택/단계/상태 복원 | 기존 사용 및 일부 확장 | 화면과 VM 그대로 사용 |
| FusedLocation/UX 제한/SDK 이벤트/분할 생명주기 | 기존 사용 | 새 콜백·관리자 없음 |
| Mock Repository/UseCase/DI 계약 | 일부 확장 | 기존 바인딩 유지, 데이터 모드 분기 |
| 실제 검색 HTTP와 응답 매핑 | 신규 필요 | 데이터 계층 클라이언트만 추가 |
| 거리·시간·방문 이력·재추천 | 일부 확장/신규 계산 | 순수 Kotlin 규칙과 기존 VM에 연결 |
| 외부 좌표 길안내 | SDK 검증 필요 | 기본 차단, 명시적 시험 옵션 |
| 카카오 키·검색 프록시 | 외부 설정 필요 | 서버 키 주입 계약 제시, 서버 신규 구축 없음 |

## B. 추가 구현

### 데이터 흐름과 설정

`기존 입력 → Navigation 인수 → 기존 RecommendationViewModel → GetRecommendationsUseCase → 기존 MockRecommendationRepository → KakaoPlaceSearch → HTTPS 프록시 → 카카오 로컬 → Place → 기존 후보/상세 UI`

역할이 같은 Repository/ViewModel/UseCase는 새로 만들지 않았다. `MockRecommendationRepository`는 이름을 보존하고 실제 데이터 분기도 담당한다. 기본 설정은 `placeDataSource=mock`이며 기존 샘플 매핑과 Mock 추천을 유지한다.

실제 설정:

```powershell
.\gradlew.bat -PpleosSdk=true -PplaceDataSource=kakao '-PplaceProxyUrl=https://YOUR_EXISTING_SERVER' :app:assembleDebug
```

`placeProxyUrl`은 비밀정보가 아닌 HTTPS origin 또는 base path이다. 카카오 REST 키는 앱/BuildConfig/소스에 넣지 않는다. 프록시 미설정, 위치 미확보, HTTP 오류, 검색 결과 부족 시 명시적으로 오류를 표시한다. Mock/공식 샘플 목적지로 자동 대체하지 않는다. 모드 전환은 실제 데이터일 때 기존 설정 화면으로 이동하여 해당 모드 입력을 다시 받는다.

### 최소 프록시 계약 — 서버 연결은 남은 작업

기존 서버가 없으므로 배포된 검색 서비스는 아직 없다. 아래 두 GET만 기존/추후 서버에서 제한적으로 제공하면 Android 코드를 연결할 수 있다.

| 앱 요청 | 서버의 고정 upstream |
| --- | --- |
| `/v2/local/search/address.json?query=...` | `https://dapi.kakao.com/v2/local/search/address.json` |
| `/v2/local/search/keyword.json?query=...&x=...&y=...&radius=20000&size=15&sort=distance[&category_group_code=FD6/CE7]` | `https://dapi.kakao.com/v2/local/search/keyword.json` |

서버에서 `Authorization: KakaoAK <서버 환경의 REST 키>`를 추가하고 카카오의 `documents` JSON 구조를 유지한다. 비정상 upstream 상태를 200 성공으로 바꾸지 않는다. 임의 URL 전달형 프록시로 만들지 않는다. 서비스 인증·쿼터·위치 로그 보관 정책은 실제 서버 연결 시 정해야 한다. 현재 앱에는 서버 계정/인증 세션이 없어 해당 통합은 미구현이다.

카카오의 ID/이름/카테고리/도로명 또는 지번 주소/경도 x·위도 y/연락처/상세 URL을 매핑한다. ID에는 `kakao:` namespace를 붙이고 `provider=KAKAO`로 구분한다. 카카오 ID를 Pleos POI 필드에 넣지 않는다. 연락처가 없으면 미제공으로 표시한다.

실제 카드·상세·최근 장소에는 Mock 사진을 사용하지 않는다. 메뉴/가격/영업시간/주차 정보는 생성하지 않는다. 상세 URL은 텍스트로 제공한다. 새 지도·오버레이·지도 SDK는 없다.

### 추천과 코스 시간

- 여행은 기존 선택 지역의 주소 검색으로 중심을 구한 뒤 지역명을 포함한 키워드를 검색하고 응답 주소의 지역도 검사한다. 최신 차량 위치가 있으면 이동 추정 출발점으로 사용하고, 없으면 지역 중심 출발 가정을 명시한다. 지역 선택 목록은 기존 17개 시·도/주요 시·군·구 목록이며 전국 전체 행정구역 DB로 확장하지 않았다.
- 데이트는 현재 `LocationRepository`의 Pleos fix와 기존 freshness 규칙을 사용한다. Mock/STALE/권한 거부 위치로 실제 주변 검색을 진행하지 않는다.
- 기존 단계의 식당/카페 유형은 유지하며 `FD6/CE7`로 검색한다. 관광·야경 단계는 자연/역사/문화·예술/쇼핑/체험/드라이브 및 관련 분위기를 검색어에 반영한다. 분위기는 검색어 수준이며 분위기·품질이 검증됐다는 점수를 만들지 않는다.
- 검색 범위는 중심 반경 20km, 검색어당 15건이다. 전국 완전 탐색/페이지 전체 수집은 아니다.
- Haversine 직선거리, 가정 속도 25km/h, 우회 계수 1.4, 이동 준비 5분으로 이동시간을 추정한다. 식당/관광 60분, 다른 단계 40분은 제품의 체류 가정이다. 영업시간/정체/도로 ETA가 아니다.
- `RecommendationWeights`에 거리 패널티/기방문 패널티/속도/우회 계수/준비 시간을 모았다. 새로운 장소 옵션을 켜면 방문 장소에 거리 점수 20 상당의 패널티를 준다. 리뷰/평점/분위기의 가짜 평가 점수는 없다.
- 방문/제외/재추천에서 이미 표시한 ID를 제외하고 최대 3곳을 제공한다. 결과가 적으면 복제하지 않는다. 현재 단계가 0곳이면 오류/조건 변경을 제공한다.
- 후속 단계의 최초 후보를 기준으로 예상 체류·이동을 차감하여 임시 순서를 구성한다. 다른 후보를 선택하면 후속 코스는 방문 완료 후 다시 검색한다. 전체 경유 요청 시에도 현재 선택 순서의 추정 합계가 남은 시간 안인지 확인한다.
- 시작 시각을 SavedStateHandle에 저장한다. 방문 완료/다음 추천/다음 코스/길안내 요청 시 실제 경과시간을 빼므로 백그라운드 시간을 포함한다. UI의 남은 분 표시는 동작 시 갱신하며 실시간 초 단위 타이머는 아니다.
- `일` 입력 여부를 Navigation 인수로 전달한다. 1일 이상은 **첫날 활동 8시간만 지원**한다고 표시한다. 다일 숙박·날짜별 일정은 구현하지 않았다. 24시간 입력과 1일 입력은 구분한다.
- 도착 신호는 안내 문구만 갱신한다. 기존 다음 코스 버튼을 실제 데이터에서 `방문 완료 · 다음 코스/코스 완료`로 표시하여 사용자 확인 시에만 방문을 기록한다. 추정 체류시간을 실제 경과시간에서 한 번 더 차감하지 않는다.
- 방문 ID(최대 500) 및 최근 실제 장소 상세(최대 20, 홈 표시 4)를 SharedPreferences에 저장한다. 도착만으로 저장하지 않는다. 코스 진행 자체의 장기 DB 저장은 추가하지 않았다.

### SDK 연동

공식 현재 목적지 가이드는 `RouteInfo(latitude, longitude, poiName, poiId, poiSubId, address)` 예시를 제공하고 완료 콜백 확인을 요구한다. 고정된 NaviHelper 3.0.5의 기존 어댑터/API 형태를 유지했다. **외부 좌표만으로 요청할 때 POI 문자열을 비워도 된다는 최신 지원 보장이나 외부 ID→Pleos POI 변환 API는 확인하지 못했다.**

- 기본 `pleosCoordinateRouting=false`: 실제 카카오 장소는 미검증 사유와 필요한 SDK 확인을 표시하고 경로 전송을 차단한다. 기존 공식 샘플의 POI 길안내는 계속 동작한다.
- 대상 SDK/차량의 지원 확인 후 시험용 `-PpleosCoordinateRouting=true`를 명시하면 외부 장소의 실제 좌표·이름·주소와 빈 POI 문자열로 기존 요청 경로에 전달한다. 이것은 실험 경로이며 지원/성공을 보장하지 않는다. 외부 ID를 Pleos ID로 가장하거나 샘플로 대체하지 않는다.
- 결과 대조는 KAKAO 요청↔PLEOS SDK 응답의 좌표 비교를 허용하되 Mock↔실제 비교는 거절한다. 완료·안내 모드·경유 순서 확인은 유지한다. 차량이 입구 좌표로 보정하면 기존 약 0.0001도 오차 기준 때문에 확인 실패할 수 있으며 임의로 성공 처리하지 않는다.
- 기존 단일 목적지와 `requestRouteWithWaypoints`를 구분한다. 공식 샘플 패널은 현재 추천 UI의 스크롤 영역에 다시 연결했다. 새 창 실행·자동 분할·강제 포커스 변경은 추가하지 않았다.
- 기존 Navigation Compose의 `remember(navController)`를 `remember(entry)`로 바꿔 Lint의 복원 키 오류를 수정했다.

공식 근거: [Pleos 목적지/콜백](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations), [카카오 로컬 검색 필드와 파라미터](https://developers.kakao.com/docs/ko/local/dev-guide).

## C. 검증

Mock 설정: Debug APK, 계측 테스트 APK 빌드, 단위 테스트 45개 통과(실패 0), Lint 오류 0. 기존 Mock 추천·후보/코스·복원·UX/경로/위치 테스트를 유지했다. 신규 7개는 거리/시간/방문 우선순위, 일 입력, 남은 시간, 방문 확인, 재추천 제외, 실제↔SDK 목적지 대조, 데이터 공급자 변경 시 Mock 상태 제거를 검증한다.

최종 실행 결과:

| 설정 | Debug APK | 계측 APK | 단위 테스트 | Lint |
| --- | --- | --- | --- | --- |
| `-PpleosSdk=false` / 기본 Mock 추천 | 성공 | 성공 | 45개 통과 | 오류 0, 경고 28 |
| `-PpleosSdk=true -PplaceDataSource=kakao` | 성공 | 성공 | 45개 통과 | 오류 0, 경고 28 |

두 설정 모두 다음 명령 형태로 검증했다. `MS949`는 기존 Windows 한글 경로의 Gradle worker 실행 문제를 피하는 실행 옵션이며 저장소 인코딩 설정을 바꾸지 않았다.

```powershell
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' -PpleosSdk=false :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' -PpleosSdk=true -PplaceDataSource=kakao :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

경고는 라이브러리/도구 버전 안내, 중복 Activity label, 기존 테마 리소스 등에 관한 항목이다. 무시 baseline이나 검사 제외는 추가하지 않았다. `git diff --check`도 통과했다. 생성된 마지막 Debug APK는 실제 데이터 설정이지만 프록시 URL이 비어 있는 검증용 산출물이며 설치하지 않았다.

`adb devices -l` 조회에서 연결 기기가 없었다. 카카오 프록시/키도 없으므로 실제 네트워크 응답/주변 검색, 실제 좌표 경로 성공, 연속 목적지 변경, 차량 분할 창·생명주기 UI 시나리오는 **미검증**이다. 테스트용 fake Repository의 통과를 API/차량 성공으로 해석하지 않는다. 계측 테스트 APK 빌드와 계측 테스트 실행은 구분한다.

## D. 남은 작업

1. 카카오 앱/REST 키와 위 계약의 최소 HTTPS 프록시 연결 및 서비스 인증 통합.
2. Pleos 담당 문서/실제 차량에서 외부 좌표 + 빈 POI 지원 확인. 미지원이면 공식 POI 검색/변환 공급자를 확보해야 한다. 현재 변환을 임의 구현하지 않았다.
3. 실제 설정으로 데이트: 위치→검색→최대 3후보→상세→실제 장소 요청→도착 신호→사용자 방문 완료→남은 시간→새 현재 위치 검색→다음 목적지 전체 시나리오 실행. 여행은 기존 지역 선택으로 같은 흐름 확인.
4. 수동 구성된 분할 창에서 연속 목적지 요청, 앱 숨김/복귀, 권한 취소, SDK 시간 초과, 프로세스 복원 검증. 자동 1:1 배치는 여전히 지원 확인이 필요하다.
5. 실제 도로 ETA, 영업시간, 더 넓은 결과 수집, 다일 일정은 별도 범위다.

## 변경 파일

- 설정: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- 기존 domain: `domain/model/Recommendation.kt`, `PlatformState.kt`, `domain/repository/RecommendationRepository.kt`, `domain/usecase/GetRecommendationsUseCase.kt`, `RequestNavigationUseCase.kt`
- 신규 계산: `domain/usecase/RecommendationRules.kt`
- 기존 data: `data/repository/MockRecommendationRepository.kt`, `data/mapper/DestinationMatching.kt`
- 신규 HTTP/매핑: `data/remote/KakaoPlaceSearch.kt`
- 기존 화면/VM: `presentation/trip/TripViewModel.kt`, `presentation/date/DateViewModel.kt`, `presentation/recommendation/RecommendationViewModel.kt`, `RecommendationScreen.kt`, `presentation/home/HomeViewModel.kt`, `HomeScreen.kt`, `core/navigation/FlowTripNavHost.kt`
- 기존 Pleos: `app/src/pleos/java/com/github/flowtrip/data/repository/PleosNavigationRepository.kt`
- 테스트: `data/RecommendationRulesTest.kt`(신규), `platform/RouteConfirmationTest.kt`, `presentation/recommendation/RecommendationViewModelTest.kt`
- 문서: 이 파일, `README.md`, `docs/PROGRESS.md`

domain/data/presentation 경로의 기준은 `app/src/main/java/com/github/flowtrip/`, 테스트는 `app/src/test/java/com/github/flowtrip/`이다.
