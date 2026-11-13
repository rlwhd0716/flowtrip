# FlowTrip 여행·데이트 공통 UI 구현 보고

작성일: 2026-11-14

## 검증 범위

구현과 간단한 정적 코드 점검만 수행했다. 초기 Gradle 컴파일 시도는 배포본 다운로드 권한 오류로 컴파일 단계에 도달하지 못했다. 이후 사용자 지시에 따라 빌드, 단위 테스트, 기기 테스트, APK 설치, UI 프리뷰 렌더링을 실행하지 않았다. 아래의 테스트 항목은 작성된 검증 시나리오이며 통과 결과가 아니다.

## 작업 전 구조 확인

- 언어/UI: Kotlin, Jetpack Compose, Material 3. Hilt로 플랫폼·추천 저장소와 UseCase를 주입한다.
- 화면: Compose Navigation의 홈 → 여행/데이트 입력 → 공통 추천 화면. 입력 화면과 기존 뒤로 이동은 유지했다.
- 기존 추천: RecommendationRepository → GetRecommendationsUseCase → RecommendationViewModel. 기존에는 두 모드 모두 데이트 단계/Mock 후보를 사용했다.
- 기존 후보/코스 이동: SDK 경로 요청 성공 후 후보와 코스를 변경했다. 이번 요구에 따라 탐색과 길안내를 분리했다.
- 상태: ViewModel의 StateFlow, SavedStateHandle, collectAsStateWithLifecycle. 후보 및 진행 상태를 저장하고 프로세스 복원 시 경로를 자동 재전송하지 않는 기존 원칙을 유지했다.
- SDK: RequestNavigationUseCase → NavigationRepository → PleosNavigationRepository. Mock/Pleos 구현은 Gradle 설정과 PlatformModule로 분기한다.
- 콜백: 요청 전달과 안내 시작을 구분한다. 기존 RouteConfirmation은 경로 완료 신호, 목적지/경유지 일치, 안내 상태 확인을 조합하며 오류·타임아웃을 처리한다. 이 구현은 변경하지 않았다.
- 생명주기: 기존 MainActivity의 onStart 연결/onStop 해제 및 구성 변경 예외를 유지한다. 화면에 보이는 분할 Activity의 일시정지만으로 서비스를 해제하지 않는 처리를 유지한다.
- 분할 화면: 이전 자동 배치 실험은 이미 제거된 상태였다. 강제 창 크기·방향·오버레이·내부 지도 렌더링을 추가하지 않았다. Pleos window bar의 플랫폼 창 배치를 유지한다.
- 차량 안전: 기존 UxRestrictionsRepository와 NavHost 입력 제한, 경로 UseCase/SDK 어댑터의 UX 확인을 유지한다.

## 이번 작업 파일

기존 작업 트리에 다른 수정 사항이 있었으며, 아래는 이번 UI 구현에서 변경한 파일만 기재한다.

| 파일 | 변경 내용 |
| --- | --- |
| `app/src/main/java/com/github/flowtrip/domain/model/Recommendation.kt` | 모드별 3단계, 상세 항목, 이미지 유형, 명시적 DemoRouteMapping 추가 |
| `app/src/main/java/com/github/flowtrip/data/model/MockRecommendationDataSource.kt` | 신규: 2모드 × 3단계 × 3후보, 총 18곳의 예시 데이터 |
| `app/src/main/java/com/github/flowtrip/data/model/MockPlaceData.kt` | 삭제: 대체된 옛 데이트 전용 fixture 및 가상 목적지 좌표 |
| `app/src/main/java/com/github/flowtrip/data/repository/MockRecommendationRepository.kt` | 모드별 공급, 명시적 공식 목적지 ID 매핑. 기존 Repository 계약 유지 |
| `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModel.kt` | 후보 선택·코스 이동과 길안내 분리, 모드별 저장/복원, 현재 모드 다시 추천 |
| `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationScreen.kt` | 공통 밝은 UI, 2열/좁은 창 대응, 사진·상세, 고정 액션, 시연 안내 |
| `app/src/main/java/com/github/flowtrip/core/navigation/FlowTripNavHost.kt` | 공통 화면 콜백 연결 |
| `app/src/main/java/com/github/flowtrip/presentation/common/preview/FlowTripPreviewData.kt` | 실제 Mock 공급원과 일치하는 여행·데이트 프리뷰 fixture |
| `app/src/main/res/drawable-nodpi/demo_travel_reference.png` | 제공된 여행 시안 원본을 사진 소스로 포함 |
| `app/src/main/res/drawable-nodpi/demo_date_reference.png` | 제공된 데이트 시안 원본을 사진 소스로 포함 |
| `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt` | 새 탐색 규칙·모드 복원·오류/재시도·중복 방지·직렬화 회귀 시나리오 |
| `app/src/test/java/com/github/flowtrip/data/MockRecommendationRepositoryTest.kt` | 신규: 전체 18후보와 각 단계의 목적지 매핑 검사 |
| `app/src/androidTest/java/com/github/flowtrip/FlowTripNavigationTest.kt` | Mock 전용 UI 흐름, 모드 전환·재생성·미설정 버튼 비활성 시나리오 |
| `docs/UI_REDESIGN_IMPLEMENTATION.md` | 이 구현 보고 |

SDK 어댑터, 공식 목적지 카탈로그, 경로 콜백 판정, MainActivity 생명주기, Manifest/창 설정은 이번 작업에서 변경하지 않았다.

## 화면 및 모드

두 모드는 하나의 RecommendationScreen을 공유한다. 공통 구성요소는 ModeSelector, CourseProgress, RecommendationList/Card, PlaceDetail, NavigationAction, CourseAction, DemoModeNotice다.

- 여행: 관광지 → 맛집 → 카페.
- 데이트: 처음 가보는 식당 → 야경 명소 → 디저트 카페.
- 각 단계 3후보를 표시하고 카드 터치 또는 다음 추천으로 현재 선택만 변경한다. 후보 끝에서는 첫 후보로 순환한다.
- 다음 코스는 현재 선택을 기록하고 다음 단계로 이동한다. 마지막 단계에서는 완료 상태를 표시한다.
- 길안내 성공/오류/도착 신호는 코스나 후보를 자동 변경하지 않는다.
- 코스 다시 추천은 현재 모드의 진행/선택을 초기화하고 Mock 목록을 다시 불러온다. 고정 fixture이므로 목록 자체는 동일하다.
- 모드 변경 시 해당 모드의 후보·단계·선택·선택 기록을 복원한다. 진행 상태가 없으면 첫 단계부터 시작한다. 활성 모드와 비활성 모드의 스냅샷 모두 SavedStateHandle에 저장한다.
- 로딩/길안내 요청 중에는 모드 전환과 중복 액션을 막는다. 프로세스 재생성 시 중단된 요청은 재전송하지 않고 명시적인 재시도를 기다린다.
- 기본 가로 창은 2열이다. 앱 콘텐츠 폭 640dp 미만이면 목록과 상세를 세로로 배치한다. 스크롤은 콘텐츠에 적용하고 길안내·다음 추천·다음 코스 버튼은 아래 고정 영역에 둔다.
- 제공된 시안의 사진 영역을 BitmapPainter의 source region으로 표시한다. 차량 이미지·시스템 바·문구 영역을 UI에 그리지 않는다. 사진은 실제 추천 장소의 검증된 사진이 아니며 예시 이미지로 표시한다.
- 예시 사진을 여러 후보에 공유한다. 배포용 고유 장소 사진이 필요한 경우 별도 자산을 준비해야 한다.
- 사용자 화면에서 SDK 목적지 패널, 플랫폼 상태 패널, 중복 길안내 및 개발 로그 영역을 제외했다. 기존 진단 컴포넌트/메서드와 SDK 로그, 별도 opt-in Pleos smoke test는 남겨두었다.

## 데이터와 실제 목적지

기존 RecommendationRepository 계약을 유지하고 MockRecommendationDataSource를 데이터 공급원으로 분리했다. Place는 추천 정보와 `details`, `image`, `demoRoute`, 실제 SDK 요청용 `destination`을 갖는다.

매핑은 배열 위치나 stage ordinal로 계산하지 않는다. 각 fixture가 DemoRouteMapping을 명시하고 Repository가 해당 ID를 공식 카탈로그에서 찾는다. 화면에는 추천 장소의 정보가 표시되고 SDK에는 별도의 실제 Destination 전체 payload가 전달된다.

| 단계 내 후보 | 명시적 매핑 | SDK 목적지 |
| --- | --- | --- |
| 1 | DOT | 42dot |
| 2 | DREAM | 소프트웨어드림센터 |
| 3 | UNSET | 없음, 길안내 비활성화 |

여행·데이트의 모든 단계에 동일하게 적용한다. Debug/Pleos 여부에 따라 순서가 바뀌던 옛 계산식을 제거했다. Mock 빌드도 같은 목적지 데이터로 시뮬레이션하지만 SDK를 호출하지 않으며, 화면 메시지에서도 이를 구분한다.

공식 샘플의 기존 OfficialNavigationTestCatalog를 그대로 재사용했다.

- 42dot: `(37.48544722, 127.03636666)`, POI `36khp37hh8gqcm5nqhe5`, sub-ID `0`.
- 소프트웨어드림센터: `(37.41275833, 127.09481944)`, POI `36khp2hdj2cfcm5nqhe3`, sub-ID `0`.

주소와 provider도 카탈로그에서 그대로 사용한다. 새 좌표나 POI를 생성하지 않았다. 설치된 내비게이션의 POI 지원 및 경로 성공은 이번 작업에서 확인하지 않았다.

시연 안내: **시연 모드 · 추천 장소와 실제 길안내 목적지가 다릅니다.**

버튼은 `42dot으로 길안내 테스트`, `소프트웨어드림센터로 길안내 테스트`처럼 실제 목적지를 표시한다. 가격·시간·주차·동선·사진은 예시임을 명시하며 실제 운영 정보로 보장하지 않는다. 관광지 후보들은 전국 예시이므로 실제 인접 코스 계산이나 최적 경로 추천은 수행하지 않는다.

## 실제 수행한 간단한 점검

- Kotlin 59개 파일에서 문자열/주석/괄호 균형과 대체 문자 유무를 간단한 스캐너로 확인했다. Kotlin 컴파일러 또는 완전한 구문/타입 검사에 해당하지 않는다.
- Mock 18후보의 ID 중복 없음, 6단계 각각 DOT/DREAM/UNSET 매핑을 정적으로 확인했다.
- 바뀐 화면의 콜백과 NavHost 연결, 모드별 단계 참조를 수동 점검했다.
- 좁은 창의 동일 방향 중첩 스크롤을 제거했다. 실제 런타임 측정은 하지 않았다.
- 변경 소스 대상 `git diff --check`는 공백 오류 없이 완료했다.
- 단위/UI 테스트 코드는 새 동작에 맞게 작성했으나 실행하지 않았다.
- 빌드 성공, SDK 실제 길안내 성공, UI 잘림 없음, 분할화면 회귀 없음은 **미검증**이다.

## 사용자가 진행할 확인

- Pleos SDK 빌드 및 Mock 빌드의 컴파일, 단위 테스트.
- 여행/데이트의 모든 단계와 후보, 상세 정보 변경, 다음 추천 순환, 마지막 코스 완료.
- 모드 왕복, Activity 재생성, 외부 내비게이션에서 복귀했을 때 모드·단계·선택 유지.
- 전체 화면 및 분할 창, 짧은 가로 창/큰 글꼴에서 버튼·글자 잘림과 스크롤 동작.
- 각 모드의 후보 1 → 42dot, 후보 2 → 소프트웨어드림센터 실제 요청/콜백, 후보 3 버튼 비활성.
- SDK 연결/권한 오류, 경로 시간 초과와 재시도, 요청 중 연속 터치, 외부 목적지 변경 및 도착 신호.
- 주행 제한 시 입력 차단, 시스템 UI 유지, 기존 플랫폼 분할화면 동작.
- 실제 Pleos 내비게이션 표시와 두 앱 동시 표시 여부. 앱이 자동 분할을 보장하지 않는다.

## 향후 AI·장소 정보 연동

1. RecommendationRepository의 실제 구현을 추가하고 RepositoryModule의 Hilt binding을 교체한다. UI를 모드별로 복제할 필요는 없다.
2. AI 결과를 Place로 변환하고 장소 API의 검증된 상세 정보와 이미지 자산/URL을 연결한다. 실제 사진 로딩과 출처/캐시 정책도 별도로 구현한다.
3. DemoRouteMapping을 실제 장소 Destination으로 교체한다. 시연 고정 배너·길안내 테스트 문구·예시 정보 표시는 데이터 출처에 맞게 변경한다.
4. 실제 거리·이동 시간·지역 연결성·주차/영업 정보는 검증된 API에서 제공받는다. 현재의 예시 값으로 계산하지 않는다.
5. 직렬화된 추천 모델을 변경할 때 SavedState 스키마 버전/마이그레이션을 갱신한다. 현재 구형 fixture는 재생성하되 자동 길안내는 수행하지 않는다.

이전 DEVELOPMENT_ROUTING 문서의 순번별 순환 매핑과 추천 변경 시 자동 SDK 요청 설명은 과거 구현 기록이다. 현재 동작은 이 문서를 따른다.
