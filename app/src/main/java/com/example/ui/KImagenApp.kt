package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.ui.components.MediaInfoBottomSheet
import com.example.ui.components.MediaViewerOverlay
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.GalleryTimelineScreen
import com.example.ui.screens.HiddenVaultScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.AtmosphericGlassBackground
import com.example.ui.theme.CrimsonGlassTint
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.SmokedGlassHigh
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass
import com.example.ui.viewmodel.KImagenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

object KImagenRoutes {
    const val ROUTE_GALLERY = "gallery"
    const val ROUTE_FOLDERS = "folders"
    const val ROUTE_VAULT = "vault"
    const val ROUTE_TRASH = "trash"
    const val ROUTE_BACKUP = "backup"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val kImagenTabs = listOf(
    NavTabItem(KImagenRoutes.ROUTE_GALLERY, "Galería", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    NavTabItem(KImagenRoutes.ROUTE_FOLDERS, "Carpetas", Icons.Filled.Folder, Icons.Outlined.Folder),
    NavTabItem(KImagenRoutes.ROUTE_VAULT, "Ocultos", Icons.Filled.Lock, Icons.Outlined.Lock),
    NavTabItem(KImagenRoutes.ROUTE_TRASH, "Papelera", Icons.Filled.DeleteOutline, Icons.Outlined.DeleteOutline),
    NavTabItem(KImagenRoutes.ROUTE_BACKUP, "Respaldo", Icons.Filled.CloudDone, Icons.Outlined.CloudDone)
)

private enum class ViewerSource {
    TIMELINE,
    FOLDER,
    VAULT
}

private data class ViewerLaunchState(
    val source: ViewerSource,
    val initialIndex: Int
)

private fun requiredGalleryPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun hasGalleryReadAccess(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val fullImages = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val fullVideos = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            val partial = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
            fullImages || fullVideos || partial
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val images = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val videos = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            images || videos
        }
        else -> {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun KImagenApp(
    viewModel: KImagenViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: KImagenRoutes.ROUTE_GALLERY

    val hasMediaPermission by viewModel.hasMediaPermission.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val timelineEntries by viewModel.timelineGridEntries.collectAsStateWithLifecycle()
    val flatActiveMedia by viewModel.filteredActiveMedia.collectAsStateWithLifecycle()
    val allActiveMedia by viewModel.allActiveMedia.collectAsStateWithLifecycle()
    val monthChips by viewModel.availableMonthChips.collectAsStateWithLifecycle()
    val selectedMonthChip by viewModel.selectedDateChip.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
    val viewDensity by viewModel.viewDensity.collectAsStateWithLifecycle()

    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val hiddenMedia by viewModel.hiddenMedia.collectAsStateWithLifecycle()
    val trashedMedia by viewModel.trashedMedia.collectAsStateWithLifecycle()
    val backupRecords by viewModel.backupRecords.collectAsStateWithLifecycle()
    val lastBackupMillis by viewModel.lastBackupMillis.collectAsStateWithLifecycle()

    val hasPinConfigured by viewModel.hasPinConfigured.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val undoAction by viewModel.undoAction.collectAsStateWithLifecycle()
    val pendingDeleteRequest by viewModel.pendingDeleteRequest.collectAsStateWithLifecycle()

    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var pendingImportFolderId by remember { mutableStateOf<Long?>(null) }
    var viewerLaunch by remember { mutableStateOf<ViewerLaunchState?>(null) }
    var infoSheetItem by remember { mutableStateOf<MediaItemEntity?>(null) }
    var askedPermissionOnce by remember { mutableStateOf(false) }

    val selectedFolder = remember(folders, selectedFolderId) {
        folders.firstOrNull { it.id == selectedFolderId }
    }
    val selectedFolderMediaFlow = remember(selectedFolderId) {
        selectedFolderId?.let { viewModel.observeFolderMedia(it) }
    }
    val selectedFolderMedia by (selectedFolderMediaFlow?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    // Runtime MediaStore Gallery Permission Launcher
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = hasGalleryReadAccess(context)
        viewModel.onMediaPermissionUpdated(granted)
    }

    // OS confirmation launcher for permanent MediaStore file deletion (Android 10/11+)
    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.confirmPendingDelete()
        } else {
            viewModel.cancelPendingDelete()
        }
    }

    LaunchedEffect(pendingDeleteRequest) {
        pendingDeleteRequest?.let { req ->
            runCatching {
                deleteConfirmLauncher.launch(
                    IntentSenderRequest.Builder(req.intentSender).build()
                )
            }.onFailure {
                viewModel.cancelPendingDelete()
            }
        }
    }

    // Check permission on startup and whenever returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasGalleryReadAccess(context)
                viewModel.onMediaPermissionUpdated(granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val granted = hasGalleryReadAccess(context)
        viewModel.onMediaPermissionUpdated(granted)
        if (!granted && !askedPermissionOnce) {
            askedPermissionOnce = true
            galleryPermissionLauncher.launch(requiredGalleryPermissions())
        }
    }

    // Optional Photo Picker (for adding extra files into specific folders)
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPickedMedia(uris, pendingImportFolderId)
        }
        pendingImportFolderId = null
    }

    // Optional SAF Folder Picker
    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importSafFolder(uri)
        }
    }

    // SAF Backup ZIP Export
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    // SAF Backup ZIP Restore
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3200L)
            viewModel.consumeToast()
        }
    }
    LaunchedEffect(undoAction) {
        if (undoAction != null) {
            delay(4500L)
            viewModel.clearUndoAction()
        }
    }

    val openInfoWithLazyExif: (MediaItemEntity) -> Unit = { item ->
        infoSheetItem = item
        viewModel.enrichItemMetadata(item) { enriched ->
            if (infoSheetItem?.id == enriched.id) {
                infoSheetItem = enriched
            }
        }
    }

    val openAppSettings: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    val folderNames = remember(folders) { folders.associate { it.id to it.name } }

    AtmosphericGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp
            val bottomContentPad = if (isWideScreen) 16.dp else 94.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen) {
                    GlassSideNavigationRail(
                        currentRoute = currentRoute,
                        trashCount = trashedMedia.size,
                        onNavigate = { route ->
                            selectedFolderId = null
                            navController.navigate(route) {
                                popUpTo(KImagenRoutes.ROUTE_GALLERY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    KImagenTopHeader(
                        isBusy = isBusy || isSyncing,
                        hasPermission = hasMediaPermission,
                        onSyncOrGrantClick = {
                            if (hasMediaPermission) {
                                viewModel.syncDeviceMedia(showToast = true)
                            } else {
                                galleryPermissionLauncher.launch(requiredGalleryPermissions())
                            }
                        }
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NavHost(
                            navController = navController,
                            startDestination = KImagenRoutes.ROUTE_GALLERY,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(KImagenRoutes.ROUTE_GALLERY) {
                                GalleryTimelineScreen(
                                    hasMediaPermission = hasMediaPermission,
                                    isSyncing = isSyncing,
                                    gridEntries = timelineEntries,
                                    flatMediaItems = flatActiveMedia,
                                    monthChips = monthChips,
                                    selectedMonthChip = selectedMonthChip,
                                    searchQuery = searchQuery,
                                    mediaFilter = mediaFilter,
                                    viewDensity = viewDensity,
                                    contentBottomPadding = bottomContentPad,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onFilterChange = viewModel::setMediaFilter,
                                    onSelectMonthChip = viewModel::selectDateChip,
                                    onCycleDensity = viewModel::cycleViewDensity,
                                    onRequestGalleryPermission = {
                                        galleryPermissionLauncher.launch(requiredGalleryPermissions())
                                    },
                                    onOpenAppSettings = openAppSettings,
                                    onSyncMediaClick = { viewModel.syncDeviceMedia(showToast = true) },
                                    onOpenViewer = { flatIndex ->
                                        viewerLaunch = ViewerLaunchState(ViewerSource.TIMELINE, flatIndex)
                                    },
                                    onShowMediaInfo = openInfoWithLazyExif
                                )
                            }

                            composable(KImagenRoutes.ROUTE_FOLDERS) {
                                if (selectedFolder != null) {
                                    FolderDetailScreen(
                                        folder = selectedFolder,
                                        items = selectedFolderMedia,
                                        viewDensity = viewDensity,
                                        contentBottomPadding = bottomContentPad,
                                        onBack = { selectedFolderId = null },
                                        onCycleDensity = viewModel::cycleViewDensity,
                                        onImportIntoFolder = { folderId ->
                                            pendingImportFolderId = folderId
                                            pickMediaLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        onOpenViewer = { idx ->
                                            viewerLaunch = ViewerLaunchState(ViewerSource.FOLDER, idx)
                                        },
                                        onShowMediaInfo = openInfoWithLazyExif
                                    )
                                } else {
                                    FoldersScreen(
                                        folders = folders,
                                        columns = if (isWideScreen) 3 else 2,
                                        contentBottomPadding = bottomContentPad,
                                        onFolderClick = { folder -> selectedFolderId = folder.id },
                                        onCreateFolder = viewModel::createFolder,
                                        onImportSafFolder = {
                                            if (hasMediaPermission) {
                                                viewModel.syncDeviceMedia(showToast = true)
                                            } else {
                                                galleryPermissionLauncher.launch(requiredGalleryPermissions())
                                            }
                                        }
                                    )
                                }
                            }

                            composable(KImagenRoutes.ROUTE_VAULT) {
                                HiddenVaultScreen(
                                    hasPinConfigured = hasPinConfigured,
                                    isUnlocked = isVaultUnlocked,
                                    pinError = pinError,
                                    hiddenItems = hiddenMedia,
                                    viewDensity = viewDensity,
                                    contentBottomPadding = bottomContentPad,
                                    onSetupPin = viewModel::setupVaultPin,
                                    onUnlock = viewModel::unlockVault,
                                    onLock = viewModel::lockVault,
                                    onChangePin = viewModel::changeVaultPin,
                                    onClearPinError = viewModel::clearPinError,
                                    onOpenViewer = { idx ->
                                        viewerLaunch = ViewerLaunchState(ViewerSource.VAULT, idx)
                                    },
                                    onUnhideItem = viewModel::unhideMedia,
                                    onShowMediaInfo = openInfoWithLazyExif
                                )
                            }

                            composable(KImagenRoutes.ROUTE_TRASH) {
                                TrashScreen(
                                    trashedItems = trashedMedia,
                                    contentBottomPadding = bottomContentPad,
                                    onRestoreItem = viewModel::restoreFromTrash,
                                    onRestoreAll = viewModel::restoreAllFromTrash,
                                    onDeletePermanently = viewModel::deletePermanently,
                                    onEmptyTrash = viewModel::emptyTrash,
                                    onShowMediaInfo = openInfoWithLazyExif
                                )
                            }

                            composable(KImagenRoutes.ROUTE_BACKUP) {
                                BackupScreen(
                                    activeMedia = allActiveMedia,
                                    hiddenMedia = hiddenMedia,
                                    folders = folders,
                                    backupRecords = backupRecords,
                                    lastBackupMillis = lastBackupMillis,
                                    isBusy = isBusy,
                                    contentBottomPadding = bottomContentPad,
                                    onCreateBackupClick = {
                                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                        exportBackupLauncher.launch("KImagen_Respaldo_$stamp.zip")
                                    },
                                    onRestoreBackupClick = {
                                        restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!isWideScreen) {
                GlassBottomNavigationBar(
                    currentRoute = currentRoute,
                    trashCount = trashedMedia.size,
                    onNavigate = { route ->
                        selectedFolderId = null
                        navController.navigate(route) {
                            popUpTo(KImagenRoutes.ROUTE_GALLERY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            val bannerModifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = if (isWideScreen) 20.dp else 88.dp)

            AnimatedVisibility(
                visible = undoAction != null || toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = bannerModifier
            ) {
                Surface(
                    color = SmokedGlassHigh,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = undoAction?.message ?: toastMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (undoAction != null) {
                            TextButton(onClick = viewModel::undoLastAction) {
                                Text(
                                    text = "DESHACER",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CrimsonPrimary
                                )
                            }
                        }
                    }
                }
            }

            viewerLaunch?.let { launch ->
                val viewerItems = when (launch.source) {
                    ViewerSource.TIMELINE -> flatActiveMedia
                    ViewerSource.FOLDER -> selectedFolderMedia
                    ViewerSource.VAULT -> hiddenMedia
                }
                if (viewerItems.isEmpty()) {
                    viewerLaunch = null
                } else {
                    MediaViewerOverlay(
                        items = viewerItems,
                        initialIndex = launch.initialIndex,
                        folders = folders,
                        isVaultMode = launch.source == ViewerSource.VAULT,
                        onClose = { viewerLaunch = null },
                        onSwipeUpDelete = { item -> viewModel.moveToTrash(item) },
                        onSwipeDownToggleHide = { item ->
                            if (launch.source == ViewerSource.VAULT) {
                                viewModel.unhideMedia(item)
                            } else {
                                viewModel.hideInVault(item)
                            }
                        },
                        onMoveToFolder = { item, folderId, folderName ->
                            viewModel.moveItemToFolder(item.id, folderId, folderName)
                        }
                    )
                }
            }

            infoSheetItem?.let { item ->
                MediaInfoBottomSheet(
                    item = item,
                    folderName = folderNames[item.folderId] ?: "Cámara",
                    onDismiss = { infoSheetItem = null }
                )
            }
        }
    }
}

@Composable
private fun KImagenTopHeader(
    isBusy: Boolean,
    hasPermission: Boolean,
    onSyncOrGrantClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.kimagen_app_icon),
                contentDescription = "Logo K-Imagen",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "K-IMAGEN",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CrimsonPrimary)
                    )
                }
                Text(
                    text = "Gestor Polarizado",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(44.dp)
                    .frostedGlass(shape = RoundedCornerShape(16.dp), tint = CrimsonGlassTint, accentBorder = true)
                    .clickable(onClick = onSyncOrGrantClick)
                    .testTag("top_sync_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = if (hasPermission) "Sincronizar galería" else "Conceder permisos",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (hasPermission) "Sincronizar" else "Permitir",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassBottomNavigationBar(
    currentRoute: String,
    trashCount: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .frostedGlass(shape = RoundedCornerShape(26.dp), tint = SmokedGlassSurface, accentBorder = false)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("bottom_nav_bar")
    ) {
        kImagenTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            val pillColor = if (selected) CrimsonGlassTint else Color.Transparent

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(pillColor)
                    .clickable { onNavigate(tab.route) }
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .testTag("nav_tab_${tab.route}")
            ) {
                Box {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) CrimsonPrimary else TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    if (tab.route == KImagenRoutes.ROUTE_TRASH && trashCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassSideNavigationRail(
    currentRoute: String,
    trashCount: Int,
    onNavigate: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxHeight()
            .width(88.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .frostedGlass(shape = RoundedCornerShape(26.dp), tint = SmokedGlassSurface)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        kImagenTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) CrimsonGlassTint else Color.Transparent)
                    .clickable { onNavigate(tab.route) }
                    .testTag("rail_tab_${tab.route}")
            ) {
                Box {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) CrimsonPrimary else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    if (tab.route == KImagenRoutes.ROUTE_TRASH && trashCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) TextPrimary else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
