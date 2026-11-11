# 실제 목적지 길안내 테스트

2026-11-11. 추천 API를 연결하기 전에 실제 NaviHelper 요청을 시험하기 위한 고정 목적지 2곳을 추가했다.

## 사용법

1. 기본 Pleos SDK 빌드로 실행한다 (`pleosSdk=true`). 주차/UX 허용 상태 및 내비게이션 초기 설정·약관 동의가 필요하다.
2. 홈 → 데이트 모드 → 데이트 시작으로 추천 화면에 들어간다. 여행 모드에서도 동일하다.
3. 상단 **실제 장소 길안내 테스트**를 펼친다.
4. **42dot 길안내** 또는 **소프트웨어드림센터 길안내**를 누른다. 이 동작은 실제 목적지를 바꾼다. 식당·야경·카페 추천은 바꾸지 않는다.
5. 경유지 시험은 **소프트웨어드림센터 경유 → 42dot**을 누른다.
6. 내비게이션이 보이지 않을 때만 **테스트 내비게이션 화면 요청**을 누른다. 두 앱 배치는 Pleos window bar에서 조절한다. 목적지 변경 버튼 자체는 화면 실행/복귀 Intent를 호출하지 않는다.
7. 성공 문구와 하단 현재 목적지·경유지·경로 상태를 확인한다. 성공은 SDK의 탐색 완료, 일치하는 목적지/경유지 및 안내 모드를 대조한 결과이다. 실제 지도 화면 및 TBT/음성 동작도 직접 확인해야 한다.

Mock 빌드에서는 같은 버튼이 Mock 내비게이션에서만 시뮬레이션된다. 실제 안내는 제공하지 않는다. 주행 중 UX 제한을 해제하는 기능은 추가하지 않았다.

## 데이터 출처

[공식 NaviHelper 샘플의 NaviTestData.kt](https://github.com/Pleos-Playground/pleos-sample-navi-app/blob/main/app/src/main/java/ai/pleos/playground/navidevapp/data/sample/NaviTestData.kt)의 요청 값을 그대로 사용했다. 제공자는 공식 샘플과 동일한 SDK 기본값(빈 문자열)이다.

| 이름 | 위도 / 경도 | POI ID | Sub ID | 주소 |
| --- | --- | --- | --- | --- |
| 42dot | 37.48544722 / 127.03636666 | 36khp37hh8gqcm5nqhe5 | 0 | 서울시 강남구 남부순환로 2621 |
| 소프트웨어드림센터 | 37.41275833 / 127.09481944 | 36khp2hdj2cfcm5nqhe3 | 0 | 경기도 성남시 수정구 창업로40번길 6 |

공식 문서의 일부 경유 예시는 동일 좌표/POI를 다른 이름·주소로 설명한다. 이 구현은 공식 실행 샘플 코드의 이름과 주소를 기준으로 통일했다. 현재 내비게이션 POI DB의 유효성, 실제 입주/영업 여부, 출입·주차 가능 여부까지 확인된 데이터는 아니다. 경로 성공 여부는 설치된 내비게이션 결과로 판단한다. 가상 장소의 source만 변경하거나 임의 POI를 발급하지 않았다.

두 곳은 업무시설이므로 식당·야경·카페 추천으로 표시하지 않는다. 고정 테스트 목적지이며 지역별 실제 장소 검색/추천 API는 여전히 별도 작업이다.

## 구현과 복원

- Domain `NavigationTestCatalog` / Data `OfficialNavigationTestCatalog`와 Hilt 바인딩으로 목적지를 분리했다. SDK 타입이 UI로 노출되지 않는다.
- 기존 `RequestNavigationUseCase`와 실제 Pleos Adapter의 권한·UX·목적지·경유지 확인을 그대로 사용한다.
- 기존 추천 후보/단계/기록을 유지하고, 테스트 중 중복 조작을 막는다.
- 실패/중단 시 테스트 목적지와 경유 요청 여부를 SavedStateHandle에 저장한다. 복원 시 자동 전송하지 않으며 사용자 재시도로 같은 요청을 수행한다.
- 신규 라이브러리나 SDK 버전 변경 없음.

## 검증

- 단위 테스트 **37개 통과** (기존 33 + 테스트 목적지/경유지 요청의 추천 보존, 복원 후 재시도, UX 제한 3개 + 경로 교체 중 취소 후 새 완료/상태 확인 1개).
- Mock 테스트 APK, Pleos 앱/테스트 APK 및 Lint 통과. Lint 오류 0개.
- Pleos_Connect_v2.1.0(emulator-5554)에 앱/테스트 APK를 설치해 실제 SDK 요청 실행. 첫 소프트웨어드림센터 요청은 UX 허용, Service READY, 권한 검사를 통과했지만 25초 동안 목적지·안내 상태 확인이 되지 않아 스모크 테스트 실패로 기록했다.
- 기본 내비게이션을 정상 Launcher Intent로 표시한 뒤 **소프트웨어드림센터 목적지, 안내 종료 버튼, TBT 안내가 활성화된 화면**을 직접 확인했다. 지도 배경 타일은 비어 있었다. 따라서 요청 전달 및 내비게이션 표시와 SDK 콜백 기반 성공을 구분한다. 정식 도로 경로·음성 안내·경유지 성공까지 검증된 것은 아니다.
- [공식 SDK 릴리즈 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note)에 따라 3.0.5에서 유지되는 구형 onRouteEnded도 완료 신호로 처리하도록 보완했다. onRoutingCompleted와 동일하게 목적지/경유지/안내 상태를 재조회하며 성공 조건을 완화하지 않았다.
- Debug에 한해 FlowTrip.Navi 태그로 연결·요청·완료/조회 콜백 종류를 기록한다. 현재 위치나 좌표는 로그에 추가하지 않았다.
- 목적지 교체 중 onRouteCancelled가 먼저 수신되면 기존 안내 종료 상태를 반영하고 새 완료/상태 결과를 기다리도록 보완했다. 취소 자체로 성공 처리하지 않으며, 새 결과가 없으면 시간 초과로 실패한다.
- 최신 소스의 Pleos 앱/계측 APK 빌드, 단위 테스트 37개 및 Lint(오류 0개) 완료. 앞선 AVD에서는 구버전 스모크 테스트가 시간 초과/취소로 실패했으며 최신 보완 APK의 성공을 확인한 상태는 아니다.
- 재개 시 연결 AVD가 없어 Android Studio에서 기존 Pleos AVD를 부팅했다. 직접 CLI 실행은 그래픽 초기화 중 종료되었고, IDE 경로에서는 부팅됨. USB 디버깅 허용 화면은 사용자가 직접 처리해야 하므로 현재 최신 APK 설치/최종 안내 검증은 대기 중이다.
- APK 설치 또는 SDK 호출 수락만으로 안내 성공을 주장하지 않는다.

명시적 실행 전용 `PleosNavigationSmokeTest`는 일반 테스트에서는 건너뛴다. 물리 차량에서 실행하지 않으며 `Build.MODEL`에 emulator가 포함된 경우에만 실행한다. 별도 허가 인수 `runRealNavigation=true`가 있어야 실제 경로를 요청한다.

```powershell
# Pleos 앱/테스트 APK 설치 후, 에뮬레이터에서만 실행
C:/src/androidSDK/platform-tools/adb.exe -s emulator-5554 shell am instrument -w -e class com.github.flowtrip.PleosNavigationSmokeTest -e runRealNavigation true com.github.flowtrip.test/androidx.test.runner.AndroidJUnitRunner
```

## 이번 실제 목적지 기능의 변경 파일

기준 경로는 `app/src/main/java/com/github/flowtrip/`이다.

- 신규: `domain/repository/NavigationTestCatalog.kt`, `data/repository/OfficialNavigationTestCatalog.kt`, `presentation/recommendation/NavigationTestPanel.kt`
- 수정: `core/di/RepositoryModule.kt`, `core/navigation/FlowTripNavHost.kt`, `presentation/recommendation/RecommendationViewModel.kt`, `RecommendationScreen.kt`, `presentation/common/ScreenLayout.kt`, `data/mapper/RouteConfirmation.kt`
- 수정: `app/src/pleos/java/com/github/flowtrip/data/repository/PleosNavigationRepository.kt`
- 신규: `app/src/androidTest/java/com/github/flowtrip/PleosNavigationSmokeTest.kt`
- 수정: `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt`, `platform/RouteConfirmationTest.kt`
- 문서: `README.md`, `docs/PROGRESS.md`, `docs/SPLIT_SCREEN_INTEGRATION.md`, 신규 `docs/REAL_NAVIGATION_TEST.md`
