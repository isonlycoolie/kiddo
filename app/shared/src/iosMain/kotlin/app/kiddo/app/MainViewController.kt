package app.kiddo.app

import androidx.compose.ui.window.ComposeUIViewController
import app.kiddo.core.designsystem.component.ScrollToTop
import app.kiddo.core.platform.SwitcherCover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

// The one view controller the Swift shell hosts; every screen is a Compose destination inside it.
@Suppress("FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController { KiddoApp() }

// The Swift shell saw a tap on the status bar.
fun onStatusBarTap() = ScrollToTop.request()

// Whether the app switcher should see a cover instead of Kiddo ("Hide in app switcher").
fun hidesInSwitcher(): Boolean = SwitcherCover.on

// Told when a background inbox check is over.
interface RefreshDone {
    fun finished(success: Boolean)
}

// One background inbox check for iOS's app refresh task. iOS gives it about half a minute; cancel() is called if
// that runs out, and the shell then reports the task as unfinished itself.
class InboxRefresh {
    private var job: Job? = null

    fun start(done: RefreshDone) {
        job = CoroutineScope(Dispatchers.Main).launch {
            val ok = runCatching { KoinPlatform.getKoin().get<InboxCheck>().run(background = true) }.isSuccess
            done.finished(ok)
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
