package com.cleanify.ui.screens.settings.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.cleanify.R
import com.cleanify.ui.theme.predefinedAccentColors
import kotlin.math.roundToInt

@Composable
fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItem(title: String, summary: String, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null || onLongClick != null)
        Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick?.invoke() }, onLongPress = { onLongClick?.invoke() }) }
    else Modifier
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

val settingsPickerAccents = mapOf(
    R.string.language_title to Color(0xFF9B59B6),
    R.string.theme_title to Color(0xFF9B59B6),
    R.string.folder_name_position_title to Color(0xFF9B59B6),
    R.string.folder_bar_layout_title to Color(0xFF9B59B6),
    R.string.swipe_sensitivity_title to Color(0xFF00BCD4),
    R.string.swipe_down_action_title to Color(0xFF00BCD4),
    R.string.folder_selection_mode_title to Color(0xFF00BCD4),
    R.string.initial_dialog_focus_title to Color(0xFF00BCD4),
    R.string.unselect_all_behavior_title to Color(0xFF00BCD4),
    R.string.similarity_level_title to Color(0xFFE74C3C),
    R.string.scan_scope_title to Color(0xFFE74C3C),
    R.string.default_video_speed_title to Color(0xFF4CAF50),
    R.string.screenshot_quality_title to Color(0xFF4CAF50),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsPickerItem(titleRes: Int, descriptionRes: Int, options: List<T>, selectedOption: T, onOptionSelected: (T) -> Unit, getDisplayName: @Composable (T) -> String) {
    SettingsPickerItem(
        titleRes = titleRes,
        description = stringResource(descriptionRes),
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = onOptionSelected,
        getDisplayName = getDisplayName
    )
}

@Composable
fun <T> SettingsPickerItem(titleRes: Int, description: String, options: List<T>, selectedOption: T, onOptionSelected: (T) -> Unit, getDisplayName: @Composable (T) -> String) {
    val accentColor = settingsPickerAccents[titleRes] ?: Color(0xFF7F8C8D)
    var showDropdown by remember { mutableStateOf(false) }
    val displayValue = getDisplayName(selectedOption)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { showDropdown = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(displayValue, style = MaterialTheme.typography.bodyMedium, color = accentColor)
                    Spacer(Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            shape = RoundedCornerShape(16.dp),
            offset = DpOffset(x = (-280).dp, y = 8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                getDisplayName(option),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = { onOptionSelected(option); showDropdown = false }
                )
            }
        }
    }
}

@Composable
fun SettingSwitch(titleRes: Int, descriptionRes: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
fun SettingSwitch(titleRes: Int, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
fun ToolAboutCard(
    icon: ImageVector,
    title: String,
    description: String,
    warning: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("About this tool", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (warning != null) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AccentColorSetting(currentAccentKey: String, onClick: () -> Unit) {
    val accent = predefinedAccentColors.find { it.key == currentAccentKey } ?: predefinedAccentColors.first()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.accent_color_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(accent.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

@Composable
fun AccentColorDialog(currentAccentKey: String, onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    var localKey by remember { mutableStateOf(currentAccentKey) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val selAccent = predefinedAccentColors.find { it.key == localKey } ?: predefinedAccentColors.first()
    fun Color.toHex() = String.format("#%06X", 0xFFFFFF and this.toArgb())
    val hexColor = if (isDark) selAccent.darkColor else selAccent.lightColor

    com.cleanify.ui.components.AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.customize_colors_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            val colors = remember(isDark) { predefinedAccentColors.map { if (isDark) it.darkColor else it.lightColor } }
            Column {
                androidx.compose.foundation.Canvas(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                        .pointerInput(Unit) { detectTapGestures { o -> localKey = predefinedAccentColors[((o.x / size.width).coerceIn(0f, 1f) * (colors.size - 1)).roundToInt()].key } }
                ) {
                    drawRoundRect(brush = Brush.linearGradient(colors = colors), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
                    val idx = predefinedAccentColors.indexOfFirst { it.key == localKey }
                    if (idx != -1) {
                        val px = (size.width * idx.toFloat() / (colors.size - 1).toFloat()).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
                        drawCircle(Color.White, 12.dp.toPx(), Offset(px, size.height / 2))
                        drawCircle(colors[idx], 8.dp.toPx(), Offset(px, size.height / 2))
                        drawCircle(Color.Black.copy(alpha = 0.2f), 12.dp.toPx(), Offset(px, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(hexColor.toHex(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        buttons = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Button(onClick = { onColorSelected(localKey) }) { Text(stringResource(R.string.ok)) }
        }
    )
}
