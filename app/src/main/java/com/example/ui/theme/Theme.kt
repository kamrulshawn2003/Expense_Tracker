package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  onPrimary = EmeraldOnPrimaryDark,
  primaryContainer = EmeraldPrimaryContainerDark,
  onPrimaryContainer = EmeraldOnPrimaryContainerDark,
  secondary = SlateSecondaryDark,
  onSecondary = SlateOnSecondaryDark,
  secondaryContainer = SlateSecondaryContainerDark,
  onSecondaryContainer = SlateOnSecondaryContainerDark,
  tertiary = AmberTertiaryDark,
  onTertiary = AmberOnTertiaryDark,
  tertiaryContainer = AmberTertiaryContainerDark,
  onTertiaryContainer = AmberOnTertiaryContainerDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  error = ExpenseRed
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimaryLight,
  onPrimary = EmeraldOnPrimaryLight,
  primaryContainer = EmeraldPrimaryContainerLight,
  onPrimaryContainer = EmeraldOnPrimaryContainerLight,
  secondary = SlateSecondaryLight,
  onSecondary = SlateOnSecondaryLight,
  secondaryContainer = SlateSecondaryContainerLight,
  onSecondaryContainer = SlateOnSecondaryContainerLight,
  tertiary = AmberTertiaryLight,
  onTertiary = AmberOnTertiaryLight,
  tertiaryContainer = AmberTertiaryContainerLight,
  onTertiaryContainer = AmberOnTertiaryContainerLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  error = ExpenseRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}


