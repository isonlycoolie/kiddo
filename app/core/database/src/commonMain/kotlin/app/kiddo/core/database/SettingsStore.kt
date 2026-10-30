package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.Appearance
import app.kiddo.core.model.LockAfter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// App settings as key-value rows per workspace. None of them is personal, so they are stored plainly.
class SettingsStore(private val s: Storage) {
    fun settings(workspaceId: String): Flow<AppSettings> =
        s.q.settingsFor(workspaceId).asFlow().mapToList(s.io).map { rows -> read(rows.associate { it.key to it.value_ }) }

    suspend fun update(workspaceId: String, change: (AppSettings) -> AppSettings) = withContext(s.io) {
        val next = change(settings(workspaceId).first())
        s.q.transaction { write(next).forEach { (key, value) -> s.q.putSetting(workspaceId, key, value) } }
    }

    private fun read(map: Map<String, String>) = AppSettings(
        lockEnabled = map[LOCK] == "true",
        lockAfter = LockAfter.entries.firstOrNull { it.name == map[LOCK_AFTER] } ?: LockAfter.RightAway,
        hideInSwitcher = map[HIDE] != "false",
        appearance = Appearance.entries.firstOrNull { it.name == map[APPEARANCE] } ?: Appearance.System,
        chatModel = map[CHAT_MODEL]?.takeIf { it.isNotEmpty() },
        smallModel = map[SMALL_MODEL]?.takeIf { it.isNotEmpty() },
        followUps = map[FOLLOW_UPS] != "false",
        followUpDays = map[FOLLOW_UP_DAYS]?.toIntOrNull() ?: AppSettings().followUpDays,
        followUpMax = map[FOLLOW_UP_MAX]?.toIntOrNull() ?: AppSettings().followUpMax,
        skipWeekends = map[SKIP_WEEKENDS] != "false",
    )

    private fun write(a: AppSettings) = mapOf(
        LOCK to a.lockEnabled.toString(),
        LOCK_AFTER to a.lockAfter.name,
        HIDE to a.hideInSwitcher.toString(),
        APPEARANCE to a.appearance.name,
        CHAT_MODEL to a.chatModel.orEmpty(),
        SMALL_MODEL to a.smallModel.orEmpty(),
        FOLLOW_UPS to a.followUps.toString(),
        FOLLOW_UP_DAYS to a.followUpDays.toString(),
        FOLLOW_UP_MAX to a.followUpMax.toString(),
        SKIP_WEEKENDS to a.skipWeekends.toString(),
    )

    private companion object {
        const val LOCK = "lock"
        const val LOCK_AFTER = "lock_after"
        const val HIDE = "hide_in_switcher"
        const val APPEARANCE = "appearance"
        const val CHAT_MODEL = "chat_model"
        const val SMALL_MODEL = "small_model"
        const val FOLLOW_UPS = "follow_ups"
        const val FOLLOW_UP_DAYS = "follow_up_days"
        const val FOLLOW_UP_MAX = "follow_up_max"
        const val SKIP_WEEKENDS = "skip_weekends"
    }
}
