# 데이트 추천 후보 없음 원인 및 수정

## 확인한 원인

사용자 화면은 데이트 모드 첫 식당 단계에서 `남은 시간과 검색 조건에 맞는 장소가 없습니다`를 표시했다. 같은 설정 화면에서 시간은 **5시간**, FusedLocation을 통해 표시된 위치는 **37.42200, -122.08400**이었다. 에뮬레이터 기본 해외 위치이며 국내 장소와 멀리 떨어져 있었다.

같은 좌표로 실제 카카오 요청을 재현했을 때 HTTP 200과 국내 식당 응답이 반환됐다. 그러나 기존 추천 로직이 이 좌표에서 국내 식당까지의 이동시간을 계산하므로 5시간 내 후보가 모두 제외됐다. API 연결 실패/키 인증 오류가 아니었다. 화면의 남은 분이 `—`로 나오고, 위치 문제도 일반적인 조건 부족 메시지로 합쳐진 점이 진단을 어렵게 했다.

## 수정

- 데이트 실제 검색 전에 현재 좌표가 국내 검색용의 대략적인 범위(위도 32~39, 경도 124~132)에 있는지 검사한다. 정확한 행정 경계 판정이 아니라 해외 기본 GPS/잘못된 좌표를 조기에 알리는 서비스 정책이다.
- 범위 밖이면 `현재 차량 위치가 국내 장소 검색 범위 밖입니다. GPS 위치를 확인해 주세요.`를 표시한다. Debug에서만 에뮬레이터 시험 위치 설정 안내를 덧붙인다.
- API 응답 장소의 실제 좌표도 검색 중심 20km 안인지 검사한다. 반경 밖 장소를 현재 주변 후보로 사용하지 않는다.
- 실제 검색 결과 없음, 좌표 파싱 실패, 지역/반경 조건 탈락, 이미 추천/제외한 장소, 시간 부족을 구분한다. 시간 부족은 실제 추정 이동·체류 합계의 최솟값과 남은 분을 보여준다.
- 기존 `RecommendationRules.rank`를 유지하고 같은 계산 결과의 후보 수/최소 필요 시간을 함께 얻도록 확장했다. 시간 필터를 무시하거나 체류 가정을 임의로 줄이지 않았다.
- 로딩 시작과 재검색 시 남은 시간을 UiState에 먼저 반영하여 오류 화면에서도 분이 보이도록 했다.
- Mock 추천/후보 선택/기존 코스 상태/NaviHelper는 재구현하지 않았다. 앱에서 GPS를 다른 좌표로 교체하거나 Mock 장소로 대체하지 않는다.

변경 파일:

- `app/src/main/java/com/github/flowtrip/data/remote/KakaoPlaceSearch.kt`
- `app/src/main/java/com/github/flowtrip/domain/usecase/RecommendationRules.kt`
- `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModel.kt`
- `app/src/test/java/com/github/flowtrip/data/RecommendationRulesTest.kt`
- `app/src/test/java/com/github/flowtrip/presentation/recommendation/RecommendationViewModelTest.kt`
- `app/src/androidTest/java/com/github/flowtrip/PlaceProxySmokeTest.kt`

## 에뮬레이터에서 확인

**실차가 아닌 `emulator-5554`만** 성남 판교의 기존 SDK 샘플 부근 시험 위치로 설정했다. 시간과 관심사 입력은 그대로 두었다.

```powershell
adb -s emulator-5554 emu geo fix 127.09481944 37.41275833
```

순서는 **경도, 위도**다. 에뮬레이터를 실행한 사용자 환경에서 명령을 실행한다. 앱 시작/프록시 자동 시작 코드에는 이 명령을 넣지 않았다.

위치 수신 후 기존 앱의 데이트 설정 화면은 위도 37.41276/경도 127.09482를 표시했고, 같은 5시간 조건으로 다음 실제 장소 3개가 추천 화면에 표시됐다.

1. 풍경한식
2. 백경한우 판교본점
3. 이태리부대찌개 제2판교점

장소 ID의 `kakao:` namespace, 실제 주소/카테고리/상세 URL, 이동·체류 추정 표시도 확인했다. 이것은 길안내 성공/방문 완료 검증이 아니다.

## 검증 결과

- Debug 앱 및 계측 APK 빌드 성공.
- 단위 테스트 **48개, 실패 0**. 추가 테스트는 해외 GPS 범위, 시간 부족/기추천 제외의 구분, 오류 사유 및 남은 시간 표시 보존을 검사한다.
- Lint 오류 0. 실제 검색 및 해외 위치 안내 계측 테스트의 최종 실행 결과는 아래 후속 기록에 남긴다.
- APK 업데이트는 기존 데이터를 유지하고 모든 사용자에 설치하여, 앞서 확인한 Pleos user 0/user 10 설치 불일치가 재발하지 않도록 한다.

공식 참고: [에뮬레이터 GPS 설정](https://developer.android.com/studio/run/emulator-console#geo), [카카오 로컬 좌표·반경 파라미터](https://developers.kakao.com/docs/ko/local/dev-guide).
