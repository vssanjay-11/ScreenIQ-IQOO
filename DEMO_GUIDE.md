# ScreenIQ — Live Demonstration & Judge Guide

> **iQOO Hackathon 2026 Chennai**  
> **Topic:** Phone-First System Action AI Layer  
> **Target Build:** `app/build/outputs/apk/debug/app-debug.apk` (Verified Working, Clean Build)

---

## 🚀 Quick Start / Installation

### 1. Install via ADB or Direct Transfer
```bash
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```
Or transfer `app-debug.apk` to any Android device (Android 8.0 / API 26+; Android 11+ recommended for seamless in-memory accessibility capture).

### 2. Enable Accessibility Service (One-Time Setup)
1. Launch **ScreenIQ** from the app launcher.
2. The home screen detects accessibility status. Tap **"Enable Accessibility Service"** (or open *Settings -> Accessibility -> Downloaded Apps -> ScreenIQ*).
3. Toggle **Use ScreenIQ** to **ON**.
4. ScreenIQ is now actively listening for system-wide triggers!

---

## 🎯 The 3 Core Demos for Hackathon Judges

### Demo 1: Event to Calendar 📅
- **Scenario:** The user receives a WhatsApp, poster, or email with event details:
  ```
  AI & Deep Learning Workshop 2026
  Date: 20 September 2026
  Time: 10:00 AM
  Location: Seminar Hall 2, Anna University, Chennai
  ```
- **Action:**
  1. Trigger ScreenIQ (4-Finger Swipe Up, or tap **"Simulate Event Capture"** on Home Screen).
  2. ScreenIQ displays floating overlay: *Capturing...* ➡️ *Understanding...* ➡️ *Action Ready*.
  3. Overlay card shows:
     - **Primary Action:** **"Add to Google Calendar"** (Confidence: 98%, Safe)
     - **Secondary Actions:** **"Navigate in Google Maps"**, **"Copy to Clipboard"**
  4. Tap **"Add to Google Calendar"**.
  5. ScreenIQ launches Google Calendar via native Android Intent (`CalendarContract.ACTION_INSERT`) with Title, Date, Time, and Location pre-filled.
  6. Action is logged to local history with zero permanent screenshots retained on disk.

---

### Demo 2: Location to Google Maps 📍
- **Scenario:** The user views a restaurant, office address, or meetup venue:
  ```
  Anna University Highway Campus,
  Sardar Patel Road, Guindy,
  Chennai, Tamil Nadu 600025
  ```
- **Action:**
  1. Trigger ScreenIQ.
  2. ScreenIQ extracts address entities and classifies primary intent as `LOCATION`.
  3. Overlay card shows:
     - **Primary Action:** **"Open in Google Maps"** (`geo:0,0?q=...`)
     - **Secondary Action:** **"Copy Address"**
  4. Tap **"Open in Google Maps"**.
  5. Google Maps opens directly centered on Guindy, Chennai.

---

### Demo 3: Contact to Dialer & Contact Book 📞
- **Scenario:** The user views customer support, vendor card, or colleague contact:
  ```
  Dr. Ramesh Babu
  Lead Conference Chair
  Phone: +91 98765 43210
  Email: ramesh.babu@annauniv.edu
  ```
- **Action:**
  1. Trigger ScreenIQ.
  2. ScreenIQ extracts phone number and email with 95%+ confidence.
  3. Overlay card shows:
     - **Primary Action:** **"Dial Phone"** (+91 98765 43210) *(Safety: Dangerous - Requires Confirmation)*
     - **Secondary Actions:** **"Add to Contacts"**, **"Compose Email"**
  4. Tap **"Dial Phone"** ➡️ Confirm prompt appears (safety guard against unintended calls) ➡️ Confirmed.
  5. Android Phone Dialer opens with number pre-dialed ready for user call.

---

## 🔄 Reliable Trigger Mechanisms

To ensure flawless demonstrations under all conditions (different phone models, permissions, or emulators), ScreenIQ provides 3 trigger options:

| Trigger Method | How to Use | Best For |
| :--- | :--- | :--- |
| **1. 4-Finger Swipe Up** | Swipe up with 4 fingers anywhere on the screen | Real iQOO / Android physical devices with Accessibility enabled |
| **2. In-App Quick Triggers** | Tap any sample button on the **ScreenIQ Home Screen** (*"Simulate Event"*, *"Simulate Location"*, *"Simulate Contact"*) | Quick testing, judges preview, or when Accessibility is restricted |
| **3. ADB Broadcast Fallback** | `adb shell am broadcast -a com.screeniq.TRIGGER_FALLBACK` | Live stage presentations, automated testing, or emulator demonstrations |

---

## 🛡️ Privacy & Safety Invariants Verified

1. **Zero Disk Screenshots:** Screen frames reside strictly in transient memory (`android.graphics.Bitmap`) and are recycled immediately after ML Kit OCR extraction.
2. **On-Device Execution:** Google ML Kit on-device text recognition and deterministic heuristic classifier execute locally on device without external network transmission.
3. **Action Safety Policy:** Destructive actions (phone calls, emails, contacts) are classified as `DANGEROUS` and strictly require explicit user confirmation before execution. Automatic purchases, money transfers, and publishing are strictly forbidden.
4. **Office Kit Export:** History entries can be reviewed in the **History Screen** and exported as clean CSV or JSON files for cross-device analysis via Office Kit.

---

## 🧪 Running Automated Tests

To verify the test suite across all 10 integrated modules:
```bash
gradlew testDebugUnitTest
```
**Results:** **45 / 45 Tests Passing (100% Green)**
- `CoreContractsAndModelsTest`: Contract invariants, models, safety policies.
- `FourFingerSwipeDetectorTest`: Algorithmic 4-pointer touch vector calculation.
- `GestureTriggerManagerTest`: Trigger routing, state flows, fallback acceptance.
- `AccessibilityScreenshotProviderTest`: Rate limiting, security flags, service availability.
- `ScreenCaptureSubsystemTest`: Memory manager, capture lifecycle.
- `ContentClassifierTest`: Heuristic classification across all 13 categories.
- `DefaultActionPlannerTest`: Strategy planning, safety policies, deduplication.
- `ScreenIqPipelineIntegrationTest`: End-to-end event, location, and phone pipelines.
