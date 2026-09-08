package com.screeniq.core.contracts

import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionResult

/**
 * Agent 7: Native Android Action Executor
 * Dispatches validated action requests into native Android Intents (Calendar, Maps, Dialer, etc.).
 */
interface ActionExecutor {
    suspend fun executeAction(request: ActionRequest): ActionResult
}
