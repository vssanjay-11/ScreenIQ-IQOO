package com.screeniq

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.screeniq.demo.DemoHarness
import com.screeniq.gesture.GestureTriggerManager
import com.screeniq.integration.ScreenIqPipeline
import com.screeniq.service.accessibility.ScreenIQAccessibilityService
import com.screeniq.ui.navigation.ScreenIQMainApp
import kotlinx.coroutines.launch

/**
 * ScreenIQ Primary Activity.
 * Launches the polished Jetpack Compose Dashboard, observes system accessibility gestures,
 * and hosts interactive demo scenarios.
 */
class MainActivity : ComponentActivity() {

    private lateinit var pipeline: ScreenIqPipeline
    private val gestureManager = GestureTriggerManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pipeline = ScreenIqPipeline.getInstance(this)

        // Observe gestures dispatched from ScreenIQAccessibilityService or DevFallbackTriggerReceiver
        lifecycleScope.launch {
            gestureManager.gestureEvents.collect { event ->
                Log.i(TAG, "Gesture trigger event received: pointerCount=${event.pointerCount}. Running pipeline...")
                pipeline.triggerPipeline()
            }
        }

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxSize()) {
                ScreenIQMainApp(
                    overlayController = pipeline.overlayController,
                    actionExecutor = pipeline.actionExecutor,
                    historyRepository = pipeline.historyRepository
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Check AccessibilityService status on launch
            LaunchedEffect(Unit) {
                if (ScreenIQAccessibilityService.instance == null) {
                    snackbarHostState.showSnackbar(
                        message = "Accessibility Service is not enabled. Tap Settings to enable ScreenIQ gesture detection.",
                        actionLabel = "Enable",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    /**
     * Helper to open system accessibility settings
     */
    fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ScreenIQ:MainActivity"
    }
}
