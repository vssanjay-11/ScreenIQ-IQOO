package com.screeniq.classifier.heuristic

import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.EntityType

/**
 * Result of heuristic scoring across content categories.
 */
data class HeuristicResult(
    val primaryCategory: ContentCategory,
    val secondaryCategories: List<ContentCategory>,
    val confidence: Float,
    val reasoning: String
)

/**
 * Stage 2 of the Layered Classification Pipeline:
 * Lightweight, zero-battery, deterministic scoring matrix based on entity co-occurrence,
 * semantic keyword density, and structural cues.
 */
class HeuristicClassifier {

    companion object {
        private val EVENT_KEYWORDS = setOf(
            "workshop", "webinar", "conference", "meetup", "hackathon", "summit",
            "session", "seminar", "symposium", "festival", "concert", "exhibition",
            "ceremony", "rsvp", "register", "venue", "agenda", "speakers", "hall",
            "am", "pm", "september", "october", "november", "december", "january",
            "february", "march", "april", "may", "june", "july", "august"
        )

        private val PRODUCT_KEYWORDS = setOf(
            "buy", "cart", "order", "price", "mrp", "discount", "off", "deal",
            "delivery", "stock", "warranty", "specs", "specification", "camera",
            "battery", "mah", "processor", "ram", "storage", "snapdragon", "display",
            "amazon", "flipkart", "iqoo", "smartphone", "laptop", "reviews", "rating"
        )

        private val TASK_KEYWORDS = setOf(
            "todo", "task", "action item", "checklist", "deadline", "due date",
            "priority", "urgent", "assigned", "follow up", "submit before", "reminder"
        )

        private val CONTACT_KEYWORDS = setOf(
            "dr", "dr.", "prof", "prof.", "mr", "mr.", "ms", "ms.", "mrs", "mrs.", "ceo", "cto", "founder", "director",
            "manager", "engineer", "architect", "designer", "consultant", "officer", "executive", "lead",
            "contact person", "designation", "business card", "phone", "phone:", "mobile", "mobile:", "tel", "tel:", "email"
        )

        private val LOCATION_KEYWORDS = setOf(
            "street", "road", "ave", "avenue", "nagar", "salai", "lane", "building",
            "block", "floor", "opp", "opposite", "near", "landmark", "pin", "pincode",
            "zip", "postal", "chennai", "bengaluru", "bangalore", "mumbai", "delhi"
        )
    }

    /**
     * Evaluates screen content and detected entities to compute classification scores.
     */
    fun classify(rawText: String, entities: List<DetectedEntity>): HeuristicResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return HeuristicResult(
                primaryCategory = ContentCategory.UNKNOWN,
                secondaryCategories = emptyList(),
                confidence = 0.99f,
                reasoning = "Screen text is blank or non-existent."
            )
        }

        val words = trimmed.lowercase().split(Regex("""[\s,.:;!?()\[\]{}]+""")).filter { it.isNotBlank() }
        val wordCount = words.size
        val entityTypes = entities.map { it.type }.toSet()

        // 1. QR Code check
        if (entityTypes.contains(EntityType.QR_BARCODE)) {
            return HeuristicResult(
                primaryCategory = ContentCategory.QR_CODE,
                secondaryCategories = emptyList(),
                confidence = 0.96f,
                reasoning = "Detected high-confidence QR payload pattern."
            )
        }

        // 2. Pure single-entity screens (Phone, Email, URL)
        if (wordCount <= 8) {
            val hasPhone = entities.any { it.type == EntityType.PHONE_NUMBER }
            val hasEmail = entities.any { it.type == EntityType.EMAIL }
            val hasUrl = entities.any { it.type == EntityType.URL }

            if (hasPhone && !hasEmail && !hasUrl && (wordCount <= 4 || words.any { it in listOf("call", "phone", "dial", "tel", "mobile") })) {
                return HeuristicResult(
                    primaryCategory = ContentCategory.PHONE,
                    secondaryCategories = emptyList(),
                    confidence = 0.95f,
                    reasoning = "Isolated phone number detected with call intent."
                )
            }

            if (hasEmail && !hasPhone && !hasUrl && wordCount <= 4) {
                return HeuristicResult(
                    primaryCategory = ContentCategory.EMAIL,
                    secondaryCategories = emptyList(),
                    confidence = 0.95f,
                    reasoning = "Isolated email address detected."
                )
            }

            if (hasUrl && !hasPhone && !hasEmail && wordCount <= 4) {
                return HeuristicResult(
                    primaryCategory = ContentCategory.URL,
                    secondaryCategories = emptyList(),
                    confidence = 0.95f,
                    reasoning = "Isolated web URL detected."
                )
            }
        }

        // 3. Multi-signal scoring
        val scores = mutableMapOf<ContentCategory, Float>()

        // EVENT Scoring
        val hasDate = entities.any { it.type == EntityType.DATE_TIME }
        val hasLocation = entities.any { it.type == EntityType.LOCATION_ADDRESS }
        val eventKeywordHits = words.count { it in EVENT_KEYWORDS }

        var eventScore = 0.0f
        if (hasDate && hasLocation) eventScore += 0.50f
        if (hasDate) eventScore += 0.25f
        if (eventKeywordHits > 0) eventScore += (eventKeywordHits * 0.12f).coerceAtMost(0.40f)
        if (eventScore > 0f) scores[ContentCategory.EVENT] = eventScore

        // PRODUCT Scoring
        val hasPrice = entities.any { it.type == EntityType.NUMERIC_FINANCIAL }
        val productKeywordHits = words.count { it in PRODUCT_KEYWORDS }

        var productScore = 0.0f
        if (hasPrice) productScore += 0.45f
        if (productKeywordHits > 0) productScore += (productKeywordHits * 0.12f).coerceAtMost(0.45f)
        if (productScore > 0f) scores[ContentCategory.PRODUCT] = productScore

        // TASK Scoring
        val hasTaskEntity = entities.any { it.type == EntityType.TASK_TODO }
        val taskKeywordHits = words.count { it in TASK_KEYWORDS }

        var taskScore = 0.0f
        if (hasTaskEntity) taskScore += 0.55f
        if (taskKeywordHits > 0) taskScore += (taskKeywordHits * 0.15f).coerceAtMost(0.40f)
        if (taskScore > 0f) scores[ContentCategory.TASK] = taskScore

        // CONTACT Scoring
        val hasContactPhone = entities.any { it.type == EntityType.PHONE_NUMBER }
        val hasContactEmail = entities.any { it.type == EntityType.EMAIL }
        val contactKeywordHits = words.count { it in CONTACT_KEYWORDS }

        var contactScore = 0.0f
        if (hasContactPhone && hasContactEmail) contactScore += 0.45f
        else if (hasContactPhone || hasContactEmail) contactScore += 0.20f
        if (contactKeywordHits > 0) contactScore += (contactKeywordHits * 0.15f).coerceAtMost(0.45f)
        if (contactScore > 0f) scores[ContentCategory.CONTACT] = contactScore

        // LOCATION / ADDRESS Scoring (standalone address without event)
        if (hasLocation && !hasDate) {
            val locationKeywordHits = words.count { it in LOCATION_KEYWORDS }
            val locationScore = 0.50f + (locationKeywordHits * 0.10f).coerceAtMost(0.40f)
            scores[ContentCategory.LOCATION] = locationScore
        }

        // DOCUMENT Scoring (high word count, prose, articles, notes)
        if (wordCount >= 35) {
            val lines = trimmed.lines()
            val documentScore = (0.45f + (wordCount / 150f).coerceAtMost(0.45f))
            scores[ContentCategory.DOCUMENT] = documentScore
        }

        // Select the winning category
        if (scores.isEmpty()) {
            return if (wordCount > 3) {
                HeuristicResult(
                    primaryCategory = ContentCategory.TEXT,
                    secondaryCategories = emptyList(),
                    confidence = 0.70f,
                    reasoning = "General plain text without strong category markers."
                )
            } else {
                HeuristicResult(
                    primaryCategory = ContentCategory.UNKNOWN,
                    secondaryCategories = emptyList(),
                    confidence = 0.50f,
                    reasoning = "Insufficient semantic signal to classify."
                )
            }
        }

        val sorted = scores.entries.sortedByDescending { it.value }
        val top = sorted.first()
        val secondary = sorted.drop(1).map { it.key }
        val normalizedConfidence = (top.value).coerceIn(0.55f, 0.96f)

        val reasoning = "Matched category ${top.key} based on weighted signals (score: ${"%.2f".format(top.value)})."

        return HeuristicResult(
            primaryCategory = top.key,
            secondaryCategories = secondary,
            confidence = normalizedConfidence,
            reasoning = reasoning
        )
    }
}
