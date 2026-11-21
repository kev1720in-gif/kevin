package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.components.KImagenImageLoader
import com.example.ui.theme.CrimsonGlassTint
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.MediaFormatters
import com.example.ui.util.MediaUriHelper

@Composable
fun TrashScreen(
    trashedItems: List<MediaItemEntity>,
    contentBottomPadding: Dp,
    onRestoreItem: (MediaItemEntity) -> Unit,
    onRestoreAll: () -> Unit,
    onDeletePermanently: (MediaItemEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onShowMediaInfo: (MediaItemEntity) -> Unit
) {
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (trashedItems.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = onRestoreAll,
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedGlassElevated),
                    border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("trash_restore_all_button")
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar todo", color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { showEmptyConfirm = true },
                    border = BorderStroke(1.dp, CrimsonPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("trash_empty_button")
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vaciar", color = CrimsonPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (trashedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPadding + 16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassSurfaceCard(tint = SmokedGlassSurface, accentBorder = true) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Papelera vacía", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Las fotos o videos que elimines deslizando hacia arriba aparecerán aquí para poder restaurarlos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 6.dp, bottom = contentBottomPadding + 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().testTag("trash_list")
            ) {
                items(trashedItems, key = { it.id }) { item ->
                    TrashItemRow(
                        item = item,
                        onRestore = { onRestoreItem(item) },
                        onDeleteForever = { onDeletePermanently(item) },
                        onInfoClick = { onShowMediaInfo(item) }
                    )
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            containerColor = Color(0xEB14141A),
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = { Text("Vaciar papelera", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Se eliminarán definitivamente ${trashedItems.size} archivos de tu teléfono. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Eliminar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TrashItemRow(
    item: MediaItemEntity,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { KImagenImageLoader.get(context) }

    GlassSurfaceCard(
        tint = SmokedGlassSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInfoClick)
            .testTag("trash_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(MediaUriHelper.toCoilModel(item.localPath))
                    .size(240)
                    .apply {
                        if (item.mediaType == MediaType.VIDEO) videoFrameMillis(500L)
                    }
                    .build(),
                imageLoader = imageLoader,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${MediaFormatters.formatBytes(item.sizeBytes)} · Eliminado ${MediaFormatters.formatFullDateTime(item.trashedAtMillis ?: item.dateTakenMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRestore,
                modifier = Modifier.size(48.dp).testTag("trash_restore_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = "Restaurar archivo",
                    tint = SuccessEmerald
                )
            }

            IconButton(
                onClick = onDeleteForever,
                modifier = Modifier.size(48.dp).testTag("trash_delete_forever_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = "Eliminar definitivamente",
                    tint = CrimsonPrimary
                )
            }
        }
    }
}
