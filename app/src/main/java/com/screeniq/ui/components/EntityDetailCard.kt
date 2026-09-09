package com.screeniq.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.ui.theme.BrandSecondary
import com.screeniq.ui.theme.ScreenIQShapes

@Composable
fun EntityDetailCard(
    classification: ContentClassification,
    modifier: Modifier = Modifier
) {
    val categoryHeader = getCategoryHeader(classification.primaryCategory)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ScreenIQShapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                ScreenIQShapes.medium
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category Badge Header (e.g. 🧠 EVENT DETECTED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryHeader,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = BrandSecondary
                )

                Text(
                    text = "${(classification.confidenceScore * 100).toInt()}% match",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary Content Summary / Title
            val title = classification.summary ?: classification.extractedEntities.firstOrNull()?.rawValue ?: "Detected Information"
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dynamic entity attribute details
            val entityDetails = extractDetailLines(classification)
            if (entityDetails.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entityDetails.forEach { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryHeader(category: ContentCategory): String {
    return when (category) {
        ContentCategory.EVENT -> "🧠 EVENT DETECTED"
        ContentCategory.LOCATION -> "📍 LOCATION DETECTED"
        ContentCategory.PHONE -> "📞 PHONE NUMBER DETECTED"
        ContentCategory.CONTACT -> "👤 CONTACT DETECTED"
        ContentCategory.EMAIL -> "✉️ EMAIL DETECTED"
        ContentCategory.URL -> "🌐 LINK DETECTED"
        ContentCategory.PRODUCT -> "🛒 PRODUCT DETECTED"
        ContentCategory.TASK -> "📋 TASK DETECTED"
        ContentCategory.DOCUMENT -> "📄 DOCUMENT DETECTED"
        ContentCategory.QR_CODE -> "🔳 QR CODE DETECTED"
        ContentCategory.IMAGE -> "🖼️ IMAGE DETECTED"
        ContentCategory.TEXT -> "📝 TEXT DETECTED"
        ContentCategory.UNKNOWN -> "💡 CONTENT DETECTED"
    }
}

private fun extractDetailLines(classification: ContentClassification): List<String> {
    val lines = mutableListOf<String>()

    val dateTimes = classification.extractedEntities.filter { it.type == EntityType.DATE_TIME }
    val locations = classification.extractedEntities.filter { it.type == EntityType.LOCATION_ADDRESS }
    val phones = classification.extractedEntities.filter { it.type == EntityType.PHONE_NUMBER }
    val emails = classification.extractedEntities.filter { it.type == EntityType.EMAIL }
    val urls = classification.extractedEntities.filter { it.type == EntityType.URL }

    if (dateTimes.isNotEmpty()) {
        lines.add("📅 " + dateTimes.joinToString(" • ") { it.rawValue })
    }
    if (locations.isNotEmpty()) {
        lines.add("📍 " + locations.joinToString(", ") { it.rawValue })
    }
    if (phones.isNotEmpty()) {
        lines.add("📞 " + phones.joinToString(", ") { it.rawValue })
    }
    if (emails.isNotEmpty()) {
        lines.add("✉️ " + emails.joinToString(", ") { it.rawValue })
    }
    if (urls.isNotEmpty()) {
        lines.add("🔗 " + urls.joinToString(", ") { it.rawValue })
    }

    return lines
}
