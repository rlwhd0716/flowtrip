# FlowTrip 구현 진행도

기준: 2026-11-10 (Asia/Seoul).
현재 상태: **여행·데이트 Mock 추천, Pleos Navigation/Location/UX Adapter 및 지도 화면 요청 구현 완료. Pleos 대상 환경의 실제 동작 검증은 대기 중.**

상세 구현은 [연동 결과](PLEOS_INTEGRATION.md), 공식 API·권한 및 외부 준비사항은 [SDK 조사](PLEOS_SDK_RESEARCH.md),
화면 흐름과 빌드 안내는 [README](../README.md)를 참고한다.

## 개발 방향

- 차량 기능은 Pleos API/SDK를 우선 사용한다.
- 추천 장소 정보는 당분간 Mock 데이터를 유지한다.
- 지도·내비게이션은 Pleos Connect Navigation 화면에 연결한다. OpenStreetMap·OSRM·Leaflet 지도 및 외부 지도 요청은 제거했다.
- 가게 검색·상세 정보도 Pleos 제공 범위를 먼저 확인한다. 외부 장소 API는 아직 연동하지 않았다.

## 기능별 진행 상태

| 구분 | 구현 상태 | 남은 확인 |
| --- | --- | --- |
| 여행·데이트 입력 / 화면 이동 | 완료 | 실제 차량 화면에서 사용성 확인 |
| 식당 → 야경 → 디저트 추천 | Mock 구현 완료 | 실제 장소 공급자 및 추천 알고리즘 연결 |
| 후보 변경·선택·건너뛰기·완료 요약 | 완료 | 실제 차량 흐름 검증 |
| 추천 정보 표시 | 설명·추천 이유·예시 주소·체류 시간·비용·주차 정보 표시 | 실제 영업시간·가격·주차 등 데이터 확보 |
| 목적지 변경 승인 | 별도 승인 창 및 경로 결과 확인 후 추천 진행 구현 | 실제 POI와 차량 경로 요청 검증 |
| NaviHelper | SDK 초기화/해제·목적지/경로 상태·도착 이벤트 Adapter 구현 | Pleos 서비스의 실제 콜백·오류·복귀 검증 |
| FusedLocation | 위치 조회/구독·권한·만료·오류 처리 구현 | Pleos 환경의 실제 위치 수신 검증 |
| 차량 UX 제한 | AAOS 제한 상태 확인·화면 차단/복원 구현 | 실제 주행/정차 신호 및 권한 검증 |
| Pleos 지도 | `Pleos 지도 열기` → 내비게이션 화면 표시 요청 구현 | Pleos 지도 화면 표시 및 FlowTrip 복귀 검증 |
| TTS / STT | 공식 API·권한 조사 완료 | 음성 기능 구현 및 대상 환경 검증 |
| Vehicle | 조회 API 및 활용 방향 조사 완료 | 차량 데이터 연동 구현 및 대상 환경 검증 |

## 이번 지도 변경 내용

- 추천 페이지의 외부 지도를 `Pleos 내비게이션 지도` 카드와 `Pleos 지도 열기` 버튼으로 변경했다.
- `NavigationRepository.openMap()`을 추가하고 UseCase에서 차량 UX 제한을 검사한다.
- 실제 Adapter는 SDK 연결과 권한을 확인한 뒤 `requestNavigationScreen(NavigationScreenRequest.Foreground)`를 호출한다.
- 실제 빌드 Manifest 및 권한 확인 목록에 `pleos.car.permission.NAVI_CUSTOM_MAP`을 추가했다.
- 지도 열기는 **Pleos Navigation 화면 전환 요청**이다. 추천 페이지 내부에 지도를 삽입한 구현은 아니다.
- 지도 열기로 현재 목적지·경로·추천 단계를 바꾸지 않는다. 요청 전달을 화면 표시 완료로 취급하지 않는다.
- Mock 빌드에서는 사용 환경 안내와 비활성 버튼을 제공한다. 외부 지도로 대체하거나 실제 화면 표시 성공을 시뮬레이션하지 않는다.
- 지도 전환으로 앱이 백그라운드에 들어가면 기존 수명주기 정책대로 SDK를 정리하고 복귀 시 재연결한다. 실제 동작은 대상 환경에서 확인해야 한다.

## 검증 결과

아래는 지도 변경 후 실행한 결과다. 실제 Pleos 차량 통합 테스트와 구분한다.

| 환경 / 항목 | 결과 |
| --- | --- |
| 기본 Mock Debug APK | 빌드 성공 |
| 기본 Mock 단위 테스트 | 28개 통과 |
| 기본 Mock Lint | 오류 0개, 기존 경고 유지 |
| `-PpleosSdk=true` Debug APK | 공식 SDK를 포함해 빌드 성공 |
| `-PpleosSdk=true` 공통 단위 테스트 | 28개 통과 |
| `-PpleosSdk=true` Lint | 오류 0개, 기존 경고 유지 |
| 일반 Android 35 에뮬레이터 UI 테스트 | 6개 통과 |
| Pleos Navigation 지도 표시 / 복귀 | 미검증 |
| Pleos 실제 경로·위치·도착·차량 UX 신호 | 미검증 |

UI 검증에는 Mock 환경의 지도 안내·버튼 차단, 후보 변경 및 Activity 재생성, 여행 입력 복원,
장소 선택/건너뛰기/완료, 목적지 승인 취소, UX 제한 시 차단/복원이 포함된다.
단위 검증에는 지도 요청의 UX 차단, Mock에서 성공을 가장하지 않는 처리, 지도 요청 실패 시 추천/경로 유지도 포함된다.

### 재현 명령

```bash
# 기본 Mock 빌드·단위 테스트·Lint
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

# 일반 에뮬레이터 UI 테스트 (연결된 기기 ID에 맞게 지정)
ANDROID_SERIAL=emulator-5554 bash gradlew :app:connectedDebugAndroidTest

# 실제 Pleos SDK 빌드·공통 단위 테스트·Lint
bash gradlew -PpleosSdk=true :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

두 빌드 옵션은 같은 applicationId와 APK 출력 경로를 사용한다. 설치 전 빌드 옵션을 확인한다.

## 다음 작업

1. Pleos Connect 에뮬레이터 또는 차량을 준비하고 SDK 사용 가능 상태·권한을 확인한다.
2. 실제 빌드에서 지도 열기, 권한 거부/철회, 서비스 미연결, 화면 전환 후 복귀 및 재연결을 검증한다.
3. Pleos의 장소 검색·가게 상세 정보 제공 범위와 목적지에 필요한 POI 식별 정보를 확인한다.
4. 확인된 실제 POI로 목적지 승인 → 경로 요청 → 안내 상태 → 도착 신호 흐름을 검증한다.
5. 이후 TTS/STT 및 Vehicle 연동을 진행한다. 현재는 조사만 완료한 상태다.

## 기술 기준 및 확인 사항

- 단일 app, Clean Architecture + MVVM + Hilt 유지.
- compileSdk 36.1 / targetSdk 36 / minSdk 28 / AGP 9.1.1 / Kotlin Compose 2.2.10 / Gradle 9.3.1 유지.
- 실제 연동 SDK: NaviHelper 3.0.5, FusedLocation 1.1.3. 공개 AAR 시그니처와 실제 Adapter 컴파일을 확인했다.
- FusedLocation은 이전 가이드의 1.1.2 대신 공식 샘플 및 다운로드 가능한 AAR로 확인한 1.1.3을 사용한다. 상세 근거는 SDK 조사 문서에 기록했다.
- AGP 9 내장 Kotlin에 맞춰 조건부 소스를 `android.sourceSets.main.kotlin.directories`에 등록한다.
- NaviHelper `RouteInfo`의 POI ID/sub-ID는 non-null이다. 임의 식별자를 생성하지 않는다.
- NaviHelper 대체 위치는 측정 timestamp가 없어 STALE로 표시하고 근접 판정에서 제외한다.

## 반드시 유지할 경계

- Mock 장소를 실제 목적지로 전송하지 않는다. 실제 좌표/POI 검증 전까지 실제 빌드에서는 요청을 거부한다.
- 목적지 변경은 사용자가 별도 확인한 경우에만 요청한다.
- `requestRoute()` 반환을 경로 성공으로 취급하지 않고 SDK 이벤트를 기다린다.
- 도착 이벤트는 방문 완료나 자동 다음 단계 확정이 아니다.
- 실제 빌드에서 미지원/권한 거부를 Mock 성공으로 대체하지 않는다.
- 실제 UX 제한이 불명확하면 정차로 간주하지 않는다. DO 선언 없이 플랫폼 제한과 앱의 보수적 차단을 적용한다.
- 위치는 메모리에서만 사용하고 서버 전송·영구 저장하지 않는다.

## Compose Preview (2026-11-10)

- HomeScreen, TripScreen, DateScreen, RecommendationScreen 파일에 Preview 진입점을 추가했다.
- 공통 `FlowTripScreenPreviews`로 가로 1280×720 / 세로 480×960, 밝은/어두운 테마를 제공한다.
- 추천 화면은 로딩·오류·목적지 승인·코스 완료 상태도 별도로 제공한다.
- Preview는 정적 샘플 UiState를 사용하며 Hilt·ViewModel·Pleos SDK·위치 수신을 시작하지 않는다.
- 여행/데이트 입력은 Preview Interactive mode에서 로컬 상태로 수정할 수 있다. 화면 이동·지도·경로 요청 콜백은 실행하지 않는다.
- `gradlew.bat :app:assembleDebug` 성공으로 Preview 코드와 샘플 상태의 컴파일을 확인했다. Android Studio Preview 패널의 실제 렌더링은 IDE에서 확인해야 한다.

## Pleos AVD UX 실행 대응 (2026-11-10)

- 기본 Mock 바인딩이 Automotive에서 UX를 항상 UNKNOWN으로 두던 문제를 수정했다. Mock 장소/내비게이션을 쓰더라도 실제 CarUxRestrictionsManager로 화면 접근을 판정한다.
- CarUxRestrictionsRepository를 공통 main 소스로 옮기고 `android.car` 선택적 공유 라이브러리 선언도 공통 Manifest로 이동했다.
- DO 미선언 정차용 Activity의 접근 판정은 `isRequiresDistractionOptimization`을 사용한다. `activeRestrictions != BASELINE`만으로 추가 차단하지 않는다.
- 연결 끊김/재연결 시 이전 UX 리스너를 정리한다. UNKNOWN 및 DO 필요 상태에서는 계속 차단한다.
- `distractionOptimized=true`를 추가하거나 차량 상태를 임의로 정차로 바꾸지 않았다. AVD가 실제로 조작 허용 UX 상태를 제공해야 한다.
- 근거: [Pleos 주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [AAOS 정차용 앱 UX 지침](https://developer.android.com/training/cars/parked/automotive-os).
- 사용자 요청에 따라 이번 변경의 빌드·테스트·AVD 실행 검증은 수행하지 않았다. 검증은 사용자가 진행한다.

## Pleos 지도 요청 수정 (2026-11-11)

- 코드상 기본 Gradle 설정이 false여서 Android Studio 일반 실행 시 Mock 내비게이션/비활성 지도 버튼이 선택되는 문제를 확인했다. `gradle.properties`에 `pleosSdk=true`를 지정했다. 일반 Android의 Mock 빌드는 `-PpleosSdk=false`로 명시한다.
- 지도 화면 API는 `NAVI_CUSTOM_MAP` 권한을 사용한다. SDK 연결을 `NAVI_ROUTE` 조회 권한으로 선행 차단하던 코드를 제거하고 정보 조회 API만 해당 권한을 검사한다. 실제 권한 거부는 그대로 처리한다.
- 지도 열기 시 서비스 연결을 기다리고, 실패/중단된 SDK 연결은 다시 생성한다. 연결 실패 상태에서도 재시도 버튼을 사용할 수 있다. 대기 시간은 25초다.
- 서비스 연결 대기 이후에도 UX 제한 및 지도 표시 권한을 다시 확인한다.
- `requestNavigationScreen(Foreground)`와 `requestNavigationScreen(MoveToCurrentLocationOnMap)`으로 내비게이션 전경 및 현재 위치 지도 화면을 요청한다. 두 타입과 `NAVI_CUSTOM_MAP` 어노테이션은 설치된 공식 3.0.5/3.0.4 SDK의 공개 시그니처로 확인했다.
- 비동기 SDK 오류는 지도 카드에도 표시한다. 요청 전달을 실제 화면 표시 완료로 취급하지 않는다. 목적지 변경이나 가상 장소 경로 전송은 하지 않는다.
- 이 기능은 Pleos 내비게이션 앱으로 전환하는 방식이며 FlowTrip Compose 내부에 내장 지도를 렌더링하는 방식은 아니다.
- Android Studio에서는 Gradle Sync 후 새 APK를 설치해야 한다. 이전 Mock APK를 실행해 놓은 채로 AVD만 교체하면 SDK 모드가 바뀌지 않는다.
- 공식 근거: [NaviHelper 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions), [NavigationScreenRequest](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/navigation-screen-request/entry).
- 앞선 사용자 요청에 따라 이번 수정의 빌드·테스트·AVD 실행 검증은 수행하지 않았다. 기존 검증 기록은 변경 전 기준이다. 실제 지도 표시 및 서비스 권한/지원 여부는 사용자가 확인해야 한다.

## 지도 요구사항 정정: FlowTrip 내부 지도 (2026-11-11)

- 사용자가 원하는 것은 외부 Pleos 내비게이션 화면 전환이 아니라 FlowTrip 내부 지도, 현재 위치/목적지 1/목적지 2 마커 및 구간별 자동차 도로 경로 표시다. 앞선 Foreground 화면 요청 구현은 이 요구를 충족하지 않는다.
- 공식 NaviHelper 3.0.5 공개 메서드 및 navi-data 3.0.4 AAR를 다시 확인했다. 내장 MapView나 전체 도로 polyline API는 확인하지 못했다.
- `TripSummary.routePoints`의 `RoutePointType`은 ORIGIN/WAYPOINT/DESTINATION이다. 이를 도로 경로 형상으로 간주해 직선 연결하는 구현은 하지 않는다.
- 목표 구조: Pleos Location → 현재 위치 / 별도 지도·경로 API → 구간별 도로 형상 / FlowTrip Compose 화면 → 지도·마커·경로 표시.
- SDK 선택과 무관한 `CourseStop`, `CourseLeg`, `CourseRoute` 및 읽기 전용 `CourseRouteRepository` 계약을 추가했다. 차량 목적지 변경 기능과 지도 경로 미리보기를 분리한다.
- 지도 공급자는 사용자에게 카카오맵+카카오모빌리티 / TMAP 지도+경로 API 중 선택을 요청한 상태다. 앱 키가 필요하며 소스 하드코딩 없이 로컬 설정으로 받을 계획이다.
- 공급자 선택 전에는 임의의 외부 서비스 연결이나 가짜 도로 경로를 추가하지 않았다. 실제 지도 UI 및 구현체 연결은 아직 미완료다.
- 빌드·테스트·실행 검증은 사용자 요청대로 수행하지 않았다.

## 2026-11-11 분할 화면 요청 적용 시작
- 최신 요청 우선: 앱 내부 지도/외부 지도 공급자 검토는 종료하고 Pleos 기본 내비게이션 + FlowTrip 추천 패널로 변경한다.
- 분석: Compose/Hilt/MVVM, 홈/여행/데이트/3단계 추천 재사용. 현재 Mock 장소 2개/단계, 별도 목적지 승인 후 즉시 단계 진행하는 동작을 변경해야 한다.
- 공식 앱 구조 문서에서 최대 2개 앱 window, 기본 방향 미지정 시 한 window, 사용자 window bar/제스처 배치 확인. 앱에서 강제 배치하지 않는다.
- NaviHelper 3.0.5 AAR의 requestRouteWithWaypoints(RouteWithWaypointsRequest(RouteInfo,List<RequestWaypointInfo>)) 및 콜백 확인 중.
- 진행 예정: 3개 후보, 성공 후 후보 확정, 다음 코스 분리, 상태 복원, 좁은 추천 패널, 경유지 요청, 단위 테스트/두 빌드.
- 아직 미검증: 실제 Pleos 두 앱 동시 표시, 경로 요청 시 포커스/창 유지, 실제 POI 공급자(현재 추천은 시연 fixture), 차량 심사상 주행 중 사용 허용.

### 분할 화면 구현 중간 기록
- 추천 지도 카드와 미사용 내장 지도 경로 계약 제거. 홈/여행/데이트 설정, 테마, 프리뷰, 선택·건너뛰기 기록은 재사용.
- 3후보/단계, 성공 후 후보/단계 확정, 다음 추천 경로/다음 코스 분리, 실패 재시도 대상 저장, 경유 코스 요청 구현.
- SavedStateHandle에 9개 장소의 Serializable 스냅샷, 단계/후보/기록/안내 방식/마지막 확인 목적지/이전 경로 상태/중단 요청 대상 저장. 재생성 시 자동 재요청 금지.
- NaviHelper 3.0.5 실제 AAR 서명에 맞춰 경유지 요청 및 경유지/목적지/안내 상태 3가지 결과 대조. 후보 변경에서는 requestNavigationScreen 호출하지 않음.
- resizeableActivity 명시, 화면 방향 고정 없음. 설정 변경 시 SDK 연결 유지, 실제 백그라운드에서는 해제 후 복귀 시 재조회.
- 단위 테스트와 UI 테스트를 새 동작에 맞춰 갱신 중. 아직 빌드/실행 결과 확정 전.

### 분할 화면 검증 기록
- Mock :app:testDebugUnitTest / assembleDebug / assembleDebugAndroidTest 통과. 단위 테스트 33개, 실패 0개.
- 실제 SDK assembleDebug / lintDebug 최초 통과. Lint 오류 0, 경고 23(버전 안내/기존 리소스 등). 최종 콜백 매개변수명/재탐색 조회 보완 후 재검사 중.
- 일반 Pixel_4_2 Android 36.1 읽기 전용 AVD가 초기화 중 종료되어 UI 계측은 실행하지 못함. UI 테스트 코드와 APK 빌드는 통과함.
- 작업 중 Pleos_Connect_v2.1.0 AVD가 연결됨. automotive feature와 Navi Service/기본 내비게이션 패키지만 읽기 전용 확인. 변경 APK 설치 및 차량 경로 전송은 하지 않음.
- 최신 상세 보고: docs/SPLIT_SCREEN_INTEGRATION.md. 이전 내장 지도 공급자 결정 대기 항목은 최신 사용자 요청으로 폐기됨.
- 미완료: 실제 장소/POI 공급자, Pleos 두 앱 분할 및 경로 변경 시 창 유지, 도착/직접 목적지 변경의 실제 콜백, UX 카테고리/주행 허용 심사. 완료 기준 3/6은 미검증으로 유지.
- 최종 검토 보완: 일부 단계만 반환된 추천 응답이 캐시에 남아 재시도가 영구 실패하는 문제를 수정. 부분 응답→정상 응답 복구를 기존 단위 테스트에 포함함.

### 최종 기능 상태
- 홈/여행/데이트 설정, Hilt/MVVM 및 기존 3단계 흐름 보존. 자체 지도 카드/미사용 내장 경로 계약 제거.
- 단계별 3후보, 다음 추천 경로/다음 코스 분리, 실제 장소 순서를 표시하는 남은 코스 경유 요청, 중복 방지, 결과 확인 후 확정, 실패 대상 재시도, 도착/외부 변경 상태 처리 구현.
- 구성 변경 연결 유지와 SavedStateHandle 복원 구현. 프로세스 복원 시 경로 자동 전송 없음.
- 마지막 추천 부분 응답 복구 수정까지 포함한 Mock 단위 테스트 33개(실패/오류 0), 앱 APK 및 UI 테스트 APK 빌드 최종 통과.
- 실제 Pleos 검증 필요 항목과 실제 장소 데이터 미연결 제약은 docs/SPLIT_SCREEN_INTEGRATION.md에 상세 기록. 주행 중 추천 조작 허용 및 분할 창 유지 완료로 간주하지 않음.
- 실제 Pleos SDK 최종 assembleDebug / lintDebug 재검증 통과 (BUILD SUCCESSFUL). Lint 오류 0개, 경고 23개. 신규 라이브러리/SDK 버전 변경 없음.
- 최종 APK는 Pleos SDK 포함 모드로 생성됨. 실제 AVD 설치·분할/연속 안내 실행은 미검증으로 남김.

## 2026-11-11 실제 목적지 데이터 테스트 추가
- 사용자 요청: 실제 데이터 몇 곳으로 Pleos 길안내까지 확인하고 싶음.
- 공식 pleos-sample-navi-app/NaviTestData.kt에서 42dot / 소프트웨어드림센터의 좌표·POI ID·sub ID·주소 확인. 문서의 현대백화점 명칭/주소와 상충해 공식 샘플 코드의 소프트웨어드림센터를 채택.
- 추천 데이터와 별도인 NavigationTestCatalog + Hilt 바인딩 추가. 실제 장소 테스트 패널에서 단일 목적지 두 곳 및 소프트웨어드림센터 경유→42dot 요청 가능하도록 구현 중.
- 추천 후보/단계/선택 기록은 변경하지 않음. 기존 UX·권한·SDK 결과 확인·중복 방지 정책 유지. 실패/중단 테스트 요청 대상 저장 후 명시적 재시도.
- 실제 장소를 식당/카페로 위장하지 않으며 현재 영업 여부·주차·비용은 꾸며 넣지 않음. 실제 설치 내비게이션의 POI 유효성 및 안내 결과는 런타임에서 확인해야 함.
- AVD 실제 시도: UX 허용/SDK READY/권한 검사 통과, 소프트웨어드림센터 requestRoute 전달. 25초 내 목적지·안내 결과 미수신으로 스모크 테스트 실패(성공으로 간주하지 않음).
- 기본 내비게이션을 정상 Launcher Intent로 표시해 소프트웨어드림센터 목적지 및 활성 안내/TBT 화면을 확인함. 지도 배경 타일은 비어 있음. 요청 전달과 앱 결과 동기화 사이 차이를 조사 중.
- Debug 한정 FlowTrip.Navi 이벤트 로그를 추가하여 SDK 연결·요청·각 콜백 수신 여부를 확인할 예정. 화면 결과만으로 SDK 성공 조건을 완화하지 않음.
- SDK 공식 릴리즈 노트와 AAR 확인: 3.0.5는 onRoutingCompleted를 새로 추가하면서 기존 onRouteEnded를 deprecated 상태로 유지한다. 2.1 AVD 호환을 위해 공개 onRouteEnded도 완료 신호로 처리하고 목적지/경유지/안내 모드 재확인 조건은 동일하게 유지하도록 보완.
- 실제 AVD에서 소프트웨어드림센터 안내/TBT 화면 확인. 현재 지도 타일 배경은 비어 있으며 이를 앱 지도 렌더링 문제로 취급하지 않는다.
- 재시험: 기존 테스트 APK 재설치가 AVD 검증 단계에서 2분 이상 지연되어 해당 설치 클라이언트만 종료함. 자동 후속 테스트 실행은 취소되었고, 앱 업데이트 자체는 성공했다.
- 이미 설치된 테스트로 다시 요청하니 onRouteCancelled가 수신됨. 목적지 교체 중 기존 안내 취소를 즉시 실패 처리하던 로직을 수정: 새 완료 이벤트와 목적지/경유지/안내 상태 대조를 기다린다. 실제 취소 후 새 완료가 없으면 시간 초과/오류로 실패 처리하며 성공을 가정하지 않는다.
- RouteConfirmation의 취소→새 완료/새 스냅샷 검증 회귀 테스트 추가. 최종 SDK 단위 테스트/앱/계측 APK/Lint 진행 중.

## 2026-11-11 진행 재개
- 사용자 요청으로 진행도 확인 후 미완료 실제 길안내 검증을 재개함.
- 현재 소스/산출물: 단위 테스트 37개 실패 0, 실제 SDK 앱/계측 APK 빌드 및 Lint 오류 0 확인. 이전 기록의 36개/진행 중 표기는 갱신 필요.
- 시작 시 Android Studio/AVD가 종료되어 연결된 기기 없음. 직접 CLI 읽기 전용 실행은 host GPU와 ASCII 경로+SwiftShader/Vulkan 비활성화 모두 초기화 중 exit 1. 사용자 AVD 설정·이미지·보안 정책은 변경하지 않음.
- computer-use 스킬의 지원 API로 Android Studio를 열어 Device Manager에서 기존 Pleos Connect v2.1.0을 정상 실행하는 경로로 재시도 중.
- 재개 확인 결과: 단위 테스트 37개 실패 0, Pleos 앱/계측 APK 빌드 완료, Lint 오류 0. 소스 변경 후 검증이 끝났던 결과를 진행 기록에 반영함.
- Android Studio Device Manager를 통한 기존 Pleos AVD 기동 성공. 부팅 완료(sys.boot_completed=1), USB 디버깅 허용 화면 확인. 보안 권한 버튼은 computer-use 필수 지침상 대신 누르지 않고 사용자에게 직접 허용 요청함.
- 최신 APK의 최종 안내 검증은 사용자 USB 디버깅 확인 뒤 진행할 항목으로 유지. 초기 요청의 실제 내비 목적지/TBT 화면 확인과 SDK 결과 기반 성공 확인은 구분함.

## 2026-11-12 USB 디버깅 허용 후 검증 재개
- 사용자 직접 USB 디버깅 허용 완료 응답을 받음. 실제 AVD/SDK 검증을 재개함.
- Android Studio의 Pleos Connect v2.1.0 프로세스와 부팅 애니메이션 확인. 현재 ADB 디바이스 목록은 비어 있어 부팅/재연결을 확인 중.
- 최신 Pleos APK는 34,690,640 bytes이며 교체 취소 처리와 실제 테스트 목적지 2곳을 포함함. 단위 테스트 37개/Lint 오류 0의 기존 검증 결과는 유지.
- 2026-11-12: 연결/부팅/사용자10 확인 후 최신 앱 및 계측 APK 모두 --no-streaming 업데이트 설치 성공. NAVI_ROUTE/NAVI_ROUTE_SEARCH/NAVI_CUSTOM_MAP 허용 확인.
- 최신 실제 스모크 테스트에서 42dot 목적지 정보가 SDK로 수신되었으나, 직후 상태 대조가 즉시 REJECTED되어 실패함. 목적지·경로·경유지 조회는 비동기이고 요청 ID가 없어 과거/전환 중 응답을 즉시 영구 실패로 보는 로직을 수정.
- 성공 조건(완료 신호 + 목적지/경유지 일치 + 안내 모드)은 유지. 일시 불일치는 대기하고 완료 신호 이후에만 공개 조회 API를 1초 간격으로 재조회하며 25초 제한 내 확인. 경로 요청 자체를 반복 전송하지 않음.
- 완료 뒤 과거 스냅샷→올바른 스냅샷 전환 회귀 테스트 추가(예정 총 38개), 스모크 테스트는 실제 PLEOS source를 검사하고 기존 목적지와 다른 곳부터 시험하도록 보완. 빌드/검증 진행 중.
- 2026-11-12 비동기 상태 대조 수정 검증: 단위 테스트 38개 실패 0, Pleos assembleDebug/assembleDebugAndroidTest/lintDebug 모두 BUILD SUCCESSFUL. 최신 APK AVD 업데이트/실제 재시험 진행 중.
- 최신 앱 APK의 기기/로컬 SHA-256 일치 확인: e87555282c7577b0e6f9888059f0ad8428da6cb9cd867783de72780c0215c12e. 설치 클라이언트 시간 초과와 별개로 최신 앱은 설치 완료됨.
- 최신 계측 APK 해시는 불일치하여 새 계측 테스트를 실행하지 않음. 실제 UI 버튼 검증으로 전환하려 했으나 사용자의 물리 Escape 입력으로 Computer Use가 중지됨.
- 실제 화면 검증을 즉시 중단하고 이번 작업에서 시작한 SDK 로그 수집 프로세스만 정리함. 최종 SDK 안내 성공/연속 목적지 변경은 미완료로 유지.

## 2026-11-13 분할 UX 불일치 수정
- 사용자 보고: 내비게이션과 FlowTrip이 동시에 보이지 않고 내비 화면으로 이동한 뒤 FlowTrip에 복귀해야 함.
- 분석: 리사이즈 허용은 두 앱 배치 구현이 아님. 일반/실제 장소 테스트 화면에 남아 있던 화면 요청 버튼이 requestNavigationScreen(Foreground)을 호출하여 앱 전환을 유도함. 경로 버튼 자체는 기존부터 이 API를 호출하지 않음.
- 공식 앱 구조/Window Policy 및 NaviHelper 3.0.5 AAR 대조: portrait는 한 앱 window, 배치는 시스템 창 바/창 메뉴가 담당. 공개 NaviHelper에 자동 두 앱 배치 API는 확인되지 않음.
- 수정: Pleos 전용 manifest overlay에 portrait 초기 창 요청 명시(일반 Mock/폰 설정은 그대로). 추천/테스트 화면의 Foreground 버튼 및 UI 콜백 제거. 홈/추천에 다른 앱을 열지 않는 시스템 분할 구성 안내 추가.
- 목적지/후보/코스 요청과 추천 상태 관리는 유지. SDK foreground 요청 계약은 진단용으로 분리되어 남아 있으나 일반 추천 UI에서는 호출 경로 없음.
- 예정 검증: Mock 단위 테스트/계측 APK 컴파일, 실제 SDK 빌드/Lint 및 manifest 병합 확인. AVD의 실제 두 앱 동시 표시/목적지 변경 시 창 유지 검증은 아직 완료 전.
- 2026-11-13: 수정 전 AVD는 FlowTrip/Navi 모두 같은 앱 영역 Rect(853,90-2535,1300), FlowTrip만 visible=true/Navi=false로 확인됨. Android multi-window 플래그만으로 두 앱 동시 표시를 판단할 수 없음을 실증함.
- Pleos/Mock 빌드와 Mock 단위 테스트 38개 및 UI 테스트 컴파일 통과, Pleos Lint 오류 없음. portrait 선호 설정 병합 확인 및 수정 APK 기기 해시 일치.
- portrait 실행 후 표시 플래그 일부는 바뀌었지만 앱 UI가 실제 표시되지 않고 ANR 발생. 이를 분할 성공으로 간주하지 않음.
- 공개 ApplicationExitInfo API로 우리 앱의 ANR 메인 스택을 조회하는 opt-in 진단 추가. 실제 정지 위치는 android.os.ServiceManager.checkService → ClipboardManager 생성 → AndroidComposeView → MainActivity.setContent. NaviHelper 경로 API/지도 UI를 원인으로 단정하지 않음.
- AVD 시스템의 clipboard 서비스 조회 정지를 우회하지 않고 일반 재부팅 후 재검증 예정. 데이터 wipe, 시스템 서비스 대체, adb root/강제 task 배치는 사용하지 않음.
- AVD 정상 재부팅 완료, clipboard 서비스 존재 확인 후 재실행했으나 초기 draw/창 전환 안정성 검증이 아직 완료되지 않음.
- 검토했던 고정 portrait 선언은 두 앱 배치를 보장하지 못하고 방향/크기 대응을 제한할 수 있어 최종 코드에서 제거함. 일반/실제 Activity 방향 고정 없음 유지. 미검증 설정을 자동 분할 해결책으로 남기지 않음.
- 최종 방향: 시스템 창 메뉴로 최초 두 앱 배치를 구성하고, 앱은 화면 전환 버튼 없이 목적지만 요청. 세로 창 한 열/넓은 가로 창 두 열 적응 및 앱 내부 분할 구성 안내 유지.

- 최종 방향 고정 제거 후 Pleos assembleDebug/lintDebug 재확인 BUILD SUCCESSFUL. 최종 APK 로컬/AVD 설치 SHA-256 일치 확인. 정상 런처 실행 후 실제 홈 UI 표시 확인(가로 전체 앱 영역).
- 시스템 앱 목록과 창 바가 표시되는 것은 확인했으나 두 앱 동시 분할 및 목적지 변경 중 창 유지 검증은 아직 완료하지 않음. Android multi-window 플래그만으로 완료 판단하지 않음.

- 실제 분할 POC 성공: Pleos AVD 2.1.0에서 FlowTrip 오른쪽 점 3개 창 바를 왼쪽으로 드래그. FlowTrip Rect(853,90-1694,1300), OEM Navi Rect(1718,90-2559,1300)로 겹치지 않는 두 앱 창 확인. 둘 다 reportedDrawn/reportedVisible/allDrawn=true, 실제 화면에 홈과 내비 동시 표시 확인. 별도 강제 배치 API/adb task resize 사용 없음.

- 분할 창에서 실제 공식 목적지 42dot 요청: OEM 내비 42dot TBT/안내 종료 화면과 FlowTrip 동시 표시 유지. 단 최초 요청은 앱의 목적지 일치 확인 제한 시간 초과로 성공 확정하지 않음.
- 이어 소프트웨어드림센터 요청: 동일 두 창에서 OEM 내비 목적지 변경 및 FlowTrip의 “안내 상태를 SDK에서 확인했습니다” 성공 메시지 확인. 화면 전환/FlowTrip 복귀/Foreground 요청 없이 진행됨. 지도 타일 배경은 AVD에서 계속 빈 상태로 관찰됨.

- 소프트웨어드림센터 → 42dot 재변경도 OEM TBT 목적지 변경 및 분할 창 유지 확인. 42dot의 SDK 목적지 일치 판정은 대기하여 완전 성공으로 처리하지 않음. 3요청 모두 불필요한 Foreground/앱 복귀 없이 진행.
- 실제 확인한 점 3개 창 바 드래그 조작을 인앱 안내에 명시하고 Pleos assembleDebug/lintDebug 다시 통과. 최종 문구 반영 APK 업데이트 중.

- 최종 안내문 반영 APK도 AVD 설치 SHA-256 일치 확인. 정상 런처 재실행 완료. 최종 검증: SDK assembleDebug/lintDebug 성공, Mock 단위 38개 통과, UI 테스트 컴파일 통과, diff --check 통과.

- 최종 문구 반영 APK 업데이트로 시스템 앱 배치가 다시 전체 내비 창으로 바뀜. 이후 정상 실행 재시도는 am start -W에서 timeout(17.4초)로 최종 재시작 UI/재배치를 확정하지 못함. 업데이트 전 실제 두 창/연속 목적지 변경 POC 결과와 구분한다. 최초 배치 및 패키지 업데이트 후 재배치는 시스템 창 바에서 필요할 수 있음. AVD 초기화/재시작 안정성은 남은 검증 항목이며 자동 두 앱 배치 완료라고 보고하지 않음.

## 자동 분할 요청 작업 인계 원문 (아래 구현 기록 참고)

- 사용자 요청(2026-11-13): 점 3개 창 바를 직접 드래그하는 방식 대신, **‘이 장소로 길안내’ 버튼을 누르면 FlowTrip과 Pleos 기본 내비게이션이 자동으로 나란히 표시되고 경로 안내가 시작되는 UX**로 수정한다. 이번 턴은 기록만 하며 실제 수정은 다음 요청에서 진행한다.
- 정정: NaviHelper에 전용 분할 API가 확인되지 않았다는 이유만으로 자동 분할이 불가능하거나 수동 드래그가 필수라고 단정하지 않는다. Android 공개 API `FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK`를 이용한 인접 창 실행 가능성을 우선 검증한다. 현재 이 방식은 **미구현·Pleos 실기 미검증**이다.
- 검토 근거: https://developer.android.com/develop/ui/views/layout/support-multi-window-mode — Android 12L(API 32) 이상에서 전체 화면 앱의 인접 창 실행/분할 진입 요청을 지원한다. Pleos의 자체 창 정책이 이를 반영하는지는 별도 확인이 필요하다: https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window
- 다음 구현 순서: (1) 현재 코드/SDK와 내비게이션의 공개 실행 진입점을 확인한다. (2) FlowTrip 버튼 클릭에서 공개 Intent 플래그로 내비 인접 창 실행을 요청하는 POC를 만든다. (3) NaviHelper로 선택 목적지를 요청한다. (4) 이미 두 창이 구성된 뒤 다음 추천/다음 코스 요청에서는 내비 재실행 없이 목적지만 변경한다.
- 최초 실행 및 기존 내비 실행 중인 경우 모두 확인한다. 실제 화면에 두 앱이 동시에 그려지고 창 영역이 겹치지 않는지, 버튼 클릭 한 번으로 경로 요청이 되는지, 연속 목적지 변경 중 두 창이 유지되는지 검증한다. `isInMultiWindowMode`만으로 성공 처리하지 않는다.
- 기존 추천·데이트·코스 로직과 중복 요청 방지, SDK 결과 확인 후 후보 확정, 실패/재시도 처리는 유지한다. 분할 실행은 화면 코드와 SDK 경로 요청 로직의 결합을 최소화해 분리한다.
- 공개 인접 창 실행 요청은 강제 창 위치/비율 조정과 구분한다. 숨겨진 API, 시스템 권한 우회, adb 강제 task 배치, 오버레이, 임의의 미지원 SDK API는 사용하지 않는다. Pleos에서 요청이 무시되거나 기존 앱을 덮으면 자동 분할 성공으로 보고하지 않고 실제 제약을 기록한다.
- 자동 분할 검증 전에는 현재 수동 구성 안내를 자동 분할 완료로 바꾸지 않는다. 성공 검증 후 안내/UI를 새 버튼 흐름에 맞춰 수정한다.
- 별도 남은 문제: 42dot은 OEM 안내 UI가 표시돼도 SDK 목적지 대조 시간 초과가 있었음. 소프트웨어드림센터는 SDK 안내 성공 확인. OEM 지도 타일 빈 배경 및 최종 APK 업데이트 후 앱 재실행 시간 초과는 별도 확인 필요.

## 자동 분할 요청 코드 구현 — 사용자 검증 대기

- 최신 사용자 지시: 기록한 자동 분할 UI를 구현하되 검증/테스트는 사용자가 수행한다. 구현/진행도를 기록하고 확인 항목은 별도 문서로 분리한다.
- 구현 완료: `이 장소로 길안내` → 유효 목적지/현재 UX/SDK 준비 검사 → Activity 컨텍스트에서 내비 공개 런처를 `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT`로 실행 요청 → NaviHelper 경로 요청. 내부 Activity 이름/비공개 SDK API를 가정하지 않았다.
- 명시적 길안내/경로 재시도에서는 창 요청을 다시 보낼 수 있다. 다음 추천/다음 코스/남은 코스/실제 장소 테스트는 Activity 범위 ViewModel에 창 요청 전달 이력이 있으면 내비를 재실행하지 않고 경로만 요청한다. 첫 길안내 동작으로 사용하면 창 요청도 보낸다.
- 분할 요청 이력은 Activity 재생성 동안 유지하되 Activity 참조/실제 두 창 성공 여부는 저장하지 않는다. 프로세스 복원이나 화면 재구성에서 외부 앱/목적지를 자동 실행하지 않는다. 기존 추천/코스 SavedStateHandle 복원, 중복 방지, 실패/재시도 및 SDK 결과 기반 후보 확정은 유지한다.
- 창 실행 실패 시 해당 경로 요청을 중단하고 기존 추천/단계를 유지한다. Mock 후보/잘못된 POI/SDK 미준비/권한/UX 제한은 외부 창 실행 전 차단한다. Mock 빌드에서는 다른 앱을 실행하지 않는다.
- 기본 내비 패키지는 기존 AVD에서 관찰한 `ai.umos.maps.android.navigation.app`. `pleosNavigationPackage` Gradle 속성으로 조정 가능하며 BuildConfig와 Pleos manifest의 대상 패키지 `<queries>`에 동일 값 적용. 새 라이브러리/SDK 버전/강제 창 배치/오버레이 추가 없음.
- UI는 자동 분할 **요청** 흐름을 설명하며 ‘실제 분할 완료’라고 표시하지 않는다. 창 위치/비율과 플래그 반영 여부는 Pleos 시스템에 종속된다. `isInMultiWindowMode`만으로 두 앱 동시 표시를 판단하지 않는다.
- 구현/전체 파일 목록: [AUTO_SPLIT_IMPLEMENTATION.md](AUTO_SPLIT_IMPLEMENTATION.md).
- **사용자가 확인할 항목 별도 분리:** [AUTO_SPLIT_VERIFICATION.md](AUTO_SPLIT_VERIFICATION.md). 최초 전체 화면에서 버튼 한 번으로 분할되는지, 이미 실행 중인 내비 재사용, 연속 변경, 실패/재시도, 회전/재생성/복귀, Mock/Preview/UX 제한 등을 기록했다.
- **이번 변경의 빌드, Lint, 단위/계측 테스트, APK 설치, AVD/실차 실행은 요청대로 수행하지 않았다. 테스트 코드도 추가/수정하지 않았다. 이전 빌드 통과/수동 분할 POC를 이번 자동 분할 검증으로 보고하지 않는다.**
- 미완료/외부 확인: Pleos의 실제 인접 실행 플래그 반영 여부, 기존 내비 task 재사용 시 창 배치 및 안내 유지, 42dot 목적지 대조 시간 초과, 지도 타일 빈 배경, AVD 시작 ANR, 일반 추천 데이터의 실제 공급자 연동. 현재 일반 추천은 Mock이므로 실제 자동 분할/경로 시험은 공식 샘플 테스트 패널 또는 실제 장소 공급자가 필요하다.

## 자동 분할 실패 재확인 — 미해결

- 사용자 ‘자동으로 화면 분할 안 됨’ 보고 후 설치된 앱의 실제 화면과 adb 읽기 조회로 확인함. 빌드/재설치는 하지 않음.
- 최초 화면은 여행 모드 ‘골목의 작은 식탁’ Mock 후보였고 실제 목적지 전송 불가 오류가 표시됨. 이 경우 source 검사에서 차단되어 분할 요청 콜백 자체가 실행되지 않음.
- 실제 장소 패널에서 소프트웨어드림센터 길안내를 요청해 별도 재현. 내비 Intent의 `flg=0x10001000`(NEW_TASK|LAUNCH_ADJACENT) 전달 확인. 그 뒤 OEM Navi와 FlowTrip 모두 Rect(853,90-2535,1300), Navi visible=true / FlowTrip visible=false. 실제 화면은 전체 내비 실행 화면으로 전환됨. 따라서 현재 방식의 자동 분할 실패를 확인했고 완료 처리하지 않음.
- 새 내비 task t1000067도 관찰되어 ‘기존 task를 반드시 재사용한다’는 보장도 할 수 없음. 추가 MULTIPLE_TASK/비공개 API/강제 창 배치로 우회하지 않음.
- 소스 보완: Pleos Mock 추천 카드에 시연/실제 안내 불가를 표시하고 해당 길안내 버튼 비활성화. 기존 실제 장소 패널은 Pleos에서 기본 펼침. 패널과 창 안내의 잘못된 설명 수정.
- 소스 보완: Activity 실제 숨김(onStop, 설정 변경 제외)을 NavigationWindowSession에 기록. 숨김 후 후속 요청은 과거 전달 이력만으로 창 준비 성공을 반환하지 않음. 명시적 버튼으로 재요청은 가능. 이는 실패 처리 보완이지 자동 분할 해결이 아님.
- **이 소스 보완의 빌드/테스트/설치는 실행하지 않음. 설치돼 있던 버전의 재현 결과와 구분한다.**
- 상세 기록: [AUTO_SPLIT_FAILURE_ANALYSIS.md](AUTO_SPLIT_FAILURE_ANALYSIS.md). 필요한 다음 단계는 Pleos의 공식 자동 두 앱 배치 진입점/지원 여부 확인 및 실제 추천 POI 공급자 연결이다. 공개 플래그만으로 기능이 완성됐다고 보고하지 않는다.


## 2차 실험 기록 복구 — 현재 제거됨

이전 저장 과정에서 이 구간의 한글 인코딩이 깨져 내용을 복구한다. 2차 실험은 자신의 runtime portrait 요청과 WindowMetrics 크기 변화 확인 후 SDK 경로를 요청하는 방식이었다. 창 준비 콜백을 suspend로 바꾸고 Activity 재생성을 넘는 세션/가시 상태/8초 대기를 구현했으며 내비 런처 의존성은 제거했다. 다만 두 앱 실제 동시 표시/반반 배치는 미검증이었고 아래 최신 결정에 따라 코드에서 제거했다.

## 자동 반반 배치 실험 종료 및 화면 정리

- 사용자 지시: 코드로 자동 반반 배치가 지원되면 수정하고, 그렇지 않다면 불필요하게 늘어난 화면을 정리한다.
- 현재 공개 NaviHelper 3.0.5/일반 앱 권한에서 두 앱 1:1 배치를 지정할 공식 진입점을 확인하지 못함. 인접 실행은 AVD 실기 실패, runtime portrait도 두 앱 배치/비율을 보장하지 못하므로 자동 배치는 미완료로 남기고 화면 정리 분기로 진행.
- NavigationWindowController/Session 및 PleosWindowGuide 삭제. Activity의 관측/명령/방향 요청, NavHost/VM/UseCase의 창 준비 콜백/8초 대기 제거. 경로는 SDK만 요청하며 자동 창 크기/다른 앱 실행을 시도하지 않음. SDK 자체 화면 동작은 플랫폼에 종속됨.
- 홈에서 분할 안내 제거. 추천은 코스/후보 번호/장소 카드 하나/주요 버튼 중심의 한 열로 정리. 반복 설명과 별도 내비 창 안내 카드 제거.
- 상세/주차/추천 이유/경유 안내는 상세 보기로 접음. 연결/경로/위치 상세는 연결 정보 보기로 접음. 현재 목적지/도착/연결 오류는 기본 표시 유지.
- 실제 목적지 샘플은 하단 기본 접힘으로 이동. 접어도 요청 결과와 오류 재시도는 표시하며 샘플/경유 기능은 유지. 테스트 오류/일반 오류와 추천 불일치 경고의 중복 표시를 줄임.
- 추천/데이트/후보 변경/다음 코스/건너뛰기/경유/실패 재시도, Hilt/Repository/저장 상태/SDK 성공 대조/주행 UX 제한은 유지. 일반 추천 Mock source는 위장하지 않음.
- 기록과 사용자 확인 목록: SCREEN_CLEANUP.md. 자동 분할 문서들은 과거 실험 이력임을 표시. 한글이 깨졌던 실험 문서도 UTF-8로 복구함.
- 기존 사용자 요청대로 이번 수정의 빌드/Lint/테스트/설치/AVD 실행은 수행하지 않음. 화면 반영/컴파일/버튼 동작과 수동 구성 창 유지 확인은 사용자 확인 항목으로 남김.

## 개발용 추천 카드에 실제 테스트 목적지 연결

- 사용자 승인: 개발 단계에서는 가상 식당 자체 주소가 아니라 실제 테스트 목적지로 길안내할 수 있게 연결한다.
- Pleos SDK Debug 빌드의 MockRecommendationRepository가 기존 NavigationTestCatalog를 주입받아 실제 샘플의 Destination 전체 값을 사용하도록 수정. 가상 Place ID/이름/추천/단계 정보는 유지하고 요청 목적지만 교체한다. 시연 좌표/source를 실제인 것처럼 바꾸지 않는다.
- 첫 식당은 소프트웨어드림센터, 다음 후보는 42dot, 셋째는 소프트웨어드림센터. 야경 첫 후보는 42dot, 카페 첫 후보는 소프트웨어드림센터로 단계마다 번갈아 연결한다. 기존 샘플이 두 곳이므로 세 후보 중 반복이 생김.
- 추천 카드에 개발용 길안내 대상 이름을 한 줄로 표시하고 상세 주소도 구분. 별도 테스트 패널 없이 이 장소로 길안내/다음 추천/다음 코스가 실제 샘플을 SDK로 요청한다. 실제 SDK 결과 확인 후 상태 확정/재시도/UX 제한은 유지.
- 기존 저장 후보가 Mock 목적지이면 다시 불러와 새 연결을 적용. 단계/후보 인덱스/방문 ID는 유지하며 복원 시 경로를 자동 전송하지 않음.
- Mock 빌드와 새 Release 추천은 기존 가상 목적지를 유지. 자동 분할/강제 내비 화면 실행/차량 위치 이동/주행 시뮬레이션은 추가하지 않음.
- 기록 및 사용자 확인 목록: [DEVELOPMENT_ROUTING.md](DEVELOPMENT_ROUTING.md). 42dot 대조 시간 초과와 반복 목적지를 포함한 경유 요청은 확인 필요.
- 기존 요청대로 이번 수정의 빌드/테스트/설치/실행은 수행하지 않음.

## MockRecommendationRepository 주입 생성자 오류 수정

- 사용자 빌드 오류: 기본 인자가 있는 @Inject 생성자로 인해 인자 없는 생성자까지 주입 생성자로 인식되어 중복 오류 발생.
- 주 생성자의 NavigationTestCatalog 기본값을 제거해 @Inject 생성자를 하나로 제한. 기존 테스트의 MockRecommendationRepository() 호출을 위해 @Inject 없는 명시적 보조 생성자를 추가함.
- 개발용 실제 목적지 연결 로직은 변경하지 않음. 기존 사용자 요청에 따라 빌드/테스트는 실행하지 않음.

## 1차 실제 장소 추천 확장 — 2026-11-15

- `main/2f116f0`와 `69875e7` 이후 커밋, 현재 소스·문서, 연결된 이전 Codex 2개 작업의 최근 기록을 대조했다. 기존 화면·MVVM·Repository·FusedLocation·SDK 경로 확인·분할 생명주기를 재사용했다.
- 기존 Repository에 `placeDataSource=mock/kakao` 분기를 추가. 기본 Mock 유지, 실제 검색 오류 시 Mock/샘플로 자동 대체하지 않음. 카카오 키는 앱에 넣지 않으며 HTTPS 프록시가 필요하다. 현재 저장소에 서버가 없어 서버 계약과 설정을 문서화했다.
- 실제 장소 ID/provider/주소/카테고리/좌표/전화/URL 매핑. Mock 사진·메뉴·가격과 혼합하지 않는다. 거리·체류·이동 추정, 관심 검색어, 방문 이력 우선순위, 이미 표시한 후보 제외를 적용했다.
- 기존 다음 추천/다음 코스에서 최신 위치와 실제 경과시간으로 재검색. 도착은 방문 완료가 아니며 사용자가 방문 완료 버튼을 눌러야 기록한다. 일 입력은 첫날 8시간 활동만 지원함을 표시한다.
- 외부 좌표 길안내는 공식 지원 확인 전 기본 차단. 명시적인 `pleosCoordinateRouting=true` 시험 분기는 실제 좌표와 빈 POI 문자열을 전달하며 기존 완료/목적지/안내/경유 대조를 유지한다. 카카오 ID를 Pleos POI로 쓰지 않음. 공식 샘플 패널과 경유 기능 유지.
- 기존 Navigation Compose 복원 키 Lint 오류를 수정했다. 이번 요청에 따라 빌드·단위 테스트·Lint·계측 APK 빌드를 수행했다. 최종 명령별 결과와 변경 파일 목록은 아래 보고서를 기준으로 한다.
- 실기기 연결 없음, 프록시/키 없음: 실제 검색 응답, 외부 좌표 길안내, 연속 경로 변경, 분할/복귀 UI 시나리오는 미검증. 자동 두 앱 배치가 해결됐다고 보고하지 않는다.
- 상세 분류·설정·검증·남은 작업: [REAL_PLACE_IMPLEMENTATION.md](REAL_PLACE_IMPLEMENTATION.md).

## REST 키 및 개발용 카카오 프록시 연결 — 2026-11-15

- 전달받은 REST 키를 Git 제외 `secrets.properties`에만 저장. 앱/BuildConfig/버전 관리 소스에는 넣지 않았다.
- `tools/place-proxy`에 Node 기본 모듈만 사용하는 loopback 개발 서버, 독립 테스트, 실제 카카오 호출 검증 명령을 추가했다. 고정 주소/키워드 검색 경로만 허용하고 실패를 Mock으로 대체하지 않는다.
- `place-proxy.local.properties`의 주소가 있는 Debug 빌드만 실제 검색으로 전환한다. Release의 HTTPS 전용 정책은 유지한다. HTTP 예외는 Debug의 로컬/에뮬레이터 주소만 허용한다.
- 실제 카카오 두 API는 모두 403 / NotAuthorizedError / `App(FlowTrip) disabled OPEN_MAP_AND_LOCAL service.` 응답을 확인했다. 카카오맵 사용 설정 ON이 필요하며 검색 성공으로 보고하지 않는다. 앱에도 서비스 비활성화 원인을 구체적으로 표시한다.
- 상세 실행 및 검증: [개발용 프록시](../tools/place-proxy/README.md).

## Pleos → 로컬 프록시 연결 시간 초과 수정

- 서비스 활성화 뒤 PC의 주소/장소 검색은 HTTP 200을 확인했다. 이후 Pleos의 `10.0.2.2:8787` 연결 시간 초과 신고 시 PC 8787 포트에 프록시가 없고 ADB reverse 매핑도 없었다.
- 개발 주소를 `127.0.0.1:8787` + ADB reverse로 바꾸고 `tools/place-proxy/start.ps1`을 추가했다. 상태 확인, 숨김 백그라운드 서버 시작, 기기별 포트 연결, Git 제외 Debug URL 저장을 한 번에 수행한다. PC/AVD 재시작 뒤 다시 실행한다.
- `/health`는 카카오를 호출하지 않고 로컬 프록시 실행 여부만 응답한다. 키/위치는 출력하지 않는다. Debug의 로컬 연결 예외에는 실행 스크립트를 안내하도록 오류 문구를 수정했다.
- 현재 실행 중인 프록시로 PC 검색 HTTP 200/3건 및 `adb reverse` 매핑을 확인했다. 앱에서의 최종 검증은 프록시 안내 문서의 후속 결과를 기준으로 한다.
- 후속 검증: Pleos 에뮬레이터 내부에서 `127.0.0.1:8787`, 기존 `10.0.2.2:8787` 모두 실제 검색 HTTP 200/장소 JSON 확인. 수정 메인 APK 설치 `Success` 확인. 계측 APK 설치 완료 응답 지연으로 계측 실행은 미완료이며 소켓 경로 검증과 구분한다. 빌드/45개 단위 테스트/2개 프록시 테스트/Lint 통과.

## 개발용 프록시 자동 시작

- 사용자 요청에 따라 `preDebugBuild` 전에 `startLocalPlaceProxy` 작업을 연결했다. Git 제외 로컬 설정 `autoStartPlaceProxy=true`로 활성화하며 다른 환경의 기본값은 false이다. Release에는 연결하지 않는다.
- 자동 모드는 Pleos에서 HTTP 200을 확인한 `10.0.2.2:8787` + `start.ps1 -ServerOnly`를 사용해, AVD 부팅/ADB reverse 재연결을 기다리지 않는다. 기존 127.0.0.1/ADB 수동 경로도 유지한다.
- Gradle의 Windows 관리 프로세스가 서버 수명까지 기다리는 문제를 확인해 직접 실행기와 `launch.mjs`로 분리했다. 새 서버 시작, 빌드 종료 뒤 서버 유지, 기존 서버 재사용 및 Debug 빌드 성공을 확인했다. 검증용 포트의 프로세스만 정리했다.
- 현재 Android Studio `app` 실행 구성에는 기존 Gradle BeforeRun 단계가 켜져 있어 일반 Run/Debug 빌드에 적용된다. 최초 Sync가 필요하며, 기기 아이콘 실행이나 빌드를 생략하는 실행은 PC 서버를 새로 시작하지 않는다. 실제 IDE 실행 버튼 자동 조작 검증은 하지 않았다.

## Pleos Activity class does not exist 해결

- 소스/병합 Manifest에는 `com.github.flowtrip.MainActivity`가 정상 등록되어 있었다. 클래스 이름이나 Manifest를 바꾸지 않았다.
- AVD의 현재 사용자 ID는 10인데 본 앱은 user 0에만 설치되어 있었다. user 10에서는 테스트 APK만 조회되고 MainActivity resolve가 `No activity found`였다.
- `cmd package install-existing --user 10 com.github.flowtrip`으로 현재 운전자 계정에 기존 패키지를 등록했다. 앱 삭제/데이터 초기화 없이 처리했다. 이후 user 10에서 본 앱/Activity resolve 및 앱 프로세스 PID를 확인했다.
- `am start -W`는 첫 실행 권한 요청 및 화면 지연으로 timeout을 표시했지만, ActivityManager에서 user 10의 MainActivity와 그 위의 FlowTrip 권한 요청 Activity가 표시된 것을 확인했다. 권한은 사용자가 선택하도록 남겼다.
- 로컬 Android Studio `app` 실행 구성의 `ALL_USERS`를 true로 변경했다. `.idea/workspace.xml`은 기존 Git 제외 파일이다. 이 변경은 앱 코드/Release APK 설정을 바꾸지 않는다.

## 데이트 실제 장소 후보 없음 진단

- 현재 UI에서 5시간 입력과 해외 기본 GPS(37.42200, -122.08400)를 확인했다. API는 국내 장소를 반환했지만 거리 기반 이동시간 계산에서 모든 후보가 제외됐다. 일반 오류 문구가 위치 문제를 숨겼다.
- 에뮬레이터에만 성남 판교 시험 GPS를 설정하고, 기존 5시간 조건에서 실제 식당 후보 3개가 표시되는 것을 확인했다. 실제 차량/앱 코드의 GPS 대체나 Mock fallback은 추가하지 않았다.
- 해외 위치 사전 안내, 검색 반경 검증, 후보 탈락 원인별 오류, 오류 화면의 남은 분 표시를 추가했다. 기존 점수/시간 필터를 우회하지 않는다.
- 빌드/48개 단위 테스트/Lint 통과. 계측 테스트 및 최종 설치 결과와 변경 파일은 [PLACE_SEARCH_DIAGNOSTICS.md](PLACE_SEARCH_DIAGNOSTICS.md)에 기록한다.
