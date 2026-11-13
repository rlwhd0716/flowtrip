# 메인·여행 입력·데이트 입력 화면 구현

작성일: 2026-11-14

추가된 3개 시안을 기준으로 앱 콘텐츠 영역을 구현했다. 이전 추천 화면 작업은 `UI_REDESIGN_IMPLEMENTATION.md`에 있으며, 이 문서는 이후 추가된 진입/입력 화면 변경을 설명한다.

## 구현 내용

### 메인 화면

- 밝은 배경, FlowTrip 제목과 안내, 여행/데이트 사진 카드, 시작하기 영역을 적용했다.
- 넓은 화면은 두 모드 카드를 나란히 배치하고 좁은 분할 창은 세로로 전환한다.
- 최근 장소는 창 폭에 따라 4열/2열/1열로 배치한다.
- 실제 기록이 없을 때 4개 Mock 장소를 예시로 표시한다. `실제 방문 기록이 아닙니다` 문구를 함께 표시한다.
- 추천 화면에서 사용자가 다음 코스/완료로 확정한 장소를 최근 코스 선택 기록으로 최대 4개 저장한다. SDK 길안내 성공/도착만으로 방문 기록을 만들지 않는다.
- 최근 목록은 HomeViewModel이 관리한다. Place 스냅샷은 SavedStateHandle, 장소 ID 목록은 앱 내부 SharedPreferences에 저장한다. 실제 차량 GPS는 여기에 저장하지 않는다.
- 다시 이용하기는 해당 모드·단계·후보의 상세 화면을 연다. 경로를 자동 요청하지 않는다. 안내가 필요하면 기존 실제 목적지 길안내 버튼을 눌러야 한다.
- 현재 기록은 실제 방문 여부를 확인하거나 중복 추천을 제거하는 기능이 아니다. 이 사실을 화면에 표시한다.

### 여행 입력 화면

- 해안 사진 헤더, 여행 목적지, 여행 가능 시간, 여행 관심사, 하단 추천 버튼을 구현했다.
- 시/도 → 시/군/구 연동 드롭다운을 제공한다. 상위 지역을 바꾸면 하위 선택을 초기화하며, 유효한 두 지역을 고르기 전까지 추천 버튼을 비활성화한다.
- 지역 데이터는 **전국 17개 시/도와 주요 시/군/구의 오프라인 시연 목록**이다. 전국 모든 행정구역을 포함하는 주소 데이터베이스가 아니며, 화면에서도 이 범위를 안내한다. 전체 주소 지원이 필요하면 DemoRegionCatalog를 검증된 전체 데이터로 교체해야 한다.
- 관심사 8개 중 최대 3개를 복수 선택한다. 선택 상한에서는 선택하지 않은 항목을 비활성화하고, 선택 항목 해제는 허용한다.
- 지역·관심사·시간·단위를 SavedStateHandle에 보존하고 기존 RecommendationRequest에 전달한다.

### 데이트 입력 화면

- 야경 사진 헤더, 현재 위치, 데이트 가능 시간, 분위기, 장소 유형, 하단 추천 버튼을 구현했다.
- 기존 ObserveLocationUseCase를 재사용하고 Mock/실제 위치/권한 거부/지원 안 됨/오래된 위치 표시를 유지한다. 실제 좌표는 소수점 5자리로 표시한다. 시안의 좌표를 실제 위치로 사용하지 않는다.
- 분위기 4개 중 최대 2개, 장소 유형 10개는 복수 선택할 수 있다.
- 기존 `새로운 장소 위주로 추천` 옵션을 유지했다.
- 분위기·장소 유형·시간·단위·새 장소 선호를 SavedStateHandle에 저장한다. 위치는 기존 위치 스트림에서 다시 관측한다.
- 선택값은 기존 RecommendationRequest의 preference/newPlaces에 전달한다. 현재는 고정 Mock 후보를 제공하므로 실제 AI 필터링이나 동선 최적화는 수행하지 않는다.

### 공통 입력 UI

- FlowFormLayout, InputSection, ChoiceGrid, RegionDropdown, TimeSelector를 공유한다.
- 1~24시간 또는 1~7일을 +/- 버튼으로 설정한다. 시간/일 전환은 표시 숫자를 유지하고 새 단위의 상한에 맞춰 제한한다. 예: 2시간 → 2일, 24시간 → 7일. 요청 모델에는 분 단위로 변환해 전달한다.
- 입력 콘텐츠만 스크롤하고 추천 시작 버튼은 아래 고정 영역에 둔다.
- 좁은 창에서는 제목/입력 배치를 조정하고 짧은 가로 창에서는 헤더 보조 문구를 줄인다. 실제 글꼴 확대·차량 창 크기별 표시 상태는 아직 확인하지 않았다.
- 제공된 시안 파일을 사진 소스로 포함하고, 공통 ReferencePhoto/PlacePhoto가 사진 영역만 표시한다. 차량 제어·시스템 상태바·하단 시스템 메뉴는 렌더링하거나 수정하지 않는다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `app/src/main/java/com/github/flowtrip/presentation/home/HomeScreen.kt` | 메인 모드/최근 장소 카드 |
| `app/src/main/java/com/github/flowtrip/presentation/home/HomeViewModel.kt` | 신규: 예시 및 최근 선택 기록 관리/보존 |
| `app/src/main/java/com/github/flowtrip/presentation/trip/TripScreen.kt` | 여행 입력 화면 |
| `app/src/main/java/com/github/flowtrip/presentation/trip/TripViewModel.kt` | 지역/복수 관심사/시간·일 입력 상태 |
| `app/src/main/java/com/github/flowtrip/presentation/date/DateScreen.kt` | 데이트 입력 화면 |
| `app/src/main/java/com/github/flowtrip/presentation/date/DateViewModel.kt` | 복수 분위기/유형/시간·일, 기존 위치 관측 유지 |
| `app/src/main/java/com/github/flowtrip/presentation/common/FlowFormComponents.kt` | 신규: 공통 입력 UI |
| `app/src/main/java/com/github/flowtrip/presentation/common/ReferencePhoto.kt` | 신규: 시안 사진 영역 표시 공통화 |
| `app/src/main/java/com/github/flowtrip/data/model/DemoRegionCatalog.kt` | 신규: 오프라인 주요 지역 선택 목록 |
| `app/src/main/java/com/github/flowtrip/core/util/TimeOptions.kt` | 시간/일 단위·범위·변환 |
| `app/src/main/java/com/github/flowtrip/core/navigation/FlowTripNavHost.kt` | 신규 입력 콜백, 홈 상태, 최근 장소 진입 연결 |
| `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModel.kt` | 최근 장소 진입 시 초기 단계·후보 선택 |
| `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationScreen.kt` | 기존 사진 표시를 공통 컴포넌트로 이동 |
| `app/src/main/res/drawable-nodpi/demo_home_reference.png` | 신규: 메인 시안 사진 소스 |
| `app/src/main/res/drawable-nodpi/demo_trip_input_reference.png` | 신규: 여행 입력 시안 사진 소스 |
| `app/src/main/res/drawable-nodpi/demo_date_input_reference.png` | 신규: 데이트 입력 시안 사진 소스 |
| `app/src/test/java/com/github/flowtrip/presentation/FormViewModelTest.kt` | 지역 연동/선택 상한/단위 범위/복원 검증 시나리오 갱신 |
| `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt` | 최근 장소 진입은 자동 길안내를 하지 않는 시나리오 추가 |
| `app/src/androidTest/java/com/github/flowtrip/FlowTripNavigationTest.kt` | 변경된 진입 화면/지역 선택에 맞춰 갱신 |
| `docs/ENTRY_SCREENS_IMPLEMENTATION.md` | 이 문서 |

SDK 어댑터·공식 목적지 매핑·경로 콜백 판정·차량 UX 제한·MainActivity 생명주기·Manifest/창 설정은 이번 변경에 포함하지 않았다.

## 점검 결과와 남은 확인

사용자 지시에 따라 **Gradle 빌드, 단위 테스트, 기기 테스트, APK 설치, Compose 프리뷰 렌더링을 실행하지 않았다.** 테스트 파일은 시나리오를 갱신했을 뿐 통과 결과가 아니다.

수행한 간단한 정적 점검:

- Kotlin 63개 파일의 괄호·문자열·주석 구조와 대체 문자 유무.
- NavHost에서 참조하는 ViewModel 메서드 존재 여부, 삭제한 입력 메서드/기존 버튼 문구의 남은 참조 검색.
- 시안 사진 소스 10곳의 PNG 유효성 및 원본 이미지 범위 내 좌표 여부.
- 변경 소스의 공백 오류와 호출 관계 수동 검토.

위 점검은 Kotlin 컴파일러의 구문/타입 검사나 실행 결과를 대신하지 않는다.

사용자가 추가 확인할 항목:

- Pleos/Mock 빌드, 기존·갱신된 단위/UI 테스트 실행.
- 메인 → 여행/데이트 입력 → 추천, 뒤로 이동, 화면 재생성 후 입력 복원.
- 지역 변경 시 하위 선택 초기화, 미선택 시작 비활성, 관심사 3개/분위기 2개 제한과 해제.
- 시간/일 전환·상한·하한·분 단위 전달, 장소 유형 복수 선택.
- 실제 위치·권한 거부·위치 갱신 및 차량 안전 제한.
- 최근 목록의 예시/선택 기록 구분, 앱 재실행 후 ID 복원, 다시 이용하기의 단계/후보 및 자동 경로 미요청.
- 전체/분할/짧은 가로 창과 큰 글꼴에서 사진 크롭, 카드 줄바꿈, 드롭다운, 고정 시작 버튼의 가독성·잘림 여부.
- 기존 두 모드의 실제 SDK 목적지 요청 및 복귀 상태 회귀 확인.
