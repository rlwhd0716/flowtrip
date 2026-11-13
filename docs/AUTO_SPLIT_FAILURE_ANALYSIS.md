# 자동 분할 실패 재확인

> 현재 자동 분할 실험은 종료하고 제품 코드에서 제거했습니다. 이 문서는 과거 시도/실패 기록입니다. 현재 적용 상태와 확인 항목은 [화면 정리 기록](SCREEN_CLEANUP.md)을 따릅니다.

## 결론

사용자의 자동 분할 실패 보고 후, 연결된 Pleos Connect API 34 AVD의 설치된 앱으로 확인했다. **현재 `NEW_TASK | LAUNCH_ADJACENT` 구현은 이 환경에서 자동 두 앱 배치를 만들지 못한다. 자동 분할 미해결 상태다.**

이번에는 설치된 앱의 UI와 adb 읽기 조회로 재현했다. 소스 보완 후의 빌드·Lint·테스트·재설치는 수행하지 않았다. 과거의 수동 창 바 드래그 성공을 자동 분할 성공으로 간주하지 않는다.

## 확인한 서로 다른 두 원인

### 1. 일반 추천 버튼은 창 요청까지 실행되지 않음

최초 AVD 화면은 여행 모드의 ‘골목의 작은 식탁’ 추천이었다. 화면에 `Mock 장소는 실제 내비게이션으로 전송할 수 없습니다. 실제 장소 데이터 연동이 필요합니다.` 오류가 보였다.

`RequestNavigationUseCase.course`에서 실제 장소 source/POI 검사를 통과하지 못하면 `prepareWindow`를 호출하지 않는다. 따라서 이 시연 장소 버튼으로 관찰한 ‘분할 안 됨’은 Intent 플래그 실행 결과가 아니다. 이전 UI가 이 버튼을 실제 안내처럼 활성화한 것이 혼동을 만들었다. Mock 데이터를 실제 장소로 위장하거나 좌표를 임의로 바꾸지 않았다.

### 2. 실제 샘플에서도 플래그 전달 후 두 창이 만들어지지 않음

같은 앱의 실제 장소 테스트 패널을 열어 소프트웨어드림센터 길안내를 요청했다. 이후 `dumpsys activity activities`에서 내비 Activity의 Intent를 확인했다.

```text
act=android.intent.action.MAIN
cat=[android.intent.category.LAUNCHER]
flg=0x10001000
pkg=ai.umos.maps.android.navigation.app
cmp=ai.umos.maps.android.navigation.app/.MainActivity
```

`0x10001000`은 `NEW_TASK | LAUNCH_ADJACENT`이므로 플래그 미전달이 원인이 아니다. 전환 뒤 창 상태는 다음과 같았다.

| 앱 | 실제 bounds | 표시 상태 |
| --- | --- | --- |
| OEM 내비 | Rect(853,90-2535,1300) | visible/reportedVisible=true |
| FlowTrip | Rect(853,90-2535,1300) | visible/reportedVisible=false |

실제 화면도 내비 실행 화면이 앱 영역 전체를 덮었고 FlowTrip이 함께 보이지 않았다. Android multi-window 플래그는 두 앱 동시 표시의 증거가 아니다. 이번 전환에서 새 내비 task도 관찰됐다(t1000067, 기존 t1000063 별도 존재). `NEW_TASK`만으로 기존 내비 task가 반드시 재사용된다고 보장할 수도 없다. `MULTIPLE_TASK`를 추가해 해결된다고 가정하지 않는다.

참고용 읽기 조회 발췌: `.gradle/auto-split-failure-state.txt` (로컬 진단 기록, 버전 관리 대상 아님).

## 소스에서 보완한 부분

- Pleos에서 현재 추천이 Mock 시연 장소일 때 이를 카드에 명시하고 ‘이 장소로 길안내’를 비활성화했다. Mock 개발 빌드의 추천 시뮬레이션은 유지한다.
- Pleos에서는 기존 실제 장소 테스트 패널을 기본 펼침으로 표시해 실제 목적지 요청과 시연 추천을 구분한다.
- 테스트 패널의 ‘목적지만 요청한다’는 낡은 안내를 현재 흐름(최초 옆 창 표시 요청 + 목적지)으로 고쳤다.
- `NavigationWindowSession`에 화면 숨김 이력을 추가했다. 설정 변경이 아닌 `MainActivity.onStop`에서 이력을 기록한다. 숨겨진 뒤 후속 추천/코스 변경은 단지 과거 Intent 전달 이력이 있다는 이유로 창 준비 성공을 반환하지 않고 오류를 반환한다. 명시적 길안내/재시도 버튼은 다시 요청할 수 있다.
- 창 요청 안내는 두 앱 동시 표시를 보장하지 않는다고 명시한다. 내비가 FlowTrip을 덮으면 분할이 적용되지 않은 상태임을 설명한다.

이 보완은 **실패 상태/시연 데이터 UI 수정이며 자동 분할 성공 수정이 아니다.** 임의의 창 강제 배치, 비공개 API, 권한 우회, 추가 task 생성 플래그를 해결책으로 넣지 않았다.

## 근거와 남은 작업

- [Android 인접 창 실행](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode)은 공개 요청 방식을 설명하지만 Pleos 자체 창 정책의 동작 보장은 아니다.
- [Pleos Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)는 앱 실행 시 기존 창 방향을 계승하며 App-to-App 실행은 기존 앱을 덮을 수 있다고 설명한다.
- NaviHelper 3.0.5의 확인된 API에서 두 앱을 함께 배치하거나 창 크기를 지정하는 전용 공개 API는 아직 확인되지 않았다.

다음으로 필요한 것은 **Pleos가 지원하는 자동 두 앱 배치 진입점 또는 공식 지원 여부 확인**이다. 공개 Intent 플래그 재전달만으로 해결됐다고 보고할 수 없다. 전용 진입점이 확인되면 기존 Adapter 경계에서 교체해야 한다. 고정 portrait/런타임 방향 요청이 자동 두 앱 배치와 같다고 가정하지 않는다(과거 고정 portrait 실험도 미확정 후 제거됨).

일반 추천을 실제 경로와 연결하려면 별도로 실제 장소 공급자/POI 연동도 필요하다. 이 원인을 플랫폼의 분할 요청 문제와 섞지 않는다.

