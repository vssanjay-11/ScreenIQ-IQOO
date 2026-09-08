package com.screeniq.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.screeniq.core.model.ScreenCaptureResult
import java.util.UUID

/**
 * Agent 10: Controlled Demo Harness
 * Generates realistic on-device bitmaps containing target demo text for:
 * 1. EVENT -> Add to Calendar
 * 2. LOCATION -> Open Maps
 * 3. PHONE / CONTACT -> Dial / Save Contact
 *
 * Allows 100% deterministic demonstration of OCR, Classification, Action Planning,
 * and Native Execution even on emulators or devices without active screen events.
 */
object DemoHarness {

    const val EVENT_TEXT = """AI Workshop
20 September 2026
10:00 AM
Seminar Hall 2, Anna University"""

    const val LOCATION_TEXT = """Marina Beach Promenade
Kamarajar Salai, Triplicane
Chennai, Tamil Nadu 600005"""

    const val CONTACT_TEXT = """Dr. Ramesh Babu
Lead Conference Chair
Phone: +91 98765 43210
Email: ramesh.babu@annauniv.edu"""

    const val PRODUCT_TEXT = """iQOO 13 5G Smartphone
Snapdragon 8 Elite, 144Hz AMOLED
Price: ₹54,999
Available on Amazon India"""

    const val URL_TEXT = """Explore ScreenIQ Documentation
Official Guide: https://screeniq.ai/docs
Chennai Hackathon 2026 Edition"""

    /**
     * Renders high-contrast clean text onto an in-memory bitmap.
     * Perfect for OCR testing and validation.
     */
    fun createSampleBitmap(text: String, width: Int = 1080, height: Int = 1920): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark modern background matching iQOO styling
        canvas.drawColor(Color.parseColor("#121212"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            isFakeBoldText = true
        }

        val lines = text.split("\n")
        var y = 350f

        // Draw card background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1E2E")
            style = Paint.Style.FILL
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6B00") // iQOO orange accent
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        canvas.drawRoundRect(60f, 250f, width - 60f, 250f + (lines.size * 90f) + 120f, 32f, 32f, cardPaint)
        canvas.drawRoundRect(60f, 250f, width - 60f, 250f + (lines.size * 90f) + 120f, 32f, 32f, cardBorder)

        for (line in lines) {
            canvas.drawText(line, 120f, y, paint)
            y += 85f
        }

        return bitmap
    }

    fun createCaptureResult(text: String, packageName: String = "com.demo.screeniq"): ScreenCaptureResult {
        val bitmap = createSampleBitmap(text)
        return ScreenCaptureResult(
            captureId = UUID.randomUUID().toString(),
            bitmap = bitmap,
            width = bitmap.width,
            height = bitmap.height,
            timestampMs = System.currentTimeMillis(),
            sourcePackage = packageName
        )
    }

    fun getEventCaptureResult(): ScreenCaptureResult = createCaptureResult(EVENT_TEXT, "com.whatsapp")
    fun getLocationCaptureResult(): ScreenCaptureResult = createCaptureResult(LOCATION_TEXT, "com.google.android.apps.maps")
    fun getContactCaptureResult(): ScreenCaptureResult = createCaptureResult(CONTACT_TEXT, "com.google.android.dialer")
    fun getProductCaptureResult(): ScreenCaptureResult = createCaptureResult(PRODUCT_TEXT, "in.amazon.mShop.android.shopping")
    fun getUrlCaptureResult(): ScreenCaptureResult = createCaptureResult(URL_TEXT, "com.android.chrome")
}
