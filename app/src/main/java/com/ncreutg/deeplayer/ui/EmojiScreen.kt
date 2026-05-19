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

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.ContentInfo
import android.view.OnReceiveContentListener
import android.view.View
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.ncreutg.deeplayer.R
import io.ncreutg.deeplayer.utils.saveLayer
import io.ncreutg.deeplayer.utils.saveValueConfig
import io.ncreutg.deeplayer.utils.uriToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiScreen(
    stickerUri: Uri?,
    onStickerSelected: (Uri) -> Unit,
    onMenuPreferencesClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    var emojiSet by remember { mutableStateOf("👾🔥🚀") }
    var useStickerImage by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Engine mode state
    var isEmojiModeActive by remember { mutableStateOf(false) }

    // Read initial data from file system on screen start
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
            val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")

            if (emojiFile.exists()) emojiSet = emojiFile.readText().trim()
            if (stickerToggleFile.exists()) useStickerImage = stickerToggleFile.readText().trim() == "true"
        }
    }

    // Dynamic configuration loop to prevent multi-engine layout conflicts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val modeFile = File("/data/local/tmp/config_mode.txt")
                val activeMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                val shouldBeActive = activeMode == "emoji"

                if (isEmojiModeActive != shouldBeActive) {
                    isEmojiModeActive = shouldBeActive
                }
                delay(300) // Efficient background polling interval
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.tab_emoji), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialog_title)) },
                            onClick = { menuExpanded = false; onMenuPreferencesClicked() }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
        )

        // CARD 1: Engine mode safety switch controller
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
                Text("Enable Emoji & Sticker Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isEmojiModeActive,
                    onCheckedChange = { active ->
                        isEmojiModeActive = active
                        val targetMode = if (active) "emoji" else "photo"
                        scope.launch(Dispatchers.IO) { saveValueConfig("config_mode.txt", targetMode, context) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARD 2: Rich Media Input Zone (Frees interface container when disabled)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.emoji_input_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmojiModeActive) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isEmojiModeActive) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        factory = { ctx ->
                            EditText(ctx).apply {
                                background = null
                                hint = "Paste stickers or text emojis (Max 10)..."
                                textSize = 16f
                                setTextColor(android.graphics.Color.WHITE)
                                setHintTextColor(android.graphics.Color.GRAY)
                                setText(emojiSet)

                                var activeStickersCount = 0

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val mimeTypes = arrayOf("image/*")
                                    setOnReceiveContentListener(mimeTypes, object : OnReceiveContentListener {
                                        override fun onReceiveContent(view: View, payload: ContentInfo): ContentInfo? {
                                            val clipData = payload.clip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val uri = clipData.getItemAt(0).uri
                                                if (uri != null) {
                                                    if (activeStickersCount >= 10) return null

                                                    scope.launch {
                                                        isProcessing = true
                                                        onStickerSelected(uri)
                                                        val rawBitmap = uriToBitmap(context, uri)

                                                        // Scale adjustment algorithm optimizing resolution sizes based on counts volume
                                                        val scalingFactor = when {
                                                            activeStickersCount >= 7 -> 0.60f
                                                            activeStickersCount >= 4 -> 0.80f
                                                            else -> 1.0f
                                                        }

                                                        val targetSize = (48 * resources.displayMetrics.density * scalingFactor).toInt()
                                                        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, targetSize, targetSize, true)
                                                        if (scaledBitmap != rawBitmap) rawBitmap.recycle()

                                                        val spannable = SpannableString(" ")
                                                        val drawable = android.graphics.drawable.BitmapDrawable(resources, scaledBitmap).apply {
                                                            setBounds(0, 0, targetSize, targetSize)
                                                        }
                                                        spannable.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                                        text.append(spannable)

                                                        saveLayer(context, scaledBitmap, "sticker_$activeStickersCount.png")
                                                        activeStickersCount++

                                                        saveValueConfig("config_emoji_set.txt", activeStickersCount.toString(), context)
                                                        saveValueConfig("config_emoji_use_sticker.txt", "true", context)
                                                        isProcessing = false
                                                    }
                                                    return null
                                                }
                                            }
                                            return payload
                                        }
                                    })
                                }

                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        val txt = s?.toString() ?: ""

                                        // Enforce a strict 10 element boundary constraint matching iOS asset allocations
                                        if (txt.length > 10) {
                                            val trimmed = txt.substring(0, 10)
                                            setText(trimmed)
                                            setSelection(10)
                                            return
                                        }

                                        if (emojiSet != txt) {
                                            emojiSet = txt
                                            scope.launch(Dispatchers.IO) {
                                                saveValueConfig("config_emoji_set.txt", txt, context)
                                                saveValueConfig("config_emoji_use_sticker.txt", "false", context)
                                            }
                                        }
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                })
                            }
                        },
                        update = { editText ->
                            if (editText.text.toString() != emojiSet) {
                                editText.setText(emojiSet)
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.DarkGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Switch mode above to unlock input zone", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))

        // CARD 3: Safe transactional operations controller
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val process = Runtime.getRuntime().exec("su")
                        process.outputStream.use { os ->
                            os.write("pkill -f com.android.systemui\n".toByteArray())
                            os.flush()
                        }
                        process.waitFor()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = isEmojiModeActive
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_apply), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
