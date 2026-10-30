package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.kiddo.core.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.kiddo.core.model.Workspace as WorkspaceModel

class WorkspaceStore(private val s: Storage) {
    val workspace: Flow<WorkspaceModel?> = s.q.workspaceFirst().asFlow().mapToOneOrNull(s.io).map { it?.toModel() }

    val profile: Flow<Profile> = s.q.workspaceFirst().asFlow().mapToOneOrNull(s.io)
        .map { Codec.profile(s.cipher.openOrNull(it?.profile_sealed)) }

    suspend fun current(): WorkspaceModel? = withContext(s.io) { s.q.workspaceFirst().executeAsOneOrNull()?.toModel() }

    suspend fun create(name: String, email: String): WorkspaceModel = withContext(s.io) {
        val id = s.ids.next()
        val now = s.clock.now()
        s.q.insertWorkspace(id, s.cipher.seal(name.trim()), s.cipher.seal(email.trim()), now)
        WorkspaceModel(id, name.trim(), email.trim(), now)
    }

    suspend fun save(workspace: WorkspaceModel, profile: Profile) = withContext(s.io) {
        s.q.updateWorkspace(
            s.cipher.seal(workspace.name.trim()),
            s.cipher.seal(workspace.email.trim()),
            s.cipher.seal(Codec.profile(profile)),
            workspace.id,
        )
    }

    private fun Workspace.toModel() =
        WorkspaceModel(id, s.cipher.openOrEmpty(name_sealed), s.cipher.openOrEmpty(email_sealed), created_at)
}
