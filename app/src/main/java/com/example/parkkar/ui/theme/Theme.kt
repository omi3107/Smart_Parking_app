package com.example.parkkar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define your application's specific Light Color Scheme
private val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),       // Purple 500
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),     // Teal 200
    onSecondary = Color.Black,
    background = Color(0xFFF5F5F5),   // Light Grey
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFFB00020),        // Red 700
    onError = Color.White
    // You can define other colors as needed
)

// Define your application's Typography
// You would typically define text styles like H1, Body1, etc.
// For now, we'll use MaterialTheme defaults by passing an empty Typography object,
// or you can customize it as per your designs.
private val AppTypography = Typography(
    /* Example:
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    */
)

// Define your application's Shapes
// You can customize component shapes (small, medium, large)
private val AppShapes = Shapes(
    /* Example:
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
    */
)

@Composable
fun ParkkarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}