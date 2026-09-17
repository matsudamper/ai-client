package net.matsudamper.gptclient

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SyncSystemBarsAppearance(isDark: Boolean) {
    val view = LocalView.current
    val window = LocalActivity.current?.window
    if (window != null && view.isInEditMode.not()) {
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isDark.not()
                isAppearanceLightNavigationBars = isDark.not()
            }
        }
    }
}
