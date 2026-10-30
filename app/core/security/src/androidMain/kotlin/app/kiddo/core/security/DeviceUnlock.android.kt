package app.kiddo.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

// The system's own prompt, no extra library: BiometricPrompt with the phone PIN as fallback on Android 10 and up,
// the keyguard's confirm-credential screen before that.
@Composable
actual fun rememberDeviceUnlock(title: String, onResult: (UnlockResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    val keyguard = remember(context) { context.getSystemService(KeyguardManager::class.java) }
    val confirm = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        latest(if (it.resultCode == Activity.RESULT_OK) UnlockResult.Unlocked else UnlockResult.Cancelled)
    }
    return remember(context, title) {
        {
            when {
                keyguard?.isDeviceSecure != true -> latest(UnlockResult.NoDeviceLock)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> prompt(context, title) { latest(it) }
                else -> {
                    @Suppress("DEPRECATION")
                    val intent = keyguard.createConfirmDeviceCredentialIntent(title, null)
                    if (intent == null) latest(UnlockResult.NoDeviceLock) else confirm.launch(intent)
                }
            }
        }
    }
}

private fun prompt(context: Context, title: String, onResult: (UnlockResult) -> Unit) {
    val builder = BiometricPrompt.Builder(context).setTitle(title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
    } else {
        @Suppress("DEPRECATION")
        builder.setDeviceCredentialAllowed(true)
    }
    builder.build().authenticate(
        CancellationSignal(),
        context.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                onResult(UnlockResult.Unlocked)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                onResult(if (cancelled) UnlockResult.Cancelled else UnlockResult.Failed)
            }
        },
    )
}
