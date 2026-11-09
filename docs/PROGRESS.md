# Pleos P0 연동 진행도

기준: 2026-11-10 (Asia/Seoul). P0만 구현하고 TTS/STT/Vehicle은 조사 문서만 작성한다.

## 확인 완료
- 기존 단일 app, Clean Architecture + MVVM + Hilt 유지.
- compileSdk 36.1 / targetSdk 36 / minSdk 28 / AGP 9.1.1 / Kotlin Compose 2.2.10 / Gradle 9.3.1 유지.
- 공식 문서에서 NaviHelper 3.0.5 및 FusedLocation 1.1.2 기준을 확인.
- 공개 Maven에서 NaviHelper 3.0.5 POM/AAR 다운로드 성공, 실제 클래스 시그니처 확인.
- FusedLocation 1.1.2 POM/AAR는 HTTP 404. 공식 Pleos 샘플이 사용하는 1.1.3 POM/AAR는 HTTP 200. 실제 API 검사 중.
- 초기 작업 트리에 기존 `core/di/`가 untracked 상태였음. 이전 Hilt 작업 파일이며 보존한다.

## 진행 순서
1. [완료] P0 실제 API/권한/SDK 초기화·콜백·데이터 필드 및 AAOS UX 제한 확인.
2. [완료] Domain Navigation/Location/UX 모델·Repository와 Mock 구현 추가.
3. [구현 완료/검증 중] 기본 Mock / `-PpleosSdk=true` 빌드 옵션과 소스 분리·Hilt 바인딩, 실제 Adapter 연결.
4. [구현 완료/검증 중] 추천 화면 별도 목적지 승인, 비동기 경로 확인, 도착 신호(방문 확정 아님), 위치 상태 연결.
5. [대기] P1/P2 조사 결과 및 외부 준비사항 문서 작성.
6. [대기] 상태·오류 단위 테스트, 양쪽 빌드, 일반 에뮬레이터 회귀 검증.

## 구현 중 확인한 차이
- FusedLocation은 공식 샘플/POM/AAR로 확인된 1.1.3을 채택. 문서 1.1.2와의 차이를 최종 조사 문서에 명시할 것.
- AGP 9 내장 Kotlin에서는 `android.sourceSets.main.kotlin.directories`에 추가 소스를 등록해야 함. Java 소스셋만 등록 시 KSP가 Kotlin 컴파일과 불일치했으며 수정함.
- NaviHelper의 `RouteInfo`는 POI ID/sub-ID가 non-null. 누락 시 임의 식별자를 만들지 않고 요청 거부.
- NaviHelper 위치는 측정 timestamp 미제공. Fused 미지원 시 대체 조회하되 STALE로 표시하고 근접 판정에서 제외.
- 실제 SDK APK는 아직 최종 빌드 검증 중이며, 현재 환경은 일반 Pixel AVD뿐임. Pleos 차량 서비스 실행 검증과 구분해야 함.

## 반드시 유지할 경계
- Mock 장소를 실제 목적지로 전송하지 않는다. 실제 좌표/POI 검증 전까지 실제 빌드에서는 요청을 거부한다.
- 목적지 변경은 사용자가 별도 확인한 경우에만 요청한다.
- `requestRoute()` 반환을 경로 성공으로 취급하지 않고 SDK 이벤트를 기다린다.
- 도착 이벤트는 방문 완료나 자동 다음 단계 확정이 아니다.
- 실제 빌드에서 미지원/권한 거부를 Mock 성공으로 대체하지 않는다.
- 실제 UX 제한이 불명확하면 정차로 간주하지 않는다. DO 선언 없이 플랫폼 제한과 앱의 보수적 차단을 적용한다.
- 위치는 메모리에서만 사용하고 서버 전송·영구 저장하지 않는다.
