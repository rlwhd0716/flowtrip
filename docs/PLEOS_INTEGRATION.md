# Pleos P0 연동 결과

기준: 2026-11-10. 이번 작업은 P0 Navigation/Location 기반 및 UX 제한 처리다. P1/P2는 [SDK 조사](PLEOS_SDK_RESEARCH.md)만 수행했다.

## 연결 상태

| 구분 | 상태 |
| --- | --- |
| 공식 P0 SDK 다운로드 / 실제 Adapter 컴파일 | 확인 완료 |
| 기본 Mock 모드의 화면 및 상태 흐름 | 단위/계측 테스트 확인 |
| 실제 SDK APK의 일반 Android 실행 | 차량 UX 미지원 안내 및 화면 차단 확인 |
| Pleos 서비스의 경로 탐색·위치·도착·차량 UX 신호 | 미검증: 대상 서비스/이미지 없음 |
| 실제 장소 API / 실제 추천 장소 좌표 | 없음: 기존 가상 추천 유지 |

실제 SDK를 포함해 빌드된다는 사실과 실제 차량 서비스에서 동작한다는 사실은 구분한다.
현재 추천 장소는 가상 데이터이므로 **실제 SDK Adapter는 해당 장소의 목적지 요청을 거부**한다.
실제 장소 공급자를 연결해 Domain Destination에 실제 좌표 및 POI 식별 정보를 전달해야 차량 경로 요청을 사용할 수 있다.
기존 장소 선택/코스 진행은 기본 Mock 모드에서 실행 가능하다.

## 빌드 모드

기존 단일 app 및 SDK/AGP/Kotlin/Gradle 버전을 유지했다. Flavor를 추가하지 않고 Gradle property로 조건부 Kotlin 소스를 등록한다.
`pleosSdk`는 `true`/`false`만 허용하며 기본값은 false다. 앱 UI에서 실시간으로 실제/Mock을 자동 전환하지 않는다.

```powershell
# 일반 개발 / 기존 화면 테스트 (Pleos SDK 의존성 및 사용자 권한 없음)
.\gradlew.bat :app:assembleDebug

# 실제 Pleos Adapter / 공식 SDK 포함
.\gradlew.bat -PpleosSdk=true :app:assembleDebug
```

- 기본: `src/main/java` + `src/mock/java`.
- 실제: `src/main/java` + `src/pleos/java`, debug/release의 Pleos Manifest overlay 및 공식 P0 SDK 의존성.
- 실제 빌드만 자동차 feature와 P0 권한을 선언한다. `android.car` 공유 라이브러리를 선택적으로 선언하고 SDK optional jar를 컴파일에 사용한다.
- 실제 환경 미지원/권한 거부/연결 실패를 Mock 성공으로 바꾸지 않는다.
- 기본 Mock 빌드도 자동차 feature가 있는 기기에서는 조작 허용 상태를 가정하지 않고 UX UNKNOWN으로 차단한다.
- 두 옵션은 같은 applicationId와 기본 APK 출력 경로를 사용한다. 설치/보관 전 어떤 옵션으로 빌드했는지 확인한다.

## 아키텍처

```text
Activity foreground lifecycle
  ├─ NavigationRepository.start/stop
  ├─ LocationRepository.start/stop
  └─ UxRestrictionsRepository.start/stop

Hilt PlatformModule (compile-time selection)
  ├─ mock:  MockNavigation / MockLocation / MockUx
  └─ pleos: PleosNavigation / PleosLocation / CarUxRestrictions

SDK callback → data mapper → Domain StateFlow / events
  ├─ DateViewModel → location status
  └─ RecommendationViewModel
       ├─ pending destination approval
       ├─ RequestNavigationUseCase (approval + UX guard)
       ├─ routing result / failure
       └─ arrivalSignal (never a confirmed visit)
```

Domain은 Android/Pleos/Hilt 타입에 의존하지 않는다. Composable과 ViewModel에는 SDK 인스턴스나 콜백 타입을 노출하지 않는다.
Hilt는 앱 세션의 Repository를 공유하고 UseCase를 제공한다. Activity는 SDK 직접 호출 대신 Repository의 전경 수명주기만 조정한다.
실제 SDK Adapter는 applicationContext를 사용하고 listener 등록·제거·SDK 해제를 수행한다.
이전 세션 콜백은 종료된 scope/인스턴스 확인으로 무시한다. SDK 해제 후 같은 인스턴스를 다시 사용하지 않는다.

## 주요 동작

1. 앱 전경 진입 시 UX 제한 및 Navigation/Location 세션 시작. 차량 UX가 조작을 허용한 이후에만 실제 dangerous 권한을 Android 권한 창으로 요청한다.
2. 추천 화면에서 현재 내비게이션 출처/상태/목적지 및 위치 사용 가능 상태를 확인한다.
3. `이 장소 선택`은 별도 승인 창만 연다. 취소하면 목적지와 추천 단계 모두 바뀌지 않는다.
4. `목적지로 설정`을 눌러야 UseCase가 사용자 승인과 UX 제한을 검사하고 경로를 요청한다.
5. 경로 탐색 중에는 중복 요청·다른 후보·건너뛰기를 막는다. 결과가 실패하면 선택 이력/단계는 유지하고 오류를 표시한다.
6. Mock의 시뮬레이션 성공 또는 실제 Adapter의 검증된 안내 상태를 받은 후에만 선택 이력과 다음 추천 단계로 진행한다.
7. 목적지/경유지 도착 신호는 `arrivalSignal`로 추천 상태에 전달한다. 현재 추천을 자동으로 건너뛰거나 방문 완료로 기록하지 않는다.
8. 외부 목적지 변경은 Navigation StateFlow에 반영한다. 변경을 인지했다고 앱이 자동으로 다시 덮어쓰지 않는다.
9. 앱이 전경에서 벗어나면 SDK/리스너를 정리한다. 위치 메모리 상태를 비우고 진행 중 경로 확인을 실패 처리한다. 차량의 실제 경로를 자동 취소하지 않는다.

실제 경로 요청은 `onRoutingCompleted` 이후 확인한 목적지와 안내 상태를 함께 검사한다. 확인 제한 시간은 25초다.
SDK에 요청 ID가 없으므로 다른 앱의 동시 변경/콜백 중복·순서에 대한 차량 검증이 추가로 필요하다.
App의 선택 이력은 방문 이력이 아니라 코스 후보 선택 이력이다.

위치는 60초 만료 및 미래/미제공 측정 시각을 처리한다. 근접 힌트는 유효한 동일 출처의 위치만 사용한다.
NaviHelper 대체 위치에는 측정 시각이 없어 최신 위치 및 근접 판정에 사용하지 않는다.
위도·경도·측정 시각은 ViewModel/Data의 메모리에만 있고 SavedStateHandle, 파일, DB, 서버에 쓰지 않는다.
화면 이동용 데이트 요청에도 실제 좌표 문자열을 넣지 않는다.

## UX 안전

실제 빌드는 AAOS CarUxRestrictionsManager의 초기 값 및 변경 콜백으로 화면 접근을 제어한다.
DO 필요 또는 baseline 외 제한이 있거나 상태가 UNKNOWN이면 설정/추천 UI 대신 정차 안내를 표시하고 입력 포커스를 해제한다.
제한 해제 후 기존 Navigation controller와 화면 상태를 복원하는 흐름을 Mock 계측 테스트로 검증했다.
GPS 속도로 주행 제한을 추정하지 않고, DO나 Maps 카테고리를 임의 선언하지 않는다.
현재 구현은 정차/조작 허용 시 사용하는 보수적인 UI 기반이다. 주행 중 허용되는 별도 음성/단순 UI는 이번 범위에 포함하지 않는다.

## 신규 파일

공통 접두사 `app/src/main/java/com/github/flowtrip/`:

```text
core/platform/PleosPermissions.kt
core/platform/UnavailableUxRestrictionsRepository.kt
domain/model/PlatformState.kt
domain/repository/NavigationRepository.kt
domain/repository/LocationRepository.kt
domain/repository/UxRestrictionsRepository.kt
domain/usecase/RequestNavigationUseCase.kt
domain/usecase/ObserveLocationUseCase.kt
domain/usecase/CheckProximityUseCase.kt
data/mapper/DestinationMatching.kt
data/mapper/RouteConfirmation.kt
data/repository/MockNavigationRepository.kt
data/repository/MockLocationRepository.kt
data/repository/MockUxRestrictionsRepository.kt
presentation/common/PlatformStatusCard.kt
```

조건부 소스 / Manifest:

```text
app/src/mock/java/com/github/flowtrip/core/di/PlatformModule.kt
app/src/pleos/AndroidManifest.xml
app/src/pleos/java/com/github/flowtrip/core/di/PlatformModule.kt
app/src/pleos/java/com/github/flowtrip/core/platform/CarUxRestrictionsRepository.kt
app/src/pleos/java/com/github/flowtrip/data/mapper/PleosMappers.kt
app/src/pleos/java/com/github/flowtrip/data/repository/PleosNavigationRepository.kt
app/src/pleos/java/com/github/flowtrip/data/repository/PleosLocationRepository.kt
```

테스트 / 문서:

```text
app/src/test/java/com/github/flowtrip/platform/NavigationRepositoryTest.kt
app/src/test/java/com/github/flowtrip/platform/LocationTest.kt
app/src/test/java/com/github/flowtrip/platform/RouteConfirmationTest.kt
docs/PLEOS_SDK_RESEARCH.md
docs/PLEOS_INTEGRATION.md
docs/PROGRESS.md
```

## 수정 파일

- `settings.gradle.kts`: Pleos Maven 저장소를 해당 group에 한정해 등록.
- `gradle/libs.versions.toml`: P0 SDK 두 좌표/버전 추가. 기존 라이브러리 버전 유지.
- `app/build.gradle.kts`: 명시적 실제 SDK 옵션, 소스셋/Manifest 및 BuildConfig 설정.
- `MainActivity.kt`: Repository 전경 세션, 런타임 dangerous 권한, UX 상태 수집.
- `core/di/UseCaseModule.kt`: Navigation 및 Location UseCase 제공.
- `core/navigation/FlowTripNavHost.kt`: 안전 화면 접근 및 목적지 승인 이벤트 연결.
- `domain/model/Recommendation.kt`: 선택 장소의 Domain Destination 추가.
- `data/model/MockPlaceData.kt`: 가상 좌표/출처를 포함하는 Mock Destination 제공.
- `presentation/date/DateViewModel.kt`, `DateScreen.kt`: Repository 위치 상태 표시.
- `presentation/recommendation/RecommendationViewModel.kt`, `RecommendationScreen.kt`: 승인·비동기 결과·도착 신호.
- `presentation/common/ScreenLayout.kt`: 추천 장소가 여전히 Mock임을 표시.
- `FormViewModelTest.kt`, `RecommendationViewModelTest.kt`, `FlowTripNavigationTest.kt`: 생성자 변경/승인 흐름/오류/도착/안전 회귀 테스트.
- `README.md`: 현재 실제/Mock 모드와 문서 링크 반영.

기존 `core/di` 파일은 작업 시작 시 untracked 상태였으며 삭제하지 않았다. RepositoryModule의 기존 추천 바인딩은 유지했다.

## 의존성 및 권한

실제 옵션에만 `ai.pleos.playground:NaviHelper:3.0.5`, `ai.pleos.playground:FusedLocation:1.1.3`을 추가했다.
Hilt/KSP/Coroutines/Compose의 기존 버전을 유지했고 추가 HTTP/DB/음성/Vehicle 라이브러리는 없다.
실제 옵션의 사용자 권한은 `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP`, `FUSED_LOCATION`이며 정확한 이름/출처 및 보호 수준은 조사 문서에 정리했다.

## 검증

```powershell
# 기본 Mock (Windows 테스트 worker 인코딩 우회는 이번 명령에만 적용)
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest

# 실제 SDK Adapter 컴파일 및 동일 공통 로직 단위 테스트
.\gradlew.bat -PpleosSdk=true '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- 실제 SDK 옵션: Debug APK, 공통 단위 테스트 25개 및 Lint 통과. 실제 SDK를 호출하는 차량 통합 테스트는 아님.
- 기본 Mock 옵션: 최종 재검증 결과는 PROGRESS에 기록.
- 일반 Pixel / Android 36.1: 계측 테스트 5개 통과 (기존 예제 1 + 화면 테스트 4).
- 추가 검증: 실제 SDK APK를 일반 AVD에서 실행하면 차량 UX 미지원 안내로 차단되며 앱 시작 크래시가 없음을 확인.
- Lint의 최신 버전 안내/기존 미사용 리소스 등 경고는 남겨두며 SDK/AGP를 불필요하게 업데이트하지 않았다.
- 실제 대상 서비스, 차량, Pleos AVD가 없어 실제 권한 정의/경로/위치/도착/UX 신호는 아직 검증하지 못했다. 필요한 외부 준비는 SDK 조사 문서 8절 참고.

단위 테스트는 명시적 승인과 UX 차단, 탐색 실패/재시도, 요청 취소, 외부 목적지 변경, 콜백 확인 순서,
잘못된 목적지, 위치 null/만료/미지원/권한 오류, 근접 출처 일치, 도착 신호의 비자동 진행 및 입력/추천 상태 복원을 검증한다.


## 추천 화면 지도 전환 (2026-11-10)

추천 화면의 외부 지도 미리보기를 제거하고 `Pleos 지도 열기`를 연결했습니다.
`NavigationRepository.openMap()` → UX 제한 확인 → 실제 Adapter의 SDK 준비/권한 확인 →
`NaviHelper.requestNavigationScreen(NavigationScreenRequest.Foreground)` 순서로 호출합니다.
이 API는 Pleos Navigation 앱 화면 표시 요청이며 Compose 내부 지도 View가 아닙니다.
목적지/경로/추천 단계는 바꾸지 않고, 화면 표시 요청 전달을 실제 표시 완료로 취급하지 않습니다.
Mock에서는 미지원 안내만 제공하며 성공을 시뮬레이션하지 않습니다.
추가 권한은 실제 빌드 전용 `pleos.car.permission.NAVI_CUSTOM_MAP`입니다.
지도 전환이 앱의 onStop을 유발하면 기존 정책대로 SDK 연결을 정리하고 복귀 시 재연결합니다.
실제 화면 표시 및 복귀는 Pleos Connect 환경에서 별도 검증이 필요합니다.

검증: 기본 Mock 및 `-PpleosSdk=true` 각각 Debug APK·단위 테스트 28개·Lint 오류 0개.
일반 Android 35 에뮬레이터 UI 테스트 6개 통과(지도 미지원 안내·버튼 차단·후보 변경/재생성 및 기존 추천 흐름).
Pleos Navigation 화면의 실제 표시/복귀는 미검증입니다.
