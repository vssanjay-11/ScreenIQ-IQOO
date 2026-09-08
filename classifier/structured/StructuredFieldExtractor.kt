package com.screeniq.classifier.structured

import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.EntityType

/**
 * Extracts normalized, structured key-value fields for each specific [ContentCategory].
 * For example:
 * EVENT -> title, date, time, location
 * PRODUCT -> title, price, merchant, specs
 * LOCATION -> address, city, postalCode
 */
class StructuredFieldExtractor {

    fun extractFields(
        category: ContentCategory,
        rawText: String,
        entities: List<DetectedEntity>
    ): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        when (category) {
            ContentCategory.EVENT -> {
                // Event Title is usually the first prominent line
                val title = lines.firstOrNull { line ->
                    !line.matches(Regex("""(?i).*(?:am|pm|\d{1,2}:\d{2}|hall|chennai|delhi|mumbai|campus).*""")) &&
                            line.length in 3..60
                } ?: lines.firstOrNull() ?: "Event"
                fields["title"] = title

                // Date
                val dateEntity = entities.firstOrNull { it.type == EntityType.DATE_TIME && !it.rawValue.contains(Regex("""(?i)(?:am|pm|:\d{2})""")) }
                fields["date"] = dateEntity?.rawValue ?: entities.firstOrNull { it.type == EntityType.DATE_TIME }?.rawValue.orEmpty()

                // Time
                val timeEntity = entities.firstOrNull { it.type == EntityType.DATE_TIME && it.rawValue.contains(Regex("""(?i)(?:am|pm|:\d{2})""")) }
                fields["time"] = timeEntity?.rawValue.orEmpty()

                // Location / Venue
                val locationEntity = entities.firstOrNull { it.type == EntityType.LOCATION_ADDRESS }
                fields["location"] = locationEntity?.rawValue ?: lines.lastOrNull { line ->
                    line != title && line != fields["date"] && line != fields["time"]
                }.orEmpty()
            }

            ContentCategory.PRODUCT -> {
                fields["title"] = lines.firstOrNull { it.length in 5..80 } ?: "Product"
                val priceEntity = entities.firstOrNull { it.type == EntityType.NUMERIC_FINANCIAL }
                fields["price"] = priceEntity?.rawValue.orEmpty()

                // Merchant detection
                val lower = rawText.lowercase()
                val merchant = when {
                    lower.contains("amazon") -> "Amazon"
                    lower.contains("flipkart") -> "Flipkart"
                    lower.contains("iqoo") -> "iQOO Store"
                    lower.contains("myntra") -> "Myntra"
                    else -> "Online Merchant"
                }
                fields["merchant"] = merchant

                // Specs / Details snippet
                val specsLine = lines.firstOrNull { it.contains(Regex("""(?i)(?:ram|gb|snapdragon|mah|display|camera)""")) }
                fields["specs"] = specsLine.orEmpty()
            }

            ContentCategory.LOCATION -> {
                val locationEntity = entities.firstOrNull { it.type == EntityType.LOCATION_ADDRESS }
                fields["address"] = locationEntity?.rawValue ?: lines.joinToString(", ")

                // City extraction
                val cities = listOf("Chennai", "Bengaluru", "Bangalore", "Mumbai", "Delhi", "Hyderabad", "Kolkata", "Pune")
                val matchedCity = cities.firstOrNull { rawText.contains(it, ignoreCase = true) }
                fields["city"] = matchedCity.orEmpty()

                // PIN / ZIP code
                val pinMatch = Regex("""\b[1-9][0-9]{5}\b""").find(rawText)
                fields["postalCode"] = pinMatch?.value.orEmpty()
            }

            ContentCategory.PHONE -> {
                val phoneEntity = entities.firstOrNull { it.type == EntityType.PHONE_NUMBER }
                val rawPhone = phoneEntity?.rawValue ?: lines.firstOrNull() ?: ""
                fields["phoneNumber"] = rawPhone
                fields["label"] = if (rawPhone.startsWith("+91") || rawPhone.length == 10) "Mobile" else "Contact"
            }

            ContentCategory.EMAIL -> {
                val emailEntity = entities.firstOrNull { it.type == EntityType.EMAIL }
                val email = emailEntity?.rawValue ?: lines.firstOrNull() ?: ""
                fields["emailAddress"] = email
                fields["domain"] = email.substringAfter("@", "")
            }

            ContentCategory.URL -> {
                val urlEntity = entities.firstOrNull { it.type == EntityType.URL }
                val url = urlEntity?.normalizedValue ?: urlEntity?.rawValue ?: lines.firstOrNull() ?: ""
                fields["url"] = url
                fields["domain"] = url.removePrefix("http://").removePrefix("https://").substringBefore("/")
                fields["protocol"] = if (url.startsWith("https://", ignoreCase = true)) "https" else "http"
            }

            ContentCategory.CONTACT -> {
                // Name usually first line
                fields["name"] = lines.firstOrNull { it.length in 3..40 && !it.contains("@") && !it.contains(Regex("""\d{5}""")) } ?: "Contact"
                fields["phone"] = entities.firstOrNull { it.type == EntityType.PHONE_NUMBER }?.rawValue.orEmpty()
                fields["email"] = entities.firstOrNull { it.type == EntityType.EMAIL }?.rawValue.orEmpty()

                // Designation or Organization
                val orgLine = lines.firstOrNull { line ->
                    line.contains(Regex("""(?i)(?:director|engineer|manager|lead|founder|officer|consultant|technologies|solutions|pvt|ltd|inc)"""))
                }
                fields["organization"] = orgLine.orEmpty()
            }

            ContentCategory.TASK -> {
                val taskEntity = entities.firstOrNull { it.type == EntityType.TASK_TODO }
                fields["title"] = taskEntity?.normalizedValue ?: lines.firstOrNull() ?: "New Task"
                fields["dueDate"] = entities.firstOrNull { it.type == EntityType.DATE_TIME }?.rawValue.orEmpty()
                fields["priority"] = if (rawText.contains(Regex("""(?i)(?:urgent|high priority|asap)"""))) "HIGH" else "NORMAL"
            }

            ContentCategory.DOCUMENT -> {
                fields["title"] = lines.firstOrNull { it.length in 5..80 } ?: "Document Note"
                val words = rawText.split(Regex("""\s+""")).filter { it.isNotBlank() }
                fields["wordCount"] = words.size.toString()
                fields["summarySnippet"] = lines.take(2).joinToString(" ").take(150)
            }

            ContentCategory.QR_CODE -> {
                val qrEntity = entities.firstOrNull { it.type == EntityType.QR_BARCODE }
                val payload = qrEntity?.rawValue ?: rawText.trim()
                fields["payload"] = payload
                fields["qrType"] = when {
                    payload.startsWith("upi://", ignoreCase = true) -> "UPI_PAYMENT"
                    payload.startsWith("WIFI:", ignoreCase = true) -> "WIFI_CONFIG"
                    payload.startsWith("http", ignoreCase = true) -> "WEB_URL"
                    else -> "TEXT_DATA"
                }
            }

            ContentCategory.IMAGE -> {
                fields["visualDescription"] = "Graphical/visual screen element"
            }

            ContentCategory.TEXT -> {
                fields["snippet"] = rawText.take(120)
            }

            ContentCategory.UNKNOWN -> {
                fields["rawSnippet"] = rawText.take(80)
                fields["reason"] = "Content does not match any recognized action patterns."
            }
        }

        return fields
    }
}
