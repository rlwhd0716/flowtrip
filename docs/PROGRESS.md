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
