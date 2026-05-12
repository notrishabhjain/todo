package com.procrastinationkiller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SourceAppIcon(
    sourceApp: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val (backgroundColor, label) = resolveAppDisplay(sourceApp)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = (size.value / 3).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun resolveAppDisplay(sourceApp: String): Pair<Color, String> {
    val packageLower = sourceApp.lowercase()
    return when {
        packageLower.contains("whatsapp") -> Color(0xFF25D366) to "WA"
        packageLower.contains("telegram") -> Color(0xFF0088CC) to "TG"
        packageLower.contains("slack") -> Color(0xFF4A154B) to "SL"
        packageLower.contains("gmail") || packageLower.contains("google.android.gm") -> Color(0xFFEA4335) to "GM"
        packageLower.contains("sms") || packageLower.contains("messaging") -> Color(0xFF2196F3) to "SM"
        packageLower.contains("calendar") -> Color(0xFF4285F4) to "CA"
        else -> Color(0xFF757575) to sourceApp.take(2).uppercase()
    }
}
