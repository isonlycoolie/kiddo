package app.kiddo.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// The system's own prompt: Face ID or Touch ID, with the passcode as the fallback iOS offers itself.
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceUnlock(title: String, onResult: (UnlockResult) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    return remember(title) {
        {
            val context = LAContext()
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)) {
                latest(UnlockResult.NoDeviceLock)
            } else {
                context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = title) { ok, error ->
                    val result = when {
                        ok -> UnlockResult.Unlocked
                        error?.code in CANCELLED -> UnlockResult.Cancelled
                        error?.code == PASSCODE_NOT_SET -> UnlockResult.NoDeviceLock
                        else -> UnlockResult.Failed
                    }
                    dispatch_async(dispatch_get_main_queue()) { latest(result) }
                }
            }
        }
    }
}

// LocalAuthentication error codes: cancelled by the user, the system or the app; no passcode set.
private val CANCELLED = setOf(-2L, -4L, -9L)
private const val PASSCODE_NOT_SET = -5L
