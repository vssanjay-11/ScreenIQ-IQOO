package com.screeniq.gesture

import com.screeniq.core.contracts.GestureTriggerEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestureTriggerManagerTest {

    private lateinit var manager: GestureTriggerManager

    @Before
    fun setUp() {
        GestureTriggerManager.resetForTesting()
        manager = GestureTriggerManager.getInstance()
    }

    @After
    fun tearDown() {
        GestureTriggerManager.resetForTesting()
    }

    @Test
    fun testNotifyTriggerEmitsEventToCollector() = runTest {
        val testEvent = GestureTriggerEvent(pointerCount = 4, timestampMs = 123456789L)

        val receivedEvents = mutableListOf<GestureTriggerEvent>()
        val job = launch {
            manager.gestureEvents.take(1).toList(receivedEvents)
        }

        val accepted = manager.notifyTrigger(testEvent)
        assertTrue("Event should be accepted when enabled", accepted)

        job.join()
        assertEquals(1, receivedEvents.size)
        assertEquals(4, receivedEvents[0].pointerCount)
        assertEquals(123456789L, receivedEvents[0].timestampMs)
    }

    @Test
    fun testDisableDetectionDropsEvents() = runTest {
        manager.disableDetection()
        assertFalse("Detection should report disabled", manager.isDetectionEnabled())

        val testEvent = GestureTriggerEvent(pointerCount = 4, timestampMs = 1000L)
        val accepted = manager.notifyTrigger(testEvent)
        assertFalse("Event must be rejected when detection is disabled", accepted)
    }

    @Test
    fun testReEnableDetectionRestoresEmission() = runTest {
        manager.disableDetection()
        val rejected = manager.notifyTrigger(GestureTriggerEvent(4, 1000L))
        assertFalse(rejected)

        manager.enableDetection()
        assertTrue(manager.isDetectionEnabled())

        val deferredEvent = async {
            manager.gestureEvents.first()
        }

        val accepted = manager.notifyTrigger(GestureTriggerEvent(4, 2000L))
        assertTrue("Event should be accepted after re-enabling", accepted)

        val emitted = deferredEvent.await()
        assertEquals(2000L, emitted.timestampMs)
    }

    @Test
    fun testFallbackTriggerProducesFourFingerEvent() = runTest {
        val deferredEvent = async {
            manager.gestureEvents.first()
        }

        val accepted = manager.notifyFallbackTrigger()
        assertTrue("Fallback trigger should be accepted", accepted)

        val emitted = deferredEvent.await()
        assertEquals(4, emitted.pointerCount)
        assertTrue(emitted.timestampMs > 0L)
    }

    @Test
    fun testServiceConnectionStateTracking() = runTest {
        assertFalse(manager.isAccessibilityServiceConnected.value)

        manager.updateServiceConnectionState(true)
        assertTrue(manager.isAccessibilityServiceConnected.value)

        manager.updateServiceConnectionState(false)
        assertFalse(manager.isAccessibilityServiceConnected.value)
    }
}
