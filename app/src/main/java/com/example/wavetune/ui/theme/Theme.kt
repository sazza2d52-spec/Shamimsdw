package com.example.wavetune.ui.theme

import androidx.compose.runtime.Composable
import com.example.ui.theme.WaveTuneTheme

@Composable
fun WaveTuneAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WaveTuneTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
