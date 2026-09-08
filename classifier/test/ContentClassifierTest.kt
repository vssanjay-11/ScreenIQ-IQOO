package com.screeniq.classifier.test

import com.screeniq.classifier.LayeredContentClassifier
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ScreenContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentClassifierTest {

    private lateinit var classifier: LayeredContentClassifier

    @Before
    fun setUp() {
        classifier = LayeredContentClassifier()
    }

    @Test
    fun testEventClassification() = runBlocking {
        val input = """
            AI Workshop
            20 September
            10 AM
            Chennai
        """.trimIndent()

        val content = ScreenContent(captureId = "cap_event", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.EVENT, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.80f)
        assertEquals("AI Workshop", result.structuredFields["title"])
        assertEquals("20 September", result.structuredFields["date"])
        assertEquals("10 AM", result.structuredFields["time"])
        assertEquals("Chennai", result.structuredFields["location"])
    }

    @Test
    fun testProductClassification() = runBlocking {
        val input = """
            iQOO 12 5G Smartphone
            ₹52,999
            Snapdragon 8 Gen 3, 16GB RAM, 5000mAh Battery
            Free Delivery on Amazon
        """.trimIndent()

        val content = ScreenContent(captureId = "cap_prod", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.PRODUCT, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.80f)
        assertEquals("₹52,999", result.structuredFields["price"])
        assertEquals("Amazon", result.structuredFields["merchant"])
    }

    @Test
    fun testAddressLocationClassification() = runBlocking {
        val input = "Anna University Highway Campus, Sardar Patel Road, Guindy, Chennai 600025"

        val content = ScreenContent(captureId = "cap_loc", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.LOCATION, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.80f)
        assertEquals("Chennai", result.structuredFields["city"])
        assertEquals("600025", result.structuredFields["postalCode"])
    }

    @Test
    fun testPhoneClassification() = runBlocking {
        val input = "Call +91 98765 43210"

        val content = ScreenContent(captureId = "cap_phone", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.PHONE, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.85f)
        assertEquals("+91 98765 43210", result.structuredFields["phoneNumber"])
    }

    @Test
    fun testUrlClassification() = runBlocking {
        val input = "https://github.com/iqoo/screen-iq"

        val content = ScreenContent(captureId = "cap_url", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.URL, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.90f)
        assertEquals("https://github.com/iqoo/screen-iq", result.structuredFields["url"])
        assertEquals("github.com", result.structuredFields["domain"])
    }

    @Test
    fun testContactClassification() = runBlocking {
        val input = """
            Dr. Arvind Raman
            Chief AI Architect
            TechCorp India
            arvind@techcorp.in
            +91 98401 12345
        """.trimIndent()

        val content = ScreenContent(captureId = "cap_contact", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.CONTACT, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.80f)
        assertEquals("Dr. Arvind Raman", result.structuredFields["name"])
        assertEquals("arvind@techcorp.in", result.structuredFields["email"])
        assertEquals("+91 98401 12345", result.structuredFields["phone"])
    }

    @Test
    fun testTaskClassification() = runBlocking {
        val input = """
            [ ] Prepare presentation slides for iQOO Hackathon demo by 20 Sept
            Priority: Urgent
        """.trimIndent()

        val content = ScreenContent(captureId = "cap_task", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.TASK, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.80f)
        assertTrue(result.structuredFields["title"]!!.contains("Prepare presentation slides"))
        assertEquals("HIGH", result.structuredFields["priority"])
    }

    @Test
    fun testDocumentClassification() = runBlocking {
        val input = """
            Privacy Policy and Data Governance Guidelines
            This document outlines the strict privacy-first architecture of the on-device AI system.
            All screen buffers are held strictly in volatile memory. No screen pixels are ever persisted to disk
            or transmitted to remote cloud servers without explicit user consent. Data retention is strictly local
            and user-controlled, ensuring compliance with hackathon data handling and phone-first privacy protocols.
        """.trimIndent()

        val content = ScreenContent(captureId = "cap_doc", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.DOCUMENT, result.primaryCategory)
        assertTrue(result.confidenceScore >= 0.75f)
        assertNotNull(result.structuredFields["wordCount"])
    }

    @Test
    fun testUnknownContentClassification() = runBlocking {
        val input = "#@!$%^&* ???"

        val content = ScreenContent(captureId = "cap_unknown", rawFullText = input)
        val result = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.UNKNOWN, result.primaryCategory)
        assertNotNull(result.structuredFields["reason"])
    }
}
