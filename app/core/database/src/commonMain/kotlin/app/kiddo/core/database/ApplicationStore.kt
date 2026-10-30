package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.Notice
import app.kiddo.core.model.NoticeKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.kiddo.core.model.Application as ApplicationModel

class ApplicationStore(private val s: Storage) {
    fun applications(workspaceId: String): Flow<List<ApplicationModel>> =
        s.q.applicationsFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun all(workspaceId: String): List<ApplicationModel> =
        withContext(s.io) { s.q.applicationsFor(workspaceId).executeAsList().mapNotNull { it.toModel() } }

    suspend fun get(id: String): ApplicationModel? = withContext(s.io) { s.q.applicationById(id).executeAsOneOrNull()?.toModel() }

    @Suppress("LongParameterList")
    suspend fun add(
        workspaceId: String,
        role: String,
        company: String,
        to: String,
        chatId: String?,
        messageId: String?,
        cvName: String?,
        status: AppStatus = AppStatus.Sent,
    ): String = withContext(s.io) {
        val id = s.ids.next()
        val now = s.clock.now()
        s.q.insertApplication(
            id,
            workspaceId,
            s.cipher.seal(role),
            s.cipher.seal(company),
            s.cipher.seal(to),
            status.name,
            chatId,
            s.cipher.sealOrNull(messageId),
            s.cipher.sealOrNull(cvName),
            now,
            now,
        )
        id
    }

    suspend fun setStatus(id: String, status: AppStatus) = withContext(s.io) { s.q.setApplicationStatus(status.name, s.clock.now(), id) }

    suspend fun setReply(id: String, status: AppStatus, reply: String, at: Long) =
        withContext(s.io) { s.q.setApplicationReply(status.name, s.cipher.seal(reply), at, s.clock.now(), id) }

    suspend fun countFollowUp(id: String) = withContext(s.io) { s.q.countFollowUp(s.clock.now(), id) }

    suspend fun delete(id: String) = withContext(s.io) { s.q.deleteApplication(id) }

    private fun Application.toModel(): ApplicationModel? {
        val known = AppStatus.entries.firstOrNull { it.name == status } ?: return null
        return ApplicationModel(
            id = id,
            role = s.cipher.openOrEmpty(role_sealed),
            company = s.cipher.openOrEmpty(company_sealed),
            to = s.cipher.openOrEmpty(to_sealed),
            status = known,
            chatId = chat_id,
            messageId = s.cipher.openOrNull(message_id_sealed),
            cvName = s.cipher.openOrNull(cv_name_sealed),
            followUps = follow_ups.toInt(),
            reply = s.cipher.openOrNull(reply_sealed),
            replyAt = reply_at,
            sentAt = sent_at,
            updatedAt = updated_at,
        )
    }
}

// Updates: replies found, follow-ups due, sends, inbox problems.
class NoticeStore(private val s: Storage) {
    fun notices(workspaceId: String): Flow<List<Notice>> =
        s.q.noticesFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun all(workspaceId: String): List<Notice> =
        withContext(s.io) { s.q.noticesFor(workspaceId).executeAsList().mapNotNull { it.toModel() } }

    suspend fun add(workspaceId: String, kind: NoticeKind, applicationId: String?, body: String) = withContext(s.io) {
        s.q.insertNotice(s.ids.next(), workspaceId, kind.name, applicationId, s.cipher.seal(body), s.clock.now())
    }

    suspend fun read(id: String) = withContext(s.io) { s.q.markNoticeRead(id) }

    suspend fun readAll(workspaceId: String) = withContext(s.io) { s.q.markNoticesRead(workspaceId) }

    suspend fun clear(applicationId: String, kind: NoticeKind) = withContext(s.io) { s.q.deleteNoticesFor(applicationId, kind.name) }

    private fun app.kiddo.core.database.Notice.toModel(): Notice? {
        val known = NoticeKind.entries.firstOrNull { it.name == kind } ?: return null
        return Notice(id, known, application_id, s.cipher.openOrEmpty(body_sealed), read == 1L, created_at)
    }
}
