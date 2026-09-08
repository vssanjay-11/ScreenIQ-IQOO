package com.screeniq.privacy

/**
 * Official transparency disclosure and privacy specification for ScreenIQ.
 *
 * ScreenIQ is designed around strict "Privacy-by-Design" principles:
 * 1. On-Demand Only: Never records in the background. Screen is inspected ONLY when the user
 *    explicitly invokes the 4-finger swipe gesture.
 * 2. In-Memory Processing: Screen pixels exist purely in volatile RAM during extraction and are
 *    recycled immediately after OCR and classification finish.
 * 3. Zero Cloud Ingestion: Visual frames are NEVER uploaded to external servers.
 * 4. Transparent History: History stores only metadata summaries and action status—never screenshots.
 * 5. Instant Erasure: The user can purge all locally stored logs at any time.
 */
object PrivacyPolicy {

    const val SUMMARY_TITLE = "ScreenIQ Privacy & Data Processing Notice"

    const val SHORT_DISCLOSURE =
        "ScreenIQ analyzes on-screen content strictly on-demand when you trigger the 4-finger swipe. " +
        "Visual captures reside purely in volatile memory and are never saved to disk or transmitted off your device."

    const val DETAILED_EXPLANATION = """
### 1. What We Process
When you perform the 4-finger gesture, ScreenIQ captures the visible display content solely to extract actionable items (such as calendar dates, addresses, phone numbers, or links).

### 2. How Long Data is Retained
- **Screen Images:** Discarded and recycled in memory immediately after optical text extraction (<800ms). Never saved to device storage or SD card.
- **Action History:** Summaries of actions taken (e.g., 'Added AI Workshop to Calendar') are stored locally on your device for your convenience.

### 3. Third-Party Sharing
None. ScreenIQ uses on-device machine learning (ML Kit). No screen data, text snippets, or personal information are sent to cloud servers or third parties.

### 4. Your Control
You can disable gesture detection, adjust confirmation behavior, or permanently clear your action history at any time from ScreenIQ Settings.
    """

    val PRIVACY_HIGHLIGHTS = listOf(
        "⚡ On-Demand Only — Triggered solely by your intentional 4-finger swipe.",
        "🔒 In-Memory Only — Screen bitmaps are immediately recycled after OCR.",
        "🚫 No Permanent Screenshots — No images are saved to device storage.",
        "📱 100% Local AI — All entity detection runs on your phone.",
        "🧹 Instant Clear — Clear your entire action history in one tap."
    )
}
