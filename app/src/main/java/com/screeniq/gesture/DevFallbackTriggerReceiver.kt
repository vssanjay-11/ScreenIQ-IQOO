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
            val scenario = intent.getStringExtra("scenario")
            Log.d(TAG, "ScreenIQ dev fallback trigger broadcast received (scenario=$scenario).")
            val emitted = GestureTriggerManager.getInstance().notifyFallbackTrigger()
            Log.i(TAG, "Emitted 2-finger fallback trigger event. Accepted: $emitted")

            if (context != null) {
                try {
                    com.screeniq.ui.overlay.ScreenIQOverlayActivity.start(context, scenario)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch overlay activity: ${e.message}")
                }
            }
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
