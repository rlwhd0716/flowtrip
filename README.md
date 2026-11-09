# FlowTrip

현대자동차 Pleos Connect 공모전을 위한 차량용 여행·데이트 동행 서비스의 Android 기본 프로젝트입니다.
현재 단계는 외부 API 없이 실행되는 Mock 프로토타입입니다. 실제 위치, 차량 상태, 경로, 방문 여부를 수집하거나 판단하지 않습니다.

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

다음 트리는 `app/src/main/java/com/github/flowtrip` 아래의 최종 소스 구조이며, 기재한 파일은 모두 이번 단계에서 생성하거나 수정했습니다.

```text
com.github.flowtrip/
├── MainActivity.kt                         # 수정: 앱 UI 시작점
├── FlowTripApplication.kt                  # Application, 수동 DI 컨테이너
├── core/
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
`FlowTripApplication`의 `AppContainer`가 Mock Repository와 UseCase를 생성하며 Navigation의 ViewModel factory에 전달합니다.
Hilt, 데이터베이스, HTTP 클라이언트는 추가하지 않았습니다.

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
- **이 장소 선택**: 현재 장소를 선택 이력에 추가하고 다음 단계로 진행합니다.
- **건너뛰기**: 현재 장소를 건너뛴 이력에 추가하고 다음 단계로 진행합니다.
- 마지막 단계 이후에는 선택 장소 요약과 홈 버튼을 표시합니다. 완료 후 추가 진행은 무시합니다.
- 불러오는 중에는 상태 변경을 무시합니다. Repository 오류나 필수 단계 누락은 재시도 가능한 오류 상태로 표시합니다.

장소명·위치·거리·시간은 모두 가상 데이터입니다. 두 모드 모두 같은 3단계 예시 코스를 사용합니다.
설정값은 요청 객체와 추천 이유에 반영되며, 실제 취향별 필터링·시간 최적화·현재 시각에 따른 야경 판정은 수행하지 않습니다.

Material 3 기반으로 밝은/어두운 테마를 지원합니다. 가용 너비 760dp 이상에서는 두 열, 그 미만에서는 한 열로 배치하고 작은 가로 화면에서도 스크롤할 수 있습니다.
CTA 최소 높이는 64dp, 선택 컨트롤 영역은 56dp입니다. 브랜드 색상과 글꼴 및 모서리는 Theme 파일에서 변경합니다.

## 추가 의존성

| 의존성 | 버전 | 목적 |
| --- | --- | --- |
| `androidx.navigation:navigation-compose` | 2.9.0 | 화면 이동 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.10.0 | lifecycle-aware 상태 수집 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.10.0 | 화면별 ViewModel |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.10.0 | 상태 저장/복원 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Main dispatcher와 코루틴 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.9.0 | 단위 테스트 전용 |

## 빌드와 테스트

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
# Android 기기 또는 에뮬레이터 연결 후:
.\gradlew.bat :app:connectedDebugAndroidTest
```

이 Windows 환경에서는 UTF-8 Gradle daemon이 한글 사용자 경로의 worker classpath를 전달할 때
`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`이 발생했습니다.
프로젝트 설정을 수정하지 않고 실행 시에만 아래 옵션을 적용해 Gradle 단위 테스트를 정상 실행했습니다.

```powershell
.\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949' :app:testDebugUnitTest
```

검증 결과:

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
- `RecommendationRepository`의 실제 구현을 추가하고 `AppContainer`의 연결을 교체합니다.
- 위치·경로·차량 상태처럼 별도 책임이 생길 때 domain에 해당 계약과 모델을 추가하고, data에서 SDK를 감쌉니다.
- NaviHelper, FusedLocation, Vehicle, TTS, STT 호출이나 SDK 타입을 Screen/ViewModel에 직접 노출하지 않습니다.
- 권한, SDK 초기화/해제 및 생명주기, 실제 단위/오류 변환은 SDK 문서에 따라 data 및 앱 시작 계층에서 처리합니다.
- 주행 중 조작 정책과 제한은 공식 문서와 앱 카테고리 확인 후 구현합니다. 현재 안내 문구는 실제 조작 제한 장치가 아닙니다.

현재 Manifest는 SDK 없는 프로토타입 실행을 위한 구성입니다. 실제 Pleos/Android Automotive 탑재 및 배포 호환성은 아직 검증하지 않았습니다.
