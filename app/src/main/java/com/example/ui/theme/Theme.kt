package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ZentrixDarkColorScheme = darkColorScheme(
    primary = ZentrixPrimaryBlue,
    onPrimary = ZentrixTextPrimary,
    primaryContainer = ZentrixSurfaceVariant,
    onPrimaryContainer = ZentrixSecondaryBlue,
    secondary = ZentrixSecondaryBlue,
    onSecondary = ZentrixBackground,
    background = ZentrixBackground,
    onBackground = ZentrixTextPrimary,
    surface = ZentrixSurface,
    onSurface = ZentrixTextPrimary,
    surfaceVariant = ZentrixSurfaceVariant,
    onSurfaceVariant = ZentrixTextSecondary,
    outline = ZentrixCardBorder,
    error = ZentrixError,
    onError = ZentrixTextPrimary
)

val ZentrixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZentrixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZentrixDarkColorScheme,
        typography = Typography,
        shapes = ZentrixShapes,
        content = content
    )
}
