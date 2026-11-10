package com.github.flowtrip.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "가로 · 밝게", group = "화면 크기 / 테마", widthDp = 1280, heightDp = 720, locale = "ko")
@Preview(name = "가로 · 어둡게", group = "화면 크기 / 테마", widthDp = 1280, heightDp = 720,
    locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "세로 · 밝게", group = "화면 크기 / 테마", widthDp = 480, heightDp = 960, locale = "ko")
@Preview(name = "세로 · 어둡게", group = "화면 크기 / 테마", widthDp = 480, heightDp = 960,
    locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class FlowTripScreenPreviews
