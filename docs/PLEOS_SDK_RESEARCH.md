# Pleos SDK 조사 결과

확인일: 2026-11-10 (Asia/Seoul). 공식 문서, 공개 Maven의 POM/AAR, AAR의 공개 클래스 시그니처를 대조했다.
아래 버전은 이번에 확인한 배포본/문서 기준이며 모든 차량 플랫폼에서의 지원을 보장하는 뜻은 아니다.

## 1. SDK 및 배포 방식

공통 Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`.
이 환경에서는 아래 배포본의 POM/AAR를 인증 정보 없이 HTTP 200으로 받았다. 프로젝트에 외부 AAR 파일을 복사하지 않고 Gradle Maven 의존성을 사용한다.

| SDK | 공식 문서 / 확인 버전 | Gradle 좌표 | 이번 범위 |
| --- | --- | --- | --- |
| NaviHelper | 3.0.5 | `ai.pleos.playground:NaviHelper:3.0.5` | 실제 Adapter 및 SDK 빌드 |
| FusedLocation | 가이드 1.1.2, 공식 샘플 및 공개 AAR 1.1.3 | `ai.pleos.playground:FusedLocation:1.1.3` | 실제 Adapter 및 SDK 빌드 |
| TextToSpeech | 3.3.1 | `ai.pleos.playground:TextToSpeech:3.3.1` | 조사만, 프로젝트 의존성 미추가 |
| SpeechToText | 3.3.1 | `ai.pleos.playground:SpeechToText:3.3.1` | 조사만, 프로젝트 의존성 미추가 |
| Vehicle | 릴리스 노트 2.3.1, 현재 설치 가이드 및 공개 AAR 2.3.2 | `ai.pleos.playground:Vehicle:2.3.2` | 조사만, 프로젝트 의존성 미추가 |

근거: [릴리스 노트](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-release-note),
[NaviHelper 설치](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk),
[FusedLocation 설치 가이드](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup),
[공식 FusedLocation 샘플](https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app),
[TTS 설치](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk),
[STT 설치](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk),
[Vehicle 설치](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup).

### 확인한 불일치와 선택 근거

- FusedLocation 1.1.2의 정확한 POM/AAR URL은 HTTP 404였다. 1.1.3은 공식 샘플에 명시되어 있고 다운로드 및 컴파일이 가능했다. 가이드의 버전 문자열만 보고 1.1.2를 강제하지 않았다.
- Vehicle은 릴리스 노트와 현재 설치 문서의 버전이 다르다. P2 구현 전에 실제 대상 차량/에뮬레이터 서비스와 배포 기준을 확정해야 한다. 이번에는 2.3.2의 API 조사만 했다.
- NaviHelper 3.0.5의 POM은 `navi-data:3.0.4`, `navi-aidl:3.0.1`을 참조한다. 이 전이 의존성 버전을 임의로 NaviHelper 버전에 맞추지 않았다.
- FusedLocation 1.1.3은 `fused-location-data:0.0.3`, `fused-location-aidl:0.0.5`를 참조한다. Gradle이 공식 POM대로 처리한다.

내려받은 AAR SHA-256:

```text
NaviHelper-3.0.5.aar    15FADD11C7C611B1ABDF7EBF9CE91F2A6CE2728E60B4E38B4343E11BB9C12E97
FusedLocation-1.1.3.aar 20FF6DC2EFBEA53F32D1CE4712D517FC4ABAC3A9CE4B990F4ACA52C35EBEF081
```

## 2. P0 NaviHelper

실제 진입 클래스는 `ai.pleos.playground.navi.helper.NaviHelper(Context)`다.
`initialize(onConnected, onFailed)` 연결 완료 후 동일한 `NaviHelperEventListener`를 등록하고, 종료 시 해당 인스턴스를 제거하고 `release()`한다.
해제한 객체는 재사용하지 않는다. 근거: [초기화/해제](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release).

| 확인한 API | 처리 / 이번 구현 |
| --- | --- |
| `getDestinationInfo()` | 반환값 대신 `onDestinationInfo(DestinationInfo)`에서 Domain 목적지로 변환 |
| `getRouteStateInfo()` | `onRouteStateInfo(RouteStateInfo)`의 `isRouting` 및 `naviMode` 확인 |
| `getCurrentLocationInfo()` | `onCurrentLocationInfo(CurrentLocationInfo)` 수신. 측정 시각이 없어 STALE 대체 위치로만 사용 |
| `requestRoute(RouteInfo)` | 사용자 승인 및 실제 출처/POI 정보 확인 후 호출. 호출 반환은 성공이 아님 |
| `requestNavigationScreen(NavigationScreenRequest.Foreground)` | 추천 화면의 지도 열기. 실제 내비게이션 화면 표시 요청이며 목적지 변경이나 앱 내부 지도 삽입이 아님 |
| `addListener` / `removeListener` | 전경 세션 시작/종료에 맞춰 동일 인스턴스 관리 |
| `addWaypoint(RequestWaypointInfo)` / `getWaypointInfo()` / `removeWaypoint(WaypointIndex)` | 실제 시그니처 확인, P0 UI에서는 호출하지 않음 |
| `requestReRoute()` / `cancelRoute()` | 실제 시그니처 확인, 자동 경로 변경 기능 미구현 |

근거: [목적지](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations),
[API Reference](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/entry), 공개 Maven AAR.

이벤트는 `onRoutingCompleted()`, `onDestinationChanged()`, `onDestinationArrived(DestinationArrivedInfo)`,
`onWayPointArrived(WaypointArrivedInfo)`, `onError(NaviErrorCode)`를 확인했다. 대소문자, 인수 및 콜백 형태를 실제 AAR와 대조했다.
`onRouteEnded()` 대신 현재 완료 콜백을 사용한다. 이벤트 전달은 실제 Navigation 앱 상태/구현에 따라 달라질 수 있다.
[이벤트 가이드](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners).

경로 요청은 `onRoutingCompleted` 후 다시 받은 목적지와 경로 안내 상태를 함께 확인한다. 요청 장소와 좌표/POI가 다르거나 안내가 시작되지 않으면 실패로 처리한다.
결과가 없으면 25초 후 실패한다. SDK 이벤트에 앱 요청 ID가 없으므로 동시 외부 경로 변경 및 콜백 순서는 Pleos 환경에서 추가 검증해야 한다.
도착 이벤트는 추천 ViewModel의 `arrivalSignal`로 전달되며, 방문 완료·자동 단계 진행·자동 재추천을 유발하지 않는다.

`RouteInfo` 생성자의 위도/경도 순서를 3.0.5 AAR에서 확인했다. POI ID/sub-ID는 non-null이다.
아직 가상 추천 장소밖에 없으므로 실제 Adapter는 Mock 출처를 거부한다. 실제 장소 공급자가 제공한 좌표와 식별 정보가 있어야 경로 요청이 가능하다.

## 3. P0 FusedLocation

진입 클래스는 `ai.pleos.playground.location.fused.FusedLocation(Context)`다.
`isSupported()` 확인 → 권한 확인 → `initialize()` → `getLastKnownLocation(listener)` 및
`registerFusedLocationCallback(listener)` 순서로 사용한다. 종료 시 같은 listener를 unregister한 다음 release한다.
[SDK 수명주기](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release).

1.1.3 AAR의 `LocationData`에서 `latitude`, `longitude`, `timestampMillis`를 확인했다. Domain에는 이 값과 출처만 전달하고 SDK 타입을 노출하지 않는다.
`onChanged(LocationData?)`의 null, `onError(Exception)`, 권한 취소, 미지원 상태를 별도로 처리한다.
마지막 위치 조회는 새 측정을 수행하지 않는다. [위치 데이터 가이드](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access).

- 위치 유효 기간은 앱의 초기 정책으로 60초. 미래 시각·측정 시각 0·만료는 신뢰 가능한 최신 위치에서 제외한다.
- 추가 콜백이 오지 않아도 유효 시간이 끝나면 STALE이 된다. 이 기간은 Pleos SDK 보장이 아닌 앱 정책이다.
- FusedLocation **미지원**일 때만 NaviHelper 대체 조회를 검토/사용한다. 권한 거부나 개인정보 차단을 우회하려고 대체 조회하지 않는다.
- NaviHelper의 측정 시각이 불명확하므로 수신 시간을 측정 시간으로 조작하지 않는다. 대체 위치는 STALE이고 근접 판단에서 제외된다.
- 신선한 동일 출처 좌표에만 직선 거리 150m 기준의 근접 힌트를 제공한다. 경로 거리나 방문 완료 판정이 아니다.
- `addProximityAlert` / `removeProximityAlert`는 실제 SDK에서 확인했지만 이번 P0에서는 호출하지 않는다.
- 앱이 전경을 벗어나면 리스너 및 SDK를 해제하고 위치 상태를 비운다. 서버 전송·DB·파일 저장·백그라운드 서비스는 없다.

## 4. 권한

| 정확한 Manifest 이름 | 필요 API | 보호 수준 확인 | 이번 추가 |
| --- | --- | --- | --- |
| `pleos.car.permission.NAVI_ROUTE` | 내비게이션 정보 조회 | 공개 가이드에서 보호 수준 미확정. 실행 기기의 PermissionInfo 확인 필요 | 실제 빌드만 |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 목적지 경로 요청 | 동일 | 실제 빌드만 |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | 내비게이션 화면 표시 요청 | 대상 기기의 PermissionInfo로 보호 수준 확인 | 실제 빌드만 |
| `pleos.car.permission.FUSED_LOCATION` | 위치 조회/콜백 등록·해제 | 공식 문서에서 dangerous 확인 | 실제 빌드만 |
| `pleos.car.permission.TTS_SERVICE` | 음성 합성 서비스 | 해당 가이드에 보호 수준 미명시 | 미추가 |
| `pleos.car.permission.STT_SERVICE` | 음성 인식 서비스 | 해당 가이드에 보호 수준 미명시 | 미추가 |
| `pleos.car.permission.CAR_ENERGY` | Fuel / EvBattery 조회 | 대상 플랫폼에서 보호 수준 확인 필요 | 미추가 |
| `pleos.car.permission.CAR_DRIVE` | Odometer 조회 | 동일 | 미추가 |
| `pleos.car.permission.CAR_INFO` | CarInfo 조회 | 동일 | 미추가 |
| `pleos.car.permission.CAR_SAFETY` | Safety 조회 | 동일 | 미추가 |

근거: [NaviHelper 권한](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions),
[위치 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions),
[TTS 권한](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions),
[STT 권한](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions),
[Vehicle 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission).

실제 차량에서 정의된 dangerous 권한만 런타임 요청하고 각 Adapter에서 실제 승인 상태를 다시 검사한다.
정의되지 않은 권한이나 signature 권한을 dangerous라고 가정하지 않는다. 선언만으로 승인된 것으로 간주하지 않는다.
FusedLocation AAR 자체가 선언한 서비스 binding 권한과 패키지 queries는 공식 라이브러리 Manifest에서 병합된다.
Google의 `ACCESS_FINE_LOCATION` 등은 Pleos 문서의 보장과 다르므로 임의 추가하지 않았다.

## 5. P1 음성: 조사만

### TextToSpeech 3.3.1

공개 AAR에서 `initialize`, `addEventListener`, `setLocale`, `speak`, `stop`, `removeEventListener`, `release`를 확인했다.
한국어 `ko-KR` / `Locale.KOREA`를 공식적으로 명시한다. [언어 설정](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale).
실제 연동 시 요청을 직렬화하고 새 요청/화면 종료 시 재생 중단 및 동일 문장 중복 방지 정책을 별도로 구현할 계획이다.
미지원·권한 오류는 화면 안내로 대체하며, 이 기능은 아직 구현하지 않았다.

SERVER 및 서버를 사용하는 HYBRID 모드는 My Project의 Client ID/Secret으로 초기화 후 `registerApp` 인증이 필요하다.
ON_DEVICE 전용은 서버 등록이 필요 없다고 명시한다. 자격 증명을 소스에 넣지 않는다.
[앱 등록](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration).

### SpeechToText 3.3.1

공개 AAR에서 `initialize`, `addListener(ResultListener)`, `setLocale`, `request`, `stop`, `removeListener`, `release`를 확인했다.
`ResultListener.onUpdated(String, Boolean)`을 포함한 실제 결과 인터페이스가 있으며, Boolean 의미/최종 결과 조건은 다음 구현 시 최신 KDoc 및 서비스 동작과 다시 확인한다.
한국어 지원은 [언어 설정](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale)에 명시된다.
아직 마이크 권한 요청, 인식 세션, 문자열 파서는 구현하지 않았다.

향후 최종 인식 문자열만 Domain 명령 해석기로 전달할 계획:

| 앱 내부 명령 | 예시 | 후속 처리 원칙 |
| --- | --- | --- |
| NEXT_RECOMMENDATION | 다른 곳 추천해 줘 | 현재 단계 후보 변경 |
| SKIP_PLACE | 여기는 건너뛰자 | 현재 단계 건너뛰기 |
| SEARCH_PLACE | 가까운 카페로 가자 | 검색 요청 의미로만 변환, 목적지 자동 변경 금지 |
| CHANGE_CATEGORY | 야경 볼 수 있는 곳 찾아줘 | 추천 카테고리 요청 |
| UNKNOWN | 기타 | 실행하지 않고 확인 안내 |

Pleos 음성 서비스 및 오디오 입력을 제공하는 에뮬레이터/차량에서 검증해야 한다. 일반 Pixel AVD의 Google 음성 서비스는 Pleos SDK 검증을 대체하지 않는다.

## 6. P2 Vehicle: 조회 계획만

`Vehicle(Context).initialize()` 후 `getFuel`, `getEvBattery`, `getOdometer`, `getCarInfo`, `getSafety` 도메인을 얻는 API와 `release()`를 AAR에서 확인했다.
다음은 2.3.2 공개 AAR에서 확인한 조회 콜백의 JVM 타입이다. null 및 미지원 응답을 처리해야 하며 값의 단위·정상 범위·차량별 제공 여부는 실제 플랫폼 기준으로 확정해야 한다.

| 도메인 | 확인한 읽기 API / 콜백 값 | 활용 계획 |
| --- | --- | --- |
| Fuel | `getFuelPercent` → Integer, `getCurrentFuelEconomy` → Float | 연료 기반 주유소 후보 제안 |
| EvBattery | `getEvBatteryStateOfCharge` → Integer, `getEvChargeSwitchState` → Boolean | 충전소 후보 제안 |
| Odometer | `getCurrentDistanceDriven` → Float, `getTripCurrentDrivingTime` → Integer | 운행 통계, 휴식 제안 |
| CarInfo | `getVINInfo` → String | 필요한 경우만 차량 정보 조회. VIN 수집/저장 계획 없음 |
| Safety | `getTelltalesInfo` → Telltales | 경고 상태의 참고 안내. 진단·안전 보장으로 표현 금지 |

근거: [Fuel](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/fuel/entry),
[EvBattery](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/ev-battery/entry),
[Odometer](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/odometer/entry),
[CarInfo](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/car-info/entry),
[Safety](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/safety/entry).

각 기능의 `check...Capabilities`를 먼저 호출한다. `CapabilityConfig.accessType`이 READ 또는 READ_WRITE인지 확인하고, NONE 또는 WRITE 전용이면 조회 기능을 비활성화한다.
예: `checkFuelPercentCapabilities`, `checkEvBatteryStateOfChargeCapabilities`, `checkVINInfoCapabilities`.
[Capability 가이드](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability).
지원하는 차량/Virtual Console에서 각 데이터·null·권한 오류를 검증해야 한다. 일반 AVD나 모든 차량에서 동일 값이 제공된다고 가정하지 않는다.
이번에는 Vehicle 의존성·권한·실행 코드·제어 API를 추가하지 않았다.

## 7. 주행 안전 / 앱 카테고리

[Pleos 주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)는
AAOS `CarUxRestrictionsManager`의 현재 제한과 변경 리스너를 사용하도록 명시한다.
앱은 Mock/실제 옵션 모두 Automotive 환경에서 해당 manager를 등록/해제하며, GPS 속도는 제한 판단에 사용하지 않는다.
DO 미선언 정차용 화면은 `isRequiresDistractionOptimization`이 true이면 복잡한 설정/추천 화면을 차단한다.
개별 `activeRestrictions` 플래그는 DO 화면의 기능별 제한에 사용하는 값이며 정차용 화면의 별도 접근 조건으로 사용하지 않는다.
[AAOS 정차용 앱 UX 지침](https://developer.android.com/training/cars/parked/automotive-os).
연결 끊김·알 수 없음도 차단하며, DO를 선언하지 않는다.

[지역별 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)에 따르면 DO 미선언 앱은 주행 시 사용 제한을 받는다.
화면 조작이 허용된다는 임의 가정이나 버튼 하나로 해제하는 UI를 제공하지 않는다.
[Maps/Navigation 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide)은 카테고리 선언 일치,
단일 경로, 명시적 경로 전환 및 주행 중 UX 제한을 요구한다.
FlowTrip은 현재 OEM 내비게이션에 목적지를 전달하는 동행 서비스이며, 자체 Maps/Navigation 앱으로 승인받은 상태가 아니다.
`APP_MAPS`를 임의로 추가하거나 경로/음성 소유권을 얻었다고 표시하지 않았다. 최종 카테고리는 공모전/배포 담당자와 확정해야 한다.

## 8. 외부 준비 및 미확정 사항

- Pleos 개발자 계정, My Project 및 실제 서비스/배포 승인 요건을 확정해야 한다. 이번 P0의 공개 Maven 다운로드에는 별도 자격 증명을 사용하지 않았다.
- Pleos Connect Emulator 이미지/Virtual Console 및 실제 차량 서비스가 필요하다. 이 환경에는 일반 Pixel AVD만 있다.
- 공식 시작 가이드는 제공받은 시스템 이미지 URL로 API 34 Pleos AVD를 구성하도록 안내한다. 임의의 다운로드 URL을 만들지 않았다.
  [공식 시작 가이드](https://document.pleos.ai/docs/pleos-only/getting-started).
- 일부 API Reference 페이지는 텍스트 추출에서 제목/메뉴만 제공해 세부 단위 설명을 확인할 수 없었다. 메서드/필드 타입은 공개 AAR로 검증했다.
- 공개 create-project 페이지의 텍스트만으로 모든 승인 조건을 확인할 수 없었다. 계정의 Workspace/SDK 접근 범위는 별도 확인이 필요하다.
- 실제 차량에서는 권한 protectionLevel, 부분 거부/철회, 개인정보 위치 차단, SDK/서비스 버전, 콜백 순서·중복·연결 끊김·복귀,
  타 앱 목적지 변경, 위도/경도, 측정 시각 기준, 사용자 UX 제한, 리스너 해제 및 장시간 안정성을 추가 검증해야 한다.
- 가상 장소가 실제 목적지로 전달되지 않는지, 실제 POI 데이터 도입 후에도 목적지 승인 절차가 유지되는지 확인해야 한다.
