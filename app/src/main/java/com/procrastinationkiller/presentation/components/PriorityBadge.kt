package com.procrastinationkiller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procrastinationkiller.domain.model.TaskPriority

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (priority) {
        TaskPriority.CRITICAL -> Color(0xFFFF1744) to Color.White
        TaskPriority.HIGH -> Color(0xFFFF9100) to Color.Black
        TaskPriority.MEDIUM -> Color(0xFFFFC107) to Color.Black
        TaskPriority.LOW -> Color(0xFF4CAF50) to Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.name,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
