package com.screeniq.ocr.entity

import android.graphics.Rect
import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.EntityType
import java.util.regex.Pattern

/**
 * High-precision extractor for structured entities in raw or clustered OCR text:
 * - Phone numbers (domestic & international)
 * - Email addresses
 * - URLs & web links
 * - Dates (relative & absolute)
 * - Times (12h & 24h)
 * - Addresses and physical venues
 * - Currencies and prices
 * - Event clues & workshop notices
 */
class EntityPatternExtractor {

    companion object {
        // Phone numbers: +91 98765 43210, +1-800-555-0199, (555) 123-4567, 9876543210
        private val PHONE_PATTERN = Pattern.compile(
            """(?:\+?\d{1,3}[-.\s]?)?(?:\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{4,5}\b"""
        )

        // Email addresses: standard RFC-compliant format
        private val EMAIL_PATTERN = Pattern.compile(
            """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b"""
        )

        // URLs & Domains: https://..., http://..., www...., or domain.com/path
        private val URL_PATTERN = Pattern.compile(
            """\b(?:https?://|www\.)[a-zA-Z0-9\-._~:/?#[\]@!$&'()*+,;=]+|(?:[a-zA-Z0-9\-]+\.)+(?:com|org|net|io|edu|gov|in|ai|co|app|dev)(?:/[a-zA-Z0-9\-._~:/?#[\]@!$&'()*+,;=]*)?\b""",
            Pattern.CASE_INSENSITIVE
        )

        // Currency / Price patterns: $99.99, ₹1,499, Rs. 500, EUR 45, USD 100, etc.
        private val CURRENCY_PATTERN = Pattern.compile(
            """(?:\$|₹|€|£|Rs\.?|INR|USD|EUR)\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\b"""
        )

        // Date patterns: 20 September 2026, 20 Sept 2026, 20/09/2026, 2026-09-20, Sept 20, Tomorrow, etc.
        private val DATE_PATTERN = Pattern.compile(
            """\b(?:\d{1,2}(?:st|nd|rd|th)?[\s/-](?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?|\d{1,2})[\s/-]\d{2,4}|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:,?\s+\d{2,4})?|\d{4}-\d{2}-\d{2}|today|tomorrow|yesterday)\b""",
            Pattern.CASE_INSENSITIVE
        )

        // Time patterns: 10:00 AM, 10:00am, 14:30, 7:00 PM, 10 AM, etc.
        private val TIME_PATTERN = Pattern.compile(
            """\b(?:(?:1[0-2]|0?[1-9]):[0-5][0-9]\s*(?:AM|PM|am|pm)?|(?:2[0-3]|[01]?[0-9]):[0-5][0-9]|(?:1[0-2]|0?[1-9])\s*(?:AM|PM|am|pm))\b"""
        )

        // Address / Location indicators
        private val LOCATION_KEYWORDS = listOf(
            "hall", "auditorium", "seminar", "campus", "university", "institute",
            "college", "road", "street", "st.", "avenue", "ave.", "block", "floor",
            "nagar", "colony", "layout", "sector", "chennai", "bangalore", "delhi", "mumbai",
            "room", "building", "complex", "lane", "junction"
        )

        // Event clues / indicators
        private val EVENT_KEYWORDS = listOf(
            "workshop", "webinar", "conference", "hackathon", "meetup", "summit",
            "symposium", "seminar", "orientation", "masterclass", "keynote", "ceremony"
        )
    }

    /**
     * Extracts all structured entities found within [text].
     * Can optionally correlate with a given [boundingBox].
     */
    fun extractEntities(text: String, boundingBox: Rect? = null): List<DetectedEntity> {
        if (text.isBlank()) return emptyList()

        val entities = mutableListOf<DetectedEntity>()

        // 1. Phone Numbers
        extractMatches(PHONE_PATTERN, text) { match ->
            val clean = match.replace(Regex("""[^\d+]"""), "")
            // Only consider if at least 7 digits to avoid false positives with dates or prices
            if (clean.replace("+", "").length in 7..15 && !match.contains("/")) {
                DetectedEntity(
                    type = EntityType.PHONE_NUMBER,
                    rawValue = match.trim(),
                    normalizedValue = formatPhoneNumber(match.trim()),
                    boundingBox = boundingBox,
                    confidence = 0.95f
                )
            } else null
        }?.let { entities.addAll(it) }

        // 2. Email Addresses
        extractMatches(EMAIL_PATTERN, text) { match ->
            DetectedEntity(
                type = EntityType.EMAIL,
                rawValue = match.trim(),
                normalizedValue = match.trim().lowercase(),
                boundingBox = boundingBox,
                confidence = 0.99f
            )
        }?.let { entities.addAll(it) }

        // 3. URLs
        extractMatches(URL_PATTERN, text) { match ->
            // Filter out email matches
            if (!match.contains("@")) {
                var normalized = match.trim()
                if (!normalized.startsWith("http://", ignoreCase = true) &&
                    !normalized.startsWith("https://", ignoreCase = true)) {
                    normalized = "https://$normalized"
                }
                DetectedEntity(
                    type = EntityType.URL,
                    rawValue = match.trim(),
                    normalizedValue = normalized,
                    boundingBox = boundingBox,
                    confidence = 0.95f
                )
            } else null
        }?.let { entities.addAll(it) }

        // 4. Currency / Price
        extractMatches(CURRENCY_PATTERN, text) { match ->
            DetectedEntity(
                type = EntityType.NUMERIC_FINANCIAL,
                rawValue = match.trim(),
                normalizedValue = normalizeCurrency(match.trim()),
                boundingBox = boundingBox,
                confidence = 0.90f
            )
        }?.let { entities.addAll(it) }

        // 5. Dates
        extractMatches(DATE_PATTERN, text) { match ->
            DetectedEntity(
                type = EntityType.DATE_TIME,
                rawValue = match.trim(),
                normalizedValue = normalizeDate(match.trim()),
                boundingBox = boundingBox,
                confidence = 0.92f,
                metadata = mapOf("subType" to "DATE")
            )
        }?.let { entities.addAll(it) }

        // 6. Times
        extractMatches(TIME_PATTERN, text) { match ->
            // Avoid standalone 1-2 digit numbers that match by boundary if no AM/PM
            if (match.contains(":") || match.contains("am", ignoreCase = true) || match.contains("pm", ignoreCase = true)) {
                DetectedEntity(
                    type = EntityType.DATE_TIME,
                    rawValue = match.trim(),
                    normalizedValue = match.trim().uppercase(),
                    boundingBox = boundingBox,
                    confidence = 0.90f,
                    metadata = mapOf("subType" to "TIME")
                )
            } else null
        }?.let { entities.addAll(it) }

        // 7. Venue / Address Lines
        val addressEntities = detectAddress(text, boundingBox)
        entities.addAll(addressEntities)

        // 8. Event Clues
        val eventEntities = detectEventClues(text, boundingBox)
        entities.addAll(eventEntities)

        return entities.distinctBy { it.type to it.rawValue }
    }

    private fun detectAddress(text: String, boundingBox: Rect?): List<DetectedEntity> {
        val lines = text.split("\n")
        val detected = mutableListOf<DetectedEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 5) continue

            val lower = trimmed.lowercase()
            val matchCount = LOCATION_KEYWORDS.count { lower.contains(it) }
            if (matchCount >= 1) {
                // Ensure it's not purely a date/url/email line
                if (!EMAIL_PATTERN.matcher(trimmed).find() && !URL_PATTERN.matcher(trimmed).find()) {
                    detected.add(
                        DetectedEntity(
                            type = EntityType.LOCATION_ADDRESS,
                            rawValue = trimmed,
                            normalizedValue = trimmed,
                            boundingBox = boundingBox,
                            confidence = if (matchCount >= 2) 0.92f else 0.82f,
                            metadata = mapOf("keywordMatches" to matchCount.toString())
                        )
                    )
                }
            }
        }
        return detected
    }

    private fun detectEventClues(text: String, boundingBox: Rect?): List<DetectedEntity> {
        val lines = text.split("\n")
        val detected = mutableListOf<DetectedEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 4) continue

            val lower = trimmed.lowercase()
            val match = EVENT_KEYWORDS.firstOrNull { lower.contains(it) }
            if (match != null) {
                detected.add(
                    DetectedEntity(
                        type = EntityType.PRODUCT_INFO, // Can represent structured subject/event
                        rawValue = trimmed,
                        normalizedValue = trimmed,
                        boundingBox = boundingBox,
                        confidence = 0.88f,
                        metadata = mapOf("clue" to "EVENT_TOPIC", "keyword" to match)
                    )
                )
            }
        }
        return detected
    }

    private fun extractMatches(
        pattern: Pattern,
        text: String,
        transform: (String) -> DetectedEntity?
    ): List<DetectedEntity> {
        val matcher = pattern.matcher(text)
        val list = mutableListOf<DetectedEntity>()
        while (matcher.find()) {
            val matched = matcher.group()
            val entity = transform(matched)
            if (entity != null) {
                list.add(entity)
            }
        }
        return list
    }

    private fun formatPhoneNumber(raw: String): String {
        return raw.replace(Regex("""[^\d+]"""), "")
    }

    private fun normalizeCurrency(raw: String): String {
        return raw.trim()
            .replace("Rs.", "₹")
            .replace("Rs", "₹")
            .replace("INR", "₹")
            .replace("USD", "$")
            .replace("EUR", "€")
    }

    private fun normalizeDate(raw: String): String {
        return raw.trim()
    }
}
