package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedGlassHigh
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.util.MediaFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
    item: MediaItemEntity,
    folderName: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xEB111116),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Detalles del archivo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = CrimsonPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (item.mediaType == MediaType.VIDEO) "VIDEO" else "IMAGEN",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            GlassSurfaceCard(
                tint = SmokedGlassHigh,
                accentBorder = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetadataRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "Fecha de creación",
                        value = MediaFormatters.formatFullDateTime(item.dateTakenMillis)
                    )
                    HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                    MetadataRow(
                        icon = Icons.Filled.SdStorage,
                        label = "Peso del archivo",
                        value = "${MediaFormatters.formatBytes(item.sizeBytes)} (${item.sizeBytes} bytes)"
                    )
                    HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                    MetadataRow(
                        icon = Icons.Filled.AspectRatio,
                        label = "Resolución",
                        value = MediaFormatters.formatResolution(item.width, item.height)
                    )
                    if (item.mediaType == MediaType.VIDEO) {
                        HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                        MetadataRow(
                            icon = Icons.Filled.Timer,
                            label = "Duración",
                            value = MediaFormatters.formatDuration(item.durationMs)
                        )
                    }
                    HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                    MetadataRow(
                        icon = Icons.Filled.InsertDriveFile,
                        label = "Formato",
                        value = item.mimeType
                    )
                    HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                    MetadataRow(
                        icon = Icons.Filled.Folder,
                        label = "Carpeta / Álbum",
                        value = folderName
                    )
                    if (!item.cameraModel.isNullOrBlank()) {
                        HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                        val cameraExtras = listOfNotNull(item.cameraModel, item.apertureValue, item.isoValue)
                            .joinToString(" · ")
                        MetadataRow(
                            icon = Icons.Filled.CameraAlt,
                            label = "Cámara / EXIF",
                            value = cameraExtras
                        )
                    }
                    HorizontalDivider(color = GlassBorderLight.copy(alpha = 0.25f))
                    MetadataRow(
                        icon = if (item.isBackedUp) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        label = "Copia de seguridad",
                        value = if (item.isBackedUp && item.lastBackupMillis != null) {
                            "Respaldado (${MediaFormatters.formatFullDateTime(item.lastBackupMillis)})"
                        } else {
                            "Pendiente de respaldo"
                        },
                        valueTint = if (item.isBackedUp) SuccessEmerald else WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueTint: Color = TextPrimary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = CrimsonPrimary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueTint
            )
        }
    }
}
