package net.matsudamper.gptclient.ui.platform

import androidx.compose.runtime.Composable

@Composable
public expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)
