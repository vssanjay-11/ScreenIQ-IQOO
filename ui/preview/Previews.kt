package com.screeniq.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.screeniq.core.model.ContentCategory
import com.screeniq.ui.navigation.ScreenIQMainApp
import com.screeniq.ui.overlay.OverlayState
import com.screeniq.ui.overlay.ScreenIQOverlay
import com.screeniq.ui.screens.AboutScreen
import com.screeniq.ui.screens.ActionResultScreen
import com.screeniq.ui.screens.HistoryScreen
import com.screeniq.ui.screens.HomeScreen
import com.screeniq.ui.screens.SettingsScreen
import com.screeniq.ui.theme.ScreenIQTheme

@Preview(name = "Overlay - AI Workshop Event", showBackground = true)
@Composable
fun PreviewEventOverlay() {
    ScreenIQTheme(darkTheme = true) {
        ScreenIQOverlay(
            state = OverlayState.SuggestionsReady(
                classification = MockData.sampleEventClassification,
                suggestions = MockData.sampleEventSuggestions,
                elapsedMs = 380L
            ),
            onActionSelected = {},
            onConfirmAction = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Home Screen - Dark", showBackground = true)
@Composable
fun PreviewHomeScreenDark() {
    ScreenIQTheme(darkTheme = true) {
        HomeScreen(
            onTriggerScenario = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Home Screen - Light", showBackground = true)
@Composable
fun PreviewHomeScreenLight() {
    ScreenIQTheme(darkTheme = false) {
        HomeScreen(
            onTriggerScenario = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Action Result Inspector", showBackground = true)
@Composable
fun PreviewActionResultScreen() {
    ScreenIQTheme(darkTheme = true) {
        ActionResultScreen(
            classification = MockData.sampleEventClassification,
            suggestions = MockData.sampleEventSuggestions,
            onActionSelected = {},
            onBack = {}
        )
    }
}

@Preview(name = "History Screen", showBackground = true)
@Composable
fun PreviewHistoryScreen() {
    ScreenIQTheme(darkTheme = true) {
        HistoryScreen(
            historyItems = MockData.sampleHistoryItems,
            onClearHistory = {}
        )
    }
}

@Preview(name = "Settings Screen", showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    ScreenIQTheme(darkTheme = true) {
        SettingsScreen()
    }
}

@Preview(name = "About Screen", showBackground = true)
@Composable
fun PreviewAboutScreen() {
    ScreenIQTheme(darkTheme = true) {
        AboutScreen()
    }
}

@Preview(name = "Full App Host", showBackground = true)
@Composable
fun PreviewFullApp() {
    ScreenIQTheme(darkTheme = true) {
        ScreenIQMainApp()
    }
}
