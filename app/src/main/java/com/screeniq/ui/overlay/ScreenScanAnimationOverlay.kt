package com.screeniq.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.ui.theme.BrandPrimary
import com.screeniq.ui.theme.BrandSecondary

/**
 * Full-screen high-tech scanning & analysis animation overlay.
 *
 * Displays:
 * 1. A glowing cyan/indigo laser beam sweeping smoothly from top to bottom.
 * 2. Corner targeting reticles / viewfinder brackets.
 * 3. A pulsing "AI SCANNING SCREEN..." status badge.
 * 4. Holographic horizontal grid scanlines.
 */
@Composable
fun ScreenScanAnimationOverlay(
    modifier: Modifier = Modifier,
    statusText: String = "AI SCANNING SCREEN...",
    subtitleText: String = "Analyzing text, entities & context on-device"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanAnimation")

    // Laser beam vertical sweep: 0f (top) to 1f (bottom)
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LaserSweep"
    )

    // Glowing pulse for HUD elements
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x880A0D14)) // Semi-transparent cyber backdrop
    ) {
        // High-tech laser beam and viewfinder brackets Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw subtle horizontal grid scanlines
            val lineSpacing = 32.dp.toPx()
            var currentY = 0f
            while (currentY < canvasHeight) {
                drawLine(
                    color = Color(0x0A06B6D4),
                    start = Offset(0f, currentY),
                    end = Offset(canvasWidth, currentY),
                    strokeWidth = 1f
                )
                currentY += lineSpacing
            }

            // 2. Calculate current laser beam position
            val beamY = scanProgress * canvasHeight
            val beamTrailHeight = 120.dp.toPx()

            // Laser beam trailing gradient
            val trailBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x1A06B6D4),
                    Color(0x6606B6D4),
                    Color(0xFF06B6D4)
                ),
                startY = (beamY - beamTrailHeight).coerceAtLeast(0f),
                endY = beamY
            )

            drawRect(
                brush = trailBrush,
                topLeft = Offset(0f, (beamY - beamTrailHeight).coerceAtLeast(0f)),
                size = Size(canvasWidth, (beamY - (beamY - beamTrailHeight).coerceAtLeast(0f)))
            )

            // Primary bright laser sweep line
            drawLine(
                color = Color(0xFFE0F2FE),
                start = Offset(0f, beamY),
                end = Offset(canvasWidth, beamY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Glow line slightly behind primary laser
            drawLine(
                color = Color(0xFF06B6D4).copy(alpha = glowAlpha),
                start = Offset(0f, beamY),
                end = Offset(canvasWidth, beamY),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 3. Corner Viewfinder Brackets / Reticles
            val cornerPadding = 24.dp.toPx()
            val bracketLength = 36.dp.toPx()
            val bracketStroke = 3.dp.toPx()
            val bracketColor = Color(0xFF38BDF8).copy(alpha = glowAlpha)

            // Top-Left Corner
            drawLine(bracketColor, Offset(cornerPadding, cornerPadding), Offset(cornerPadding + bracketLength, cornerPadding), bracketStroke)
            drawLine(bracketColor, Offset(cornerPadding, cornerPadding), Offset(cornerPadding, cornerPadding + bracketLength), bracketStroke)

            // Top-Right Corner
            drawLine(bracketColor, Offset(canvasWidth - cornerPadding, cornerPadding), Offset(canvasWidth - cornerPadding - bracketLength, cornerPadding), bracketStroke)
            drawLine(bracketColor, Offset(canvasWidth - cornerPadding, cornerPadding), Offset(canvasWidth - cornerPadding, cornerPadding + bracketLength), bracketStroke)

            // Bottom-Left Corner
            drawLine(bracketColor, Offset(cornerPadding, canvasHeight - cornerPadding), Offset(cornerPadding + bracketLength, canvasHeight - cornerPadding), bracketStroke)
            drawLine(bracketColor, Offset(cornerPadding, canvasHeight - cornerPadding), Offset(cornerPadding, canvasHeight - cornerPadding - bracketLength), bracketStroke)

            // Bottom-Right Corner
            drawLine(bracketColor, Offset(canvasWidth - cornerPadding, canvasHeight - cornerPadding), Offset(canvasWidth - cornerPadding - bracketLength, canvasHeight - cornerPadding), bracketStroke)
            drawLine(bracketColor, Offset(canvasWidth - cornerPadding, canvasHeight - cornerPadding), Offset(canvasWidth - cornerPadding, canvasHeight - cornerPadding - bracketLength), bracketStroke)
        }

        // Futuristic HUD Status Pill at top center
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xCC111827),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(BrandPrimary, BrandSecondary)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animated pulsing indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4).copy(alpha = glowAlpha))
                    )

                    Text(
                        text = statusText,
                        color = Color(0xFFF0FDF4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
