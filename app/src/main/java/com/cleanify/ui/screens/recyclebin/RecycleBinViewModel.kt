package com.cleanify.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanify.data.db.dao.RecycleBinDao
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import com.cleanify.util.RecycleBinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val entries: List<RecycleBinEntry> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val selectedCategory: FileCategory? = null,
    val totalSize: Long = 0,
    val isRefreshing: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinDao: RecycleBinDao,
    private val recycleBinManager: RecycleBinManager
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)

    private val _isRefreshing = MutableStateFlow(false)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _restoredCount = MutableStateFlow(0)
    val restoredCount: StateFlow<Int> = _restoredCount.asStateFlow()

    private val _permaDeletedCount = MutableStateFlow(0)
    val permaDeletedCount: StateFlow<Int> = _permaDeletedCount.asStateFlow()

    private val entriesFlow = combine(
        recycleBinDao.getAllEntries(),
        _selectedCategory
    ) { entries, category ->
        if (category == null) entries else entries.filter { it.fileCategory == category }
    }

    val uiState: StateFlow<RecycleBinUiState> = combine(
        entriesFlow,
        _selectedIds,
        _selectedCategory,
        recycleBinDao.observeTotalSize(),
        _isRefreshing
    ) { entries, selected, category, totalSize, refreshing ->
        RecycleBinUiState(
            entries = entries,
            selectedIds = selected,
            selectedCategory = category,
            totalSize = totalSize,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecycleBinUiState())

    fun setCategory(category: FileCategory?) {
        _selectedCategory.value = category
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: String) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAll() {
        val current = uiState.value.entries.map { it.id }.toSet()
        _selectedIds.value = if (_selectedIds.value.size == current.size) emptySet() else current
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            var restored = 0
            ids.forEach { id ->
                val entry = recycleBinDao.getEntryById(id)
                if (entry != null && recycleBinManager.restoreEntry(entry)) {
                    restored++
                }
            }
            _selectedIds.value = emptySet()
            _snackbarMessage.value = "$restored files restored"
            _restoredCount.value = restored
        }
    }

    fun permanentlyDeleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            var deleted = 0
            ids.forEach { id ->
                val entry = recycleBinDao.getEntryById(id)
                if (entry != null && recycleBinManager.permanentlyDeleteEntry(entry)) {
                    deleted++
                }
            }
            _selectedIds.value = emptySet()
            _snackbarMessage.value = "$deleted files permanently deleted"
            _permaDeletedCount.value = deleted
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            val count = recycleBinManager.emptyBin()
            _snackbarMessage.value = "Recycle bin emptied ($count files)"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
