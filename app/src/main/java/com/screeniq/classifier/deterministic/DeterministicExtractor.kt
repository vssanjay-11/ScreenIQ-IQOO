package com.screeniq.classifier.deterministic

import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.EntityType

/**
 * Stage 1 of the Layered Classification Pipeline:
 * High-precision, zero-latency deterministic entity extraction using compiled regexes and patterns.
 */
class DeterministicExtractor {

    companion object {
        // High-precision email pattern
        val EMAIL_REGEX = Regex(
            """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""
        )

        // Web URL pattern (http/https, www, or clear top-level domains)
        val URL_REGEX = Regex(
            """\b(?:https?://|www\.)[^\s/$.?#].[^\s]*\b|\b[a-zA-Z0-9-]+\.(?:com|org|net|io|edu|gov|in|co|ai)(?:/[^\s]*)?\b""",
            RegexOption.IGNORE_CASE
        )

        // Phone numbers (international, Indian 10-digit mobile, dashed/spaced formats)
        val PHONE_REGEX = Regex(
            """(?:\+?\d{1,3}[-.\s]?)?(?:\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{4,5}\b"""
        )

        // Date patterns (e.g., "20 September 2026", "Sept 20", "20/09/2026", "2026-09-20")
        val DATE_REGEX = Regex(
            """\b(?:\d{1,2}(?:st|nd|rd|th)?\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\s+\d{4})?|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:\s*,\s*\d{4})?|\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2})\b""",
            RegexOption.IGNORE_CASE
        )

        // Time patterns (e.g., "10:00 AM", "10 AM", "6:30 PM", "18:00")
        val TIME_REGEX = Regex(
            """\b(?:\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?|\d{1,2}\s*(?:AM|PM|am|pm))\b"""
        )

        // Currency and price patterns (e.g., "₹49,999", "$299.99", "Rs. 1,499", "INR 500")
        val PRICE_REGEX = Regex(
            """(?<!\w)(?:[₹$€£]|Rs\.?|INR|USD)\s*[\d,]+(?:\.\d{1,2})?\b|\b[\d,]+(?:\.\d{1,2})?\s*(?:INR|USD)\b""",
            RegexOption.IGNORE_CASE
        )

        // Common task markers (e.g. "[ ]", "TODO:", "Task:")
        val TASK_MARKER_REGEX = Regex(
            """(?m)^\s*(?:\[[\sXx]?\]|TODO:|Task:|Action Item:|- \[ \])\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        // Address and venue indicator keywords
        val ADDRESS_KEYWORDS = listOf(
            "street", "road", "rd", "st", "avenue", "ave", "nagar", "salai", "lane",
            "hall", "auditorium", "seminar hall", "campus", "university", "institute",
            "chennai", "bengaluru", "bangalore", "delhi", "mumbai", "hyderabad", "floor",
            "building", "block", "pincode", "pin:", "zip:", "opp", "near"
        )

        // PIN / ZIP code (e.g. 6-digit Indian PIN)
        val PINCODE_REGEX = Regex("""\b[1-9][0-9]{5}\b""")

        // QR Code signatures (e.g. UPI strings, WiFi setup, generic URI schemes)
        val QR_PAYLOAD_REGEX = Regex(
            """\b(?:upi://pay\?[^\s]+|WIFI:S:[^;]+;|MATMSG:[^;]+;)\b""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Extracts all deterministic entities present in the raw text and returns a typed list.
     */
    fun extract(rawText: String): List<DetectedEntity> {
        if (rawText.isBlank()) return emptyList()

        val entities = mutableListOf<DetectedEntity>()

        // 1. QR signatures
        QR_PAYLOAD_REGEX.findAll(rawText).forEach { match ->
            entities.add(
                DetectedEntity(
                    type = EntityType.QR_BARCODE,
                    rawValue = match.value.trim(),
                    normalizedValue = match.value.trim(),
                    confidence = 0.98f
                )
            )
        }

        // 2. URLs
        URL_REGEX.findAll(rawText).forEach { match ->
            val url = match.value.trim()
            val normalized = if (!url.startsWith("http://", ignoreCase = true) &&
                !url.startsWith("https://", ignoreCase = true)) {
                "https://$url"
            } else url
            entities.add(
                DetectedEntity(
                    type = EntityType.URL,
                    rawValue = url,
                    normalizedValue = normalized,
                    confidence = 0.95f
                )
            )
        }

        // 3. Emails
        EMAIL_REGEX.findAll(rawText).forEach { match ->
            entities.add(
                DetectedEntity(
                    type = EntityType.EMAIL,
                    rawValue = match.value.trim(),
                    normalizedValue = match.value.trim().lowercase(),
                    confidence = 0.98f
                )
            )
        }

        // 4. Phone numbers (exclude simple dates or isolated 4-digit numbers)
        PHONE_REGEX.findAll(rawText).forEach { match ->
            val digitsOnly = match.value.filter { it.isDigit() }
            if (digitsOnly.length in 10..13) {
                // Avoid matching PIN code if exactly 6 digits
                entities.add(
                    DetectedEntity(
                        type = EntityType.PHONE_NUMBER,
                        rawValue = match.value.trim(),
                        normalizedValue = digitsOnly,
                        confidence = 0.90f
                    )
                )
            }
        }

        // 5. Dates
        DATE_REGEX.findAll(rawText).forEach { match ->
            entities.add(
                DetectedEntity(
                    type = EntityType.DATE_TIME,
                    rawValue = match.value.trim(),
                    normalizedValue = match.value.trim(),
                    confidence = 0.92f
                )
            )
        }

        // 6. Times
        TIME_REGEX.findAll(rawText).forEach { match ->
            entities.add(
                DetectedEntity(
                    type = EntityType.DATE_TIME,
                    rawValue = match.value.trim(),
                    normalizedValue = match.value.trim(),
                    confidence = 0.90f
                )
            )
        }

        // 7. Prices
        PRICE_REGEX.findAll(rawText).forEach { match ->
            entities.add(
                DetectedEntity(
                    type = EntityType.NUMERIC_FINANCIAL,
                    rawValue = match.value.trim(),
                    normalizedValue = match.value.trim(),
                    confidence = 0.92f
                )
            )
        }

        // 8. Task markers
        TASK_MARKER_REGEX.findAll(rawText).forEach { match ->
            val taskText = match.groups[1]?.value?.trim() ?: match.value.trim()
            if (taskText.isNotBlank()) {
                entities.add(
                    DetectedEntity(
                        type = EntityType.TASK_TODO,
                        rawValue = match.value.trim(),
                        normalizedValue = taskText,
                        confidence = 0.88f
                    )
                )
            }
        }

        // 9. Location & Address cues
        val lines = rawText.lines()
        for (line in lines) {
            val lower = line.lowercase()
            val hasAddressKeyword = ADDRESS_KEYWORDS.any { lower.contains(it) }
            val hasPin = PINCODE_REGEX.containsMatchIn(line)
            if (hasAddressKeyword || hasPin) {
                entities.add(
                    DetectedEntity(
                        type = EntityType.LOCATION_ADDRESS,
                        rawValue = line.trim(),
                        normalizedValue = line.trim(),
                        confidence = if (hasPin && hasAddressKeyword) 0.92f else 0.78f
                    )
                )
            }
        }

        return entities
    }
}
