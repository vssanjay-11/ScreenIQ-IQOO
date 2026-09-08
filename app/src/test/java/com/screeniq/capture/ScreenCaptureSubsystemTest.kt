package com.screeniq.capture

import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.capture.memory.CaptureMemoryManager
import com.screeniq.capture.testing.FakeScreenCaptureEngine
import com.screeniq.capture.trigger.GestureCaptureCoordinator
import com.screeniq.core.contracts.GestureTriggerEvent
import com.screeniq.core.contracts.GestureTriggerListener
import com.screeniq.core.model.ScreenCaptureResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import android.graphics.Bitmap

/**
 * Unit tests validating AGENT 3 Screen Capture Subsystem requirements:
 * 1. Capture normal screen.
 * 2. Return valid image data.
 * 3. Handle failure gracefully (secure window, unavailable, rate limit, timeout).
 * 4. Avoid memory leaks / verify recycling.
 * 5. Preferred flow (Gesture -> Capture -> Downstream).
 */
class ScreenCaptureSubsystemTest {

    @Test
    fun testCaptureNormalScreenReturnsValidResult() = runTest {
        val fakeEngine = FakeScreenCaptureEngine()
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1080)
        `when`(mockBitmap.height).thenReturn(2400)

        fakeEngine.enqueueSuccess(
            width = 1080,
            height = 2400,
            sourcePackage = "com.android.chrome",
            mockBitmap = mockBitmap
        )

        val result = fakeEngine.captureCurrentScreen()

        assertTrue("Capture should succeed", result.isSuccess)
        val capture = result.getOrNull()
        assertNotNull(capture)
        assertEquals(1080, capture!!.width)
        assertEquals(2400, capture.height)
        assertEquals("com.android.chrome", capture.sourcePackage)
        assertNotNull(capture.captureId)
        assertTrue(capture.timestampMs > 0)
        assertEquals(1, fakeEngine.captureInvocationCount)
    }

    @Test
    fun testHandleSecureWindowFailureGracefully() = runTest {
        val fakeEngine = FakeScreenCaptureEngine()
        fakeEngine.enqueueFailure(CaptureError.SecureWindowDetected)

        val result = fakeEngine.captureCurrentScreen()

        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("SECURE_WINDOW", exception!!.error.code)
        assertTrue(exception.error is CaptureError.SecureWindowDetected)
    }

    @Test
    fun testHandleServiceUnavailableFailureGracefully() = runTest {
        val fakeEngine = FakeScreenCaptureEngine()
        fakeEngine.enqueueFailure(CaptureError.ServiceUnavailable)

        val result = fakeEngine.captureCurrentScreen()

        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("SERVICE_UNAVAILABLE", exception!!.error.code)
        assertTrue(exception.error is CaptureError.ServiceUnavailable)
    }

    @Test
    fun testHandleRateLimitedFailureGracefully() = runTest {
        val fakeEngine = FakeScreenCaptureEngine()
        fakeEngine.enqueueFailure(CaptureError.RateLimited)

        val result = fakeEngine.captureCurrentScreen()

        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("RATE_LIMITED", exception!!.error.code)
        assertTrue(exception.error is CaptureError.RateLimited)
    }

    @Test
    fun testHandleTimeoutFailureGracefully() = runTest {
        val fakeEngine = FakeScreenCaptureEngine()
        fakeEngine.enqueueFailure(CaptureError.Timeout(3000L))

        val result = fakeEngine.captureCurrentScreen()

        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("TIMEOUT", exception!!.error.code)
        assertTrue(exception.error is CaptureError.Timeout)
    }

    @Test
    fun testGestureCoordinatorPreferredFlow() = runTest {
        val flow = MutableSharedFlow<GestureTriggerEvent>()
        val mockGestureListener = object : GestureTriggerListener {
            override val gestureEvents = flow.asSharedFlow()
            var detectionEnabled = false
            override fun enableDetection() { detectionEnabled = true }
            override fun disableDetection() { detectionEnabled = false }
        }

        val fakeEngine = FakeScreenCaptureEngine()
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1080)
        `when`(mockBitmap.height).thenReturn(2400)
        fakeEngine.enqueueSuccess(mockBitmap = mockBitmap)

        var downstreamResult: Result<ScreenCaptureResult>? = null
        val coordinator = GestureCaptureCoordinator(
            gestureListener = mockGestureListener,
            captureEngine = fakeEngine,
            onCaptureResult = { downstreamResult = it }
        )

        // Directly simulate trigger
        val event = GestureTriggerEvent(pointerCount = 4, timestampMs = 12345678L)
        coordinator.onGestureReceived(event)

        assertNotNull("Downstream analyzer must receive capture result", downstreamResult)
        assertTrue(downstreamResult!!.isSuccess)
        assertEquals(1080, downstreamResult!!.getOrNull()!!.width)
    }

    @Test
    fun testGestureCoordinatorGracefulFailureToDownstream() = runTest {
        val flow = MutableSharedFlow<GestureTriggerEvent>()
        val mockGestureListener = object : GestureTriggerListener {
            override val gestureEvents = flow.asSharedFlow()
            override fun enableDetection() {}
            override fun disableDetection() {}
        }

        val fakeEngine = FakeScreenCaptureEngine()
        fakeEngine.enqueueFailure(CaptureError.SecureWindowDetected)

        var downstreamResult: Result<ScreenCaptureResult>? = null
        val coordinator = GestureCaptureCoordinator(
            gestureListener = mockGestureListener,
            captureEngine = fakeEngine,
            onCaptureResult = { downstreamResult = it }
        )

        val event = GestureTriggerEvent(pointerCount = 4)
        coordinator.onGestureReceived(event)

        assertNotNull(downstreamResult)
        assertTrue(downstreamResult!!.isFailure)
        val exception = downstreamResult!!.exceptionOrNull() as? CaptureException
        assertNotNull(exception)
        assertEquals("SECURE_WINDOW", exception!!.error.code)
    }

    @Test
    fun testMemoryManagerSafeRecycle() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.isRecycled).thenReturn(false)

        CaptureMemoryManager.safeRecycle(mockBitmap)
        verify(mockBitmap).recycle()

        // Recycling an already-recycled bitmap should be a safe no-op
        val alreadyRecycledBitmap = mock(Bitmap::class.java)
        `when`(alreadyRecycledBitmap.isRecycled).thenReturn(true)
        CaptureMemoryManager.safeRecycle(alreadyRecycledBitmap)

        // Safe recycle of null should not throw
        CaptureMemoryManager.safeRecycle(null)
    }

    @Test
    fun testMemoryManagerUseScopeAutomaticallyRecycles() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.isRecycled).thenReturn(false)

        val captureResult = ScreenCaptureResult(
            captureId = "test-123",
            bitmap = mockBitmap,
            width = 100,
            height = 100,
            timestampMs = System.currentTimeMillis()
        )

        val executed = CaptureMemoryManager.use(captureResult) { result ->
            assertEquals("test-123", result.captureId)
            true
        }

        assertTrue(executed)
        verify(mockBitmap).recycle()
    }
}
