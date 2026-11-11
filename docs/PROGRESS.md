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
