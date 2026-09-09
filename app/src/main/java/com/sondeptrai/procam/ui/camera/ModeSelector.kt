package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.ShootingMode
import com.sondeptrai.procam.ui.theme.ProCamAmber
import com.sondeptrai.procam.ui.theme.TextMuted

@Composable
fun ModeSelector(
    selectedMode: ShootingMode,
    isExpanded: Boolean,
    onModeSelected: (ShootingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xE60A0A0C))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ShootingMode.values()) { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ProCamAmber.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) ProCamAmber else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
