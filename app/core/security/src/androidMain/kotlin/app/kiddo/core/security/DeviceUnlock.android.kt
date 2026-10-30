package app.kiddo.core.security

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// AndroidX BiometricPrompt: fingerprint or face, with the phone PIN as fallback, on every Android Kiddo supports.
@Composable
actual fun rememberDeviceUnlock(title: String, onResult: (UnlockResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    return remember(context, title) {
        {
            val activity = context.findActivity()
            val manager = androidx.biometric.BiometricManager.from(context)
            when {
                activity == null -> latest(UnlockResult.Failed)
                manager.canAuthenticate(AUTHENTICATORS) != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS ->
                    latest(UnlockResult.NoDeviceLock)
                else -> prompt(activity, title) { latest(it) }
            }
        }
    }
}

private fun prompt(activity: FragmentActivity, title: String, onResult: (UnlockResult) -> Unit) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(UnlockResult.Unlocked)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
            onResult(if (cancelled) UnlockResult.Cancelled else UnlockResult.Failed)
        }
    }
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()
    BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Strong biometrics with the PIN fallback; Android 9 and 10 only allow that fallback next to the weak class.
private val AUTHENTICATORS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL
