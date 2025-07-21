package com.example.llmdemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Left,
    ),
    titleMedium =  TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Left,
    )
)

val defaultText = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Left,
)

val titleFirstText = TextStyle(
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Left,
)

val titleSecondText = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Left,
)

val titleThirdText = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Left,
)

val infoText = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Left,
)

val calorieInfoText = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
)

