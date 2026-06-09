package com.cleanify.ui.screens.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactItem(
    val id: Long,
    val name: String,
    val phoneNumbers: List<String>,
    val email: String?,
    val photoUri: String?,
    val starred: Boolean
)

data class DuplicateGroup(
    val contacts: List<ContactItem>,
    val matchReason: String
)

data class ContactCleanerUiState(
    val contacts: List<ContactItem> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val issues: List<ContactItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val isMergeAllOperating: Boolean = false,
    val hasPermissions: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val snackbarMessage: String? = null,
    val currentTab: Int = 0,
    val showDeleteConfirm: Boolean = false,
    val showMergeConfirm: Int? = null
)

@HiltViewModel
class ContactCleanerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactCleanerUiState())
    val uiState: StateFlow<ContactCleanerUiState> = _uiState.asStateFlow()

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (_uiState.value.hasPermissions && !_uiState.value.isLoading) {
                loadContacts()
            }
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            ContactsContract.AUTHORITY_URI, true, contentObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    fun checkPermissions(): Boolean {
        val hasRead = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        val hasWrite = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS)
        val granted = hasRead == android.content.pm.PackageManager.PERMISSION_GRANTED
                && hasWrite == android.content.pm.PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(hasPermissions = granted)
        return granted
    }

    fun setPermanentlyDenied(denied: Boolean) {
        _uiState.value = _uiState.value.copy(permanentlyDenied = denied)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun loadContacts() {
        if (!_uiState.value.hasPermissions) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val contacts = withContext(Dispatchers.IO) { fetchContacts() }
            val (duplicates, issues) = withContext(Dispatchers.Default) {
                detectDuplicates(contacts) to findIssues(contacts)
            }
            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                duplicateGroups = duplicates,
                issues = issues,
                isLoading = false
            )
        }
    }

    private fun fetchContacts(): List<ContactItem> {
        val contactsMap = mutableMapOf<Long, ContactItem>()

        val contactUri = ContactsContract.Contacts.CONTENT_URI
        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.PHOTO_URI
        )
        val contactCursor = context.contentResolver.query(
            contactUri, contactProjection, null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
        contactCursor?.use { c ->
            val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val starredIdx = c.getColumnIndex(ContactsContract.Contacts.STARRED)
            val photoIdx = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                contactsMap[id] = ContactItem(
                    id = id,
                    name = c.getString(nameIdx) ?: "",
                    phoneNumbers = emptyList(),
                    email = null,
                    photoUri = c.getString(photoIdx),
                    starred = c.getInt(starredIdx) == 1
                )
            }
        }

        if (contactsMap.isEmpty()) return emptyList()

        val phoneMap = mutableMapOf<Long, MutableList<String>>()
        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        phoneCursor?.use { c ->
            val contactIdIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val contactId = c.getLong(contactIdIdx)
                if (contactId in contactsMap) {
                    val number = c.getString(numberIdx)
                    if (!number.isNullOrBlank()) {
                        phoneMap.getOrPut(contactId) { mutableListOf() }.add(number)
                    }
                }
            }
        }

        val emailMap = mutableMapOf<Long, String?>()
        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null, null, null
        )
        emailCursor?.use { c ->
            val contactIdIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addressIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (c.moveToNext()) {
                val contactId = c.getLong(contactIdIdx)
                if (contactId in contactsMap && !emailMap.containsKey(contactId)) {
                    emailMap[contactId] = c.getString(addressIdx)
                }
            }
        }

        return contactsMap.values.map { contact ->
            contact.copy(
                phoneNumbers = phoneMap[contact.id] ?: emptyList(),
                email = emailMap[contact.id]
            )
        }
    }

    private fun detectDuplicates(contacts: List<ContactItem>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val listedIds = mutableSetOf<Long>()

        val phoneToContacts = mutableMapOf<String, MutableList<ContactItem>>()
        contacts.forEach { contact ->
            contact.phoneNumbers.forEach { phone ->
                val normalized = phone.replace(Regex("[^\\d]"), "").takeLast(10)
                if (normalized.isNotBlank()) {
                    phoneToContacts.getOrPut(normalized) { mutableListOf() }.add(contact)
                }
            }
        }
        phoneToContacts.values.forEach { group ->
            val distinct = group.distinctBy { it.id }
            if (distinct.size > 1) {
                groups.add(DuplicateGroup(distinct, "Same phone number"))
                distinct.forEach { listedIds.add(it.id) }
            }
        }

        val nameToContacts = mutableMapOf<String, MutableList<ContactItem>>()
        contacts.filter { it.name.isNotBlank() }.forEach { contact ->
            val key = contact.name.lowercase().trim()
            nameToContacts.getOrPut(key) { mutableListOf() }.add(contact)
        }
        nameToContacts.values.forEach { group ->
            val distinct = group.distinctBy { it.id }
            if (distinct.size > 1 && distinct.none { it.id in listedIds }) {
                groups.add(DuplicateGroup(distinct, "Same name"))
                distinct.forEach { listedIds.add(it.id) }
            }
        }

        return groups
    }

    private fun findIssues(contacts: List<ContactItem>): List<ContactItem> {
        return contacts.filter {
            it.name.isBlank() || it.phoneNumbers.isEmpty()
        }
    }

    fun setTab(tab: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tab, selectedIds = emptySet())
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds)
                state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSelected)
        }
    }

    fun selectAll() {
        val all = _uiState.value.contacts.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedIds = all)
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun selectAllInGroup(groupIdx: Int) {
        val group = _uiState.value.duplicateGroups.getOrNull(groupIdx) ?: return
        val groupIds = group.contacts.map { it.id }.toSet()
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (groupIds.all { it in current }) current - groupIds else current + groupIds
        )
    }

    fun showDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }
    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun showMergeConfirm(groupIdx: Int) {
        _uiState.value = _uiState.value.copy(showMergeConfirm = groupIdx)
    }
    fun dismissMergeConfirm() {
        _uiState.value = _uiState.value.copy(showMergeConfirm = null)
    }

    fun confirmDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
        performDelete()
    }

    fun confirmMerge() {
        val groupIdx = _uiState.value.showMergeConfirm ?: return
        _uiState.value = _uiState.value.copy(showMergeConfirm = null)
        performMerge(groupIdx)
    }

    fun mergeGroup(groupIdx: Int) {
        showMergeConfirm(groupIdx)
    }

    fun mergeAllDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMergeAllOperating = true)
            val groups = _uiState.value.duplicateGroups
            var mergedCount = 0
            for (group in groups) {
                try {
                    performMergeInternal(group)
                    mergedCount += group.contacts.size
                } catch (_: Exception) { }
            }
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Merged $mergedCount contacts across ${groups.size} groups",
                isMergeAllOperating = false
            )
            loadContacts()
        }
    }

    private suspend fun performMergeInternal(group: DuplicateGroup) {
        val ops = ArrayList<ContentProviderOperation>()
        val masterId = group.contacts.first().id.toString()
        group.contacts.drop(1).forEach { contact ->
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                    .withValue(
                        ContactsContract.AggregationExceptions.TYPE,
                        ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER
                    )
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, masterId)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, contact.id.toString())
                    .build()
            )
        }
        withContext(Dispatchers.IO) {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(ops))
        }
    }

    private fun performMerge(groupIdx: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperating = true)
            val group = _uiState.value.duplicateGroups.getOrNull(groupIdx) ?: return@launch
            try {
                performMergeInternal(group)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Merged ${group.contacts.size} contacts",
                    isOperating = false
                )
                loadContacts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Merge failed: ${e.message}",
                    isOperating = false
                )
            }
        }
    }

    fun deleteSelected() {
        showDeleteConfirm()
    }

    private fun performDelete() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds
            if (ids.isEmpty()) return@launch
            _uiState.value = _uiState.value.copy(isOperating = true)

            try {
                val ops = ids.map { id ->
                    ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                            arrayOf(id.toString())
                        )
                        .build()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(ops))
                }
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Deleted ${ids.size} contacts",
                    selectedIds = emptySet(),
                    isOperating = false
                )
                loadContacts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Delete failed: ${e.message}",
                    isOperating = false
                )
            }
        }
    }

    fun deleteSingleContact(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperating = true)
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(id.toString())
                    )
                }
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Contact deleted",
                    isOperating = false
                )
                loadContacts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Delete failed: ${e.message}",
                    isOperating = false
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
