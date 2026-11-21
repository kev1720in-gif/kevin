package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.BackupRecordEntity
import com.example.data.local.FolderWithStats
import com.example.data.local.MediaItemEntity
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.MediaFormatters

@Composable
fun BackupScreen(
    activeMedia: List<MediaItemEntity>,
    hiddenMedia: List<MediaItemEntity>,
    folders: List<FolderWithStats>,
    backupRecords: List<BackupRecordEntity>,
    lastBackupMillis: Long?,
    isBusy: Boolean,
    contentBottomPadding: Dp,
    onCreateBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit
) {
    val allManageable = activeMedia + hiddenMedia
    val backedUpCount = allManageable.count { it.isBackedUp }
    val totalCount = allManageable.size
    val totalSizeBytes = allManageable.sumOf { it.sizeBytes }
    val coverage = if (totalCount > 0) backedUpCount.toFloat() / totalCount.toFloat() else 0f

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = contentBottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("backup_screen")
    ) {
        item {
            GlassSurfaceCard(
                tint = SmokedGlassElevated,
                accentBorder = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Copia de seguridad K-Imagen",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (lastBackupMillis != null) {
                                    "Último respaldo: ${MediaFormatters.formatFullDateTime(lastBackupMillis)}"
                                } else {
                                    "Aún no has exportado un respaldo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$backedUpCount de $totalCount archivos respaldados",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = MediaFormatters.formatBytes(totalSizeBytes),
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { coverage },
                        color = if (coverage >= 1f && totalCount > 0) SuccessEmerald else CrimsonPrimary,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCreateBackupClick,
                        enabled = !isBusy && totalCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("backup_export_button")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar copia de seguridad (.zip)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onRestoreBackupClick,
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("backup_restore_button")
                    ) {
                        Icon(Icons.Filled.RestorePage, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar desde archivo (.zip)", color = TextPrimary)
                    }
                }
            }
        }

        if (folders.isNotEmpty()) {
            item {
                Text(
                    text = "Carpetas incluidas",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(folders, key = { "bf_${it.id}" }) { folder ->
                GlassSurfaceCard(tint = SmokedGlassSurface, shape = RoundedCornerShape(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "${folder.itemCount} archivos · ${MediaFormatters.formatBytes(folder.totalSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = if (folder.lastBackedUpMillis != null) "Respaldada" else "Pendiente",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (folder.lastBackedUpMillis != null) SuccessEmerald else CrimsonPrimary
                        )
                    }
                }
            }
        }

        if (backupRecords.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    Text("Historial de respaldos", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }

            items(backupRecords, key = { "br_${it.id}" }) { record ->
                GlassSurfaceCard(tint = SmokedGlassSurface, shape = RoundedCornerShape(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Icon(Icons.Filled.FolderZip, contentDescription = null, tint = CrimsonPrimary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${record.itemCount} archivos · ${MediaFormatters.formatBytes(record.totalBytes)} · ${MediaFormatters.formatFullDateTime(record.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
