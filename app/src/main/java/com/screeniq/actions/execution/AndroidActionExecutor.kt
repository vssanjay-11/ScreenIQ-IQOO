package com.screeniq.actions.execution

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.util.Log
import com.screeniq.core.contracts.ActionExecutor
import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionType
import com.screeniq.planner.ActionParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Agent 7: Production Android Action Executor
 * Translates validated [ActionRequest] instances into native Android Intents.
 * Adheres strictly to security and non-destructive requirements.
 */
class AndroidActionExecutor(
    private val context: Context
) : ActionExecutor {

    override suspend fun executeAction(request: ActionRequest): ActionResult = withContext(Dispatchers.Main) {
        val params = request.parameters
        val actionId = request.actionId

        try {
            when (request.type) {
                ActionType.ADD_TO_CALENDAR -> executeAddToCalendar(actionId, params)
                ActionType.OPEN_MAPS -> executeOpenMaps(actionId, params)
                ActionType.DIAL_PHONE -> executeDialPhone(actionId, params)
                ActionType.SAVE_CONTACT -> executeSaveContact(actionId, params)
                ActionType.SEND_SMS -> executeSendSms(actionId, params)
                ActionType.COMPOSE_EMAIL -> executeComposeEmail(actionId, params)
                ActionType.OPEN_BROWSER -> executeOpenBrowser(actionId, params)
                ActionType.SEARCH_PRODUCT -> executeSearchProduct(actionId, params)
                ActionType.CREATE_TASK, ActionType.SET_REMINDER -> executeCreateTask(actionId, params)
                ActionType.COPY_TO_CLIPBOARD -> executeCopyToClipboard(actionId, params)
                ActionType.SUMMARIZE_DOCUMENT, ActionType.INSPECT_QR, ActionType.ASK_USER_CUSTOM -> {
                    executeCopyToClipboard(actionId, params, "Content extracted and ready")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${request.type}: ${e.message}", e)
            ActionResult(
                actionId = actionId,
                success = false,
                executedIntentSummary = "Failed to execute ${request.type.name}",
                errorMessage = e.message ?: "Unknown intent execution error"
            )
        }
    }

    private fun executeAddToCalendar(actionId: String, params: Map<String, String>): ActionResult {
        val title = params[ActionParameters.EVENT_TITLE]
            ?: params[ActionParameters.TEXT]
            ?: "Event from ScreenIQ"
        val location = params[ActionParameters.EVENT_LOCATION] ?: ""
        val description = params[ActionParameters.EVENT_DESCRIPTION]
            ?: params[ActionParameters.SUMMARY]
            ?: "Captured via ScreenIQ"

        val startDateStr = params[ActionParameters.EVENT_START_DATE]
        val startTimeStr = params[ActionParameters.EVENT_START_TIME]

        var startMillis = System.currentTimeMillis() + 3600_000L // Default 1 hour from now
        var endMillis = startMillis + 3600_000L

        if (!startDateStr.isNullOrBlank()) {
            val parsed = parseDateAndTimeToMillis(startDateStr, startTimeStr)
            if (parsed > 0) {
                startMillis = parsed
                endMillis = startMillis + 3600_000L
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Calendar for \"$title\" ($location)"
        )
    }

    private fun executeOpenMaps(actionId: String, params: Map<String, String>): ActionResult {
        val query = params[ActionParameters.MAP_QUERY]
            ?: params[ActionParameters.MAP_ADDRESS]
            ?: params[ActionParameters.EVENT_LOCATION]
            ?: params[ActionParameters.TEXT]
            ?: "Chennai"

        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try launching Maps, fallback to browser Google Maps if no map app
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Navigation for \"$query\""
        )
    }

    private fun executeDialPhone(actionId: String, params: Map<String, String>): ActionResult {
        val rawNumber = params[ActionParameters.PHONE_NUMBER]
            ?: params[ActionParameters.TEXT]
            ?: ""
        val sanitized = rawNumber.replace(Regex("[^0-9+]"), "")

        // Use ACTION_DIAL so it opens dialer with number filled WITHOUT directly placing unauthorized call
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Dialer with $sanitized"
        )
    }

    private fun executeSaveContact(actionId: String, params: Map<String, String>): ActionResult {
        val name = params[ActionParameters.CONTACT_NAME] ?: "Contact"
        val phone = params[ActionParameters.PHONE_NUMBER] ?: ""
        val email = params[ActionParameters.EMAIL_RECIPIENT] ?: ""

        val contactIntent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, name)
            if (phone.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            }
            if (email.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.EMAIL, email)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(contactIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Contacts to save $name ($phone)"
        )
    }

    private fun executeSendSms(actionId: String, params: Map<String, String>): ActionResult {
        val phone = params[ActionParameters.PHONE_NUMBER] ?: ""
        val body = params[ActionParameters.SMS_BODY] ?: params[ActionParameters.TEXT] ?: ""

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${phone.replace(Regex("[^0-9+]"), "")}")
            if (body.isNotBlank()) {
                putExtra("sms_body", body)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened SMS to $phone"
        )
    }

    private fun executeComposeEmail(actionId: String, params: Map<String, String>): ActionResult {
        val recipient = params[ActionParameters.EMAIL_RECIPIENT] ?: ""
        val subject = params[ActionParameters.EMAIL_SUBJECT] ?: "ScreenIQ Notes"
        val body = params[ActionParameters.EMAIL_BODY] ?: params[ActionParameters.TEXT] ?: ""

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(emailIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Email composer to $recipient"
        )
    }

    private fun executeOpenBrowser(actionId: String, params: Map<String, String>): ActionResult {
        var rawUrl = params[ActionParameters.URL]
            ?: params[ActionParameters.SEARCH_QUERY]
            ?: params[ActionParameters.TEXT]
            ?: "https://google.com"

        val targetUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else if (rawUrl.contains(".") && !rawUrl.contains(" ")) {
            "https://$rawUrl"
        } else {
            "https://www.google.com/search?q=" + Uri.encode(rawUrl)
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Opened Browser: $targetUrl"
        )
    }

    private fun executeSearchProduct(actionId: String, params: Map<String, String>): ActionResult {
        val query = params[ActionParameters.PRODUCT_TITLE]
            ?: params[ActionParameters.SEARCH_QUERY]
            ?: params[ActionParameters.TEXT]
            ?: "Product"

        val searchUrl = "https://www.google.com/search?tbm=shop&q=" + Uri.encode(query)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Searched Product: $query"
        )
    }

    private fun executeCreateTask(actionId: String, params: Map<String, String>): ActionResult {
        val title = params[ActionParameters.TASK_TITLE] ?: params[ActionParameters.TEXT] ?: "Task from ScreenIQ"
        val notes = params[ActionParameters.TASK_NOTES] ?: params[ActionParameters.SUMMARY] ?: ""

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, if (notes.isNotBlank()) "$title\n$notes" else title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Create Task / Note with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = "Shared Task \"$title\""
        )
    }

    private fun executeCopyToClipboard(actionId: String, params: Map<String, String>, defaultSummary: String? = null): ActionResult {
        val textToCopy = params[ActionParameters.TEXT]
            ?: params[ActionParameters.SUMMARY]
            ?: params[ActionParameters.RAW_CONTENT]
            ?: params.values.firstOrNull()
            ?: "ScreenIQ Content"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ScreenIQ", textToCopy)
        clipboard.setPrimaryClip(clip)

        val snippet = if (textToCopy.length > 40) textToCopy.take(37) + "..." else textToCopy
        return ActionResult(
            actionId = actionId,
            success = true,
            executedIntentSummary = defaultSummary ?: "Copied to clipboard: \"$snippet\""
        )
    }

    private fun parseDateAndTimeToMillis(dateStr: String, timeStr: String?): Long {
        val formats = arrayOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd MMMM yyyy",
            "dd MMM yyyy",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
        )
        val cal = Calendar.getInstance()
        var dateParsed = false

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = true
                val d = sdf.parse(dateStr.trim())
                if (d != null) {
                    val parsedCal = Calendar.getInstance().apply { time = d }
                    cal.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH))
                    dateParsed = true
                    break
                }
            } catch (_: Exception) {}
        }

        if (!dateParsed) {
            val monthRegex = Regex("""(\d{1,2})\s+([A-Za-z]{3,9})\s*(\d{4})?""")
            val m = monthRegex.find(dateStr)
            if (m != null) {
                val day = m.groupValues[1].toIntOrNull() ?: 1
                val monthName = m.groupValues[2].lowercase()
                val year = m.groupValues[3].toIntOrNull() ?: cal.get(Calendar.YEAR)

                val monthIdx = when {
                    monthName.startsWith("jan") -> 0
                    monthName.startsWith("feb") -> 1
                    monthName.startsWith("mar") -> 2
                    monthName.startsWith("apr") -> 3
                    monthName.startsWith("may") -> 4
                    monthName.startsWith("jun") -> 5
                    monthName.startsWith("jul") -> 6
                    monthName.startsWith("aug") -> 7
                    monthName.startsWith("sep") -> 8
                    monthName.startsWith("oct") -> 9
                    monthName.startsWith("nov") -> 10
                    monthName.startsWith("dec") -> 11
                    else -> cal.get(Calendar.MONTH)
                }
                cal.set(year, monthIdx, day)
                dateParsed = true
            }
        }

        if (!timeStr.isNullOrBlank()) {
            val timeRegex = Regex("""(\d{1,2}):?(\d{2})?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
            val match = timeRegex.find(timeStr.trim())
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 9
                val min = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3].lowercase()
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
        }

        return if (dateParsed) cal.timeInMillis else -1L
    }

    companion object {
        private const val TAG = "ScreenIQ:ActionExec"
    }
}
