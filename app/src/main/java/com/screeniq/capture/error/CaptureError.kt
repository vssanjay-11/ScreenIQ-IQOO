package com.screeniq.capture.error

/**
 * Structured taxonomy of screen capture errors.
 * Ensures all failure modes (e.g. FLAG_SECURE, service disconnection, rate limits)
 * are handled gracefully without crashing the application.
 */
sealed class CaptureError(
    val code: String,
    val message: String,
    val cause: Throwable? = null
) {
    /**
     * Target window is protected by WindowManager.LayoutParams.FLAG_SECURE
     * (e.g. banking apps, password fields, DRM video, incognito tabs).
     */
    data object SecureWindowDetected : CaptureError(
        code = "SECURE_WINDOW",
        message = "Screen content is protected by secure window policy (FLAG_SECURE) and cannot be captured."
    )

    /**
     * AccessibilityService is not running, not bound, or lacks accessibility access.
     */
    data object ServiceUnavailable : CaptureError(
        code = "SERVICE_UNAVAILABLE",
        message = "AccessibilityService is unavailable or does not have permissions to capture the screen."
    )

    /**
     * Multiple captures requested too quickly. Android limits screenshot frequency.
     * Corresponds to AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_RIGID.
     */
    data object RateLimited : CaptureError(
        code = "RATE_LIMITED",
        message = "Screen capture was rate limited because captures were requested too rapidly."
    )

    /**
     * Display ID is invalid or cannot be targeted for screenshot.
     */
    data class InvalidDisplay(val displayId: Int) : CaptureError(
        code = "INVALID_DISPLAY",
        message = "Target display ID $displayId is invalid or detached."
    )

    /**
     * Environment or Android OS version does not support requested capture API (e.g. API < 30).
     */
    data class UnsupportedEnvironment(val detail: String) : CaptureError(
        code = "UNSUPPORTED_ENVIRONMENT",
        message = "Current environment does not support screen capture: $detail"
    )

    /**
     * Capture operation timed out waiting for OS/HardwareBuffer callback.
     */
    data class Timeout(val timeoutMs: Long) : CaptureError(
        code = "TIMEOUT",
        message = "Screen capture timed out after ${timeoutMs}ms."
    )

    /**
     * Low-level Android OS internal capture failure.
     */
    data class InternalError(
        val detail: String,
        val originalCause: Throwable? = null
    ) : CaptureError(
        code = "INTERNAL_ERROR",
        message = "Internal Android system error during screen capture: $detail",
        cause = originalCause
    )
}
