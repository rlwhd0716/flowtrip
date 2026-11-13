package com.github.flowtrip.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.util.AvailableTimeUnit
import com.github.flowtrip.domain.model.TravelMode

val FlowGreen = Color(0xFF006B59)
val FlowInk = Color(0xFF142F35)
val FlowMint = Color(0xFFEEF6F3)

data class FormChoice(val label: String, val icon: FlowIconAsset)

@Composable
fun FlowFormLayout(mode: TravelMode, title: String, subtitle: String, onBack: () -> Unit,
    action: String, enabled: Boolean, onStart: () -> Unit, content: @Composable ColumnScope.() -> Unit) = FlowTripTheme(darkTheme = false) {
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF9FBFA), contentColor = FlowInk) {
        BoxWithConstraints(Modifier.safeDrawingPadding().padding(16.dp)) {
            val compact = maxHeight < 540.dp
            val narrow = maxWidth < 480.dp
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
                    ReferencePhoto(if (mode == TravelMode.TRIP) ReferenceImage.TRIP_HEADER else ReferenceImage.DATE_HEADER, Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.White, Color(0xEEFFFFFF), Color(0x44FFFFFF)))))
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                                FlowIcon(FlowIconAsset.BACK, Modifier.size(20.dp), tint = FlowGreen)
                                Text("뒤로", color = FlowGreen)
                            }
                            FlowBrand(mode, Modifier.weight(1f), fontSize = if (narrow) 18.sp else if (compact) 20.sp else 24.sp, showMode = true)
                        }
                        Text(title, fontSize = if (compact || narrow) 23.sp else 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                        if (!compact) Text(subtitle, fontSize = 16.sp, lineHeight = 22.sp)
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                Button(onClick = onStart, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = FlowGreen),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(action, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    FlowIcon(FlowIconAsset.NEXT, Modifier.size(24.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun InputSection(title: String, icon: FlowIconAsset, subtitle: String? = null, inline: Boolean = false, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(FlowMint).padding(16.dp)) {
        if (inline && maxWidth >= 650.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeading(title, icon, subtitle, Modifier.weight(.28f))
                Box(Modifier.weight(.72f)) { content() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { SectionHeading(title, icon, subtitle); content() }
        }
    }
}

@Composable
private fun SectionHeading(title: String, icon: FlowIconAsset, subtitle: String?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        FlowIcon(icon, Modifier.size(30.dp))
        Column {
            Text(title, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, fontSize = 14.sp, lineHeight = 20.sp) }
        }
    }
}

@Composable
fun ChoiceGrid(options: List<FormChoice>, selected: List<String>, maxSelection: Int, onToggle: (String) -> Unit, maxColumns: Int = 4) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when { maxWidth >= 880.dp -> maxColumns; maxWidth >= 650.dp -> minOf(4, maxColumns); maxWidth >= 400.dp -> 2; else -> 1 }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        val active = option.label in selected
                        val allowed = active || selected.size < maxSelection
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (active) Color(0xFFDCEFE7) else Color.White)
                            .border(1.dp, if (active) FlowGreen else Color(0xFFD8E0DC), RoundedCornerShape(16.dp))
                            .toggleable(value = active, enabled = allowed, role = Role.Checkbox, onValueChange = { onToggle(option.label) })
                            .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowIcon(option.icon, Modifier.size(28.dp).alpha(if (allowed) 1f else .4f))
                            Text(option.label, Modifier.weight(1f), fontSize = 16.sp, lineHeight = 21.sp, maxLines = 2, fontWeight = FontWeight.SemiBold,
                                color = if (allowed) FlowInk else Color.Gray)
                            if (active) FlowIcon(FlowIconAsset.CHECK, Modifier.size(22.dp))
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun RegionDropdown(label: String, value: String, options: List<String>, enabled: Boolean, tag: String,
    onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag)) {
            Text(value.ifBlank { label }, Modifier.weight(1f), fontSize = 17.sp)
            FlowIcon(FlowIconAsset.DOWN, Modifier.size(22.dp), tint = if (enabled) FlowInk else Color.Gray)
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option, fontSize = 18.sp) },
                onClick = { expanded = false; onSelect(option) }, modifier = Modifier.heightIn(min = 52.dp)) }
        }
    }
}

@Composable
fun TimeSelector(minutes: Int, unit: AvailableTimeUnit, onMinutes: (Int) -> Unit, onUnit: (AvailableTimeUnit) -> Unit) {
    val amount = minutes / unit.minutes
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stepper: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onMinutes((amount - 1) * unit.minutes) }, enabled = amount > 1,
                    contentPadding = PaddingValues(0.dp), modifier = Modifier.size(52.dp).testTag("time-decrease")) {
                    FlowIcon(FlowIconAsset.MINUS, tint = if (amount > 1) FlowInk else Color.Gray, description = "가능 시간 줄이기")
                }
                Text(amount.toString(), fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 40.dp).testTag("time-amount"))
                OutlinedButton(onClick = { onMinutes((amount + 1) * unit.minutes) }, enabled = amount < unit.maxAmount,
                    contentPadding = PaddingValues(0.dp), modifier = Modifier.size(52.dp).testTag("time-increase")) {
                    FlowIcon(FlowIconAsset.PLUS, tint = if (amount < unit.maxAmount) FlowInk else Color.Gray, description = "가능 시간 늘리기")
                }
            }
        }
        val units: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvailableTimeUnit.entries.forEach { item -> FilterChip(selected = unit == item, onClick = { onUnit(item) },
                    label = { Text(item.title, fontSize = 17.sp) }, modifier = Modifier.heightIn(min = 52.dp).testTag("time-unit-${item.name}")) }
            }
        }
        if (maxWidth >= 400.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            stepper(); units()
        } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { stepper(); units() }
    }
}
