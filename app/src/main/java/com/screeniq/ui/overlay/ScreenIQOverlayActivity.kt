package com.screeniq.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.demo.DemoHarness
import com.screeniq.integration.PipelineOutput
import com.screeniq.integration.ScreenIqPipeline
import com.screeniq.ui.theme.ScreenIQTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Translucent Floating Activity launched when a 2-finger swipe is detected.
 *
 * Appears directly over the foreground application:
 * 1. Shows a full-screen AI scanning animation.
 * 2. Concurrently captures the screen, runs OCR, classifies content, and plans actions.
 * 3. Transitions smoothly into a floating pop-up window listing completed processes & action options.
 * 4. Executes selected actions via native Android Intents and closes cleanly.
 */
class ScreenIQOverlayActivity : ComponentActivity() {

    private lateinit var pipeline: ScreenIqPipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pipeline = ScreenIqPipeline.getInstance(this)
        val scenarioName = intent.getStringExtra(EXTRA_SCENARIO)

        setContent {
            ScreenIQTheme(darkTheme = true) {
                var isScanning by remember { mutableStateOf(true) }
                var pipelineResult by remember { mutableStateOf<PipelineOutput?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                // Run screen capture and AI analysis concurrently with the scanning animation
                LaunchedEffect(Unit) {
                    val scanStartTime = System.currentTimeMillis()

                    val customCapture = when (scenarioName?.uppercase()) {
                        "LOCATION" -> DemoHarness.getLocationCaptureResult()
                        "CONTACT" -> DemoHarness.getContactCaptureResult()
                        "PRODUCT" -> DemoHarness.getProductCaptureResult()
                        "URL" -> DemoHarness.getUrlCaptureResult()
                        "EVENT" -> DemoHarness.getEventCaptureResult()
                        else -> null // Will use live accessibility screen capture
                    }

                    val result = pipeline.triggerPipeline(inputCapture = customCapture)

                    // Guarantee the full-screen scanning animation is shown for at least 1100ms
                    val elapsed = System.currentTimeMillis() - scanStartTime
                    if (elapsed < 1100L) {
                        delay(1100L - elapsed)
                    }

                    if (result.isSuccess) {
                        pipelineResult = result.getOrNull()
                        isScanning = false
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Analysis failed"
                        isScanning = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = isScanning,
                        animationSpec = tween(durationMillis = 350),
                        label = "ScanToResultTransition"
                    ) { scanning ->
                        if (scanning) {
                            ScreenScanAnimationOverlay(
                                statusText = "AI SCANNING SCREEN...",
                                subtitleText = "ScreenIQ 2-Finger Vision Engine"
                            )
                        } else {
                            val output = pipelineResult
                            if (output != null) {
                                FloatingProcessResultWindow(
                                    classification = output.classification,
                                    suggestions = output.suggestions,
                                    elapsedMs = output.elapsedMs,
                                    onActionSelected = { selectedAction ->
                                        handleActionAndDismiss(selectedAction, output.classification)
                                    },
                                    onDismiss = {
                                        finish()
                                    }
                                )
                            } else {
                                // In case of error, dismiss gracefully
                                LaunchedEffect(Unit) {
                                    delay(1800L)
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleActionAndDismiss(action: ActionSuggestion, classification: ContentClassification) {
        lifecycleScope.launch {
            val request = ActionRequest(
                actionId = action.actionId,
                type = action.type,
                parameters = action.payload,
                confirmedByUser = true
            )
            pipeline.actionExecutor.executeAction(request)
            pipeline.handleActionExecution(action, classification)
            finish()
        }
    }

    companion object {
        private const val TAG = "ScreenIQ:OverlayActivity"
        const val EXTRA_SCENARIO = "extra_scenario"

        fun start(context: Context, scenario: String? = null) {
            val intent = Intent(context, ScreenIQOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                if (scenario != null) {
                    putExtra(EXTRA_SCENARIO, scenario)
                }
            }
            context.startActivity(intent)
        }
    }
}
