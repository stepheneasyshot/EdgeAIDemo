package com.example.llamacppdemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LLMDemoTheme(
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (isSystemInDarkTheme()) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}