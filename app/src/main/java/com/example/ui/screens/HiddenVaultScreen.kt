package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.data.local.ViewDensity
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.theme.CrimsonGlassTint
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass

@Composable
fun HiddenVaultScreen(
    hasPinConfigured: Boolean,
    isUnlocked: Boolean,
    pinError: String?,
    hiddenItems: List<MediaItemEntity>,
    viewDensity: ViewDensity,
    contentBottomPadding: Dp,
    onSetupPin: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onLock: () -> Unit,
    onChangePin: (String, String) -> Unit,
    onClearPinError: () -> Unit,
    onOpenViewer: (Int) -> Unit,
    onUnhideItem: (MediaItemEntity) -> Unit,
    onShowMediaInfo: (MediaItemEntity) -> Unit
) {
    if (!isUnlocked) {
        VaultPinGate(
            hasPinConfigured = hasPinConfigured,
            pinError = pinError,
            contentBottomPadding = contentBottomPadding,
            onSetupPin = onSetupPin,
            onUnlock = onUnlock,
            onClearError = onClearPinError
        )
    } else {
        UnlockedVaultContent(
            hiddenItems = hiddenItems,
            viewDensity = viewDensity,
            contentBottomPadding = contentBottomPadding,
            onLock = onLock,
            onChangePin = onChangePin,
            onOpenViewer = onOpenViewer,
            onUnhideItem = onUnhideItem,
            onShowMediaInfo = onShowMediaInfo
        )
    }
}

@Composable
private fun VaultPinGate(
    hasPinConfigured: Boolean,
    pinError: String?,
    contentBottomPadding: Dp,
    onSetupPin: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onClearError: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var firstStepPin by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = localError ?: pinError
    val subtitle = when {
        !hasPinConfigured && firstStepPin == null -> "Crea un PIN de 4 a 6 dígitos para proteger tus fotos y videos ocultos"
        !hasPinConfigured && firstStepPin != null -> "Confirma tu nuevo PIN para activar la Bóveda"
        else -> "Ingresa tu PIN de acceso para ver los archivos ocultos"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = contentBottomPadding + 16.dp)
            .testTag("vault_pin_gate")
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
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_shield),
                        contentDescription = "Escudo de bóveda privada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Bóveda Privada",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(if (filled) 15.dp else 13.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) CrimsonPrimary else Color.White.copy(alpha = 0.18f)
                                )
                        )
                    }
                }

                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Frosted Numeric Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { digit ->
                                KeypadButton(
                                    label = digit,
                                    onClick = {
                                        localError = null
                                        onClearError()
                                        if (enteredPin.length < 6) {
                                            enteredPin += digit
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Backspace
                        Surface(
                            color = Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(62.dp)
                                .frostedGlass(shape = CircleShape, tint = SmokedGlassSurface)
                                .clickable {
                                    localError = null
                                    onClearError()
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Borrar dígito",
                                    tint = TextSecondary
                                )
                            }
                        }

                        KeypadButton(
                            label = "0",
                            onClick = {
                                localError = null
                                onClearError()
                                if (enteredPin.length < 6) {
                                    enteredPin += "0"
                                }
                            }
                        )

                        // Submit
                        Surface(
                            color = Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(62.dp)
                                .frostedGlass(
                                    shape = CircleShape,
                                    tint = if (enteredPin.length >= 4) CrimsonGlassTint else SmokedGlassSurface,
                                    accentBorder = enteredPin.length >= 4
                                )
                                .clickable {
                                    if (enteredPin.length < 4) {
                                        localError = "Ingresa al menos 4 dígitos"
                                        return@clickable
                                    }
                                    if (!hasPinConfigured) {
                                        if (firstStepPin == null) {
                                            firstStepPin = enteredPin
                                            enteredPin = ""
                                        } else if (firstStepPin == enteredPin) {
                                            onSetupPin(enteredPin)
                                            enteredPin = ""
                                            firstStepPin = null
                                        } else {
                                            localError = "Los códigos no coinciden, intenta de nuevo"
                                            enteredPin = ""
                                            firstStepPin = null
                                        }
                                    } else {
                                        onUnlock(enteredPin)
                                        enteredPin = ""
                                    }
                                }
                                .testTag("vault_submit_pin")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Confirmar PIN",
                                    tint = if (enteredPin.length >= 4) CrimsonPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(62.dp)
            .frostedGlass(shape = CircleShape, tint = SmokedGlassSurface)
            .clickable(onClick = onClick)
            .testTag("vault_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun UnlockedVaultContent(
    hiddenItems: List<MediaItemEntity>,
    viewDensity: ViewDensity,
    contentBottomPadding: Dp,
    onLock: () -> Unit,
    onChangePin: (String, String) -> Unit,
    onOpenViewer: (Int) -> Unit,
    onUnhideItem: (MediaItemEntity) -> Unit,
    onShowMediaInfo: (MediaItemEntity) -> Unit
) {
    var showChangePinDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .frostedGlass(shape = RoundedCornerShape(18.dp), tint = SmokedGlassElevated, accentBorder = true)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Filled.LockOpen, contentDescription = null, tint = CrimsonPrimary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Bóveda desbloqueada", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("${hiddenItems.size} archivos protegidos", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            IconButton(
                onClick = { showChangePinDialog = true },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Filled.Key, contentDescription = "Cambiar contraseña", tint = TextPrimary)
            }
            Button(
                onClick = onLock,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonGlassTint),
                border = BorderStroke(1.dp, CrimsonPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp).testTag("vault_lock_button")
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bloquear", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }
        }

        if (hiddenItems.isEmpty()) {
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
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sin archivos ocultos", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Al ver una foto o video en el visor, desliza hacia abajo para ocultarlo aquí con contraseña.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(viewDensity.columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = contentBottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().testTag("vault_hidden_grid")
            ) {
                itemsIndexed(hiddenItems, key = { _, item -> item.id }) { index, item ->
                    Box {
                        MediaThumbnailCard(
                            item = item,
                            showInfoBadge = true,
                            onClick = { onOpenViewer(index) },
                            onInfoClick = { onShowMediaInfo(item) }
                        )
                        Surface(
                            color = Color(0xCC111116),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.75.dp, GlassBorderLight.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clickable { onUnhideItem(item) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = "Mostrar en galería",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("Mostrar", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onConfirm = { current, next ->
                onChangePin(current, next)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }
}

@Composable
private fun ChangePinDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xEB14141A),
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = { Text("Cambiar PIN de bóveda", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) currentPin = it },
                    label = { Text("PIN actual") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("Nuevo PIN (4 a 6 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPin, newPin) },
                enabled = currentPin.length >= 4 && newPin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
