# FlowTrip 개발용 카카오 검색 프록시

Node.js 22 이상만 필요하며 추가 패키지를 설치하지 않는다. 앱은 카카오 REST 키를 갖지 않고 PC의 프록시를 호출한다. 프록시는 `127.0.0.1:8787`에만 바인딩하며 주소/키워드 검색 두 경로만 카카오의 고정 HTTPS upstream으로 전달한다. 서버는 키나 검색 좌표를 로그로 출력하지 않는다.

## 로컬 설정

루트 `secrets.properties` (Git 제외, Android 빌드에서 읽지 않음):

```properties
KAKAO_REST_API_KEY=발급받은_REST_API_키
```

또는 서버 프로세스의 `KAKAO_REST_API_KEY` 환경변수를 사용한다. 네이티브 키/어드민 키는 사용하지 않는다.

루트 `place-proxy.local.properties` (Git 제외, Debug 설정만 적용):

```properties
debugPlaceProxyUrl=http://10.0.2.2:8787
autoStartPlaceProxy=true
```

위 설정이 있으면 Android Studio의 일반 Run/Debug 빌드가 `startLocalPlaceProxy`를 먼저 실행한다. 서버가 없으면 숨김 백그라운드로 시작하고, 있으면 상태를 확인하여 재사용한다. 이미 APK가 최신이어도 실행 상태를 다시 확인한다. 처음에는 Gradle Sync 후 기존 `app` 실행 구성을 사용한다. 에뮬레이터의 앱 아이콘만 누르거나 빌드 없이 실행하는 동작은 PC 자동 시작을 수행하지 않는다.

자동 모드는 앞서 Pleos에서 정상 응답을 확인한 `10.0.2.2` 경로를 사용하므로, 서버 시작 시점에 에뮬레이터가 아직 부팅 중이어도 ADB 연결을 기다리지 않는다. 앱에 포함되는 값은 프록시 주소뿐이다. 일반 Android 빌드는 `-PpleosSdk=false`, Pleos AVD/차량 SDK 빌드는 기존 기본 설정을 사용한다. 실제 데이트 검색은 최신 Pleos 위치가 필요하므로 일반 Android의 Mock 위치로 대체하지 않는다.

수동으로 서버만 시작해야 할 때 프로젝트 루트에서 실행:

```powershell
.\tools\place-proxy\start.ps1 -ServerOnly
```

스크립트는 프록시 상태를 `/health`로 확인하고, 없으면 숨김 백그라운드로 실행한다. `launch.mjs`는 서버의 입출력 핸들을 분리하므로 서버가 계속 실행돼도 빌드는 완료된다. 로그는 Git 제외 `proxy.stdout.log`/`proxy.stderr.log`와 자동 시작 진단용 `proxy-startup.log`에 기록한다.

**자동 시작은 Windows 개발 PC의 Debug 빌드에만 적용한다.** 다른 PC에는 로컬 설정이 없으면 기본 비활성이며 Release에는 자동 시작 작업을 연결하지 않는다. 임시 기능을 끄려면 로컬 설정의 `autoStartPlaceProxy=false` 또는 Gradle 옵션 `-PautoStartPlaceProxy=false`를 사용한다. 별도 OS 자동 실행 등록은 필요하지 않다.

이전 연결 시간 초과 당시 PC 8787 포트의 프록시가 실행되어 있지 않았다. 서버 실행 후 `10.0.2.2`와 ADB 경유 모두 Pleos에서 HTTP 200을 확인했다. 자동 모드는 ADB 재연결 의존성을 줄이기 위해 `10.0.2.2`를 사용한다. PC의 실호출 점검(`check-live.mjs`)은 잠깐 별도 프록시를 생성하고 종료하므로, 이 명령의 성공만으로 앱용 프록시가 계속 실행되는 것은 아니다.

USB 기기 등에서 ADB 경유가 필요한 경우 기존 수동 모드를 사용한다:

```powershell
.\tools\place-proxy\start.ps1 -DeviceId emulator-5554
.\gradlew.bat '-PdebugPlaceProxyUrl=http://127.0.0.1:8787' :app:assembleDebug
```

adb 연결 없는 실차는 이 로컬 주소를 사용할 수 없다. 별도 HTTPS 서비스가 필요하다.

자동 시작 검증: 서버가 없는 임시 포트에서 새 서버 시작 후 Gradle 작업이 정상 종료되고 서버가 유지됨을 확인했다. 기본 Debug 빌드에서도 작업 실행/기존 서버 재사용 후 빌드 통과를 확인했다. 검증용 서버만 종료하고 앱용 8787 서버는 유지했다. Release 작업 목록에는 자동 시작이 없다. Android Studio UI의 실행 버튼 자체를 자동 조작해 검증한 것은 아니며, 현재 기존 실행 구성의 Gradle 빌드 단계에 연결했다.

기존 Mock 개발로 전환:

```powershell
.\gradlew.bat '-PdebugPlaceProxyUrl=' -PplaceDataSource=mock :app:assembleDebug
```

## 테스트

```powershell
node --test tools/place-proxy/server.test.mjs
node tools/place-proxy/check-live.mjs
```

첫 명령은 외부 API를 호출하지 않는다. 고정 upstream, 서버 측 키 주입, 잘못된 경로/쿼리/Host/Origin 차단, 인증 실패·서비스 비활성화·연결 실패 처리를 검증한다. 둘째 명령은 실제 키를 읽어 주소 검색과 카페 검색을 각 1회 수행하며 키를 출력하지 않는다. 성공하려면 HTTP 200과 1개 이상의 documents가 필요하다. 실패 코드를 성공으로 바꾸거나 Mock 데이터를 반환하지 않는다.

앱의 HTTP 클라이언트는 `PlaceProxySmokeTest`로 별도 점검할 수 있다. 설치된 앱/계측 APK가 최신이고 서버가 실행 중일 때 다음을 실행한다. 이 테스트는 403 같은 카카오 오류가 그대로 전달되는 것도 검사하며, 통과만으로 장소 검색 성공을 의미하지 않는다. 출력의 `realSearchSucceeded=true`를 구분해야 한다.

```powershell
adb shell am instrument -w -e class com.github.flowtrip.PlaceProxySmokeTest -e runPlaceProxy true com.github.flowtrip.test/androidx.test.runner.AndroidJUnitRunner
```

이번 변경의 검증: Debug 앱/계측 APK 빌드, 단위 테스트 45개, 프록시 테스트 2개, Lint 오류 0·경고 31개, Release 리소스/BuildConfig 생성 통과. 생성 APK를 검사해 REST 키 미포함을 확인했다. Release의 로컬 HTTP 허용 값은 false이고 로컬 URL은 없다.

Pleos AVD는 연결됐으나 후속 APK 설치 및 PackageManager 조회가 응답 지연되어 명령을 중단했다. 위 계측 테스트는 실행하지 못했으며 앱 내 검색 UI와 Android→프록시 경로는 미검증이다. PC→프록시→카카오 경로에서는 서비스 비활성화 403 응답을 확인했다. 기존 기기 데이터를 삭제하거나 AVD를 초기화하지 않았다.

## 카카오 서비스 활성화 필요

2026-11-15 실제 호출 결과: 주소/키워드 검색 모두 HTTP 403, `NotAuthorizedError`, `App(FlowTrip) disabled OPEN_MAP_AND_LOCAL service.`

카카오디벨로퍼스에서 **FlowTrip 앱 → 카카오맵 → 사용 설정 → 상태 ON** 설정 후 위 실호출 테스트를 다시 실행해야 한다. 앱에도 해당 설정이 필요하다는 오류를 표시한다. 활성화 전 장소 검색 성공으로 보고하지 않는다.

후속 재시도에서 주소 검색은 HTTP 200/1건, 장소 검색은 HTTP 200/3건을 확인했다. 위 403은 과거 설정 단계의 기록이며 현재 PC의 실제 검색은 성공한다. 이번 연결 시간 초과는 카카오 API 응답 이전의 PC 프록시 접속 문제였다.

## Pleos 연결 시간 초과 수정 검증

- PC 8787 포트에 서버가 없었던 것을 확인하고 `start.ps1`로 프록시를 별도 백그라운드 프로세스로 시작했다. `/health` 및 실행 중인 프록시의 실제 장소 검색 HTTP 200/3건을 확인했다.
- `emulator-5554`의 ADB reverse 매핑을 확인했다. **Pleos 에뮬레이터 내부에서** 새 `127.0.0.1:8787` 경로와 기존 `10.0.2.2:8787` 경로 모두 HTTP 200/실제 장소 JSON을 수신했다. 이 검증은 Android shell의 소켓 요청이며 앱 UI 전체 시나리오와는 구분한다.
- 새 주소를 포함한 Debug 앱/계측 APK 빌드, 단위 테스트 45개, 프록시 테스트 2개, Lint 검증 통과. 메인 APK 설치는 `Success`를 확인했다. 계측 APK의 후속 설치 완료 응답이 지연되어 명령을 중단했으며 `PlaceProxySmokeTest` 실행 성공은 보고하지 않는다.
- 최초 앱을 빌드할 때 주소 변경이 적용된다. 이후 같은 주소로 개발할 때는 앱 재설치 없이 서버/ADB 연결만 재설정하면 된다. 현재 기존 주소에서도 서버가 실행 중이면 응답하므로 서버 가동 여부를 먼저 확인한다.

공식 안내: [카카오맵 사용 방법](https://developers.kakao.com/docs/ko/kakaomap/common), [로컬 검색 API](https://developers.kakao.com/docs/ko/local/dev-guide).

## 배포 범위

이 서버는 PC 로컬 개발용이며 인터넷에 공개하지 않는다. 공개 배포에는 서비스 인증/쿼터 제한과 HTTPS 프런트 구성이 필요하다. Release는 로컬 설정을 읽어 실제 모드로 전환하지 않고, HTTP도 허용하지 않는다. 배포용 실제 검색 설정은 기존처럼 `-PplaceDataSource=kakao -PplaceProxyUrl=https://YOUR_SERVER`로 전달한다. Debug 로컬 파일이 있으면 Debug 주소가 우선하므로 이를 끄려면 `-PdebugPlaceProxyUrl=`를 함께 전달한다.

프록시는 장소 검색만 담당한다. Pleos 외부 좌표 길안내 지원 여부와는 별개이며, 기존 SDK 검증 차단은 유지한다.

## MainActivity가 없다는 실행 오류

Pleos AVD는 현재 운전자 계정(user 10 등)과 시스템 계정(user 0)이 다를 수 있다. 이번 `Activity class ...MainActivity does not exist` 오류는 본 앱이 user 0에만 설치되고 현재 user 10에서는 설치되어 있지 않아 발생했다. Manifest나 클래스 이름 문제는 아니었다.

현재 기기에서는 데이터 삭제 없이 다음 방식으로 해결했다:

```powershell
adb shell am get-current-user
adb shell cmd package install-existing --user 10 com.github.flowtrip
adb shell cmd package resolve-activity --brief --user 10 -n com.github.flowtrip/.MainActivity
adb shell am start --user 10 -n com.github.flowtrip/.MainActivity
```

`10`은 조회한 현재 사용자 ID에 맞춘다. Android Studio `app` 실행 설정의 전체 사용자 설치(`ALL_USERS`)도 활성화했다. 최초 실행의 권한 요청은 사용자가 선택해야 한다.

## 검색은 성공하지만 추천 후보가 없는 경우

데이트 설정의 현재 좌표를 확인한다. 이번 AVD는 해외 기본 GPS로 되어 있어 국내 장소까지의 추정 이동시간이 5시간을 초과했다. 국내 시험 GPS로 설정하자 실제 후보 3개가 표시됐다. 앱이 위치를 자동으로 바꾸지는 않는다.

```powershell
# emulator-5554에서만 수행하는 시험 설정. 순서: 경도, 위도
adb -s emulator-5554 emu geo fix 127.09481944 37.41275833
```

실차에는 이 명령을 사용하지 않는다. 상세 원인/수정/검증은 [검색 진단](../../docs/PLACE_SEARCH_DIAGNOSTICS.md)을 참고한다.
