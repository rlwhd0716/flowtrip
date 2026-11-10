package com.github.flowtrip

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.github.flowtrip.data.repository.MockUxRestrictionsRepository
import com.github.flowtrip.domain.model.UxAccess
import org.junit.Rule
import org.junit.Test

class FlowTripNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun mockBuildExplainsPleosMapRequirementWithoutExternalMap() {
        compose.onNodeWithText("02  데이트 모드").performClick()
        compose.onNodeWithText("데이트 시작").performScrollTo().performClick()
        compose.onNodeWithText("Pleos 내비게이션 지도").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("지도는 Pleos SDK 빌드와 Pleos Connect 환경에서 사용할 수 있습니다.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pleos 지도 열기").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("다른 장소 추천").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("온기 키친").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pleos 지도 열기").performScrollTo().assertIsNotEnabled()
    }

    @Test fun tripInputBackNavigationAndRecreation() {
        compose.onNodeWithText("01  여행 모드").performClick()
        compose.onNodeWithText("코스 추천 시작").assertIsNotEnabled()
        compose.onNodeWithText("여행 목적지").performTextInput("강릉 & 바다/카페?")
        compose.onNodeWithText("코스 추천 시작").performScrollTo().performClick()
        compose.onNodeWithText("골목의 작은 식탁").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다른 장소 추천").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("온기 키친").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("‹  뒤로").performScrollTo().performClick()
        compose.onNodeWithText("강릉 & 바다/카페?").assertIsDisplayed()
    }

    @Test fun dateCourseSelectSkipAndFinish() {
        compose.onNodeWithText("02  데이트 모드").performClick()
        compose.onNodeWithText("데이트 시작").performScrollTo().performClick()
        compose.onNodeWithText("골목의 작은 식탁").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("이 장소 선택").performScrollTo().performClick()
        compose.onNodeWithText("목적지로 설정").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasText("달빛 전망길")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("달빛 전망길").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("건너뛰기").performScrollTo().performClick()
        compose.onNodeWithText("느린 오후").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("이 장소 선택").performScrollTo().performClick()
        compose.onNodeWithText("목적지로 설정").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasText("오늘의 코스를 살펴보세요")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("선택 2곳 · 건너뛰기 1곳").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈으로").performScrollTo().performClick()
        compose.onNodeWithText("FlowTrip").assertIsDisplayed()
    }

    @Test fun cancelDestinationApprovalKeepsCurrentStage() {
        compose.onNodeWithText("02  데이트 모드").performClick()
        compose.onNodeWithText("데이트 시작").performScrollTo().performClick()
        compose.onNodeWithText("이 장소 선택").performScrollTo().performClick()
        compose.onNodeWithText("이 장소를 목적지로 설정할까요?").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithText("골목의 작은 식탁").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("현재 목적지: 확인된 목적지 없음").performScrollTo().assertIsDisplayed()
    }

    @Test fun restrictedUxBlocksInputsAndRestoresFormAfterRestrictionEnds() {
        compose.onNodeWithText("01  여행 모드").performClick()
        compose.onNodeWithText("여행 목적지").performTextInput("강릉")
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.RESTRICTED) }
        compose.onNodeWithText("정차 후 사용해 주세요").assertIsDisplayed()
        compose.onNodeWithText("여행 목적지").assertDoesNotExist()
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.SIMULATED) }
        compose.onNodeWithText("강릉").assertIsDisplayed()
    }
}
