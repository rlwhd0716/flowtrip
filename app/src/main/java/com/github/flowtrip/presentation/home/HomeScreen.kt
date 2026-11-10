package com.github.flowtrip.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ScreenLayout
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.designsystem.preview.FlowTripScreenPreviews

@Composable
fun HomeScreen(onTrip: () -> Unit, onDate: () -> Unit) {
    ScreenLayout("FlowTrip") {
        Text("다음 순간이 기대되는 여정", style = MaterialTheme.typography.headlineMedium)
        Text("여행도 데이트도, 다음에 갈 곳을 함께 찾아요.", style = MaterialTheme.typography.bodyLarge)
        AdaptivePanels(
            first = { ModeCard("01  여행 모드", "목적지를 정하고, 여정 속 새로운 장소를 만나세요.", onTrip) },
            second = { ModeCard("02  데이트 모드", "지금 있는 곳에서, 함께 갈 새로운 장소를 찾아요.", onDate) },
        )
    }
}

@Composable
private fun ModeCard(title: String, description: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge)
            Text("시작하기 →", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@FlowTripScreenPreviews
@Composable
private fun HomeScreenPreview() {
    FlowTripTheme {
        HomeScreen(onTrip = {}, onDate = {})
    }
}
