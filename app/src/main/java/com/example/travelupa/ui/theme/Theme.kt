package com.example.travelupa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White

private val DarkColorPalette = darkColors(
    primary = Purple80,
    secondary = PurpleGrey80,
    background = Black
)

private val LightColorPalette = lightColors(
    primary = Purple40,
    secondary = PurpleGrey40,
    background = White
)

@Composable
fun TravelupaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}