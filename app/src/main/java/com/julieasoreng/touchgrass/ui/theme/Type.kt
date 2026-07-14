package com.julieasoreng.touchgrass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val nunitoGoogleFont = GoogleFont("Nunito")
private val quicksandGoogleFont = GoogleFont("Quicksand")
private val interGoogleFont = GoogleFont("Inter")

val Nunito = FontFamily(
    Font(googleFont = nunitoGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = nunitoGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = nunitoGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// Used by the Goals + focus timer feature, matching its design spec (Quicksand headings, Inter body).
val Quicksand = FontFamily(
    Font(googleFont = quicksandGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = quicksandGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = quicksandGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val Inter = FontFamily(
    Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val BloomTypography = Typography(
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
)
