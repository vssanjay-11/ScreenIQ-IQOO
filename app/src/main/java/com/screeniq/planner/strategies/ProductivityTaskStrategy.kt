package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class ProductivityTaskStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val taskEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.TASK_TODO }
        val dateEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.DATE_TIME }

        val taskTitle = taskEntity?.normalizedValue
            ?: taskEntity?.rawValue
            ?: classification.summary
            ?: "New Task"

        val dueDate = dateEntity?.normalizedValue ?: dateEntity?.rawValue

        // 1. Primary: Create Task
        val taskSuggestion = ActionProposalBuilder(ActionType.CREATE_TASK, baseScore)
            .title("Create Task: \"$taskTitle\"")
            .description("Save to Google Tasks or Keep with deadline reminders.")
            .isPrimary(true)
            .addParam(ActionParameters.TASK_TITLE, taskTitle)
            .addParam(ActionParameters.TASK_DUE_DATE, dueDate)
            .requireKeys(ActionParameters.TASK_TITLE)
            .optionalKeys(ActionParameters.TASK_DUE_DATE)
            .build()
        suggestions.add(taskSuggestion)

        // 2. Secondary: Set Reminder
        val reminderSuggestion = ActionProposalBuilder(ActionType.SET_REMINDER, baseScore)
            .title("Set Reminder")
            .description("Set a notification reminder for \"$taskTitle\".")
            .isPrimary(false)
            .addParam(ActionParameters.TASK_TITLE, taskTitle)
            .addParam(ActionParameters.REMINDER_TIME, dueDate)
            .requireKeys(ActionParameters.TASK_TITLE)
            .build()
        suggestions.add(reminderSuggestion)

        // 3. Secondary: Copy Text
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Task Text")
            .description("Copy \"$taskTitle\" to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, taskTitle)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
