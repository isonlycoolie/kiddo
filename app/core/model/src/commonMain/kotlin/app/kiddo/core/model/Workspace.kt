package app.kiddo.core.model

// The local space created on first open. Every stored row carries its id, so more workspaces can come later.
data class Workspace(val id: String, val name: String, val email: String, val createdAt: Long)

// What Kiddo puts in every application, beside the workspace name and email.
data class Profile(
    val headline: String = "",
    val phone: String = "",
    val portfolio: String = "",
    val linkedin: String = "",
    val tone: Tone = Tone.Friendly,
    val signature: Signature = Signature.NameAndPhone,
)

enum class Tone { Formal, Friendly, Brief }

enum class Signature { Name, NameAndPhone }

enum class Appearance { System, Light, Dark }

enum class LockAfter(val millis: Long) {
    RightAway(0),
    OneMinute(60_000),
    FiveMinutes(300_000),
}

data class AppSettings(
    val lockEnabled: Boolean = false,
    val lockAfter: LockAfter = LockAfter.RightAway,
    val hideInSwitcher: Boolean = true,
    val appearance: Appearance = Appearance.System,
    val chatModel: String? = null,
    val smallModel: String? = null,
    val followUps: Boolean = true,
    val followUpDays: Int = 7,
    val followUpMax: Int = 2,
    val skipWeekends: Boolean = true,
)
