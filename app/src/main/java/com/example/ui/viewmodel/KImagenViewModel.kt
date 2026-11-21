package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BackupRecordEntity
import com.example.data.local.FolderWithStats
import com.example.data.local.MediaFilter
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.ViewDensity
import com.example.data.media.MediaFileHelper
import com.example.data.media.MediaRepository
import com.example.data.media.MediaStoreScanner
import com.example.ui.util.MediaFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimelineSection(
    val key: String,
    val title: String,
    val scrubberLabel: String,
    val dateMillis: Long,
    val items: List<MediaItemEntity>
)

sealed class TimelineGridEntry(val stableKey: String) {
    data class Header(
        val title: String,
        val scrubberLabel: String,
        val dateMillis: Long,
        val count: Int
    ) : TimelineGridEntry("header_${title}_$dateMillis")

    data class MediaEntry(
        val item: MediaItemEntity,
        val flatIndex: Int
    ) : TimelineGridEntry("media_${item.id}")
}

data class UndoAction(
    val itemId: Long,
    val message: String
)

data class PendingDeleteRequest(
    val intentSender: IntentSender,
    val itemIds: List<Long>,
    val isBatch: Boolean
)

class KImagenViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val prefs = UserPreferencesRepository(application)
    private val fileHelper = MediaFileHelper(application)
    private val mediaStoreScanner = MediaStoreScanner(application)
    private val repository = MediaRepository(
        folderDao = database.folderDao(),
        mediaDao = database.mediaDao(),
        backupDao = database.backupDao(),
        fileHelper = fileHelper,
        mediaStoreScanner = mediaStoreScanner,
        contentResolver = application.contentResolver
    )

    private val _hasMediaPermission = MutableStateFlow(false)
    val hasMediaPermission: StateFlow<Boolean> = _hasMediaPermission.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter.asStateFlow()

    private val _selectedDateChip = MutableStateFlow<String?>(null)
    val selectedDateChip: StateFlow<String?> = _selectedDateChip.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    val undoAction: StateFlow<UndoAction?> = _undoAction.asStateFlow()

    private val _pendingDeleteRequest = MutableStateFlow<PendingDeleteRequest?>(null)
    val pendingDeleteRequest: StateFlow<PendingDeleteRequest?> = _pendingDeleteRequest.asStateFlow()

    private var syncJob: Job? = null
    private var observerDebounceJob: Job? = null

    private val mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (_hasMediaPermission.value) {
                observerDebounceJob?.cancel()
                observerDebounceJob = viewModelScope.launch {
                    delay(800L)
                    syncDeviceMedia(showToast = false)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.ensureDefaultFoldersExist()
        }
        runCatching {
            val resolver = application.contentResolver
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver
            )
            resolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            getApplication<Application>().contentResolver.unregisterContentObserver(mediaContentObserver)
        }
    }

    val hasPinConfigured: StateFlow<Boolean> = prefs.hasPinConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val viewDensity: StateFlow<ViewDensity> = prefs.viewDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewDensity.STANDARD)

    val lastBackupMillis: StateFlow<Long?> = prefs.lastBackupMillis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val folders: StateFlow<List<FolderWithStats>> = repository.foldersWithStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveMedia: StateFlow<List<MediaItemEntity>> = repository.activeMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenMedia: StateFlow<List<MediaItemEntity>> = repository.hiddenMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedMedia: StateFlow<List<MediaItemEntity>> = repository.trashedMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backupRecords: StateFlow<List<BackupRecordEntity>> = repository.backupRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableMonthChips: StateFlow<List<String>> = allActiveMedia
        .combine(_mediaFilter) { items, filter ->
            items.filter { matchesTypeFilter(it, filter) }
                .map { MediaFormatters.formatMonthYear(it.dateTakenMillis) }
                .distinct()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredActiveMedia: StateFlow<List<MediaItemEntity>> = combine(
        allActiveMedia,
        folders,
        _searchQuery,
        _mediaFilter,
        _selectedDateChip
    ) { items, folderList, query, filter, dateChip ->
        val folderNames = folderList.associate { it.id to it.name }
        val q = query.trim().lowercase()

        items.filter { item ->
            val typeOk = matchesTypeFilter(item, filter)
            val dateOk = dateChip == null || MediaFormatters.formatMonthYear(item.dateTakenMillis) == dateChip
            val queryOk = if (q.isBlank()) {
                true
            } else {
                val folderName = folderNames[item.folderId]?.lowercase().orEmpty()
                val formattedDate = MediaFormatters.formatFullDateTime(item.dateTakenMillis).lowercase()
                val sizeLabel = MediaFormatters.formatBytes(item.sizeBytes).lowercase()
                item.displayName.lowercase().contains(q) ||
                    folderName.contains(q) ||
                    formattedDate.contains(q) ||
                    sizeLabel.contains(q) ||
                    (item.cameraModel?.lowercase()?.contains(q) == true) ||
                    item.mimeType.lowercase().contains(q)
            }
            typeOk && dateOk && queryOk
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelineSections: StateFlow<List<TimelineSection>> = filteredActiveMedia
        .combine(MutableStateFlow(Unit)) { items, _ ->
            buildTimelineSections(items)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelineGridEntries: StateFlow<List<TimelineGridEntry>> = timelineSections
        .combine(MutableStateFlow(Unit)) { sections, _ ->
            val entries = ArrayList<TimelineGridEntry>(sections.sumOf { it.items.size + 1 })
            var flatIdx = 0
            for (section in sections) {
                entries.add(
                    TimelineGridEntry.Header(
                        title = section.title,
                        scrubberLabel = section.scrubberLabel,
                        dateMillis = section.dateMillis,
                        count = section.items.size
                    )
                )
                for (item in section.items) {
                    entries.add(TimelineGridEntry.MediaEntry(item = item, flatIndex = flatIdx++))
                }
            }
            entries
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeFolderMedia(folderId: Long): Flow<List<MediaItemEntity>> =
        repository.observeFolderMedia(folderId)

    fun onMediaPermissionUpdated(granted: Boolean) {
        val wasGranted = _hasMediaPermission.value
        _hasMediaPermission.value = granted
        if (granted && !wasGranted) {
            syncDeviceMedia(showToast = false)
        }
    }

    fun syncDeviceMedia(showToast: Boolean = false) {
        if (!_hasMediaPermission.value) return
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            runCatching {
                repository.syncDeviceMedia()
            }.onSuccess { addedCount ->
                if (showToast) {
                    _toastMessage.value = if (addedCount > 0) {
                        "$addedCount nuevo(s) archivo(s) detectado(s) en tu teléfono"
                    } else {
                        "Galería sincronizada con tu teléfono"
                    }
                }
            }.onFailure {
                if (showToast) {
                    _toastMessage.value = "Error al leer los archivos del teléfono"
                }
            }
            _isSyncing.value = false
        }
    }

    fun enrichItemMetadata(item: MediaItemEntity, onReady: (MediaItemEntity) -> Unit) {
        viewModelScope.launch {
            val enriched = repository.enrichExifIfNeeded(item)
            onReady(enriched)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMediaFilter(filter: MediaFilter) {
        _mediaFilter.value = filter
    }

    fun selectDateChip(monthYear: String?) {
        _selectedDateChip.value = if (_selectedDateChip.value == monthYear) null else monthYear
    }

    fun cycleViewDensity() {
        val next = when (viewDensity.value) {
            ViewDensity.COMPACT -> ViewDensity.STANDARD
            ViewDensity.STANDARD -> ViewDensity.COZY
            ViewDensity.COZY -> ViewDensity.COMPACT
        }
        viewModelScope.launch { prefs.setViewDensity(next) }
    }

    fun importPickedMedia(uris: List<Uri>, targetFolderId: Long? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val count = repository.importPickedUris(uris, targetFolderId)
            _isBusy.value = false
            _toastMessage.value = if (count > 0) {
                "$count archivo(s) añadido(s) a K-Imagen"
            } else {
                "No se pudieron importar los archivos seleccionados"
            }
        }
    }

    fun importSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val (count, folderName) = repository.importSafFolderTree(treeUri)
            _isBusy.value = false
            _toastMessage.value = if (count > 0) {
                "$count archivo(s) importado(s) desde \"$folderName\""
            } else {
                "No se encontraron imágenes ni videos nuevos en esa carpeta"
            }
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name)
            _toastMessage.value = "Carpeta \"$name\" creada"
        }
    }

    fun moveToTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.moveToTrash(item.id)
            _undoAction.value = UndoAction(
                itemId = item.id,
                message = "\"${item.displayName}\" movido a la papelera"
            )
        }
    }

    fun hideInVault(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.hideInVault(item.id)
            _undoAction.value = UndoAction(
                itemId = item.id,
                message = "\"${item.displayName}\" ocultado en la bóveda"
            )
        }
    }

    fun undoLastAction() {
        val action = _undoAction.value ?: return
        _undoAction.value = null
        viewModelScope.launch {
            repository.restoreToActive(action.itemId)
        }
    }

    fun clearUndoAction() {
        _undoAction.value = null
    }

    fun unhideMedia(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.restoreToActive(item.id)
            _toastMessage.value = "\"${item.displayName}\" restaurado a la galería"
        }
    }

    fun restoreFromTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.restoreToActive(item.id)
            _toastMessage.value = "\"${item.displayName}\" restaurado"
        }
    }

    fun restoreAllFromTrash() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
            _toastMessage.value = "Todos los archivos fueron restaurados"
        }
    }

    fun deletePermanently(item: MediaItemEntity) {
        viewModelScope.launch {
            val plan = repository.requestPermanentDeleteSingle(item)
            if (plan.pendingIntentSender != null && plan.pendingItemIds.isNotEmpty()) {
                _pendingDeleteRequest.value = PendingDeleteRequest(
                    intentSender = plan.pendingIntentSender,
                    itemIds = plan.pendingItemIds,
                    isBatch = false
                )
            } else {
                _toastMessage.value = "Archivo eliminado definitivamente del teléfono"
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val plan = repository.requestEmptyTrash()
            if (plan.pendingIntentSender != null && plan.pendingItemIds.isNotEmpty()) {
                _pendingDeleteRequest.value = PendingDeleteRequest(
                    intentSender = plan.pendingIntentSender,
                    itemIds = plan.pendingItemIds,
                    isBatch = true
                )
            } else {
                _toastMessage.value = "Papelera vaciada del teléfono"
            }
        }
    }

    fun confirmPendingDelete() {
        val pending = _pendingDeleteRequest.value ?: return
        _pendingDeleteRequest.value = null
        viewModelScope.launch {
            repository.finalizePermanentDelete(pending.itemIds)
            _toastMessage.value = if (pending.isBatch) {
                "${pending.itemIds.size} archivo(s) eliminado(s) definitivamente del teléfono"
            } else {
                "Archivo eliminado definitivamente del teléfono"
            }
        }
    }

    fun cancelPendingDelete() {
        _pendingDeleteRequest.value = null
        _toastMessage.value = "Eliminación del teléfono cancelada"
    }

    fun moveItemToFolder(itemId: Long, targetFolderId: Long, folderName: String) {
        viewModelScope.launch {
            repository.moveItemToFolder(itemId, targetFolderId)
            _toastMessage.value = "Movido a \"$folderName\""
        }
    }

    fun setupVaultPin(pin: String) {
        if (pin.length < 4) {
            _pinError.value = "Ingresa al menos 4 dígitos"
            return
        }
        viewModelScope.launch {
            prefs.saveVaultPin(pin)
            _pinError.value = null
            _isVaultUnlocked.value = true
            _toastMessage.value = "Contraseña de bóveda configurada"
        }
    }

    fun unlockVault(pin: String) {
        viewModelScope.launch {
            val valid = prefs.verifyVaultPin(pin)
            if (valid) {
                _pinError.value = null
                _isVaultUnlocked.value = true
            } else {
                _pinError.value = "Contraseña incorrecta"
            }
        }
    }

    fun changeVaultPin(currentPin: String, newPin: String) {
        if (newPin.length < 4) {
            _pinError.value = "La nueva contraseña debe tener al menos 4 dígitos"
            return
        }
        viewModelScope.launch {
            if (prefs.verifyVaultPin(currentPin)) {
                prefs.saveVaultPin(newPin)
                _pinError.value = null
                _toastMessage.value = "Contraseña actualizada"
            } else {
                _pinError.value = "La contraseña actual no coincide"
            }
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _pinError.value = null
    }

    fun clearPinError() {
        _pinError.value = null
    }

    fun exportBackup(destinationUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                repository.exportBackup(destinationUri)
            }.onSuccess { result ->
                prefs.setLastBackupMillis(System.currentTimeMillis())
                _toastMessage.value = "Respaldo creado: ${result.itemCount} archivos (${MediaFormatters.formatBytes(result.totalBytes)})"
            }.onFailure { err ->
                _toastMessage.value = "Error al crear respaldo: ${err.localizedMessage ?: "intenta otra ubicación"}"
            }
            _isBusy.value = false
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                repository.restoreBackup(sourceUri)
            }.onSuccess { result ->
                _toastMessage.value = "Restaurados ${result.restoredCount} archivos en ${result.foldersRestored} carpeta(s)"
            }.onFailure { err ->
                _toastMessage.value = "No se pudo restaurar el respaldo: ${err.localizedMessage ?: "archivo inválido"}"
            }
            _isBusy.value = false
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    private fun matchesTypeFilter(item: MediaItemEntity, filter: MediaFilter): Boolean {
        return when (filter) {
            MediaFilter.ALL -> true
            MediaFilter.IMAGES -> item.mediaType == MediaType.IMAGE
            MediaFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
        }
    }

    private fun buildTimelineSections(items: List<MediaItemEntity>): List<TimelineSection> {
        if (items.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<MediaItemEntity>>()
        for (item in items) {
            val title = MediaFormatters.formatDayHeader(item.dateTakenMillis)
            grouped.getOrPut(title) { mutableListOf() }.add(item)
        }
        return grouped.entries.map { (title, list) ->
            val firstDate = list.first().dateTakenMillis
            TimelineSection(
                key = "${title}_$firstDate",
                title = title,
                scrubberLabel = MediaFormatters.formatShortScrubberLabel(firstDate),
                dateMillis = firstDate,
                items = list
            )
        }
    }
}
