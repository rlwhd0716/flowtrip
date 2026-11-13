package com.github.flowtrip

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.flowtrip.data.repository.MockUxRestrictionsRepository
import com.github.flowtrip.domain.model.UxAccess
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

/** Mock-only UI regression scenarios. Real SDK smoke tests are separately opt-in. */
class FlowTripNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dateStart() {
        assumeFalse(BuildConfig.PLEOS_SDK_ENABLED)
        compose.onNodeWithText("데이트 모드").performClick()
        compose.onNodeWithText("데이트 코스 추천하기").performClick()
        detail("골목의 작은 식탁")
    }
    private fun detail(name: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("detail-title") and hasText(name)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("detail-title").assertTextEquals(name)
    }
    @Test fun candidatesUpdateDetailAndUnsetDestinationDisablesNavigation() {
        dateStart()
        compose.onNodeWithText("SDK 샘플 목적지").assertDoesNotExist()
        compose.onNodeWithText("다음 추천").performClick()
        detail("정원 옆 레스토랑")
        compose.onNodeWithTag("navigation-action").assertIsEnabled()
            .assertTextContains("소프트웨어드림센터로 길안내 테스트", substring = true)
        compose.onNodeWithText("다음 추천").performClick()
        detail("밤의 식탁")
        compose.onNodeWithText("길안내 목적지 미설정").assertIsNotEnabled()
        compose.onNodeWithText("다음 추천").performClick()
        detail("골목의 작은 식탁")
    }
    @Test fun modeSelectionsAndCourseSurviveRecreation() {
        dateStart()
        compose.onNodeWithText("다음 코스").performClick()
        compose.onNodeWithText("다음 추천").performClick()
        detail("강변 산책로")
        compose.onNodeWithText("여행 모드").performClick()
        detail("해운대 등대 전망대")
        compose.onNodeWithText("다음 추천").performClick()
        detail("경주 불국사")
        compose.activityRule.scenario.recreate()
        detail("경주 불국사")
        compose.onNodeWithText("데이트 모드").performClick()
        detail("강변 산책로")
    }
    @Test fun explicitNavigationDoesNotAdvanceAndFinalStageCompletes() {
        dateStart()
        compose.onNodeWithTag("navigation-action").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Mock 길안내 확인 · 실제 SDK 요청 아님")).fetchSemanticsNodes().isNotEmpty() }
        detail("골목의 작은 식탁")
        compose.onNodeWithText("다음 코스").performClick()
        detail("도시 야경 전망대")
        compose.onNodeWithText("다음 코스").performClick()
        detail("작은 디저트 카페")
        compose.onNodeWithText("코스 완료").performClick()
        compose.onNodeWithText("선택한 장소 3곳 · 예시 코스").assertExists()
        compose.onNodeWithText("새 코스 시작").performScrollTo().performClick()
        detail("골목의 작은 식탁")
    }
    @Test fun restrictedUxBlocksInputsAndRestoresFormAfterRestrictionEnds() {
        assumeFalse(BuildConfig.PLEOS_SDK_ENABLED)
        compose.onNodeWithText("여행 모드").performClick()
        compose.onNodeWithTag("province-select").performScrollTo().performClick()
        compose.onNodeWithText("부산광역시").performClick()
        compose.onNodeWithTag("district-select").performScrollTo().performClick()
        compose.onNodeWithText("해운대구").performClick()
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.RESTRICTED) }
        compose.onNodeWithText("정차 후 사용해 주세요").assertIsDisplayed()
        compose.onNodeWithText("여행 목적지").assertDoesNotExist()
        compose.runOnUiThread { (compose.activity.ux as MockUxRestrictionsRepository).simulate(UxAccess.SIMULATED) }
        compose.onNodeWithText("해운대구").assertIsDisplayed()
    }
}
