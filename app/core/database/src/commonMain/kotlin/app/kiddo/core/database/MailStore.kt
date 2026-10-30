package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// One sending account per workspace in v1: connecting a new one replaces the old.
class MailStore(private val s: Storage) {
    fun accounts(workspaceId: String): Flow<List<MailAccount>> =
        s.q.mailAccountsFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun replace(workspaceId: String, account: MailAccount) = withContext(s.io) {
        s.q.transaction {
            s.q.deleteMailAccountsFor(workspaceId)
            s.q.insertMailAccount(
                account.id,
                workspaceId,
                account.kind.name,
                s.cipher.seal(account.address),
                s.cipher.seal(account.displayName),
                s.cipher.seal(account.username),
                account.smtp?.let { s.cipher.seal(Codec.server(it)) },
                account.imap?.let { s.cipher.seal(Codec.server(it)) },
                account.sendly?.let { s.cipher.seal(Codec.sendly(it)) },
                account.secretRef,
                account.lastCheckedAt,
                account.createdAt,
            )
        }
    }

    suspend fun markChecked(id: String) = withContext(s.io) { s.q.markMailChecked(s.clock.now(), id) }

    suspend fun delete(id: String) = withContext(s.io) { s.q.deleteMailAccount(id) }

    private fun Mail_account.toModel(): MailAccount? {
        val known = MailKind.entries.firstOrNull { it.name == kind } ?: return null
        return MailAccount(
            id = id,
            kind = known,
            address = s.cipher.openOrEmpty(address_sealed),
            displayName = s.cipher.openOrEmpty(display_name_sealed),
            username = s.cipher.openOrEmpty(username_sealed),
            smtp = Codec.server(s.cipher.openOrNull(smtp_sealed)),
            imap = Codec.server(s.cipher.openOrNull(imap_sealed)),
            sendly = Codec.sendly(s.cipher.openOrNull(sendly_sealed)),
            secretRef = secret_ref,
            lastCheckedAt = last_checked_at,
            createdAt = created_at,
        )
    }
}
