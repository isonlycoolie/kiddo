package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.Chat
import app.kiddo.core.model.ChatMessage
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.MessageKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.kiddo.core.database.Chat as ChatRow

class ChatStore(private val s: Storage) {
    fun chats(workspaceId: String): Flow<List<Chat>> =
        s.q.chatsFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.map { it.toModel() } }

    fun messages(chatId: String): Flow<List<ChatMessage>> =
        s.q.messagesFor(chatId).asFlow().mapToList(s.io).map { rows -> rows.map { it.toModel() } }

    suspend fun chat(id: String): Chat? = withContext(s.io) { s.q.chatById(id).executeAsOneOrNull()?.toModel() }

    suspend fun history(chatId: String): List<ChatMessage> =
        withContext(s.io) { s.q.messagesFor(chatId).executeAsList().map { it.toModel() } }

    suspend fun start(workspaceId: String, title: String, providerAccountId: String?): String = withContext(s.io) {
        val id = s.ids.next()
        s.q.insertChat(id, workspaceId, s.cipher.seal(title.take(TITLE_MAX)), providerAccountId, s.clock.now())
        id
    }

    suspend fun add(
        chatId: String,
        role: ChatRole,
        text: String,
        providerAccountId: String?,
        kind: MessageKind = MessageKind.Text,
        payload: String? = null,
    ): String =
        withContext(s.io) {
            val id = s.ids.next()
            val now = s.clock.now()
            s.q.transaction {
                s.q.insertMessage(id, chatId, role.name, s.cipher.seal(text), 0, now, kind.name, s.cipher.sealOrNull(payload))
                s.q.touchChat(now, providerAccountId, chatId)
            }
            id
        }

    suspend fun update(messageId: String, text: String, failed: Boolean) =
        withContext(s.io) { s.q.updateMessage(s.cipher.seal(text), if (failed) 1 else 0, messageId) }

    suspend fun setPayload(messageId: String, payload: String) =
        withContext(s.io) { s.q.updatePayload(s.cipher.seal(payload), messageId) }

    suspend fun message(id: String): ChatMessage? = withContext(s.io) { s.q.messageById(id).executeAsOneOrNull()?.toModel() }

    suspend fun delete(chatId: String) = withContext(s.io) {
        s.q.transaction {
            s.q.deleteMessagesFor(chatId)
            s.q.deleteChat(chatId)
        }
    }

    private fun ChatRow.toModel() = Chat(id, s.cipher.openOrEmpty(title_sealed), provider_account_id, updated_at)

    private fun Message.toModel() = ChatMessage(
        id = id,
        chatId = chat_id,
        role = if (role == ChatRole.User.name) ChatRole.User else ChatRole.Assistant,
        text = s.cipher.openOrEmpty(text_sealed),
        createdAt = created_at,
        failed = failed == 1L,
        kind = MessageKind.entries.firstOrNull { it.name == kind } ?: MessageKind.Text,
        payload = s.cipher.openOrNull(payload_sealed),
    )

    private companion object {
        const val TITLE_MAX = 80
    }
}
