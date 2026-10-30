package app.kiddo.core.model

enum class AppStatus { Sent, Replied, Interview, AppliedOnSite, Closed }

// One job the user applied to. `messageId` is the Message-ID header of the sent email, used to match replies.
data class Application(
    val id: String,
    val role: String,
    val company: String,
    val to: String,
    val status: AppStatus,
    val chatId: String?,
    val messageId: String?,
    val cvName: String?,
    val followUps: Int,
    val reply: String?,
    val replyAt: Long?,
    val sentAt: Long,
    val updatedAt: Long,
)

enum class NoticeKind { Reply, FollowUpDue, Sent, CheckFailed }

// One line in Updates.
data class Notice(
    val id: String,
    val kind: NoticeKind,
    val applicationId: String?,
    val body: String,
    val read: Boolean,
    val createdAt: Long,
)

// A reply found in the inbox for a sent application.
data class FoundReply(val from: String, val subject: String, val inReplyTo: String, val snippet: String, val at: Long)
