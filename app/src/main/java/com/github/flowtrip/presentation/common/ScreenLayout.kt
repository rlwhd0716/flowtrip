package com.github.flowtrip.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun ScreenLayout(title: String, onBack: (() -> Unit)? = null, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp),
        ) {
            if (compact) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (onBack != null) TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) { Text("‹  뒤로") }
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                if (onBack != null) {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) { Text("‹  뒤로") }
                }
                Text(title, style = MaterialTheme.typography.headlineLarge)
            }
            Text("정차 중 사용해 주세요 · 현재 추천 장소는 Mock 데이터입니다", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun AdaptivePanels(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 760.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) { first() }
                Column(Modifier.weight(1f)) { second() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) { first(); second() }
        }
    }
}
