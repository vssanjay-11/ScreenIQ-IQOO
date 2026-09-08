# ScreenIQ — Project Specification

> **Tagline:** *"See it. Understand it. Do it."*  
> **Platform:** Android (Phone-First, Jetpack Compose, Kotlin)  
> **Event:** iQOO Hackathon 2026 Chennai City Battle

---

## 1. Executive Summary & Vision

**ScreenIQ** is a phone-first AI action layer for Android devices. Traditional smartphone multitasking forces users to manually transcribe, copy-paste, switch apps, or feed screenshots into disconnected chat assistants. ScreenIQ eliminates this friction.

When a user triggers a system-wide **four-finger swipe gesture**, ScreenIQ:
1. Captures the currently visible screen content in memory.
2. Performs fast on-device visual analysis and optical character recognition (OCR).
3. Classifies content types and understands contextual user intents using localized AI.
4. Synthesizes high-confidence, actionable proposals (e.g., calendar events, map navigation, calls, tasks, payments/forms).
5. Prompts the user with a fluid Compose overlay (or safely executes low-risk, high-confidence actions).
6. Dispatches native Android Intents to complete the job seamlessly.

ScreenIQ turns the screen from a static canvas into an actionable, intelligent surface.

---

## 2. Hackathon Context & Constraints

- **Hackathon:** iQOO Hackathon 2026 — Chennai City Battle.
- **Surface:** Physical iQOO Android smartphone (touch gesture-driven, physical display).
- **Evaluation Criteria:**
  - End product quality & responsiveness
  - Novelty and real-world impact
  - Creative, native phone utilization (hardware, gestures, system services)
  - Technical depth & architectural integrity
  - Office Kit / Productivity suite integration
  - Demo reliability & presentation polish
- **API & Privileges Stance:**
  - **No OEM root/privileged APIs assumed.** The system strictly adheres to public Android SDK APIs.
  - **AccessibilityService:** Utilized appropriately for supported gesture observation, contextual screen capture, and accessibility-assisted interactions where permitted by user consent.
  - **MediaProjection / Screenshot APIs:** Fallback / complementary capture layer ensuring universal device compatibility without security bypasses.

---

## 3. End-to-End User Experience & Pipeline

### Core Pipeline Flow

```
   [ USER ACTIVE ON ANY APP ]
               │
               ▼
   [ 4-FINGER SWIPE GESTURE ]
               │
               ▼
      [ SCREEN CAPTURE ]  ─── (In-memory bitmap, privacy-guarded)
               │
               ▼
 [ SCREEN CONTENT UNDERSTANDING ] ─── (On-device OCR & visual parsing)
               │
               ▼
 [ INTENT & ENTITY CLASSIFICATION ] ─── (Local AI / Regex / Heuristics)
               │
               ▼
       [ ACTION PLANNING ] ─── (Rank suggestions, evaluate confidence)
               │
               ▼
[ USER CONFIRMATION / POPUP ] ─── (Safe confirmation for sensitive actions)
               │
               ▼
      [ ACTION EXECUTION ] ─── (Native Intents, Calendar, Maps, Dialers)
               │
               ▼
      [ RESULT & FEEDBACK ] ─── (Haptic feedback, toast, history log)
```

### Ideal Demo Walkthrough

1. **Trigger:** The user views a WhatsApp message, poster, or email containing:
   ```text
   AI Workshop
   20 September 2026
   10:00 AM
   Seminar Hall 2, Anna University
   ```
2. **Gesture:** The user performs a 4-finger swipe on the iQOO display.
3. **Detection:** ScreenIQ's Accessibility/Gesture engine intercepts the gesture instantly.
4. **Capture:** The screen frame is captured in-memory.
5. **Extraction:** Fast OCR isolates title, date, time, and venue.
6. **Classification:** Intent engine identifies this as an **EVENT** with high confidence.
7. **Action Card:** A sleek, non-intrusive Jetpack Compose overlay appears:
   - `[Add to Calendar]` *(Primary)*
   - `[View Location on Maps]` *(Secondary)*
   - `[Copy Details]`
   - `[Dismiss]`
8. **Confirmation:** User taps `[Add to Calendar]`.
9. **Execution:** Android Calendar Intent opens pre-filled with:
   - Title: *AI Workshop*
   - Date & Time: *20 Sept 2026, 10:00 AM*
   - Location: *Seminar Hall 2, Anna University*
10. **Feedback:** Success notification/haptic buzz; entry recorded in local, private history.

---

## 4. Entity & Action Taxonomy

ScreenIQ recognizes and maps common on-screen entities to native action pipelines:

| Entity Type | Example Triggers | Suggested Primary Actions | Safety Requirement |
| :--- | :--- | :--- | :--- |
| **EVENT** | Flyers, chat messages with dates, webinar notices | Add to Calendar, Set Reminder | Review event details |
| **LOCATION** | Addresses, venue names, transit info | Open Google Maps / Navigation | Direct trigger permitted |
| **PHONE NUMBER**| Contact cards, signatures, order sheets | Call via Dialer, Save Contact, Send SMS | Call requires user confirmation |
| **EMAIL** | Email addresses, inquiries | Compose Email, Copy Address | Open email composer |
| **URL / LINK** | Web addresses, article links, mentions | Open in Default Browser, Copy Link | Direct trigger permitted |
| **PRODUCT** | E-commerce items, price tags, model names | Search Product, Price Compare, Save to Wishlist | Direct trigger permitted |
| **TASK / TODO** | Checklists, action items, assigned tasks | Add to Task App (Google Tasks/Keep), Remind Me | Review task text |
| **DOCUMENT** | Articles, receipts, long memos, policy text | Summarize via AI, Extract Key Points, Export PDF | Local processing |
| **QR CODE** | Digital codes, WiFi setups, UPI handles | Scan / Open URL, Copy Data, Connect WiFi | Sensitive if payment QR |
| **UNKNOWN** | Unclassified screens, general graphics | "Ask ScreenIQ" Freeform Query / Web Search | User prompts input |

---

## 5. Critical Engineering Principles

1. **Phone-First Experience:** Built specifically for handheld touch interaction on the iQOO phone display; fast, responsive (<800ms target pipeline latency), and tactile.
2. **Local-First & On-Device AI:** Visual extraction and entity classification must prioritize on-device models (e.g., Google ML Kit, lightweight on-device LLMs/SLMs). Remote fallbacks must be isolated behind contracts.
3. **Privacy by Design:**
   - Screenshots must reside **strictly in memory**.
   - No permanent disk caching of raw screen bitmaps unless explicitly requested by the user.
   - Screen content is never transmitted externally without clear, opt-in notification.
4. **Action Safety & Non-Destructive Operations:**
   - Actions involving communications, money, identity, or data deletion **must never run automatically**.
   - Explicit user confirmation card is mandatory for payments, calls, messaging, or calendar updates.
5. **Modularity & Contract Discipline:** Every subsystem communicates strictly via defined interfaces and immutable data transfer objects.
6. **Demo Reliability Over Scope Creep:** 5 rock-solid, lightning-fast end-to-end entity flows beat 25 flaky integrations.

---

## 6. Technical Stack & Standards

- **Language:** Kotlin (1.9+)
- **OS Target:** Android 14+ (API 34+), compatible with modern iQOO devices
- **UI Toolkit:** Jetpack Compose (Material 3, edge-to-edge, dynamic theming)
- **Asynchrony:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Text & Visual Extraction:** Google ML Kit Text Recognition (On-Device) / Vision API abstraction
- **System Integration:** Android AccessibilityService, `AccessibilityGestureDetector`, `MediaProjectionManager`, Android Intents (`ACTION_INSERT`, `ACTION_VIEW`, `ACTION_DIAL`, `ACTION_SENDTO`)
- **Build System:** Gradle (Kotlin DSL, Version Catalogs `libs.versions.toml`)
- **Local Persistence (Optional/Lightweight):** Room / DataStore for history and privacy preferences.
