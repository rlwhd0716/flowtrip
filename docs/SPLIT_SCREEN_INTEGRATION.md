# Pleos 분할 화면 연동 변경 보고

기준: 2026-11-11. 추가: 별도 실제 목적지 테스트 2곳은 [실제 안내 테스트](REAL_NAVIGATION_TEST.md)를 참고한다. 추천 코스가 여전히 Mock인 것과 별개로 공식 샘플 목적지를 실제 SDK에 전송하는 기능을 추가했다. 이 문서는 이전 내장 지도 검토와 P0 결과보다 우선하는 현재 구현 설명이다.

## 기존 기능과 변경 범위

단일 app 모듈, Kotlin/Compose Material3, Navigation Compose, Hilt, MVVM/StateFlow와 Domain/Repository 구조를 유지했다. SDK/AGP/Gradle/Kotlin 및 라이브러리 버전을 변경하거나 새 라이브러리를 추가하지 않았다.

홈의 여행/데이트 모드, 여행 목적지·시간·취향 입력, 데이트 위치·시간·분위기·새로운 장소 옵션, 식당→야경→디저트의 3단계, 선택/건너뛰기 기록을 재사용한다. 기존 추천 Repository는 설정을 설명에 반영하는 가상 데이터 공급자이며 실제 지역 검색·최적화 알고리즘은 아니다. 이 성격은 변경하지 않았다. 기존 각 단계 첫 두 장소 순서를 보존하고 세 번째 후보를 추가했다.

기존 `RecommendationMap`은 실제 MapView가 아니라 Pleos 지도 열기 카드였다. 호출부를 확인한 후 이 카드를 제거했다. 사용처가 없는 내장 지도용 `CourseRoute`/`CourseRouteRepository` 계약도 제거했다. 현재 앱은 지도·마커·폴리라인을 렌더링하지 않는다.

## 사용자 동작

- `이 장소로 길안내`: 현재 카드의 장소를 요청한다. 성공해도 현재 코스 단계를 유지한다.
- `다음 추천 경로`: 같은 단계의 다음 후보를 즉시 요청한다. A→B→C→A로 순환한다. 확인 대화상자는 없다. 버튼 자체가 명시적인 목적지 변경 동작이다.
- `다음 코스`: 현재 단계를 완료하고 다음 단계 첫 후보의 경로를 요청한다. 경로 확인 후 단계와 기록을 확정한다. 마지막 단계에서는 `코스 완료`이며 내비게이션 안내를 취소하지 않는다.
- `남은 코스 경유 안내`: 현재 후보→이후 각 단계 첫 후보 순서로 경유지와 최종 목적지를 요청한다. 이후 후보 변경·다음 코스에서도 이 방식을 유지한다. 요청 전에 화면에 순서를 안내한다.
- `이 단계 건너뛰기`: 기존 제외 기능을 유지한다. 추천 단계만 이동하며 현재 안내를 변경하지 않는다.
- `내비게이션 화면 요청`: 사용자가 필요할 때만 `Foreground`를 요청한다. **경로 버튼에서는 이 API, Activity 재실행, FlowTrip 복귀 Intent를 호출하지 않는다.**

요청 중 현재 카드를 유지하고 조작을 잠근다. SDK 요청 반환만으로 성공으로 보지 않는다. 탐색 완료 후 목적지·경유지 목록/순서·안내 모드를 확인해야 후보/단계가 확정된다. 실패/시간 초과 시 기존 카드와 기록을 보존하고 요청 대상을 재시도할 수 있다. 플랫폼이 이미 경로를 바꿨을 가능성이 있으므로 자동으로 이전 경로를 재전송하거나 안내가 유지된다고 주장하지 않는다. 실제 수신 목적지와 경로 상태를 표시하고 불일치 안내 및 재조회를 수행한다.

도착은 상태 표시만 한다. 목적지/경유지 도착 모두 자동 코스 진행이나 방문 완료 처리로 사용하지 않는다. 내비게이션에서 직접 바꾼 목적지는 SDK 상태에 반영하고 현재 추천과 다르면 안내한다. 임의로 추천 경로를 덮어쓰지 않는다.

## 실제 사용 API

의존성 유지: `ai.pleos.playground:NaviHelper:3.0.5`, `FusedLocation:1.1.3`. NaviHelper의 navi-data는 3.0.4이다. 공개 AAR 클래스의 서명과 공식 가이드를 대조했다.

| 목적 | API/콜백 |
| --- | --- |
| 연결 | `initialize`, `addListener`, `removeListener`, `release` |
| 화면 요청 | `requestNavigationScreen(NavigationScreenRequest.Foreground)` |
| 목적지 설정/교체 | `requestRoute(RouteInfo)` |
| 경유 코스 | `requestRouteWithWaypoints(RouteWithWaypointsRequest(destination, waypoints))` |
| 경유지 타입 | `RequestWaypointInfo`, `WaypointIndex.FIRST/SECOND` (최대 2곳으로 제한) |
| 상태 확인 | `getDestinationInfo`, `getRouteStateInfo`, `getWaypointInfo` |
| 탐색 결과 | `onRouteStarted`, `onRoutingCompleted`, `onRouteStateInfo`, `onDestinationInfo`, `onWayPointInfo`, `onError` |
| 외부 변경/취소 | `onDestinationChanged`, `onWaypointChanged`, `onReRouteEnded`, `onRouteCancelled` |
| 도착 | `onDestinationArrived`, **`onWayPointArrived`** (SDK 실제 대소문자) |

`NAVI_ROUTE_SEARCH`와 결과 확인용 `NAVI_ROUTE` 권한을 경로 요청 전에 검사한다. 화면 요청은 `NAVI_CUSTOM_MAP`을 별도로 검사한다. 경로 요청 전 UX를 재확인한다. SDK 타입은 data/pleos에만 존재하며 화면과 ViewModel은 Domain 모델을 사용한다.

## 분할 화면과 생명주기

[공식 앱 구조](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)는 앱 영역의 최대 2개 window, 화면 방향/크기 변경, 기본 방향 미지정 시 한 window 사용을 설명한다. [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)는 Connect-S/Connect-W별 배치 차이, 사용자 window bar/제스처, App-to-App 실행이 기존 창을 덮을 수 있음을 설명한다.

이 정책에 맞춰 `resizeableActivity=true`, 방향 고정 없음, 단일 기존 Activity를 사용한다. 다른 앱의 창을 강제로 배치하거나 비율을 지정하지 않는다. `launchAdjacent`, 숨겨진 API, 오버레이 권한, adb 기반 시스템 설정 변경을 사용하지 않는다. 한 창에서는 핵심 추천/버튼을 세로 배치하고, 충분히 넓으면 부가 정보를 옆에 표시한다. 이는 **FlowTrip 내부 반응형 구성**이며 Pleos 두 앱 분할을 흉내내는 화면이 아니다.

가시 상태인 split Activity가 `onPause`되어도 연결을 해제하지 않는다. 설정 변경에 의한 재생성에서도 application-scoped Adapter와 ViewModel의 요청을 유지한다. 실제 `onStop` 백그라운드에서는 리스너를 해제하며 복귀 시 연결/목적지/경로/경유지를 재조회한다. 이때 처리 중이던 요청은 중단될 수 있으며 실패 상태와 명시적 재시도를 제공한다.

`SavedStateHandle`에는 모드/설정(기존 내비게이션 인수), 최대 9개 후보 스냅샷, 단계/후보 인덱스, 선택·제외 기록, 경유 안내 모드, 마지막 확인 목적지, 이전 경로 상태, 중단/실패 요청 대상을 저장한다. `Place`/`Destination`/`Coordinates`에 Android saved-state가 지원하는 Serializable을 사용하며 새 직렬화 라이브러리는 없다. 현재 위치는 저장하지 않는다. 프로세스 종료 후 요청을 자동 재실행하지 않으며 이전 안내 정보는 과거 상태로만 표시하고 SDK로 재확인한다. 앱 강제 종료/데이터 삭제 후 영구 복원은 범위 밖이다.

## 주행 UX

기존 `CarUxRestrictionsManager`와 최상위 UX gate를 유지한다. 제한/알 수 없음에서는 입력과 경로 조작을 제공하지 않으며, 요청 use case 및 실제 Adapter에서도 허용 상태를 검사한다. 주차 허용 상태에서만 사용한다. 버튼은 최소 64dp이다. DO나 Maps 카테고리를 편의상 선언하지 않았다.

[공식 주행 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)와 앱 카테고리 심사에 따라 주행 중 동작 허용은 별도 확인해야 한다. **다음 추천 경로를 주행 중 사용할 수 있다고 가정하지 않는다.** 따라서 현재 구현은 주행 중 추천 패널을 계속 조작하는 목표까지 완료한 상태가 아니다.

## 검증 결과

- Mock Debug APK 빌드, UI 테스트 APK 빌드: 성공.
- Mock 단위 테스트: **33개, 실패/오류 0개**. 추천 ViewModel 13개, 경로 결과 대조 5개, 내비게이션 7개, 위치 5개, 폼 2개, 기존 예제 1개.
- A→B→C→A에서 화면 요청 미호출, 비동기 성공 전 카드 유지, 실패 후 B 재시도, 빠른 연속 클릭 차단, 다음 단계 실패 보존, 도착 후 자동 진행 금지, 경유지 순서, 외부 목적지 동기화, 후보 스냅샷 및 Java 직렬화 복원, 불완전 추천 응답 후 재시도 복구, 중단 요청의 명시적 재시도, UX UNKNOWN/제한 차단을 검사했다.
- UI 계측: 새 동작에 맞춘 테스트 코드/테스트 APK 컴파일 성공. **실행 미검증**. 기존 Pixel_4_2(Android 36.1)를 읽기 전용/스냅샷 저장 없이 실행하려 했으나 초기화 중 종료(exit 1)되었다. 소프트웨어 렌더러와 Vulkan 비활성화로도 시작되지 않았다. 시스템 이미지/사용자 AVD 설정을 수정하지 않았다.
- 작업 중 연결된 Pleos_Connect_v2.1.0(emulator-5554), automotive feature, Navi Service/기본 내비게이션 설치 여부를 읽기 전용으로 확인했다. 해당 AVD에 변경 APK를 설치하거나 경로를 전송하지 않았다. **분할 POC와 연속 경로 변경은 검증하지 않았다.**
- 실제 Pleos SDK Debug APK와 Lint: **최종 소스로 통과**. Lint 오류 0개, 경고 23개(라이브러리 버전 안내/기존 리소스 등). SDK 콜백 서명 관련 컴파일 경고도 수정했다.

재현 명령 (PowerShell):

```powershell
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' -PpleosSdk=false :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' -PpleosSdk=true :app:assembleDebug :app:lintDebug
# 일반 Android 기기를 명시해 연결한 뒤 UI 실행:
$env:ANDROID_SERIAL='일반_Android_기기_ID'
.\gradlew.bat -PpleosSdk=false :app:connectedDebugAndroidTest
```

MS949는 이 PC의 한글 사용자 경로에서 Gradle test worker를 실행하기 위한 일회성 옵션이다. 프로젝트 인코딩 설정은 변경하지 않았다. Mock UI 테스트는 일반 Android용이며, 실제 CarUxRestrictionsRepository를 사용하는 Pleos AVD에서 주행 제한을 우회하는 테스트가 아니다.

## 남은 제약과 실제 Pleos 확인 순서

1. 추천은 양쪽 빌드에서 Mock이며 실제 좌표/POI 데이터 공급자가 없다. 기존 정책대로 Mock 목적지의 실제 전송을 차단한다. 따라서 현재 기본 데이터로 실제 차량 A→B→C를 실행할 수 없다. 공급자가 검증된 `Destination(source=PLEOS, coordinates, poiId, poiSubId, address, poiProvider)`를 반환하도록 기존 RecommendationRepository 바인딩을 교체해야 한다. 임의 POI, 문서 예제를 추천으로 위장하는 대체 기능은 추가하지 않았다.
2. Pleos 기본 내비게이션과 FlowTrip을 시스템 window bar/제스처로 두 창에 배치하고, 두 크기/방향 및 단독 창을 확인한다. 앱 카테고리/대상 차량에서 두 앱 동시 표시가 허용되는지 확인한다.
3. 실제 장소 공급자를 연결한 후 현재 장소 요청, A→B→C→A, 남은 코스 경유 요청을 시험한다. 목적지·경유지·안내 모드 콜백과 실제 안내가 모두 일치하는지 확인한다.
4. 후보 변경 시 FlowTrip Activity가 숨겨지거나 포커스가 강제 전환되는지 확인한다. 코드에서는 화면 요청을 하지 않지만 NaviHelper/내비게이션의 실제 window 동작은 앱이 보장할 수 없다.
5. 실패/권한 회수/25초 타임아웃/외부 목적지 변경/외부 경유지 변경/취소/도착을 확인한다. SDK에는 요청 ID가 없으므로 완료 뒤 상태 대조는 보수적이며 순서가 다른 콜백과 늦은 완료의 실제 동작을 확인해야 한다.
6. 탐색 중 창 크기 변경, 회전, 백그라운드/복귀, Activity 재생성, 시스템 프로세스 재생성 후 후보·안내 불일치가 없는지 확인한다.
7. 차량 UX 상태 UNKNOWN/제한/허용과 앱 카테고리 심사를 확인한다. 제한을 에뮬레이터/시스템 권한 우회로 풀지 않는다.

**사용자 완료 기준 3(실제 두 앱 동시 표시)과 6(연속 요청 중 불필요한 화면 전환 없음)은 미검증이며 완료 처리하지 않는다.** 4/5의 실제 차량 경로 요청 역시 실제 장소 공급자 연결 전에는 미완료이다. 지원 여부가 확인되지 않은 자동 분할 대체 기능은 제공하지 않는다.

공식 경로 참고: [목적지 설정/교체](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations), [경유지 경로](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints), [콜백](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners).

## 변경 파일

프로젝트 루트를 기준으로 한 전체 경로이다.

- 수정: `README.md`
- 수정: `app/src/androidTest/java/com/github/flowtrip/FlowTripNavigationTest.kt`
- 수정: `app/src/main/AndroidManifest.xml`
- 수정: `app/src/main/java/com/github/flowtrip/MainActivity.kt`
- 수정: `app/src/main/java/com/github/flowtrip/core/navigation/FlowTripNavHost.kt`
- 수정: `app/src/main/java/com/github/flowtrip/data/mapper/DestinationMatching.kt`
- 수정: `app/src/main/java/com/github/flowtrip/data/mapper/RouteConfirmation.kt`
- 수정: `app/src/main/java/com/github/flowtrip/data/model/MockPlaceData.kt`
- 수정: `app/src/main/java/com/github/flowtrip/data/repository/MockNavigationRepository.kt`
- 삭제: `app/src/main/java/com/github/flowtrip/domain/model/CourseRoute.kt`
- 수정: `app/src/main/java/com/github/flowtrip/domain/model/PlatformState.kt`
- 수정: `app/src/main/java/com/github/flowtrip/domain/model/Recommendation.kt`
- 삭제: `app/src/main/java/com/github/flowtrip/domain/repository/CourseRouteRepository.kt`
- 수정: `app/src/main/java/com/github/flowtrip/domain/repository/NavigationRepository.kt`
- 수정: `app/src/main/java/com/github/flowtrip/domain/usecase/RequestNavigationUseCase.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/common/PlatformStatusCard.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/common/preview/FlowTripPreviewData.kt`
- 삭제: `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationMap.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationScreen.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModel.kt`
- 수정: `app/src/pleos/java/com/github/flowtrip/data/mapper/PleosMappers.kt`
- 수정: `app/src/pleos/java/com/github/flowtrip/data/repository/PleosNavigationRepository.kt`
- 수정: `app/src/test/java/com/github/flowtrip/platform/RouteConfirmationTest.kt`
- 수정: `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt`
- 수정: `docs/PLEOS_INTEGRATION.md`
- 수정: `docs/PROGRESS.md`
- 신규: `docs/SPLIT_SCREEN_INTEGRATION.md`
