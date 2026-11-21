package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SmokedCharcoal
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.util.MediaFormatters
import com.example.ui.util.MediaUriHelper

@Composable
fun MediaThumbnailCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showInfoBadge: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
    showSizeChip: Boolean = false
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { KImagenImageLoader.get(context) }
    val request = remember(item.id, item.localPath) {
        ImageRequest.Builder(context)
            .data(MediaUriHelper.toCoilModel(item.localPath))
            .size(480)
            .apply {
                if (item.mediaType == MediaType.VIDEO) {
                    videoFrameMillis(500L)
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(SmokedCharcoal)
            .border(BorderStroke(0.75.dp, GlassBorderSubtle), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("media_thumb_${item.id}")
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark vignette at bottom for readability of badges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x99070709)
                        )
                    )
                )
        )

        if (item.mediaType == MediaType.VIDEO) {
            Surface(
                color = Color(0xBD09090C),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.75.dp, CrimsonPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = MediaFormatters.formatDuration(item.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        if (item.isBackedUp) {
            Icon(
                imageVector = Icons.Filled.CloudDone,
                contentDescription = "Respaldado",
                tint = SuccessEmerald.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(14.dp)
            )
        }

        if (showInfoBadge && onInfoClick != null) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(36.dp)
                    .testTag("media_info_btn_${item.id}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC14141A))
                        .border(0.75.dp, CrimsonPrimary.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Información del archivo",
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (showSizeChip) {
            Text(
                text = MediaFormatters.formatBytes(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            )
        }
    }
}
