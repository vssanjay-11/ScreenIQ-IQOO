package com.screeniq.capture

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import com.screeniq.capture.accessibility.AccessibilityCaptureBridge
import com.screeniq.capture.accessibility.AccessibilityScreenshotProvider
import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.core.model.ScreenCaptureResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.Executor

class AccessibilityScreenshotProviderTest {

    @Test
    fun testProviderFailsWhenServiceNotConnected() = runTest {
        val disconnectedBridge = object : AccessibilityCaptureBridge {
            override val isServiceConnected: Boolean = false
            override val currentForegroundPackage: String? = null
            override fun takeScreenshot(
                displayId: Int,
                executor: Executor,
                callback: AccessibilityService.TakeScreenshotCallback
            ) {
                // Not called
            }
        }

        val provider = AccessibilityScreenshotProvider(disconnectedBridge)
        val result = provider.capture()

        assertTrue("Should fail when service is disconnected", result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("SERVICE_UNAVAILABLE", exception!!.error.code)
    }

    @Test
    fun testProviderMapsSecureWindowErrorCode() = runTest {
        val bridge = object : AccessibilityCaptureBridge {
            override val isServiceConnected: Boolean = true
            override val currentForegroundPackage: String = "com.banking.app"
            override fun takeScreenshot(
                displayId: Int,
                executor: Executor,
                callback: AccessibilityService.TakeScreenshotCallback
            ) {
                // Simulate OS returning ERROR_TAKE_SCREENSHOT_INVALID_WINDOW (code 5)
                executor.execute {
                    callback.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW)
                }
            }
        }

        val provider = AccessibilityScreenshotProvider(bridge)
        val result = provider.capture()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("SECURE_WINDOW", exception!!.error.code)
    }

    @Test
    fun testProviderMapsRateLimitedErrorCode() = runTest {
        val bridge = object : AccessibilityCaptureBridge {
            override val isServiceConnected: Boolean = true
            override val currentForegroundPackage: String? = null
            override fun takeScreenshot(
                displayId: Int,
                executor: Executor,
                callback: AccessibilityService.TakeScreenshotCallback
            ) {
                executor.execute {
                    callback.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_RIGID)
                }
            }
        }

        val provider = AccessibilityScreenshotProvider(bridge)
        val result = provider.capture()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("RATE_LIMITED", exception!!.error.code)
    }

    @Test
    fun testProviderMapsInvalidDisplayErrorCode() = runTest {
        val bridge = object : AccessibilityCaptureBridge {
            override val isServiceConnected: Boolean = true
            override val currentForegroundPackage: String? = null
            override fun takeScreenshot(
                displayId: Int,
                executor: Executor,
                callback: AccessibilityService.TakeScreenshotCallback
            ) {
                executor.execute {
                    callback.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY)
                }
            }
        }

        val provider = AccessibilityScreenshotProvider(bridge, defaultDisplayId = 99)
        val result = provider.capture()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("INVALID_DISPLAY", exception!!.error.code)
    }

    @Test
    fun testScreenCaptureManagerFallbackInvokedWhenPrimaryFails() = runTest {
        val failingBridge = object : AccessibilityCaptureBridge {
            override val isServiceConnected: Boolean = false
            override val currentForegroundPackage: String? = null
            override fun takeScreenshot(
                displayId: Int,
                executor: Executor,
                callback: AccessibilityService.TakeScreenshotCallback
            ) {}
        }

        val mockBitmap = mock(Bitmap::class.java)
        var fallbackCalled = false
        val fallbackProvider: suspend () -> Result<ScreenCaptureResult> = {
            fallbackCalled = true
            Result.success(
                ScreenCaptureResult(
                    captureId = "fallback-id",
                    bitmap = mockBitmap,
                    width = 1080,
                    height = 2400,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }

        val manager = ScreenCaptureManager(
            accessibilityProvider = AccessibilityScreenshotProvider(failingBridge),
            fallbackProvider = fallbackProvider
        )

        val result = manager.captureCurrentScreen()

        assertTrue("Manager should succeed using fallback", result.isSuccess)
        assertTrue("Fallback provider must have been invoked", fallbackCalled)
        assertEquals("fallback-id", result.getOrNull()!!.captureId)
    }
}
