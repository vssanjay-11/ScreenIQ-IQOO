package com.screeniq.core.contracts

import kotlinx.coroutines.flow.Flow

/**
 * Agent 2: Gesture and Accessibility Observer
 *
 * Emits gesture trigger events detected on the device screen (e.g. 4-finger swipe).
 * Defined in ARCHITECTURE.md §4.
 */
interface GestureTriggerListener {
    val gestureEvents: Flow<GestureTriggerEvent>
    fun enableDetection()
    fun disableDetection()
}

data class GestureTriggerEvent(
    val pointerCount: Int,
    val timestampMs: Long = System.currentTimeMillis()
)
