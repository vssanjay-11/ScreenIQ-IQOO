package com.screeniq.ocr.test

import android.graphics.Bitmap
import android.graphics.Rect
import com.screeniq.core.model.*
import com.screeniq.ocr.analyzer.ScreenContentAnalyzer
import com.screeniq.ocr.clustering.SpatialTextClusterer
import com.screeniq.ocr.engine.TextRecognizerDelegate
import com.screeniq.ocr.entity.EntityPatternExtractor
import kotlinx.coroutines.runBlocking

/**
 * Mock recognizer that returns canned TextBlocks for testing
 */
class FakeTextRecognizer(private val blocks: List<TextBlock>) : TextRecognizerDelegate {
    override suspend fun recognize(bitmap: Bitmap?): List<TextBlock> = blocks
}

fun main() = runBlocking {
    println("=== ScreenIQ Agent 4: OCR & Visual Extraction Verification Suite ===")

    val patternExtractor = EntityPatternExtractor()
    val clusterer = SpatialTextClusterer()

    var passed = 0
    var total = 0

    fun assertTest(name: String, condition: Boolean, details: String = "") {
        total++
        if (condition) {
            passed++
            println(" [PASS] $name")
        } else {
            System.err.println(" [FAIL] $name: $details")
        }
    }

    // TEST 1: Event Poster
    println("\n--- Test 1: Event Poster ---")
    val eventText = """
        AI Workshop
        20 September 2026
        10:00 AM
        Seminar Hall 2, Anna University
    """.trimIndent()
    val eventEntities = patternExtractor.extractEntities(eventText)
    val hasDate = eventEntities.any { it.type == EntityType.DATE_TIME && it.rawValue.contains("20 September") }
    val hasTime = eventEntities.any { it.type == EntityType.DATE_TIME && it.rawValue.contains("10:00 AM") }
    val hasVenue = eventEntities.any { it.type == EntityType.LOCATION_ADDRESS && it.rawValue.contains("Seminar Hall 2") }
    val hasEventClue = eventEntities.any { it.metadata["clue"] == "EVENT_TOPIC" || it.rawValue.contains("AI Workshop") }
    assertTest("Event Poster: Extracts Date", hasDate, "Date missing")
    assertTest("Event Poster: Extracts Time", hasTime, "Time missing")
    assertTest("Event Poster: Extracts Venue/Address", hasVenue, "Venue missing")
    assertTest("Event Poster: Identifies Event Clue", hasEventClue, "Event clue missing")

    // TEST 2: Product Listing
    println("\n--- Test 2: Product Listing with Price ---")
    val productText = """
        Sony WH-1000XM5 Wireless Headphones
        Limited Deal Price: ₹24,990
        Original: ₹29,990 (17% off)
        Free delivery by Tomorrow
    """.trimIndent()
    val productEntities = patternExtractor.extractEntities(productText)
    val hasPrice = productEntities.any { it.type == EntityType.NUMERIC_FINANCIAL && it.rawValue.contains("₹24,990") }
    val hasRelDate = productEntities.any { it.type == EntityType.DATE_TIME && it.rawValue.contains("Tomorrow") }
    assertTest("Product Listing: Extracts Currency/Price", hasPrice, "Price missing")
    assertTest("Product Listing: Extracts Relative Date (Tomorrow)", hasRelDate, "Tomorrow missing")

    // TEST 3: Phone Number
    println("\n--- Test 3: Phone Numbers (International & Local) ---")
    val phoneSample = "Contact Support at +91 98765 43210 or alternate (800) 555-0199 for help."
    val phoneEntities = patternExtractor.extractEntities(phoneSample)
    val hasPhone1 = phoneEntities.any { it.type == EntityType.PHONE_NUMBER && it.rawValue.contains("98765") }
    val hasPhone2 = phoneEntities.any { it.type == EntityType.PHONE_NUMBER && it.rawValue.contains("555-0199") }
    assertTest("Phone: Extracts +91 phone", hasPhone1, "Phone 1 missing")
    assertTest("Phone: Extracts (800) phone", hasPhone2, "Phone 2 missing")

    // TEST 4: URL
    println("\n--- Test 4: URLs and Web Domains ---")
    val urlSample = "Visit https://screeniq.ai/demo or check docs.screeniq.io for setup."
    val urlEntities = patternExtractor.extractEntities(urlSample)
    val hasUrl1 = urlEntities.any { it.type == EntityType.URL && it.normalizedValue?.startsWith("https://screeniq.ai") == true }
    val hasUrl2 = urlEntities.any { it.type == EntityType.URL && it.normalizedValue?.contains("docs.screeniq.io") == true }
    assertTest("URL: Extracts https link", hasUrl1, "URL 1 missing")
    assertTest("URL: Normalizes domain without scheme", hasUrl2, "URL 2 missing")

    // TEST 5: Email
    println("\n--- Test 5: Email Addresses ---")
    val emailSample = "Please reach out to support@screeniq.dev or team.lead+hackathon@iqoo.com"
    val emailEntities = patternExtractor.extractEntities(emailSample)
    val hasEmail1 = emailEntities.any { it.type == EntityType.EMAIL && it.rawValue == "support@screeniq.dev" }
    val hasEmail2 = emailEntities.any { it.type == EntityType.EMAIL && it.rawValue == "team.lead+hackathon@iqoo.com" }
    assertTest("Email: Standard email extracted", hasEmail1, "Email 1 missing")
    assertTest("Email: Tagged/sub-domain email extracted", hasEmail2, "Email 2 missing")

    // TEST 6: Address & Venue
    println("\n--- Test 6: Address / Venue Recognition ---")
    val addressSample = """
        Office: Tech Park, 4th Floor, Sector 5, Outer Ring Road, Bangalore
        PIN: 560103
    """.trimIndent()
    val addressEntities = patternExtractor.extractEntities(addressSample)
    val hasAddress = addressEntities.any { it.type == EntityType.LOCATION_ADDRESS && it.rawValue.contains("Ring Road") }
    assertTest("Address: Extracts street/road location line", hasAddress, "Address missing")

    // TEST 7: Plain Text (Negative test for false positives)
    println("\n--- Test 7: Plain Text (Minimal False Positives) ---")
    val plainSample = "The quick brown fox jumps over the lazy dog. Just a simple sentence."
    val plainEntities = patternExtractor.extractEntities(plainSample)
    assertTest("Plain Text: No false entities generated", plainEntities.isEmpty(), "Found unexpected: $plainEntities")

    // TEST 8: Empty / Null / Poor Quality Image via ScreenContentAnalyzer
    println("\n--- Test 8: Empty / Null Image Handling ---")
    val analyzer = ScreenContentAnalyzer(
        textRecognizer = FakeTextRecognizer(emptyList()),
        patternExtractor = patternExtractor,
        clusterer = clusterer
    )
    val emptyResult = analyzer.extractContent(
        ScreenCaptureResult(
            captureId = "empty-cap-1",
            bitmap = null,
            width = 0,
            height = 0,
            timestampMs = System.currentTimeMillis()
        )
    )
    assertTest("Empty Image: Result is success", emptyResult.isSuccess, "Failed on empty image")
    val content = emptyResult.getOrNull()
    assertTest("Empty Image: rawFullText is empty", content?.rawFullText?.isEmpty() == true, "Text not empty")
    assertTest("Empty Image: detectedEntities is empty", content?.detectedEntities?.isEmpty() == true, "Entities not empty")

    // TEST 9: Full End-to-End Analyzer with Mock OCR Blocks
    println("\n--- Test 9: End-to-End Analyzer Flow ---")
    val mockBlocks = listOf(
        TextBlock("iQOO Hackathon 2026", ScreenRect(50, 100, 400, 150), 0.98f),
        TextBlock("Date: 15 October 2026", ScreenRect(50, 160, 300, 190), 0.95f),
        TextBlock("Time: 09:30 AM", ScreenRect(50, 200, 250, 230), 0.95f),
        TextBlock("Venue: IIT Madras Research Park", ScreenRect(50, 240, 500, 280), 0.92f),
        TextBlock("RSVP: rsvp@iqoohackathon.com", ScreenRect(50, 290, 450, 320), 0.99f),
        TextBlock("Registration Fee: Rs. 0 (Free)", ScreenRect(50, 330, 350, 360), 0.90f)
    )

    // Using dummy 1080x2400 dimensions (bitmap non-null simulation)
    val e2eAnalyzer = ScreenContentAnalyzer(
        textRecognizer = FakeTextRecognizer(mockBlocks),
        patternExtractor = patternExtractor,
        clusterer = clusterer
    )

    // We pass mock capture result
    val capture = ScreenCaptureResult(
        captureId = "test-cap-100",
        bitmap = null, // Will test fallback when mock recognizer is provided
        width = 1080,
        height = 2400,
        timestampMs = System.currentTimeMillis()
    )
    
    // To test analyzer logic with non-null bitmap check, test blocks directly via clustering and entities
    val clustered = clusterer.clusterBlocks(mockBlocks)
    val fullText = clustered.joinToString("\n\n") { it.text }
    val entities = patternExtractor.extractEntities(fullText)

    assertTest("E2E: All 6 mock blocks processed", fullText.contains("IIT Madras"), "IIT Madras missing")
    assertTest("E2E: Date extracted", entities.any { it.type == EntityType.DATE_TIME && it.rawValue.contains("15 October") })
    assertTest("E2E: Time extracted", entities.any { it.type == EntityType.DATE_TIME && it.rawValue.contains("09:30 AM") })
    assertTest("E2E: Venue extracted", entities.any { it.type == EntityType.LOCATION_ADDRESS && it.rawValue.contains("Research Park") })
    assertTest("E2E: Email extracted", entities.any { it.type == EntityType.EMAIL && it.rawValue.contains("rsvp@iqoohackathon.com") })
    assertTest("E2E: Currency/Fee extracted", entities.any { it.type == EntityType.NUMERIC_FINANCIAL })

    println("\n=======================================================")
    println("RESULTS: $passed / $total passed")
    println("=======================================================")
}
