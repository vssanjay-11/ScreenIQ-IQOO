package com.screeniq.planner

/**
 * Standard parameter keys used across ActionSuggestion payloads.
 * These keys are consumed by downstream executors (Agent 7) and UI cards (Agent 8).
 */
object ActionParameters {
    // General
    const val TEXT = "text"
    const val SUMMARY = "summary"
    const val RAW_CONTENT = "rawContent"

    // Calendar / Event
    const val EVENT_TITLE = "title"
    const val EVENT_START_DATE = "startDate"
    const val EVENT_START_TIME = "startTime"
    const val EVENT_END_DATE = "endDate"
    const val EVENT_END_TIME = "endTime"
    const val EVENT_LOCATION = "location"
    const val EVENT_DESCRIPTION = "description"
    const val EVENT_ALL_DAY = "allDay"

    // Location / Maps
    const val MAP_QUERY = "query"
    const val MAP_ADDRESS = "address"
    const val MAP_LATITUDE = "latitude"
    const val MAP_LONGITUDE = "longitude"

    // Communication / Phone & SMS
    const val PHONE_NUMBER = "phoneNumber"
    const val CONTACT_NAME = "contactName"
    const val SMS_BODY = "messageBody"

    // Email
    const val EMAIL_RECIPIENT = "recipient"
    const val EMAIL_SUBJECT = "subject"
    const val EMAIL_BODY = "body"

    // Web / Browser
    const val URL = "url"
    const val WEB_TITLE = "webTitle"

    // Commerce / Product
    const val PRODUCT_TITLE = "productTitle"
    const val PRODUCT_PRICE = "price"
    const val SEARCH_QUERY = "searchQuery"

    // Task / Productivity
    const val TASK_TITLE = "taskTitle"
    const val TASK_NOTES = "notes"
    const val TASK_DUE_DATE = "dueDate"
    const val REMINDER_TIME = "reminderTime"

    // QR Code
    const val QR_CONTENT = "qr_content"
    const val QR_TYPE = "qr_type"
    const val IS_PAYMENT = "is_payment"

    // Custom / AI Query
    const val USER_PROMPT = "prompt"
    const val CONTEXT_SUMMARY = "context"
}
