package com.screeniq.capture.error

/**
 * Exception wrapper around [CaptureError], allowing Kotlin's [Result.failure]
 * to carry strongly-typed capture failure information.
 */
class CaptureException(
    val error: CaptureError
) : Exception(error.message, error.cause) {
    override fun toString(): String = "CaptureException(${error.code}): ${error.message}"
}
