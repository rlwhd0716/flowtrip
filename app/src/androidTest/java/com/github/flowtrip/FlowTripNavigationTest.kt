package com.github.flowtrip

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class FlowTripNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun tripInputBackNavigationAndRecreation() {
        compose.onNodeWithText("01  여행 모드").performClick()
        compose.onNodeWithText("코스 추천 시작").assertIsNotEnabled()
        compose.onNodeWithText("여행 목적지").performTextInput("강릉 & 바다/카페?")
        compose.onNodeWithText("코스 추천 시작").performScrollTo().performClick()
        compose.onNodeWithText("골목의 작은 식탁").assertIsDisplayed()
        compose.onNodeWithText("다른 장소 추천").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("온기 키친").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("‹  뒤로").performScrollTo().performClick()
        compose.onNodeWithText("강릉 & 바다/카페?").assertIsDisplayed()
    }

    @Test fun dateCourseSelectSkipAndFinish() {
        compose.onNodeWithText("02  데이트 모드").performClick()
        compose.onNodeWithText("데이트 시작").performScrollTo().performClick()
        compose.onNodeWithText("골목의 작은 식탁").assertIsDisplayed()
        compose.onNodeWithText("이 장소 선택").performScrollTo().performClick()
        compose.onNodeWithText("달빛 전망길").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("건너뛰기").performScrollTo().performClick()
        compose.onNodeWithText("느린 오후").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("이 장소 선택").performScrollTo().performClick()
        compose.onNodeWithText("선택 2곳 · 건너뛰기 1곳").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈으로").performScrollTo().performClick()
        compose.onNodeWithText("FlowTrip").assertIsDisplayed()
    }
}
