package app.kiddo.app

import app.kiddo.core.platform.Notifier
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.notice_follow
import app.kiddo.core.resources.notify_body
import app.kiddo.core.resources.notify_title
import app.kiddo.feature.applications.InboxWatcher
import org.jetbrains.compose.resources.getString

// One look at the inbox, from the app coming to the front or a background run. In the background a notification
// says what was found; either way the next follow-up's due time is left with the phone, for phones that rarely run
// Kiddo in the background.
class InboxCheck(private val watcher: InboxWatcher, private val notifier: Notifier) {
    suspend fun run(background: Boolean): Int {
        val found = runCatching { watcher.check() }.getOrDefault(0)
        if (background && found > 0) notifier.show(FOUND, getString(Res.string.notify_title), getString(Res.string.notify_body, found))
        val next = runCatching { watcher.nextFollowUp() }.getOrNull()
        val body = next?.let { getString(Res.string.notice_follow, it.second) }.orEmpty()
        notifier.remind(FOLLOW_UP, getString(Res.string.notify_title), body, next?.first)
        return found
    }

    private companion object {
        const val FOUND = 1
        const val FOLLOW_UP = 2
    }
}
