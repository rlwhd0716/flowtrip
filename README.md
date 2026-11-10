# FlowTrip

현대자동차 Pleos Connect 공모전을 위한 차량용 여행·데이트 동행 서비스의 Android 기본 프로젝트입니다.
현재 프로젝트는 `gradle.properties`의 `pleosSdk=true`로 Pleos Connect 실행을 기본 설정합니다. Android Studio에서 Gradle Sync 후 실행하면 공식 NaviHelper·FusedLocation Adapter가 포함됩니다.
일반 Android에서 Mock으로 개발할 때는 `-PpleosSdk=false`를 명시하세요. 추천 장소 데이터는 두 모드 모두 Mock입니다.
실제 SDK APK의 컴파일은 검증했지만 Pleos 차량 서비스에서의 실행은 아직 미검증입니다. 도착 신호를 방문 완료로 확정하지 않습니다.
현재 연동 범위와 빌드 방법은 [P0 연동 결과](docs/PLEOS_INTEGRATION.md), 공식 API·권한 조사는 [SDK 조사](docs/PLEOS_SDK_RESEARCH.md),
진행도와 남은 외부 검증은 [PROGRESS](docs/PROGRESS.md)를 참고하세요.

## Compose Preview

Android Studio에서 아래 파일을 열고 우측 상단의 **Split** 또는 **Design**을 선택하면 미리보기를 볼 수 있습니다.
표시되지 않으면 Gradle Sync 후 **Build & Refresh**를 실행하세요.

- `HomeScreen.kt`: 홈
- `TripScreen.kt`: 여행 설정
- `DateScreen.kt`: 데이트 설정
- `RecommendationScreen.kt`: 추천, 로딩, 오류, 목적지 승인, 코스 완료

파일 위치는 `app/src/main/java/com/github/flowtrip/presentation/` 아래의 해당 화면 패키지입니다.
각 화면은 가로/세로와 밝은/어두운 테마를 제공합니다. Preview는 샘플 상태를 사용하므로 SDK나 에뮬레이터 연결이 필요하지 않습니다.
여행/데이트 설정은 **Interactive mode**에서 입력과 선택을 바꿀 수 있습니다. Preview의 화면 이동·실제 지도 열기·목적지 요청은 실행하지 않습니다.
[Android 공식 Preview 안내](https://developer.android.com/develop/ui/compose/tooling/previews).

## 개발 환경

| 항목 | 설정 |
| --- | --- |
| 모듈 | 단일 `app` |
| UI | Kotlin / Jetpack Compose / Material 3 |
| compileSdk | 36.1 |
| targetSdk / minSdk | 36 / 28 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin Compose 플러그인 | 2.2.10 |
| Gradle Wrapper | 9.3.1 |
| Gradle Daemon JDK | 21 (`gradle-daemon-jvm.properties`) |
| Java 소스 / 타깃 호환성 | 11 |
| Compose BOM | 2026.02.01 |

SDK, AGP, Kotlin, Gradle의 기존 설정을 유지했습니다. 최초 빌드에서 Core 1.19.0과 Lifecycle Compose 2.11.0이 compileSdk 37을 요구했습니다.
사용자 승인에 따라 Core KTX만 1.17.0, Lifecycle 계열만 2.10.0으로 조정했고, AAR 호환성 검사를 포함한 빌드로 확인했습니다.
버전과 라이브러리 선언은 `gradle/libs.versions.toml`에서 관리합니다.

## 패키지와 파일

다음 트리는 초기 뼈대의 주요 패키지 구조입니다. P0의 Domain/Repository/Platform 확장 및 조건부 소스셋의 전체 목록은 [연동 결과](docs/PLEOS_INTEGRATION.md)에 정리했습니다.

```text
com.github.flowtrip/
├── MainActivity.kt                         # 수정: 앱 UI 시작점
├── FlowTripApplication.kt                  # @HiltAndroidApp 진입점
├── core/
│   ├── di/
│   │   ├── RepositoryModule.kt             # Repository 구현 바인딩
│   │   └── UseCaseModule.kt                # domain UseCase 제공
│   ├── designsystem/
│   │   ├── FlowTripTheme.kt                # 색상, Typography, Shape, 다크 모드
│   │   └── Components.kt                   # 공통 카드, 64dp 이상 CTA
│   ├── navigation/
│   │   └── FlowTripNavHost.kt              # 라우트, 화면별 ViewModel 생성
│   └── util/
│       └── TimeOptions.kt                  # 시간 선택 목록과 표시
├── domain/
│   ├── model/
│   │   └── Recommendation.kt              # 모드, 요청, 단계, 장소
│   ├── repository/
│   │   └── RecommendationRepository.kt    # 데이터 제공 계약
│   └── usecase/
│       └── GetRecommendationsUseCase.kt
├── data/
│   ├── model/
│   │   └── MockPlaceData.kt                # 가상 장소 6개와 domain 변환
│   └── repository/
│       └── MockRecommendationRepository.kt
└── presentation/
    ├── home/
    │   └── HomeScreen.kt                   # 상태 없는 모드 진입 화면
    ├── trip/
    │   ├── TripScreen.kt
    │   └── TripViewModel.kt                # TripUiState 포함
    ├── date/
    │   ├── DateScreen.kt
    │   └── DateViewModel.kt                # DateUiState 포함
    ├── recommendation/
    │   ├── RecommendationScreen.kt
    │   └── RecommendationViewModel.kt      # RecommendationUiState 포함
    └── common/
        ├── ScreenLayout.kt                # 공통 스크롤, 가로/세로 레이아웃
        └── ChoiceGroup.kt                 # 시간, 취향, 분위기 선택
```

그 밖의 생성·수정 파일:

- `app/build.gradle.kts`: 의존성 추가
- `gradle/libs.versions.toml`: 버전 관리 및 호환 버전 조정
- `app/src/main/AndroidManifest.xml`: Application 등록
- `app/src/main/res/values/strings.xml`: 앱 이름 `FlowTrip`
- `app/src/test/java/com/github/flowtrip/MainDispatcherRule.kt`: 코루틴 테스트 디스패처
- `app/src/test/java/com/github/flowtrip/presentation/FormViewModelTest.kt`: 입력 및 복원 테스트 2개
- `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt`: 추천 상태 테스트 6개
- `app/src/androidTest/java/com/github/flowtrip/FlowTripNavigationTest.kt`: 실제 화면 흐름 테스트 2개
- `README.md`: 구조, 실행, 검증 및 향후 확장 안내

기존 `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` 템플릿은 `core/designsystem/FlowTripTheme.kt`로 대체했습니다.
기존 예제 단위 테스트와 계측 테스트는 유지했습니다.

## 아키텍처와 상태 관리

의존 방향은 `presentation → domain ← data`입니다. domain은 Android나 Compose 타입에 의존하지 않습니다.
Hilt가 객체 생성과 의존성 주입을 담당합니다. `@HiltAndroidApp` Application과 `@AndroidEntryPoint` Activity에서 시작해
`RepositoryModule`의 `@Binds @Singleton`으로 Mock Repository를 연결하고, `UseCaseModule`의 `@Provides`로 UseCase를 제공합니다.
domain에는 Hilt/Inject 어노테이션을 추가하지 않아 프레임워크 독립성을 유지합니다.

각 ViewModel은 `@HiltViewModel`과 `@Inject constructor`를 사용합니다. Navigation에서 `hiltViewModel()`로 생성하므로
각 back-stack entry의 생명주기와 `SavedStateHandle`이 유지됩니다. 추천 화면의 요청 인수도 ViewModel의 `SavedStateHandle`에서 읽습니다.
기존 `AppContainer`와 수동 ViewModel factory는 제거했습니다. `PlatformModule`은 기본 `src/mock/java`와 실제 SDK `src/pleos/java` 중 하나만 컴파일해
Navigation/Location/UX 구현을 선택합니다. 데이터베이스와 HTTP 클라이언트는 아직 추가하지 않았습니다.

화면은 `UiState`와 명시적인 이벤트 함수를 받고, ViewModel은 수정 가능한 `MutableStateFlow`를 내부에 숨겨 읽기 전용 `StateFlow`를 제공합니다.
Navigation의 화면 진입점에서 `collectAsStateWithLifecycle()`로 수집합니다. 상태가 없는 Home에는 별도의 ViewModel을 만들지 않았습니다.

여행 목적지·시간·취향과 데이트 시간·분위기·새로운 장소 옵션은 각 화면의 `SavedStateHandle`에 저장합니다.
추천 요청은 URI 인코딩한 Navigation 인수로 전달합니다. ViewModel은 각 back-stack entry에 속하므로 추천 화면에서 뒤로 돌아가도 입력값이 유지됩니다.
진행 단계, 후보 인덱스, 선택/건너뛴 장소 ID도 `SavedStateHandle`에 저장하고, 복원 시 Repository 응답으로 장소 목록을 재구성합니다.
이는 구성 변경 및 Android의 정상적인 저장 상태 복원을 위한 장치이며, 강제 종료 후 영구 이력 저장 기능은 아닙니다.

## 화면 흐름

1. Home → 여행 모드 또는 데이트 모드
2. 여행: 목적지 직접 입력 → 시간/취향 선택 → 코스 추천 시작 (공백 목적지는 시작 불가)
3. 데이트: 예시 위치 확인 → 시간/분위기/새로운 장소 옵션 → 데이트 시작
4. 공통 추천: 식당 → 야경 명소 → 디저트 카페 → 완료 요약

- **다른 장소 추천**: 현재 단계의 두 후보를 순환합니다. 선택/건너뛰기 이력에는 기록하지 않습니다.
- **이 장소 선택**: 별도 목적지 승인 창을 표시합니다. 승인 후 경로 확인이 성공해야 선택 이력에 추가하고 다음 추천 단계로 진행합니다. 승인 취소·경로 실패 시 진행하지 않습니다.
- **건너뛰기**: 현재 장소를 건너뛴 이력에 추가하고 다음 단계로 진행합니다.
- 마지막 단계 이후에는 선택 장소 요약과 홈 버튼을 표시합니다. 완료 후 추가 진행은 무시합니다.
- 불러오는 중에는 상태 변경을 무시합니다. Repository 오류나 필수 단계 누락은 재시도 가능한 오류 상태로 표시합니다.

추천 장소명·장소 정보·거리·시간은 가상 데이터입니다. 좌표는 성수·서울숲 일대의 예시 위치이며, 실제 장소 POI를 의미하지 않습니다. Mock 목적지는 실제 SDK로 전송하지 않습니다. 두 모드 모두 같은 3단계 예시 코스를 사용합니다.
설정값은 요청 객체와 추천 이유에 반영되며, 실제 취향별 필터링·시간 최적화·현재 시각에 따른 야경 판정은 수행하지 않습니다.

### 추천 화면의 Pleos 지도

추천 화면의 `Pleos 지도 열기`는 NaviHelper 3.0.5의 `requestNavigationScreen(NavigationScreenRequest.Foreground)`로
Pleos Connect 내비게이션 화면 표시를 요청합니다. 앱 내부에 지도 타일을 삽입하는 방식이 아닙니다.
OpenStreetMap·OSRM·Leaflet WebView와 외부 지도/경로 요청은 제거했습니다. 장소 설명·예상 체류·비용·주차 예시 정보는 유지합니다.

- 실제 지도는 `bash gradlew -PpleosSdk=true :app:assembleDebug`로 빌드해 Pleos Connect 에뮬레이터/차량에서 사용합니다.
- 실제 SDK Manifest에 `pleos.car.permission.NAVI_CUSTOM_MAP`을 추가했습니다. 런타임 권한은 기기에서 dangerous로 정의된 경우 요청합니다.
- UX 조작 허용·SDK 연결·화면 표시 권한을 확인한 뒤 호출합니다. 화면 표시 요청 전달은 실제 화면 표시 완료를 보장하지 않습니다.
- 지도 열기는 현재 목적지·경로 및 추천 진행을 변경하지 않습니다. Mock 추천 장소를 실제 내비게이션에 보내지 않습니다.
- 기본 Mock 빌드에서는 사용 환경 안내와 비활성 버튼을 표시하며 외부 지도로 대체하지 않습니다.
- Pleos 내비게이션의 실제 화면 전환은 대상 환경에서 검증해야 합니다. 일반 Android 에뮬레이터는 이를 대체하지 않습니다.

근거: [NaviHelper 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro),
[화면 표시 권한](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions), 실제 AAR 공개 시그니처 확인.

Material 3 기반으로 밝은/어두운 테마를 지원합니다. 가용 너비 760dp 이상에서는 두 열, 그 미만에서는 한 열로 배치하고 작은 가로 화면에서도 스크롤할 수 있습니다.
CTA 최소 높이는 64dp, 선택 컨트롤 영역은 56dp입니다. 브랜드 색상과 글꼴 및 모서리는 Theme 파일에서 변경합니다.

## 추가 의존성

| 의존성 | 버전 | 목적 |
| --- | --- | --- |
| `com.google.dagger:hilt-android` | 2.59.2 | DI 런타임 |
| `com.google.dagger:hilt-compiler` | 2.59.2 | KSP 코드 생성 전용 |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.3.0 | Compose의 `hiltViewModel()` |
| `androidx.navigation:navigation-compose` | 2.9.0 | 화면 이동 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.10.0 | lifecycle-aware 상태 수집 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.10.0 | 화면별 ViewModel |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.10.0 | 상태 저장/복원 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Main dispatcher와 코루틴 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.9.0 | 단위 테스트 전용 |

Hilt Gradle 플러그인 2.59.2와 KSP 2.3.6을 사용합니다. AGP 9의 내장 Kotlin을 유지하며 별도 Kotlin Android/KAPT 플러그인은 추가하지 않았습니다.
기존 SDK·Kotlin·Gradle·AGP 버전과 Core/Lifecycle 호환 버전은 그대로 유지합니다.
설정 근거는 [Hilt Gradle 설정](https://dagger.dev/hilt/gradle-setup)과
[Android 공식 AGP 9 이전 안내](https://developer.android.com/agents/skills/build-system/agp/agp-9-upgrade/skill)를 참고했습니다.

## 빌드와 테스트

```powershell
.\gradlew.bat -PpleosSdk=false :app:assembleDebug :app:lintDebug
.\gradlew.bat -PpleosSdk=false :app:testDebugUnitTest :app:assembleDebugAndroidTest
# Android 기기 또는 에뮬레이터 연결 후:
.\gradlew.bat -PpleosSdk=false :app:connectedDebugAndroidTest
```

이 Windows 환경에서는 UTF-8 Gradle daemon이 한글 사용자 경로의 worker classpath를 전달할 때
`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`이 발생했습니다.
프로젝트 설정을 수정하지 않고 실행 시에만 아래 옵션을 적용해 Gradle 단위 테스트를 정상 실행했습니다.

```powershell
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' :app:testDebugUnitTest
```

검증 결과:

- Hilt 전환 후 Debug 빌드·Lint·Gradle 단위 테스트 9개·Android 36.1 계측 테스트 3개 재검증 통과
- Hilt 검증에는 실제 Application/Activity 진입점, Navigation별 ViewModel 생성, 요청 인수 주입 및 Activity 재생성 시 상태 유지가 포함됨
- Debug APK 빌드: 성공 (AAR 메타데이터 검사 포함, 검사 제외 없음)
- Lint: 오류 0개. 최신 버전 안내, 기존 미사용 색상, 기존 중복 label 경고는 유지
- Gradle 단위 테스트: 9개 통과 (신규 8개 + 기존 예제 1개)
- Android UI 테스트 APK: 빌드 성공
- Android 36.1 Pixel 4 에뮬레이터 기본 화면: 계측 테스트 3개 통과 (신규 UI 2개 + 기존 예제 1개)
- 같은 에뮬레이터의 1280×720 / 160dpi 가로·다크 모드: 신규 UI 테스트 2개 모두 통과
- 가로·다크 모드 홈 화면: 소프트웨어 렌더러로 실제 표시 및 두 열 배치 확인
- 실제 Pleos 차량 화면 및 주행 상태별 정책: 미검증, 후속 SDK 연동 범위

단위 테스트는 입력 유효성, 입력 복원, 로딩 중 입력 무시, 동일 단계 후보 순환, 선택/건너뛰기,
마지막 단계 완료, 추천 진행 및 완료 상태 복원, Repository 오류 후 재시도, 불완전 응답을 검사합니다.
UI 테스트는 여행/데이트 진입, 특수문자가 있는 목적지 전달, 입력값 유지, Activity 재생성 시 후보 유지, 코스 완료 및 홈 복귀를 검사합니다.

## 향후 Pleos 연동

- 공식 SDK와 적용 가능한 앱 카테고리·배포 요건을 확인한 뒤 data 계층에 실제 구현을 추가합니다.
- `RecommendationRepository`의 실제 구현을 추가하고 `RepositoryModule`의 `@Binds` 연결을 교체합니다.
- API 클라이언트와 SDK처럼 외부에서 생성해야 하는 객체는 `core/di`의 별도 Module에서 `@Provides`로 제공합니다.
- 위치·경로·차량 상태처럼 별도 책임이 생길 때 domain에 해당 계약과 모델을 추가하고, data에서 SDK를 감쌉니다.
- NaviHelper, FusedLocation, Vehicle, TTS, STT 호출이나 SDK 타입을 Screen/ViewModel에 직접 노출하지 않습니다.
- 권한, SDK 초기화/해제 및 생명주기, 실제 단위/오류 변환은 SDK 문서에 따라 data 및 앱 시작 계층에서 처리합니다.
- 주행 중 조작 정책과 제한은 공식 문서와 앱 카테고리 확인 후 구현합니다. 현재 안내 문구는 실제 조작 제한 장치가 아닙니다.

기본 빌드는 Pleos 권한·SDK 없이 실행됩니다. 실제 SDK 옵션은 공식 권한을 별도 Manifest overlay로 추가하고 AAOS UX 제한을 확인합니다.
DO 및 Maps 앱 카테고리를 임의 선언하지 않았으며 실제 Pleos 차량 탑재·배포 승인은 별도 검증이 필요합니다.
