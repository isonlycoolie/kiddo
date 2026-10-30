package app.kiddo.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import kotlin.coroutines.resume

// The Kiddo browser on iOS: a WKWebView with a data store that only ever lives in memory, so no cookie or page
// storage reaches the disk and all of it is gone when the browser closes. https pages only.
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun KiddoBrowser(url: String, onReader: (suspend () -> String) -> Unit, modifier: Modifier) {
    val guard = remember { HttpsOnly() }
    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            config.websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
            view.navigationDelegate = guard
            view.allowsBackForwardNavigationGestures = true
            onReader { readText(view) }
            NSURL.URLWithString(url)?.let { view.loadRequest(NSURLRequest.requestWithURL(it)) }
            view
        },
        modifier = modifier,
    )
}

private class HttpsOnly : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val scheme = decidePolicyForNavigationAction.request.URL?.scheme
        val allowed = scheme == "https" || scheme == "about"
        decisionHandler(if (allowed) WKNavigationActionPolicy.WKNavigationActionPolicyAllow else WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }
}

private suspend fun readText(view: WKWebView): String = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { done ->
        view.evaluateJavaScript("document.title + '\\n\\n' + document.body.innerText") { result, _ ->
            done.resume((result as? String).orEmpty().take(MAX_PAGE))
        }
    }
}

