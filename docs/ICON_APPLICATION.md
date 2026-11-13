# 5개 화면 아이콘 적용

작성일: 2026-11-14

## 확인한 자료

`docs/FlowTrip_5screens_assets`의 README_KO.md, asset_manifest.csv, preview_contact_sheet.png, 05_references의 메인/여행 입력/데이트 입력/여행 결과/데이트 결과 시안 5개를 확인했다. 테두리 혼입·잘림이 있는 아이콘은 개별 원본도 확인했다.

## 적용

- `04_icons_upscaled_4x`의 PNG 32개를 `app/src/main/res/drawable-nodpi/ft_icon_*.png`로 복사했다. 파일 내용은 원본과 동일하며 이미지 편집/재생성을 하지 않았다.
- FlowIconAsset/FlowIcon/FlowBrand를 공통 표시 계층으로 추가했다. 고정 dp 크기와 ContentScale.Fit으로 비율을 유지하고, 선택된 단계는 흰색, 비활성 선택지는 낮은 불투명도로 표시한다.
- 문자로 대체했던 로고·종류·상태 아이콘을 이미지 리소스로 교체했다.
- 묶음에 없는 화살표/다시 추천/체크/안내/길안내/+/−/더보기/가격/대표 메뉴는 `ft_ui_*.xml` 벡터 13개로 보완했다.
- 아이콘만 있는 뒤로/시간 +/- 버튼에는 접근성 설명을 부여했다. 본문과 함께 있는 장식 아이콘은 중복 읽기를 막기 위해 별도 설명을 붙이지 않았다.

| 화면 | 적용 위치 |
| --- | --- |
| 메인 | 산 모양 브랜드/여행 카드, 하트 데이트 카드, 최근 기록 시계, 다시 이용하기 및 시작 화살표 |
| 여행 입력 | 하트 브랜드, 목적지 핀, 시간 시계, 관심사 반짝임, 자연/관광/식당/카페/역사/예술/쇼핑/캠핑 8종 |
| 데이트 입력 | 하트 브랜드, 현재 위치/시간/분위기/장소 섹션, 분위기 4종, 장소 유형 10종 |
| 여행 추천 | 하트 브랜드, 단계별 유형, 나침반 목록 제목, 후보 핀/선택 상태, 상세 정보, 길안내/다음/다시 추천 |
| 데이트 추천 | 하트 브랜드, 식당/야경/디저트 단계, 식당 목록 제목, 후보 핀/선택 상태, 상세 정보, 길안내/다음/다시 추천 |

지도·길안내 요청, 후보/모드 상태, 지역/분위기 선택 규칙 및 차량 시스템 UI는 변경하지 않았다. 기존 UI 테스트 파일의 문자 화살표가 포함된 버튼 참조만 실제 텍스트/기존 태그에 맞췄다. 테스트를 실행한 것은 아니다.

## 추출본 보정 기준

다음 4개는 원본 PNG를 그대로 유지하고 BitmapPainter의 source region으로 아이콘 부분만 표시한다.

- `category_restaurant_utensils`: 추출된 카드 상단/왼쪽 테두리 제외.
- `date_category_food`: 추출된 카드 테두리 제외.
- `mood_romantic_heart`: 하트 주변 카드 테두리 제외.
- `mood_special_star`: 별 주변 카드 테두리 제외.

원래부터 도형 일부가 잘린 `travel_mode_mountain`, `date_mode_heart`, `travel_header_heart_logo`는 적용하지 않았다. 각각 같은 묶음의 온전한 자연 산 아이콘, 로맨틱 하트, 데이트 헤더 하트 로고로 대체했다. 두 모드 헤더는 같은 온전한 하트 로고를 공유한다.

확대본은 원본 픽셀을 확대한 추출본이다. 사용자가 제공한 README의 해상도·마스킹 한계는 그대로 존재하며 새로운 디테일이나 실제 벡터 원본을 복구한 것은 아니다.

## 변경 파일

- 신규: `app/src/main/java/com/github/flowtrip/presentation/common/FlowIcon.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/common/FlowFormComponents.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/home/HomeScreen.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/trip/TripScreen.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/date/DateScreen.kt`
- 수정: `app/src/main/java/com/github/flowtrip/presentation/recommendation/RecommendationScreen.kt` (두 모드 공통)
- 수정: `app/src/androidTest/java/com/github/flowtrip/FlowTripNavigationTest.kt` (버튼 참조)
- 신규: `app/src/main/res/drawable-nodpi/ft_icon_*.png` 32개
- 신규: `app/src/main/res/drawable/ft_ui_*.xml` 13개
- 신규: `docs/ICON_APPLICATION.md`

## 간단한 정적 점검

- Kotlin 64개 파일의 괄호·문자열·주석 균형 및 대체 문자 유무.
- FlowIconAsset 참조 이름 및 drawable 45개 존재 여부.
- PNG 32개의 형식과 제공 파일과의 바이트 일치.
- 표시 영역 4개의 원본 PNG 경계 내 좌표.
- 벡터 13개의 XML 파싱 및 vector 루트.
- 이번 변경 소스의 공백 오류 점검.

이는 Kotlin 타입 검사나 Android 리소스 컴파일을 대신하지 않는다. 사용자 지시에 따라 **빌드·단위/UI 테스트·프리뷰 렌더링·기기 테스트는 실행하지 않았다.** 실제 차량/분할 창에서 아이콘 선명도, 선택 상태 대비와 줄바꿈은 추가 확인이 필요하다.
