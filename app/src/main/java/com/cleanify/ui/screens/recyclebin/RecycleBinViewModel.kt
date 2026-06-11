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
    val availableCategories: List<FileCategory> = emptyList(),
    val totalSize: Long = 0
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinDao: RecycleBinDao,
    private val recycleBinManager: RecycleBinManager
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)

    private val _previewEntry = MutableStateFlow<RecycleBinEntry?>(null)
    val previewEntry: StateFlow<RecycleBinEntry?> = _previewEntry.asStateFlow()

    private val allEntriesFlow = recycleBinDao.getAllEntries()

    val uiState: StateFlow<RecycleBinUiState> = combine(
        allEntriesFlow,
        _selectedCategory,
        _selectedIds,
        recycleBinDao.observeTotalSize()
    ) { allEntries, category, selectedIds, totalSize ->
        val filtered = if (category == null) allEntries else allEntries.filter { it.fileCategory == category }
        val available = allEntries.map { it.fileCategory }.distinct().sortedBy { it.ordinal }
        RecycleBinUiState(
            entries = filtered,
            selectedIds = selectedIds,
            selectedCategory = category,
            availableCategories = available,
            totalSize = totalSize
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

    fun showPreview(entry: RecycleBinEntry) {
        _previewEntry.value = entry
    }

    fun dismissPreview() {
        _previewEntry.value = null
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
        }
    }

    fun permanentlyDeleteEntries(ids: Set<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                val entry = recycleBinDao.getEntryById(id)
                if (entry != null) {
                    recycleBinManager.permanentlyDeleteEntry(entry)
                }
            }
            _selectedIds.value = emptySet()
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            recycleBinManager.emptyBin()
        }
    }
}
