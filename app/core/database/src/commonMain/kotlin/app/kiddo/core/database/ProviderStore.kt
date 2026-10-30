package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.AiProvider
import app.kiddo.core.model.ProviderAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProviderStore(private val s: Storage) {
    fun accounts(workspaceId: String): Flow<List<ProviderAccount>> =
        s.q.providersFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun add(workspaceId: String, provider: AiProvider, baseUrl: String, model: String, keyRef: String): String =
        withContext(s.io) {
            val id = s.ids.next()
            s.q.insertProvider(id, workspaceId, provider.name, baseUrl, model, keyRef, s.clock.now())
            id
        }

    suspend fun update(id: String, baseUrl: String, model: String) = withContext(s.io) { s.q.updateProvider(baseUrl, model, id) }

    suspend fun addTokens(id: String, tokens: Long) = withContext(s.io) { s.q.addTokens(tokens, id) }

    suspend fun delete(id: String) = withContext(s.io) { s.q.deleteProvider(id) }

    private fun Provider_account.toModel(): ProviderAccount? {
        val known = AiProvider.entries.firstOrNull { it.name == provider } ?: return null
        return ProviderAccount(id, known, base_url, model, key_ref, tokens_used, created_at)
    }
}
