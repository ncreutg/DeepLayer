/*
 * Copyright (C) 2026 ncreutg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */


package io.ncreutg.deeplayer.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ncreutg.deeplayer.R
import io.ncreutg.deeplayer.utils.saveValueConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(bgUri: Uri?, fgUri: Uri?, onMenuPreferencesClicked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    var isPhotoModeActive by remember { mutableStateOf(true) }
    var showFullscreenPreview by remember { mutableStateOf(false) }

    // Live controller protecting layout state conflicts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val modeFile = File("/data/local/tmp/config_mode.txt")
                val activeMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                val shouldBeActive = activeMode == "photo"
                if (isPhotoModeActive != shouldBeActive) isPhotoModeActive = shouldBeActive
                delay(300)
            }
        }
    }

    if (showFullscreenPreview) {
        PhotoPreviewScreen(
            bgUri = bgUri,
            fgUri = fgUri,
            onBackClicked = { showFullscreenPreview = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu Options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dialog_title)) },
                                onClick = {
                                    menuExpanded = false
                                    onMenuPreferencesClicked()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
            )

            // TOP STATUS BANNER COMPRISING THE IMMERSIVE 3D KALMAN PREVIEW ACTION BUTTON
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Photo Engine Monitor",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPhotoModeActive) "Parallax configuration ready" else "Photo configurations frozen",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { showFullscreenPreview = true },
                        enabled = isPhotoModeActive,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch Fullscreen Preview", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ====================================================================
            // ТУТ ПУСТОЕ МЕСТО ДЛЯ ТВОИХ НОВЫХ ФИЧ И ИДЕЙ (ЭКРАН РАСЧИЩЕН!)
            // ====================================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Место свободно под новые настройки...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Complete android core layout restart execution controller pipeline
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val process = Runtime.getRuntime().exec("su")
                            process.outputStream.use { os ->
                                os.write("setprop ctl.restart zygote\n".toByteArray())
                                os.flush()
                            }
                            process.waitFor()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_zygote), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
