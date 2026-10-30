package app.kiddo.core.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONTokener
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun KiddoBrowser(url: String, onReader: (suspend () -> String) -> Unit, modifier: Modifier) {
    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    // https pages only; anything else stays out.
                    override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest) =
                        request.url.scheme != "https"
                }
                onReader { readText(this) }
                loadUrl(url)
            }
        },
    )
}

private suspend fun readText(view: WebView): String = suspendCancellableCoroutine { done ->
    view.evaluateJavascript("(function(){return document.title + '\\n\\n' + document.body.innerText;})()") { raw ->
        val text = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull().orEmpty()
        done.resume(text.take(MAX_PAGE))
    }
}

class AndroidCvRenderer : CvRenderer by PdfCvRenderer()

class AndroidNotifier(private val context: Context) : Notifier {
    override fun show(id: Int, title: String, body: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL = "updates"
    }
}

@Composable
actual fun rememberNotificationAsk(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return remember(context) {
        {
            val needed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needed) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private const val MAX_PAGE = 100_000

@Composable
actual fun rememberFileSaver(onDone: (Boolean) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<ByteArray>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pending[0]
        pending[0] = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult onDone(false)
        val written = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false)
        onDone(written)
    }
    return remember(launcher) { { name, bytes -> pending[0] = bytes; launcher.launch(name) } }
}

@Composable
actual fun rememberFileOpener(onFile: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onFile(uri?.let { runCatching { context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }.getOrNull() })
    }
    return remember(launcher) { { launcher.launch(arrayOf("*/*")) } }
}
