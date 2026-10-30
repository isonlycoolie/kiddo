package app.kiddo.feature.cvs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.Cv
import app.kiddo.core.model.Failure
import app.kiddo.core.platform.CvReader
import app.kiddo.core.platform.PickResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The CV list plus each file's bytes, opened once per CV for its page preview.
@OptIn(ExperimentalCoroutinesApi::class)
class CvsViewModel(private val workspaces: WorkspaceStore, private val cvs: CvStore, private val reader: CvReader) : ViewModel() {
    val list: StateFlow<List<Cv>?> = workspaces.workspace.filterNotNull()
        .flatMapLatest { cvs.cvs(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    private val _files = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val files: StateFlow<Map<String, ByteArray>> = _files

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun loadFile(id: String) {
        if (_files.value.containsKey(id)) return
        viewModelScope.launch { cvs.bytes(id)?.let { bytes -> _files.update { it + (id to bytes) } } }
    }

    // Reads the CV on the phone, then stores it sealed. Returns what to tell the user.
    fun add(result: PickResult, onDone: (Failure?) -> Unit) {
        val picked = when (result) {
            is PickResult.Picked -> result.file
            PickResult.TooLarge -> return onDone(Failure.TooLarge)
            PickResult.Unsupported -> return onDone(Failure.Unreadable)
        }
        _busy.value = true
        viewModelScope.launch {
            val ws = workspaces.current()
            val reading = reader.read(picked.bytes, picked.format)
            if (ws == null || reading == null) {
                _busy.value = false
                return@launch onDone(Failure.Unreadable)
            }
            cvs.add(ws.id, picked.name, picked.format, reading.pages, reading.text, picked.bytes)
            _busy.value = false
            onDone(null)
        }
    }

    private companion object {
        const val STOP_AFTER = 5_000L
    }
}

