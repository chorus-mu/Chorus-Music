package pushkar.chorus.music.ui.screens.lockscreen

import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class PhoneOsFamily(val displayName: String) {
    ONE_UI("Samsung One UI"),
    PIXEL("Google Pixel / Material You"),
    NOTHING_OS("Nothing OS"),
    HYPER_OS("Xiaomi HyperOS"),
    OXYGEN_OS("OnePlus OxygenOS / ColorOS"),
    MOTO("Motorola Hello UI"),
    GENERIC("Android Standard")
}

data class LockscreenOsStyleConfig(
    val clockFontSize: TextUnit,
    val clockFontWeight: FontWeight,
    val clockLetterSpacing: TextUnit,
    val clockFontFamily: FontFamily = FontFamily.Default,
    val dateFontWeight: FontWeight,
    val dateLetterSpacing: TextUnit,
    val dateAllCaps: Boolean,
    val cardCornerRadius: Dp,
    val buttonShape: Shape,
    val glassCardAlpha: Float,
    val glassBorderAlpha: Float,
    val bottomFingerprintSafePadding: Dp,
    val accentTint: Color,
)

object PhoneOsAdapter {
    val detectedOs: PhoneOsFamily by lazy {
        detect()
    }

    private fun detect(): PhoneOsFamily {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.ROOT)

        return when {
            manufacturer.contains("samsung") || brand.contains("samsung") -> PhoneOsFamily.ONE_UI
            manufacturer.contains("google") || brand.contains("google") || model.contains("pixel") -> PhoneOsFamily.PIXEL
            manufacturer.contains("nothing") || brand.contains("nothing") -> PhoneOsFamily.NOTHING_OS
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("poco") -> PhoneOsFamily.HYPER_OS
            manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> PhoneOsFamily.OXYGEN_OS
            manufacturer.contains("motorola") || brand.contains("moto") -> PhoneOsFamily.MOTO
            fingerprint.contains("samsung") -> PhoneOsFamily.ONE_UI
            fingerprint.contains("pixel") -> PhoneOsFamily.PIXEL
            fingerprint.contains("nothing") -> PhoneOsFamily.NOTHING_OS
            else -> PhoneOsFamily.GENERIC
        }
    }

    private fun getOemFont(name: String): FontFamily {
        return try {
            val tf = android.graphics.Typeface.create(name, android.graphics.Typeface.BOLD)
            if (tf != null && tf != android.graphics.Typeface.DEFAULT) {
                FontFamily(tf)
            } else {
                FontFamily.Default
            }
        } catch (_: Exception) {
            FontFamily.Default
        }
    }

    fun getStyleConfig(os: PhoneOsFamily = detectedOs): LockscreenOsStyleConfig {
        return when (os) {
            PhoneOsFamily.ONE_UI -> LockscreenOsStyleConfig(
                clockFontSize = 52.sp,
                clockFontWeight = FontWeight.Bold,
                clockLetterSpacing = (-0.5).sp,
                clockFontFamily = getOemFont("sec-roboto-light"),
                dateFontWeight = FontWeight.Normal,
                dateLetterSpacing = 0.sp,
                dateAllCaps = false,
                cardCornerRadius = 26.dp,
                buttonShape = RoundedCornerShape(22.dp),
                glassCardAlpha = 0.60f,
                glassBorderAlpha = 0.12f,
                bottomFingerprintSafePadding = 20.dp, // Samsung in-display sensor clearance
                accentTint = Color(0xFF2E7D32)
            )
            PhoneOsFamily.NOTHING_OS -> LockscreenOsStyleConfig(
                clockFontSize = 50.sp,
                clockFontWeight = FontWeight.Bold,
                clockLetterSpacing = 3.sp,
                clockFontFamily = getOemFont("ndot57"),
                dateFontWeight = FontWeight.Medium,
                dateLetterSpacing = 2.5.sp,
                dateAllCaps = true,
                cardCornerRadius = 18.dp,
                buttonShape = CircleShape,
                glassCardAlpha = 0.75f,
                glassBorderAlpha = 0.25f,
                bottomFingerprintSafePadding = 18.dp,
                accentTint = Color.White
            )
            PhoneOsFamily.PIXEL -> LockscreenOsStyleConfig(
                clockFontSize = 56.sp,
                clockFontWeight = FontWeight.SemiBold,
                clockLetterSpacing = (-1).sp,
                clockFontFamily = getOemFont("google-sans"),
                dateFontWeight = FontWeight.Medium,
                dateLetterSpacing = 0.sp,
                dateAllCaps = false,
                cardCornerRadius = 30.dp,
                buttonShape = CircleShape,
                glassCardAlpha = 0.65f,
                glassBorderAlpha = 0.10f,
                bottomFingerprintSafePadding = 16.dp,
                accentTint = Color(0xFF90CAF9)
            )
            PhoneOsFamily.HYPER_OS -> LockscreenOsStyleConfig(
                clockFontSize = 54.sp,
                clockFontWeight = FontWeight.ExtraBold,
                clockLetterSpacing = (-1.5).sp,
                clockFontFamily = getOemFont("mipro"),
                dateFontWeight = FontWeight.Normal,
                dateLetterSpacing = 0.2.sp,
                dateAllCaps = false,
                cardCornerRadius = 24.dp,
                buttonShape = RoundedCornerShape(20.dp),
                glassCardAlpha = 0.70f,
                glassBorderAlpha = 0.18f,
                bottomFingerprintSafePadding = 18.dp,
                accentTint = Color(0xFFFF5722)
            )
            PhoneOsFamily.OXYGEN_OS -> LockscreenOsStyleConfig(
                clockFontSize = 52.sp,
                clockFontWeight = FontWeight.Bold,
                clockLetterSpacing = (-0.8).sp,
                clockFontFamily = getOemFont("oneplus-sans"),
                dateFontWeight = FontWeight.Medium,
                dateLetterSpacing = 0.sp,
                dateAllCaps = false,
                cardCornerRadius = 24.dp,
                buttonShape = CircleShape,
                glassCardAlpha = 0.68f,
                glassBorderAlpha = 0.15f,
                bottomFingerprintSafePadding = 20.dp,
                accentTint = Color(0xFFE53935)
            )
            PhoneOsFamily.MOTO, PhoneOsFamily.GENERIC -> LockscreenOsStyleConfig(
                clockFontSize = 52.sp,
                clockFontWeight = FontWeight.Bold,
                clockLetterSpacing = (-1).sp,
                clockFontFamily = FontFamily.Default,
                dateFontWeight = FontWeight.Medium,
                dateLetterSpacing = 0.sp,
                dateAllCaps = false,
                cardCornerRadius = 24.dp,
                buttonShape = CircleShape,
                glassCardAlpha = 0.65f,
                glassBorderAlpha = 0.15f,
                bottomFingerprintSafePadding = 16.dp,
                accentTint = Color.White
            )
        }
    }
}
