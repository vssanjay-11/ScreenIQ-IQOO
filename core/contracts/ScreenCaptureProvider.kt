package com.screeniq.core.contracts

/**
 * Provider contract for screen capture implementations.
 * Enables interchangeable screen capture sources (Accessibility screenshot, MediaProjection, or test doubles).
 */
interface ScreenCaptureProvider : ScreenCaptureEngine
