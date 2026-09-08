package com.screeniq.officekit

import com.screeniq.history.model.HistoryEntry
import com.screeniq.settings.model.ScreenIqSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Snapshot model capturing ScreenIQ's live state for display on a laptop companion dashboard
 * alongside phone screen mirroring during hackathon demos.
 */
data class OfficeKitDashboardSnapshot(
    val deviceModel: String = "iQOO Modern Android Device",
    val isEngineActive: Boolean,
    val isGestureEnabled: Boolean,
    val preferLocalAi: Boolean,
    val totalActions: Int,
    val successfulActions: Int,
    val successRatePercent: Int,
    val categoryDistribution: Map<String, Int>,
    val recentActions: List<HistoryEntry>,
    val generatedAtTimestampMs: Long = System.currentTimeMillis()
) {
    val formattedGeneratedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(generatedAtTimestampMs))

    companion object {
        fun from(
            history: List<HistoryEntry>,
            settings: ScreenIqSettings,
            deviceModel: String = "iQOO 12 (Snapdragon 8 Gen 3)"
        ): OfficeKitDashboardSnapshot {
            val total = history.size
            val successful = history.count { it.actionResult.wasSuccessful }
            val rate = if (total > 0) (successful * 100) / total else 100
            val distribution = history.groupingBy { it.contentType }.eachCount()

            return OfficeKitDashboardSnapshot(
                deviceModel = deviceModel,
                isEngineActive = settings.isScreenIqEnabled,
                isGestureEnabled = settings.isGestureEnabled,
                preferLocalAi = settings.preferLocalAi,
                totalActions = total,
                successfulActions = successful,
                successRatePercent = rate,
                categoryDistribution = distribution,
                recentActions = history.take(15)
            )
        }
    }
}

/**
 * Generates a standalone, beautiful HTML/CSS companion dashboard that can be
 * transferred via Office Kit File Transfer and opened in any laptop browser
 * to accompany the phone mirroring feed during live presentations.
 *
 * Strictly zero cloud backend required.
 */
object OfficeKitCompanionDashboardGenerator {

    fun generateHtml(snapshot: OfficeKitDashboardSnapshot): String {
        val rowsHtml = snapshot.recentActions.joinToString("\n") { entry ->
            val badgeClass = when (entry.contentType.uppercase()) {
                "EVENT" -> "badge-event"
                "LOCATION" -> "badge-location"
                "COMMUNICATION", "PHONE", "EMAIL" -> "badge-comm"
                else -> "badge-default"
            }
            val statusColor = if (entry.actionResult.wasSuccessful) "#10B981" else "#EF4444"
            val statusIcon = if (entry.actionResult.wasSuccessful) "✓" else "✗"

            """
            <tr>
              <td><strong>${entry.formattedTime}</strong></td>
              <td><span class="badge $badgeClass">${entry.contentType}</span></td>
              <td>${entry.summary}</td>
              <td>${entry.actionSelected}</td>
              <td style="color: $statusColor; font-weight: 600;">$statusIcon ${entry.actionResult.statusMessage}</td>
              <td><span class="confidence-pill">${entry.confidencePercentage}%</span></td>
            </tr>
            """.trimIndent()
        }

        val categoriesHtml = snapshot.categoryDistribution.entries.joinToString(" ") { (cat, count) ->
            """<div class="stat-pill"><span class="cat-name">$cat:</span> <span class="cat-count">$count</span></div>"""
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>ScreenIQ — Office Kit Companion Dashboard</title>
  <style>
    :root {
      --bg: #0F172A;
      --card-bg: #1E293B;
      --accent: #6366F1;
      --text: #F8FAFC;
      --text-muted: #94A3B8;
      --border: #334155;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
      margin: 0;
      padding: 24px;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border);
      padding-bottom: 16px;
      margin-bottom: 24px;
    }
    .brand {
      font-size: 24px;
      font-weight: 800;
      letter-spacing: -0.5px;
      color: #38BDF8;
    }
    .brand span { color: #818CF8; }
    .tagline { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .stat-card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
    }
    .stat-title { font-size: 13px; color: var(--text-muted); text-transform: uppercase; }
    .stat-value { font-size: 28px; font-weight: 700; margin-top: 8px; color: var(--text); }
    .table-container {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 14px;
    }
    th {
      background: #24324D;
      color: var(--text-muted);
      padding: 12px 16px;
      font-weight: 600;
      text-transform: uppercase;
      font-size: 11px;
      letter-spacing: 0.5px;
    }
    td {
      padding: 14px 16px;
      border-top: 1px solid var(--border);
    }
    .badge {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 700;
    }
    .badge-event { background: rgba(99, 102, 241, 0.2); color: #818CF8; border: 1px solid #6366F1; }
    .badge-location { background: rgba(16, 185, 129, 0.2); color: #34D399; border: 1px solid #10B981; }
    .badge-comm { background: rgba(245, 158, 11, 0.2); color: #FBBF24; border: 1px solid #F59E0B; }
    .badge-default { background: rgba(148, 163, 184, 0.2); color: #CBD5E1; border: 1px solid #64748B; }
    .confidence-pill {
      background: #334155;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }
    .footer {
      margin-top: 24px;
      font-size: 12px;
      color: var(--text-muted);
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="header">
    <div>
      <div class="brand">Screen<span>IQ</span> <small style="font-size:12px; color:#94A3B8;">| Office Kit Companion</small></div>
      <div class="tagline">"See it. Understand it. Do it." — Live Phone Action Feed</div>
    </div>
    <div style="text-align: right; font-size: 12px; color: var(--text-muted);">
      <div>Device: <strong>${snapshot.deviceModel}</strong></div>
      <div>Updated: ${snapshot.formattedGeneratedTime}</div>
    </div>
  </div>

  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-title">Engine Status</div>
      <div class="stat-value" style="color: ${if (snapshot.isEngineActive) "#10B981" else "#EF4444"};">
        ${if (snapshot.isEngineActive) "Active ●" else "Disabled ○"}
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-title">Actions Executed</div>
      <div class="stat-value">${snapshot.totalActions}</div>
    </div>
    <div class="stat-card">
      <div class="stat-title">Success Rate</div>
      <div class="stat-value" style="color: #38BDF8;">${snapshot.successRatePercent}%</div>
    </div>
    <div class="stat-card">
      <div class="stat-title">Local AI Mode</div>
      <div class="stat-value" style="font-size: 20px; color: #A78BFA;">
        ${if (snapshot.preferLocalAi) "On-Device (ML Kit)" else "Hybrid"}
      </div>
    </div>
  </div>

  <div class="table-container">
    <table>
      <thead>
        <tr>
          <th>Time</th>
          <th>Type</th>
          <th>Summary</th>
          <th>Action Triggered</th>
          <th>Result</th>
          <th>Confidence</th>
        </tr>
      </thead>
      <tbody>
        ${if (rowsHtml.isNotBlank()) rowsHtml else "<tr><td colspan='6' style='text-align:center; padding:24px; color:#94A3B8;'>No actions recorded yet. Perform a 4-finger swipe on the iQOO display!</td></tr>"}
      </tbody>
    </table>
  </div>

  <div class="footer">
    Privacy Notice: Zero screen images stored on disk. All actions executed via native Android Intents.
  </div>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Saves the companion dashboard HTML file to disk for Office Kit transfer.
     */
    fun saveDashboardHtml(
        snapshot: OfficeKitDashboardSnapshot,
        outputFile: File = File(System.getProperty("java.io.tmpdir"), "screeniq_companion_dashboard.html")
    ): File {
        val html = generateHtml(snapshot)
        outputFile.writeText(html, Charsets.UTF_8)
        return outputFile
    }
}
