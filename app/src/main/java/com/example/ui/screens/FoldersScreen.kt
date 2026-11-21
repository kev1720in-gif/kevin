package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.FolderWithStats
import com.example.data.local.MediaItemEntity
import com.example.data.local.ViewDensity
import com.example.ui.components.KImagenImageLoader
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedCharcoal
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass
import com.example.ui.util.MediaFormatters
import com.example.ui.util.MediaUriHelper

@Composable
fun FoldersScreen(
    folders: List<FolderWithStats>,
    columns: Int = 2,
    contentBottomPadding: Dp,
    onFolderClick: (FolderWithStats) -> Unit,
    onCreateFolder: (String) -> Unit,
    onImportSafFolder: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("create_folder_button")
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nueva carpeta", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onImportSafFolder,
                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sync_device_folders_button")
            ) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Escanear carpetas", color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(top = 6.dp, bottom = contentBottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("folders_grid")
        ) {
            items(folders, key = { it.id }) { folder ->
                FolderGlassCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun FolderGlassCard(
    folder: FolderWithStats,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { KImagenImageLoader.get(context) }

    GlassSurfaceCard(
        tint = SmokedGlassSurface,
        accentBorder = folder.isPinned,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .background(SmokedCharcoal),
                contentAlignment = Alignment.Center
            ) {
                if (!folder.coverPath.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(MediaUriHelper.toCoilModel(folder.coverPath))
                            .size(480)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = folder.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xAA08080B))
                                )
                            )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = folder.name,
                        tint = CrimsonPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                if (folder.lastBackedUpMillis != null && folder.itemCount > 0) {
                    Surface(
                        color = Color(0xB309090C),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = "Respaldado",
                            tint = SuccessEmerald,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.itemCount} archivos · ${MediaFormatters.formatBytes(folder.totalSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    folder: FolderWithStats,
    items: List<MediaItemEntity>,
    viewDensity: ViewDensity,
    contentBottomPadding: Dp,
    onBack: () -> Unit,
    onCycleDensity: () -> Unit,
    onImportIntoFolder: (Long) -> Unit,
    onOpenViewer: (Int) -> Unit,
    onShowMediaInfo: (MediaItemEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .frostedGlass(shape = RoundedCornerShape(18.dp), tint = SmokedGlassElevated, accentBorder = true)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("folder_detail_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a carpetas", tint = TextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${items.size} elementos · ${MediaFormatters.formatBytes(items.sumOf { it.sizeBytes })}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onCycleDensity,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.GridView, contentDescription = "Cambiar densidad", tint = TextPrimary)
            }

            IconButton(
                onClick = { onImportIntoFolder(folder.id) },
                modifier = Modifier.size(48.dp).testTag("folder_import_into_button")
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Añadir a carpeta", tint = CrimsonPrimary)
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = contentBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                GlassSurfaceCard(tint = SmokedGlassSurface, accentBorder = true) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Carpeta vacía", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Añade fotos o videos directamente a \"${folder.name}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onImportIntoFolder(folder.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Añadir archivos")
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(viewDensity.columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = contentBottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    MediaThumbnailCard(
                        item = item,
                        showInfoBadge = true,
                        onClick = { onOpenViewer(index) },
                        onInfoClick = { onShowMediaInfo(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xEB14141A),
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = { Text("Nueva carpeta", style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del álbum o carpeta") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = CrimsonPrimary,
                    cursorColor = CrimsonPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("new_folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
