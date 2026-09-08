package com.screeniq.gesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver providing a development fallback trigger for testing ScreenIQ.
 *
 * Allows triggering the ScreenIQ 4-finger trigger event via ADB command:
 * ```bash
 * adb shell am broadcast -a com.screeniq.TRIGGER_FALLBACK
 * ```
 * Or from test automation / debug UI in the application.
 */
class DevFallbackTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_FALLBACK_TRIGGER) {
            Log.d(TAG, "ScreenIQ dev fallback trigger broadcast received.")
            val emitted = GestureTriggerManager.getInstance().notifyFallbackTrigger()
            Log.i(TAG, "Emitted 4-finger fallback trigger event. Accepted: $emitted")
        }
    }

    companion object {
        const val ACTION_FALLBACK_TRIGGER = "com.screeniq.TRIGGER_FALLBACK"
        private const val TAG = "ScreenIQ:DevTrigger"

        /**
         * Triggers the fallback event programmatically from app code.
         */
        fun sendProgrammaticTrigger(context: Context) {
            val intent = Intent(ACTION_FALLBACK_TRIGGER).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}
