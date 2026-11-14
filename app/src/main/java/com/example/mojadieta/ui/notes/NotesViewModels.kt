package com.example.mojadieta.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.Note
import com.example.mojadieta.domain.repository.NoteRepository
import com.example.mojadieta.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

class NotesViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<NotesUiState> = combine(noteRepository.observeNotes(), query) { notes, q ->
        NotesUiState(
            notes = notes.filter {
                q.isBlank() || it.title.contains(q.trim(), ignoreCase = true) ||
                    it.content.contains(q.trim(), ignoreCase = true)
            },
            query = q,
            totalCount = notes.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.deleteNote(note.id) }
    }
}

data class NoteEditState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val error: String? = null
)

class NoteEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>(Routes.ARG_NOTE_ID) ?: -1L

    private val _state = MutableStateFlow(NoteEditState(isEditing = noteId > 0, isLoading = noteId > 0))
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                val note = noteRepository.getNote(noteId)
                _state.update { current ->
                    if (note == null) {
                        current.copy(isLoading = false, isEditing = false)
                    } else {
                        current.copy(
                            isLoading = false,
                            title = note.title,
                            content = note.content,
                            createdAt = note.createdAt,
                            updatedAt = note.updatedAt
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, error = null) }

    fun onContentChange(value: String) = _state.update { it.copy(content = value, error = null) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (current.title.isBlank() && current.content.isBlank()) {
            _state.update { it.copy(error = "Notatka jest pusta – wpisz tytuł lub treść") }
            return
        }
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            noteRepository.saveNote(
                Note(
                    id = if (current.isEditing) noteId else 0L,
                    title = current.title.trim().ifBlank { "Bez tytułu" },
                    content = current.content.trim(),
                    createdAt = current.createdAt ?: now,
                    updatedAt = now
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (noteId > 0) noteRepository.deleteNote(noteId)
            onDeleted()
        }
    }
}
