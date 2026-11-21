package com.example.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MediaFilter
import com.example.data.local.MediaItemEntity
import com.example.data.local.ViewDensity
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.theme.CrimsonGlassTint
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassHigh
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass
import com.example.ui.viewmodel.TimelineGridEntry
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun GalleryTimelineScreen(
    hasMediaPermission: Boolean,
    isSyncing: Boolean,
    gridEntries: List<TimelineGridEntry>,
    flatMediaItems: List<MediaItemEntity>,
    monthChips: List<String>,
    selectedMonthChip: String?,
    searchQuery: String,
    mediaFilter: MediaFilter,
    viewDensity: ViewDensity,
    contentBottomPadding: Dp,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onSelectMonthChip: (String?) -> Unit,
    onCycleDensity: () -> Unit,
    onRequestGalleryPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSyncMediaClick: () -> Unit,
    onOpenViewer: (Int) -> Unit,
    onShowMediaInfo: (MediaItemEntity) -> Unit
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val headerPositions by remember(gridEntries) {
        derivedStateOf {
            gridEntries.mapIndexedNotNull { idx, entry ->
                if (entry is TimelineGridEntry.Header) idx to entry else null
            }
        }
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubberFraction by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableIntStateOf(1) }

    val activeScrubberHeader by remember(headerPositions, scrubberFraction) {
        derivedStateOf {
            if (headerPositions.isEmpty()) null
            else {
                val targetIdx = (scrubberFraction * headerPositions.lastIndex)
                    .roundToInt()
                    .coerceIn(0, headerPositions.lastIndex)
                headerPositions[targetIdx]
            }
        }
    }

    val isSearching = searchQuery.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        // Frosted Search + Controls Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .frostedGlass(shape = RoundedCornerShape(16.dp), tint = SmokedGlassSurface)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar foto, fecha, peso o carpeta...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            cursorBrush = SolidColor(CrimsonPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gallery_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Grid Density Toggle Button
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .frostedGlass(shape = RoundedCornerShape(16.dp), tint = SmokedGlassSurface, accentBorder = true)
                        .clickable(onClick = onCycleDensity)
                        .testTag("density_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Cambiar vista de cuadrícula",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${viewDensity.columns}×",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Type Filter Chips + Timeline Month Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(MediaFilter.entries) { filter ->
                    val selected = mediaFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SmokedGlassSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = CrimsonGlassTint,
                            selectedLabelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = GlassBorderLight.copy(alpha = 0.25f),
                            selectedBorderColor = CrimsonPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }

                if (monthChips.isNotEmpty()) {
                    items(monthChips) { monthLabel ->
                        val selected = selectedMonthChip == monthLabel
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectMonthChip(monthLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (selected) CrimsonPrimary else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SmokedGlassSurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = CrimsonGlassTint,
                                selectedLabelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlassBorderLight.copy(alpha = 0.25f),
                                selectedBorderColor = CrimsonPrimary
                            )
                        )
                    }
                }
            }
        }

        // Main Gallery Grid, Permission Prompt, or Syncing State
        when {
            !hasMediaPermission && flatMediaItems.isEmpty() -> {
                GalleryPermissionPromptState(
                    onRequestPermission = onRequestGalleryPermission,
                    onOpenSettings = onOpenAppSettings,
                    bottomPadding = contentBottomPadding
                )
            }

            isSyncing && flatMediaItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = contentBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurfaceCard(tint = SmokedGlassElevated, accentBorder = true) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            CircularProgressIndicator(color = CrimsonPrimary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Cargando galería del teléfono...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Organizando fotos, videos y carpetas por fecha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            flatMediaItems.isEmpty() -> {
                EmptyGalleryState(
                    isFiltered = isSearching || selectedMonthChip != null || mediaFilter != MediaFilter.ALL,
                    onClearFilters = {
                        onSearchQueryChange("")
                        onSelectMonthChip(null)
                        onFilterChange(MediaFilter.ALL)
                    },
                    onSyncMediaClick = onSyncMediaClick,
                    bottomPadding = contentBottomPadding
                )
            }

            else -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(viewDensity.columns),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 26.dp,
                            top = 6.dp,
                            bottom = contentBottomPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gallery_timeline_grid")
                    ) {
                        items(
                            items = gridEntries,
                            key = { it.stableKey },
                            contentType = { entry ->
                                when (entry) {
                                    is TimelineGridEntry.Header -> "header"
                                    is TimelineGridEntry.MediaEntry -> entry.item.mediaType.name
                                }
                            },
                            span = { entry ->
                                when (entry) {
                                    is TimelineGridEntry.Header -> GridItemSpan(maxLineSpan)
                                    is TimelineGridEntry.MediaEntry -> GridItemSpan(1)
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is TimelineGridEntry.Header -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${entry.count}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CrimsonPrimary
                                        )
                                    }
                                }

                                is TimelineGridEntry.MediaEntry -> {
                                    MediaThumbnailCard(
                                        item = entry.item,
                                        showInfoBadge = isSearching || viewDensity == ViewDensity.COZY,
                                        showSizeChip = isSearching,
                                        onClick = { onOpenViewer(entry.flatIndex) },
                                        onInfoClick = { onShowMediaInfo(entry.item) }
                                    )
                                }
                            }
                        }
                    }

                    // Right-edge Interactive Timeline Date Scrubber
                    if (headerPositions.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(0.68f)
                                .width(28.dp)
                                .padding(end = 4.dp)
                                .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
                                .pointerInput(headerPositions) {
                                    detectVerticalDragGestures(
                                        onDragStart = { offset ->
                                            isScrubbing = true
                                            scrubberFraction = (offset.y / railHeightPx.toFloat()).coerceIn(0f, 1f)
                                            val idx = (scrubberFraction * headerPositions.lastIndex)
                                                .roundToInt()
                                                .coerceIn(0, headerPositions.lastIndex)
                                            scope.launch {
                                                gridState.scrollToItem(headerPositions[idx].first)
                                            }
                                        },
                                        onVerticalDrag = { change, _ ->
                                            change.consume()
                                            scrubberFraction = (change.position.y / railHeightPx.toFloat()).coerceIn(0f, 1f)
                                            val idx = (scrubberFraction * headerPositions.lastIndex)
                                                .roundToInt()
                                                .coerceIn(0, headerPositions.lastIndex)
                                            scope.launch {
                                                gridState.scrollToItem(headerPositions[idx].first)
                                            }
                                        },
                                        onDragEnd = { isScrubbing = false },
                                        onDragCancel = { isScrubbing = false }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset {
                                        val thumbY = (scrubberFraction * (railHeightPx - 36).coerceAtLeast(1)).roundToInt()
                                        IntOffset(0, thumbY)
                                    }
                                    .size(width = 10.dp, height = 36.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary)
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isScrubbing && activeScrubberHeader != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 42.dp)
                        ) {
                            activeScrubberHeader?.let { (_, header) ->
                                Surface(
                                    color = SmokedGlassHigh,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CrimsonPrimary)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = header.scrubberLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${header.count} archivos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPermissionPromptState(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomPadding: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomPadding + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurfaceCard(
            tint = SmokedGlassElevated,
            accentBorder = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_glass_banner),
                        contentDescription = "Arte de vidrio polarizado K-Imagen",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC09090B))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Acceso automático a tu galería",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Concede acceso a tus fotos y videos para que K-Imagen lea y organice todas tus carpetas del teléfono automáticamente sin importar manualmente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_gallery_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PermMedia,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permitir fotos y videos", color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir ajustes de permisos", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
    onSyncMediaClick: () -> Unit,
    bottomPadding: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomPadding + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurfaceCard(
            tint = SmokedGlassElevated,
            accentBorder = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = if (isFiltered) "Sin coincidencias" else "No hay fotos ni videos aún",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isFiltered) {
                        "Prueba limpiar los filtros o buscar por otro nombre, fecha o carpeta."
                    } else {
                        "No se detectaron imágenes o videos visibles en tu teléfono en este momento."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isFiltered) {
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Limpiar filtros", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onSyncMediaClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volver a escanear teléfono", color = Color.White)
                    }
                }
            }
        }
    }
}
