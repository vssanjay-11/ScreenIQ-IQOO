package com.screeniq.gesture

import com.screeniq.core.contracts.GestureTriggerEvent
import com.screeniq.core.contracts.GestureTriggerListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton / centralized manager for ScreenIQ gesture triggers.
 *
 * Implements [GestureTriggerListener] and bridges events detected by the
 * [com.screeniq.service.accessibility.ScreenIQAccessibilityService], overlay detectors,
 * and development fallback mechanisms to the downstream ScreenIQ pipeline.
 */
class GestureTriggerManager private constructor() : GestureTriggerListener {

    private val _gestureEvents = MutableSharedFlow<GestureTriggerEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val gestureEvents: Flow<GestureTriggerEvent> = _gestureEvents.asSharedFlow()

    private val isEnabled = AtomicBoolean(true)

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    override fun enableDetection() {
        isEnabled.set(true)
    }

    override fun disableDetection() {
        isEnabled.set(false)
    }

    fun isDetectionEnabled(): Boolean = isEnabled.get()

    /**
     * Emits a gesture trigger event if detection is enabled.
     *
     * @return true if the event was accepted and emitted, false if detection is disabled.
     */
    fun notifyTrigger(event: GestureTriggerEvent): Boolean {
        if (!isEnabled.get()) {
            return false
        }
        return _gestureEvents.tryEmit(event)
    }

    /**
     * Primary convenience method for notifying a 2-finger swipe trigger event.
     */
    fun notifyTwoFingerSwipe(pointerCount: Int = 2, timestampMs: Long = System.currentTimeMillis()): Boolean {
        return notifyTrigger(GestureTriggerEvent(pointerCount = pointerCount, timestampMs = timestampMs))
    }

    /**
     * Backward-compatibility convenience method for notifying a multi-finger swipe trigger event.
     */
    fun notifyFourFingerSwipe(pointerCount: Int = 2, timestampMs: Long = System.currentTimeMillis()): Boolean {
        return notifyTrigger(GestureTriggerEvent(pointerCount = pointerCount, timestampMs = timestampMs))
    }

    /**
     * Fallback trigger mechanism for development, testing, or demo shortcuts.
     */
    fun notifyFallbackTrigger(): Boolean {
        return notifyTrigger(
            GestureTriggerEvent(
                pointerCount = 2,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    /**
     * Updates the connection status of the AccessibilityService.
     */
    internal fun updateServiceConnectionState(connected: Boolean) {
        _isAccessibilityServiceConnected.value = connected
    }

    companion object {
        @Volatile
        private var instance: GestureTriggerManager? = null

        fun getInstance(): GestureTriggerManager {
            return instance ?: synchronized(this) {
                instance ?: GestureTriggerManager().also { instance = it }
            }
        }

        /**
         * For testing purposes only: resets the singleton instance.
         */
        internal fun resetForTesting() {
            synchronized(this) {
                instance = null
            }
        }
    }
}
