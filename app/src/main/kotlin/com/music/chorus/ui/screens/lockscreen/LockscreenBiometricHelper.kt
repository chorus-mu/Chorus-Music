package pushkar.chorus.music.ui.screens.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class BiometricDisplayType {
    IN_DISPLAY_FINGERPRINT,
    SIDE_MOUNTED_FINGERPRINT,
    FACE_UNLOCK,
    NONE
}

object LockscreenBiometricHelper {

    fun detectBiometricType(context: Context): BiometricDisplayType {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecure = keyguardManager?.isKeyguardSecure == true

        if (!isSecure) {
            return BiometricDisplayType.NONE
        }

        val pm = context.packageManager
        val hasFingerprintHardware = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFaceHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false

        // Check native Android biometric enrollment
        var hasEnrolledBiometric = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val biometricManager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                if (biometricManager != null) {
                    @Suppress("DEPRECATION")
                    val canAuth = biometricManager.canAuthenticate()
                    hasEnrolledBiometric = (canAuth == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS)
                }
            } catch (_: Exception) {}
        }

        if (!hasEnrolledBiometric && hasFingerprintHardware) {
            try {
                @Suppress("DEPRECATION")
                val fingerprintManager = context.getSystemService(FingerprintManager::class.java)
                hasEnrolledBiometric = fingerprintManager?.hasEnrolledFingerprints() == true
            } catch (_: Exception) {}
        }

        // If no biometric hardware or none enrolled, fall back to NONE
        if (!hasEnrolledBiometric && !hasFingerprintHardware && !hasFaceHardware) {
            return BiometricDisplayType.NONE
        }

        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)

        // Known in-display fingerprint devices:
        // - Nothing Phones (all models have optical in-display sensor)
        // - Pixel 6, 7, 8, 9 series (in-display)
        // - Samsung Galaxy S / Note series (S20, S21, S22, S23, S24)
        // - OnePlus flagships (7, 8, 9, 10, 11, 12, etc.)
        // - Xiaomi / Vivo / Oppo / Realme flagship series
        val isInDisplayDevice = when {
            manufacturer.contains("nothing") || brand.contains("nothing") -> true
            manufacturer.contains("google") && (model.contains("pixel 6") || model.contains("pixel 7") || model.contains("pixel 8") || model.contains("pixel 9")) -> true
            manufacturer.contains("samsung") && (model.contains("sm-s") || model.contains("sm-g9") || model.contains("galaxy s")) -> true
            manufacturer.contains("oneplus") && !model.contains("nord ce") -> true
            manufacturer.contains("xiaomi") && (model.contains("mi ") || model.contains("12") || model.contains("13") || model.contains("14")) -> true
            else -> false
        }

        // Known side-mounted fingerprint devices:
        val isSideMountedDevice = when {
            manufacturer.contains("samsung") && (model.contains("sm-f") || model.contains("flip") || model.contains("fold") || model.contains("sm-a1") || model.contains("sm-a2")) -> true
            manufacturer.contains("xiaomi") && (model.contains("redmi") || model.contains("poco")) -> true
            manufacturer.contains("motorola") || brand.contains("moto") -> true
            manufacturer.contains("sony") -> true
            manufacturer.contains("asus") -> true
            else -> false
        }

        return when {
            hasFingerprintHardware && isInDisplayDevice -> BiometricDisplayType.IN_DISPLAY_FINGERPRINT
            hasFingerprintHardware && isSideMountedDevice -> BiometricDisplayType.SIDE_MOUNTED_FINGERPRINT
            hasFingerprintHardware -> BiometricDisplayType.IN_DISPLAY_FINGERPRINT // Default fallback for modern phones
            hasFaceHardware -> BiometricDisplayType.FACE_UNLOCK
            else -> BiometricDisplayType.NONE
        }
    }

    fun getSensorBottomPadding(os: PhoneOsFamily): Dp {
        return when (os) {
            PhoneOsFamily.NOTHING_OS -> 76.dp
            PhoneOsFamily.PIXEL -> 84.dp
            PhoneOsFamily.ONE_UI -> 88.dp
            PhoneOsFamily.HYPER_OS -> 80.dp
            PhoneOsFamily.OXYGEN_OS -> 78.dp
            else -> 80.dp
        }
    }
}
