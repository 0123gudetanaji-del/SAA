package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AmberGold,
    onPrimary = AutoNavyDark,
    primaryContainer = AutoNavy,
    onPrimaryContainer = Color.White,
    secondary = SteelBlue,
    onSecondary = Color.White,
    background = AutoNavyDark,
    surface = Slate900,
    onBackground = Slate100,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AutoNavy,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = AutoNavyDark,
    secondary = AmberGoldDark,
    onSecondary = Color.White,
    secondaryContainer = AmberGoldLight,
    onSecondaryContainer = AutoNavyDark,
    tertiary = SteelBlue,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For branded identity, we use our cohesive automotive palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

