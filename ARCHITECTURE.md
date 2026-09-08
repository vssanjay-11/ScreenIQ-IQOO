# ScreenIQ — Architectural Blueprint & Contract Specifications

> **System:** Phone-First AI Action Layer for Android  
> **Target Audience:** Master Architect & 10 Autonomous Coding Agents  
> **Status:** Baseline Architectural Specification (No Feature Implementation)

---

## 1. High-Level Architectural Pattern

ScreenIQ follows a **Clean, Unidirectional Data Flow (UDF)** modular architecture. The core pipeline transitions from physical touch event to native Android OS action through strongly-typed immutable contracts.

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     GESTURE     │ ───►  │     CAPTURE     │ ───►  │       OCR       │
│    (Agent 2)    │       │    (Agent 3)    │       │    (Agent 4)    │
└─────────────────┘       └─────────────────┘       └─────────────────┘
                                                             │
                                                             ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     ACTION      │ ◄───  │    PLANNER      │ ◄───  │   CLASSIFIER    │
│    (Agent 7)    │       │    (Agent 6)    │       │    (Agent 5)    │
└────────┬────────┘       └────────┬────────┘       └─────────────────┘
         │                         │
         ▼                         ▼
┌─────────────────┐       ┌─────────────────┐
│  HISTORY & LOG  │       │   COMPOSE UI    │
│    (Agent 9)    │       │    (Agent 8)    │
└─────────────────┘       └─────────────────┘
```

---

## 2. Directory Layout & Module Boundaries

The project adopts standard Android modular packages under namespace `com.screeniq`:

```text
ScreenIQ/
├── core/
│   ├── contracts/          # Master interfaces (frozen, shared across agents)
│   ├── model/              # Immutable Data Transfer Objects (DTOs)
│   └── common/             # Utilities, Result wrappers, Dispatcher providers
├── gesture/                # Multi-touch 4-finger detector (Agent 2)
├── capture/                # In-memory screenshot capture engine (Agent 3)
├── ocr/                    # ML Kit / on-device text recognition engine (Agent 4)
├── classifier/             # Intent & entity classification engine (Agent 5)
├── planner/                # Action suggestion & confidence ranking (Agent 6)
├── actions/                # Native Android Intent dispatchers (Agent 7)
├── ui/                     # Jetpack Compose overlay & cards (Agent 8)
├── history/                # Privacy-first Room/DataStore logging & Office Kit (Agent 9)
├── integration/            # E2E pipeline wiring, demo runner & tests (Agent 10)
├── PROJECT_SPEC.md         # Full project specification & hackathon goals
├── AGENT_CONTRACT.md       # Multi-agent collaboration protocol & rules
├── TASK_STATUS.md          # Multi-agent live tracking board
└── ARCHITECTURE.md         # This architectural specification document
```

---

## 3. Shared Data Models & Contracts

The shared data models define the canonical transformation stages of the pipeline:

$$\text{ScreenCaptureResult} \longrightarrow \text{ScreenContent} \longrightarrow \text{ContentClassification} \longrightarrow \text{ActionSuggestion} \longrightarrow \text{ActionRequest} \longrightarrow \text{ActionResult}$$

### 3.1. `ScreenCaptureResult`
Produced by: **Agent 3 (Capture)**  
Consumed by: **Agent 4 (OCR)**
```kotlin
package com.screeniq.core.model

import android.graphics.Bitmap

/**
 * Encapsulates the in-memory visual frame captured from the display.
 * Strict privacy rule: Bitmap must be recycled after extraction.
 */
data class ScreenCaptureResult(
    val captureId: String,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val timestampMs: Long,
    val sourcePackage: String? = null // Originating app package if available via Accessibility
)
```

### 3.2. `ScreenContent` & `DetectedEntity`
Produced by: **Agent 4 (OCR)**  
Consumed by: **Agent 5 (Classifier)**
```kotlin
package com.screeniq.core.model

import android.graphics.Rect

/**
 * Structured textual and spatial data extracted from the screen frame.
 */
data class ScreenContent(
    val captureId: String,
    val rawFullText: String,
    val textBlocks: List<TextBlock>,
    val detectedEntities: List<DetectedEntity> = emptyList(),
    val extractionDurationMs: Long
)

data class TextBlock(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float
)

data class DetectedEntity(
    val type: EntityType,
    val rawValue: String,
    val normalizedValue: String?,
    val boundingBox: Rect?,
    val confidence: Float
)

enum class EntityType {
    DATE_TIME,
    LOCATION_ADDRESS,
    PHONE_NUMBER,
    EMAIL,
    URL,
    PRODUCT_INFO,
    TASK_TODO,
    DOCUMENT_SNIPPET,
    QR_BARCODE,
    NUMERIC_FINANCIAL,
    UNKNOWN
}
```

### 3.3. `ContentClassification`
Produced by: **Agent 5 (Classifier)**  
Consumed by: **Agent 6 (Planner)**
```kotlin
package com.screeniq.core.model

/**
 * Semantic interpretation of what the user is looking at and likely intents.
 */
data class ContentClassification(
    val captureId: String,
    val primaryCategory: ContentCategory,
    val secondaryCategories: List<ContentCategory> = emptyList(),
    val extractedEntities: List<DetectedEntity>,
    val summary: String?,
    val confidenceScore: Float // 0.0f to 1.0f
)

enum class ContentCategory {
    EVENT,
    LOCATION,
    COMMUNICATION,
    WEB_LINK,
    COMMERCE_PRODUCT,
    PRODUCTIVITY_TASK,
    DOCUMENT_SUMMARY,
    QR_ACTION,
    UNKNOWN_GENERAL
}
```

### 3.4. `ActionSuggestion` & `ActionConfidence`
Produced by: **Agent 6 (Planner)**  
Consumed by: **Agent 8 (Compose UI) & Agent 7 (Execution)**
```kotlin
package com.screeniq.core.model

/**
 * A concrete proposal presented to the user or prepared for execution.
 */
data class ActionSuggestion(
    val actionId: String,
    val type: ActionType,
    val title: String,
    val description: String,
    val payload: Map<String, String>, // Key-value arguments for execution (e.g. title, date, geo)
    val confidence: ActionConfidence,
    val safetyLevel: ActionSafetyLevel,
    val isPrimary: Boolean = false
)

data class ActionConfidence(
    val score: Float, // 0.0 to 1.0
    val reason: String
)

enum class ActionSafetyLevel {
    SAFE_AUTO,             // Non-destructive read/view (e.g. open map, view browser)
    REQUIRE_CONFIRMATION,  // Alters personal data (e.g. add calendar event, create task)
    DANGEROUS              // Sensitive (e.g. place phone call, send message, payment)
}

enum class ActionType {
    ADD_TO_CALENDAR,
    OPEN_MAPS,
    DIAL_PHONE,
    SEND_SMS,
    COMPOSE_EMAIL,
    OPEN_BROWSER,
    CREATE_TASK,
    COPY_TO_CLIPBOARD,
    SUMMARIZE_DOCUMENT,
    INSPECT_QR,
    ASK_USER_CUSTOM
}
```

### 3.5. `ActionRequest` & `ActionResult`
Produced by: **Agent 8 (UI trigger) or Agent 6 (Safe auto)**  
Consumed by: **Agent 7 (Execution Engine)**
```kotlin
package com.screeniq.core.model

/**
 * Approved command sent to the Android Execution Subsystem.
 */
data class ActionRequest(
    val actionId: String,
    val type: ActionType,
    val parameters: Map<String, String>,
    val confirmedByUser: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Result emitted following native intent execution.
 */
data class ActionResult(
    val actionId: String,
    val success: Boolean,
    val executedIntentSummary: String,
    val errorMessage: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
```

### 3.6. `ActionHistoryItem`
Produced by: **Agent 7 / 8**  
Consumed by: **Agent 9 (History & Office Kit)**
```kotlin
package com.screeniq.core.model

/**
 * Privacy-compliant local record of actions performed.
 * Note: Screenshots are NOT stored here; only metadata and summary text.
 */
data class ActionHistoryItem(
    val id: String,
    val timestampMs: Long,
    val category: ContentCategory,
    val actionType: ActionType,
    val summarySnippet: String,
    val wasSuccessful: Boolean
)
```

---

## 4. Master Engine Interfaces (Core Contracts)

All agents code directly against these public contracts. No agent creates duplicate versions of these interfaces.

```kotlin
package com.screeniq.core.contracts

import com.screeniq.core.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Agent 2: Gesture and Accessibility Observer
 */
interface GestureTriggerListener {
    val gestureEvents: Flow<GestureTriggerEvent>
    fun enableDetection()
    fun disableDetection()
}

data class GestureTriggerEvent(
    val pointerCount: Int,
    val timestampMs: Long
)

/**
 * Agent 3: Screen Capture Engine
 */
interface ScreenCaptureEngine {
    suspend fun captureCurrentScreen(): Result<ScreenCaptureResult>
}

/**
 * Agent 4: OCR Engine
 */
interface OcrEngine {
    suspend fun extractContent(captureResult: ScreenCaptureResult): Result<ScreenContent>
}

/**
 * Agent 5: Content Classifier
 */
interface ContentClassifier {
    suspend fun classify(content: ScreenContent): Result<ContentClassification>
}

/**
 * Agent 6: Action Planner
 */
interface ActionPlanner {
    suspend fun planActions(classification: ContentClassification): List<ActionSuggestion>
}

/**
 * Agent 7: Native Android Action Executor
 */
interface ActionExecutor {
    suspend fun executeAction(request: ActionRequest): ActionResult
}

/**
 * Agent 8: UI Overlay Controller
 */
interface OverlayController {
    fun showActionSuggestions(
        classification: ContentClassification,
        suggestions: List<ActionSuggestion>,
        onActionSelected: (ActionSuggestion) -> Unit,
        onDismiss: () -> Unit
    )
    fun dismissOverlay()
}

/**
 * Agent 9: History and Privacy Storage
 */
interface HistoryRepository {
    suspend fun recordAction(item: ActionHistoryItem)
    suspend fun getRecentHistory(limit: Int): List<ActionHistoryItem>
    suspend fun clearHistory()
}
```

---

## 5. Dependency Direction & Inversion

```
┌─────────────────────────────────────────────────────────────┐
│                    core/contracts/                          │
│        (All interfaces, DTOs, and Result types)             │
└─────────────────────────────────────────────────────────────┘
          ▲             ▲             ▲             ▲
          │             │             │             │
┌─────────┴──────┐ ┌────┴────────┐ ┌──┴──────────┐ ┌┴────────────┐
│   gesture/     │ │  capture/   │ │   ocr/      │ │ classifier/ │
│   (Agent 2)    │ │  (Agent 3)  │ │   (Agent 4) │ │  (Agent 5)  │
└────────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
          ▲             ▲             ▲             ▲
          │             │             │             │
┌─────────┴──────┐ ┌────┴────────┐ ┌──┴──────────┐ ┌┴────────────┐
│   planner/     │ │  actions/   │ │   ui/       │ │  history/   │
│   (Agent 6)    │ │  (Agent 7)  │ │   (Agent 8) │ │  (Agent 9)  │
└────────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
                                  ▲
                                  │
                   ┌──────────────┴──────────────┐
                   │        integration/         │
                   │         (Agent 10)          │
                   └─────────────────────────────┘
```

- Every implementation module depends **inwardly** on `core/contracts/`.
- No horizontal couplings between implementations (e.g., `classifier/` does not depend on `ocr/impl/`, only on `core/contracts/OcrEngine`).
- `integration/` wires implementations together through Dependency Injection or factory orchestrators.

---

## 6. Privacy & Safety Enforcement Matrix

| Hazard | Architectural Safeguard | Enforcing Agent |
| :--- | :--- | :--- |
| **Leak of screen pixels to disk** | Bitmaps exist solely in memory. Bitmaps are recycled in a `finally` block. | Agent 3 & 4 |
| **Unintended phone call / SMS** | Actions flagged as `DANGEROUS` must mandate explicit tap on confirmation modal. | Agent 6, 7, 8 |
| **Silent data transmission to web** | Local-first OCR & classification. Network calls require explicit user-facing opt-in flag. | Agent 4 & 5 |
| **Out-of-memory crashes on big screens** | Capture engine downscales large bitmaps if necessary for OCR analysis. | Agent 3 |
