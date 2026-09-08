# ScreenIQ — Multi-Agent Engineering Contract

> **Scope:** Operational protocol and boundaries for 10 autonomous coding agents developing in the shared `ScreenIQ` workspace.

---

## 1. Core Operating Principles

1. **Strict Ownership Boundaries:** Every agent is granted exclusive authority over one domain. Modifying another agent’s core logic without an explicit architectural mandate is prohibited.
2. **Contract-First Development:** Agents must code against the interfaces and models defined in `core/contracts/` (governed by Agent 1 and the system architect). Shared contracts are frozen unless a unanimous compatibility update is required.
3. **Stub & Mock When Blocked:** If Agent $X$ depends on Agent $Y$ and Agent $Y$ has not finished, Agent $X$ **must not** implement Agent $Y$’s component. Instead, Agent $X$ must inject a lightweight local mock or stub implementing the shared interface.
4. **Compile-Ready Invariant:** Every agent must verify that its modifications compile without breaking existing builds (`./gradlew assembleDebug` or relevant module check).
5. **Transparency & Status Tracking:** Every agent must read `TASK_STATUS.md` before starting and update it upon finishing.

---

## 2. Agent Ownership Matrix

| Agent ID | Functional Domain | Exclusive Scope / Package | Key Deliverable | Forbidden Actions |
| :--- | :--- | :--- | :--- | :--- |
| **AGENT 1** | Foundation & Shared Contracts | `core/contracts/`, `core/model/`, `core/common/`, Gradle root & build setup | Shared interfaces, DTOs, project scaffolding, base utilities | Must not implement production services or UI screens |
| **AGENT 2** | Accessibility & Gesture Engine | `gesture/`, `service/accessibility/` | Four-finger gesture detector, AccessibilityService setup, triggers | Must not implement screen capture or OCR logic |
| **AGENT 3** | Screen Capture Subsystem | `capture/` | In-memory frame capture via MediaProjection / Accessibility screenshot API | Must not write screenshots to permanent disk without consent; no OCR |
| **AGENT 4** | OCR & Visual Extraction | `ocr/` | On-device text extraction, bounding boxes, text clustering (ML Kit) | Must not classify business intent or trigger actions |
| **AGENT 5** | AI Understanding & Classification | `classifier/`, `ai/` | Intent engine, entity parser (dates, locations, numbers, QR, unknown) | Must not implement OCR or action execution |
| **AGENT 6** | Action Planning & Confidence | `planner/`, `confidence/` | Action proposal generator, safety scoring, confidence ranking | Must not execute Android Intents or build Compose UI |
| **AGENT 7** | Action Execution Integrations | `actions/execution/` | Native Android Intents (Calendar, Maps, Phone, Mail, Tasks, Browser) | Must not design UI cards or classify intents |
| **AGENT 8** | Compose UI & Action Overlay | `ui/overlay/`, `ui/components/`, `ui/theme/` | Jetpack Compose floating overlay, confirmation cards, animations | Must not execute actions directly or implement OCR |
| **AGENT 9** | History, Privacy & Office Kit | `history/`, `settings/`, `privacy/`, `officekit/` | Room/DataStore for history, privacy toggles, Office Kit export | Must not alter system gesture or screen capture logic |
| **AGENT 10**| Integration, Polish & Demo Mode | `integration/`, `demo/`, `test/` | End-to-end wiring, canned demo scenarios, latency tuning, test harness | Must not rewrite modular implementations from scratch |

---

## 3. Parallel Execution Rules & Interface Dependency

### Rule 3.1: Zero Cross-Domain Scope Creep
- If Agent 5 (Classifier) needs text, it **must** declare a dependency on `OcrEngine` interface.
- If Agent 4 (OCR) is in progress or `NOT STARTED`, Agent 5 creates a `FakeOcrEngine : OcrEngine` in test/mock scope to validate classification.
- Under **no circumstance** should Agent 5 write its own Google ML Kit implementation inside `classifier/`.

### Rule 3.2: Contract Immutability
- Shared contracts live in `core/contracts/`.
- If an agent identifies a missing field (e.g., `ActionSuggestion` needs an additional metadata key), it must propose the change by documenting it in `TASK_STATUS.md` and keeping backward compatibility (e.g., optional parameters with defaults).

### Rule 3.3: In-Memory Privacy Guarantee
- Agents 2, 3, 4, and 5 must handle screen bitmaps strictly in-memory (`android.graphics.Bitmap` or `ByteBuffer`).
- Never save raw user screen captures to `Environment.getExternalStorageDirectory()` or public cache directories.
- Always invoke `bitmap.recycle()` or let garbage collection reclaim the memory once extraction finishes.

### Rule 3.4: Action Execution Safety Classification
Every action suggestion produced by Agent 6 and consumed by Agent 7 & 8 must carry a safety rating:
- **`SAFE_AUTO`**: Completely non-destructive, read-only (e.g., viewing a web link, preparing an empty map view).
- **`REQUIRE_CONFIRMATION`**: Actions that populate personal data, create appointments, copy clipboard, or send data.
- **`DANGEROUS`**: Financial transactions, outbound calling, message sending, deleting items. **Must require explicit two-step user confirmation.**

---

## 4. Standard Agent Workflow Checklist

Before starting work, every agent must execute this sequence:

```
[STEP 1: INSPECT]
  ├── List repository files
  ├── Read PROJECT_SPEC.md & ARCHITECTURE.md
  └── Check TASK_STATUS.md for upstream dependencies

[STEP 2: VERIFY OWNERSHIP]
  ├── Verify target files belong strictly to assigned domain
  └── Import shared interfaces from core/contracts/

[STEP 3: IMPLEMENT WITH STUBS]
  ├── If upstream dependency is NOT STARTED -> Use mock/stub implementation
  └── Implement domain logic cleanly in Kotlin

[STEP 4: VERIFY & TEST]
  ├── Ensure module compiles cleanly
  └── Verify no leaked screen buffers or unsanitized intents

[STEP 5: UPDATE STATUS]
  ├── Update TASK_STATUS.md with files created, status, and integration notes
  └── Document exposed interfaces for downstream agents

[STEP 6: COMMIT & PUSH]
  ├── Stage all created/modified files (`git add -A`)
  ├── Commit with descriptive message identifying Agent ID and domain (`git commit -m "feat(<domain>): ..."`)
  └── Push to remote branch (`git push origin <branch>`)
```

---

## 5. Code & Architectural Conventions

1. **Package Namespace:** `com.screeniq.<module>`
   - Example: `com.screeniq.core.contracts`
   - Example: `com.screeniq.capture`
   - Example: `com.screeniq.ui.overlay`
2. **Data Objects:** Kotlin `data class` with immutable (`val`) fields.
3. **Async Handling:** Coroutine `suspend` functions for single-shot operations; Kotlin `Flow` for continuous streams (e.g., gesture events).
4. **Dependency Injection:** Hilt / Koin or lightweight constructor injection. Interfaces must be injected to allow effortless switching between production and test mocks.
5. **No Blind Refactoring:** Do not reformat or reorganize another agent's code based on aesthetic preference. Respect the established directory hierarchy.
