package app.kiddo.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.kiddo.core.model.Cv
import app.kiddo.core.model.CvFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.kiddo.core.database.Cv as CvRow

// CV rows plus their files. The file is sealed before it touches storage; the original is never changed.
class CvStore(private val s: Storage) {
    fun cvs(workspaceId: String): Flow<List<Cv>> =
        s.q.cvsFor(workspaceId).asFlow().mapToList(s.io).map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun add(
        workspaceId: String,
        name: String,
        format: CvFormat,
        pages: Int,
        text: String?,
        bytes: ByteArray,
        byKiddo: Boolean = false,
    ): String = withContext(s.io) {
        val id = s.ids.next()
        s.files.write(fileName(id), s.cipher.sealBytes(bytes))
        val first = s.q.cvsFor(workspaceId).executeAsList().isEmpty()
        s.q.insertCv(
            id,
            workspaceId,
            s.cipher.seal(name),
            format.name,
            pages.toLong(),
            s.cipher.sealOrNull(text),
            if (first) 1 else 0,
            if (byKiddo) 1 else 0,
            s.clock.now(),
        )
        id
    }

    suspend fun bytes(id: String): ByteArray? = withContext(s.io) { s.files.read(fileName(id))?.let(s.cipher::openBytes) }

    suspend fun rename(id: String, name: String) = withContext(s.io) { s.q.renameCv(s.cipher.seal(name), id) }

    suspend fun makeDefault(workspaceId: String, id: String) = withContext(s.io) {
        s.q.transaction {
            s.q.clearDefaultCv(workspaceId)
            s.q.setDefaultCv(id)
        }
    }

    suspend fun delete(id: String) = withContext(s.io) {
        s.q.deleteCv(id)
        s.files.delete(fileName(id))
    }

    private fun fileName(id: String) = "cv-$id.bin"

    private fun CvRow.toModel(): Cv? {
        val known = CvFormat.entries.firstOrNull { it.name == format } ?: return null
        return Cv(
            id = id,
            name = s.cipher.openOrEmpty(name_sealed),
            format = known,
            pages = pages.toInt(),
            text = s.cipher.openOrNull(text_sealed),
            isDefault = is_default == 1L,
            byKiddo = by_kiddo == 1L,
            addedAt = added_at,
        )
    }
}
