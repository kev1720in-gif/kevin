package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.FolderWithStats
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.theme.CrimsonGlassTint
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SmokedGlassHigh
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass
import com.example.ui.util.MediaFormatters
import com.example.ui.util.MediaUriHelper
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaViewerOverlay(
    items: List<MediaItemEntity>,
    initialIndex: Int,
    folders: List<FolderWithStats>,
    isVaultMode: Boolean = false,
    onClose: () -> Unit,
    onSwipeUpDelete: (MediaItemEntity) -> Unit,
    onSwipeDownToggleHide: (MediaItemEntity) -> Unit,
    onMoveToFolder: (MediaItemEntity, Long, String) -> Unit
) {
    BackHandler(onBack = onClose)

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val safeInitial = initialIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { items.size })
    val currentItem = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
    val folderNames = remember(folders) { folders.associate { it.id to it.name } }

    var showChrome by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val dragOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 115.dp.toPx() }
    var thresholdHapticFired by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        dragOffsetY.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack.copy(alpha = 0.97f))
            .testTag("media_viewer_overlay")
    ) {
        // Horizontal pager for photos & videos
        HorizontalPager(
            state = pagerState,
            key = { index -> items[index].id },
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageItem = items[pageIndex]
            val isCurrentPage = pagerState.currentPage == pageIndex

            var imageZoomScale by remember(pageItem.id) { mutableFloatStateOf(1f) }

            val cardAlpha = (1f - (abs(dragOffsetY.value) / (swipeThresholdPx * 2.4f))).coerceIn(0.35f, 1f)
            val cardScale = (1f - (abs(dragOffsetY.value) / (swipeThresholdPx * 8f))).coerceIn(0.86f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, if (isCurrentPage) dragOffsetY.value.roundToInt() else 0) }
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .pointerInput(pageItem.id, imageZoomScale) {
                        if (imageZoomScale <= 1.05f) {
                            detectVerticalDragGestures(
                                onDragStart = { thresholdHapticFired = false },
                                onDragEnd = {
                                    scope.launch {
                                        val offset = dragOffsetY.value
                                        if (offset < -swipeThresholdPx) {
                                            dragOffsetY.animateTo(-900f, spring(stiffness = Spring.StiffnessMedium))
                                            onSwipeUpDelete(pageItem)
                                            dragOffsetY.snapTo(0f)
                                        } else if (offset > swipeThresholdPx) {
                                            dragOffsetY.animateTo(900f, spring(stiffness = Spring.StiffnessMedium))
                                            onSwipeDownToggleHide(pageItem)
                                            dragOffsetY.snapTo(0f)
                                        } else {
                                            dragOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        dragOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    scope.launch {
                                        val next = dragOffsetY.value + dragAmount
                                        dragOffsetY.snapTo(next)
                                        if (!thresholdHapticFired && abs(next) >= swipeThresholdPx) {
                                            thresholdHapticFired = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (thresholdHapticFired && abs(next) < swipeThresholdPx) {
                                            thresholdHapticFired = false
                                        }
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (pageItem.mediaType == MediaType.IMAGE) {
                    ZoomableImagePage(
                        item = pageItem,
                        onZoomChanged = { imageZoomScale = it },
                        onTap = { showChrome = !showChrome }
                    )
                } else {
                    VideoPlayerPage(
                        item = pageItem,
                        isActive = isCurrentPage && !showInfoSheet && !showMoveDialog,
                        showControls = showChrome,
                        onToggleControls = { showChrome = !showChrome }
                    )
                }
            }
        }

        // Swipe-up Delete visual indicator
        val upProgress = ((-dragOffsetY.value) / swipeThresholdPx).coerceIn(0f, 1.3f)
        if (upProgress > 0.12f) {
            Surface(
                color = CrimsonGlassTint.copy(alpha = (0.4f + upProgress * 0.45f).coerceAtMost(0.95f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CrimsonPrimary),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 72.dp)
                    .alpha(upProgress.coerceAtMost(1f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = if (upProgress >= 1f) "Suelta para enviar a Papelera" else "Desliza arriba para eliminar",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Swipe-down Hide visual indicator
        val downProgress = ((dragOffsetY.value) / swipeThresholdPx).coerceIn(0f, 1.3f)
        if (downProgress > 0.12f) {
            Surface(
                color = SmokedGlassHigh.copy(alpha = (0.5f + downProgress * 0.4f).coerceAtMost(0.95f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 110.dp)
                    .alpha(downProgress.coerceAtMost(1f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isVaultMode) Icons.Filled.Visibility else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = CrimsonPrimary
                    )
                    Text(
                        text = if (isVaultMode) {
                            if (downProgress >= 1f) "Suelta para mostrar en Galería" else "Desliza abajo para mostrar"
                        } else {
                            if (downProgress >= 1f) "Suelta para ocultar en Bóveda" else "Desliza abajo para ocultar"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        // Top Frosted Glass Chrome
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .frostedGlass(shape = RoundedCornerShape(22.dp), tint = SmokedGlassSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("viewer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar visor",
                        tint = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${MediaFormatters.formatFullDateTime(currentItem.dateTakenMillis)} · ${MediaFormatters.formatBytes(currentItem.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showInfoSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("viewer_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Ver información",
                        tint = CrimsonPrimary
                    )
                }
            }
        }

        // Bottom Frosted Glass Action Dock + Gesture Guide
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    color = Color(0x990D0D12),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.75.dp, GlassBorderLight.copy(alpha = 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(13.dp))
                            Text("Eliminar", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text("·", color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(13.dp))
                            Text(if (isVaultMode) "Mostrar" else "Ocultar", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(shape = RoundedCornerShape(24.dp), tint = SmokedGlassSurface, accentBorder = true)
                        .padding(vertical = 4.dp)
                ) {
                    ViewerActionButton(
                        icon = if (isVaultMode) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        label = if (isVaultMode) "Mostrar" else "Ocultar",
                        tint = TextPrimary,
                        testTag = "viewer_hide_button",
                        onClick = { onSwipeDownToggleHide(currentItem) }
                    )
                    ViewerActionButton(
                        icon = Icons.Filled.Info,
                        label = "Detalles",
                        tint = TextPrimary,
                        testTag = "viewer_details_button",
                        onClick = { showInfoSheet = true }
                    )
                    if (!isVaultMode) {
                        ViewerActionButton(
                            icon = Icons.AutoMirrored.Filled.DriveFileMove,
                            label = "Mover",
                            tint = TextPrimary,
                            testTag = "viewer_move_button",
                            onClick = { showMoveDialog = true }
                        )
                    }
                    ViewerActionButton(
                        icon = Icons.Filled.DeleteOutline,
                        label = "Eliminar",
                        tint = CrimsonPrimary,
                        testTag = "viewer_delete_button",
                        onClick = { onSwipeUpDelete(currentItem) }
                    )
                }
            }
        }
    }

    if (showInfoSheet) {
        MediaInfoBottomSheet(
            item = currentItem,
            folderName = folderNames[currentItem.folderId] ?: "Cámara",
            onDismiss = { showInfoSheet = false }
        )
    }

    if (showMoveDialog) {
        MoveToFolderDialog(
            folders = folders,
            currentFolderId = currentItem.folderId,
            onSelectFolder = { target ->
                onMoveToFolder(currentItem, target.id, target.name)
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun ZoomableImagePage(
    item: MediaItemEntity,
    onZoomChanged: (Float) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { KImagenImageLoader.get(context) }
    var scale by remember(item.id) { mutableFloatStateOf(1f) }

    LaunchedEffect(scale) {
        onZoomChanged(scale)
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(MediaUriHelper.toCoilModel(item.localPath))
            .build(),
        imageLoader = imageLoader,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.25f
                    },
                    onTap = { onTap() }
                )
            }
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerPage(
    item: MediaItemEntity,
    isActive: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(item.id, item.localPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(MediaUriHelper.toAndroidUri(item.localPath)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var isMuted by remember(item.id) { mutableStateOf(false) }
    var positionMs by remember(item.id) { mutableLongStateOf(0L) }
    var durationMs by remember(item.id) { mutableLongStateOf(item.durationMs.coerceAtLeast(1L)) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = exoPlayer.duration
                if (dur > 0L) durationMs = dur
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isPlaying, isActive) {
        while (isActive) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0L) durationMs = dur
            delay(250L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleControls
            )
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                }
            },
            update = { playerView ->
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center frosted play/pause button
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier
                    .size(68.dp)
                    .frostedGlass(shape = CircleShape, tint = SmokedGlassHigh, accentBorder = true)
                    .testTag("video_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar video" else "Reproducir video",
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Video timeline scrubber above the bottom action dock
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp)
                .padding(bottom = 128.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(shape = RoundedCornerShape(18.dp), tint = SmokedGlassSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = MediaFormatters.formatDuration(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
                Slider(
                    value = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        val seekTarget = (fraction * durationMs).toLong()
                        positionMs = seekTarget
                        exoPlayer.seekTo(seekTarget)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonPrimary,
                        activeTrackColor = CrimsonPrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MediaFormatters.formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                        tint = if (isMuted) CrimsonPrimary else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
