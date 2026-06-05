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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.SpannableString
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onStickerSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    var emojiSet by remember { mutableStateOf("👾🔥🚀") }
    var useStickerImage by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isEmojiModeActive by remember { mutableStateOf(false) }

    var selectedLayout by remember { mutableStateOf("dense_grid") }

    // FIX: Declare independent state for the hardware-accelerated parallax engine toggle here
    var isParallaxEnabled by remember { mutableStateOf(false) }

    var colorRed by remember { mutableStateOf(18f) }
    var colorGreen by remember { mutableStateOf(18f) }
    var colorBlue by remember { mutableStateOf(20f) }

    var showFullscreenPreview by remember { mutableStateOf(false) }

    // FIX: Lift sticker counter to persistent Compose state to prevent reset during recompositions
    var stickerCountState by remember { mutableStateOf(0) }

    val computedColor = remember(colorRed, colorGreen, colorBlue) {
        Color(colorRed.toInt(), colorGreen.toInt(), colorBlue.toInt(), 255)
    }

    // Load initial system I/O configurations from root directory on screen launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
            val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")
            val bgFile = File("/data/local/tmp/config_emoji_bg.txt")
            val layoutTypeFile = File("/data/local/tmp/config_emoji_layout.txt")
            val parallaxFile = File("/data/local/tmp/config_emoji_parallax.txt")

            // FIX: Determine mode first to prevent numeric string injection into the EditText input layout
            val isStickerModeActive = stickerToggleFile.exists() && stickerToggleFile.readText().trim() == "true"
            useStickerImage = isStickerModeActive

            if (emojiFile.exists()) {
                val cachedSet = emojiFile.readText().trim()
                if (isStickerModeActive) {
                    // FIX: If in sticker mode, treat the text value as a number for internal state tracking only
                    stickerCountState = cachedSet.toIntOrNull() ?: 0
                    emojiSet = "" // FIX: Keep the input field clean instead of displaying raw counter digits like "10"
                } else {
                    // Standard emoji mode behavior
                    emojiSet = cachedSet
                    stickerCountState = 0
                }
            }

            if (layoutTypeFile.exists()) selectedLayout = layoutTypeFile.readText().trim()
            if (parallaxFile.exists()) isParallaxEnabled = parallaxFile.readText().trim() == "true"

            if (bgFile.exists()) {
                try {
                    val parsedInt = android.graphics.Color.parseColor(bgFile.readText().trim())
                    colorRed = android.graphics.Color.red(parsedInt).toFloat()
                    colorGreen = android.graphics.Color.green(parsedInt).toFloat()
                    colorBlue = android.graphics.Color.blue(parsedInt).toFloat()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val modeFile = File("/data/local/tmp/config_mode.txt")
                val activeMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                val shouldBeActive = activeMode == "emoji"
                if (isEmojiModeActive != shouldBeActive) isEmojiModeActive = shouldBeActive
                delay(300)
            }
        }
    }

    if (showFullscreenPreview) {
        EmojiPreviewScreen(
            stickerUri = stickerUri,
            onBackClicked = { showFullscreenPreview = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
            )

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
                        Text(text = stringResource(R.string.enable_Emoji_Stiker), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showFullscreenPreview = true },
                            enabled = isEmojiModeActive,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "View Live Grid Preview", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

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
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        val layoutsList = listOf(
                            "dense_grid" to stringResource(R.string.dense),
                            "mosaic" to stringResource(R.string.mosaic),
                            "matrix" to stringResource(R.string.matrix),
                            "chaos" to stringResource(R.string.chaos),
                            "grid" to stringResource(R.string.grid),
                            "spiral" to stringResource(R.string.spiral),
                            "rhombus" to stringResource(R.string.rhombus),
                            "wave" to stringResource(R.string.wave)
                        )

                        Text(
                            text = stringResource(R.string.layout_style),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            items(layoutsList) { (key, label) ->
                                FilterChip(
                                    selected = selectedLayout == key,
                                    onClick = {
                                        selectedLayout = key
                                        scope.launch(Dispatchers.IO) {
                                            saveValueConfig("config_emoji_layout.txt", key, context)
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    if (isEmojiModeActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.emoji_input_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isEmojiModeActive) MaterialTheme.colorScheme.primary else Color.Gray
                            )

                            if (isEmojiModeActive && stickerCountState > 0) {
                                TextButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            clearSavedStickers(context)
                                            // Switch back to Main dispatcher to securely mutate state variables on the UI thread
                                            withContext(Dispatchers.Main) {
                                                stickerCountState = 0
                                                emojiSet = ""
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear all cached stickers", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.clear_stickers), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
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

                                    setText(if (emojiSet.isEmpty()) " " else emojiSet)

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val mimeTypes = arrayOf("image/*")
                                        setOnReceiveContentListener(mimeTypes, object : OnReceiveContentListener {
                                            override fun onReceiveContent(view: View, payload: ContentInfo): ContentInfo? {
                                                val clipData = payload.clip
                                                if (clipData != null && clipData.itemCount > 0) {
                                                    val uri = clipData.getItemAt(0).uri
                                                    if (uri != null) {
                                                        val targetIndex = stickerCountState
                                                        if (targetIndex >= 10) return null

                                                        val nextIndex = targetIndex + 1
                                                        stickerCountState = nextIndex

                                                        scope.launch {
                                                            isProcessing = true
                                                            onStickerSelected(uri)
                                                            val rawBitmap = uriToBitmap(context, uri)

                                                            val scalingFactor = when {
                                                                targetIndex >= 7 -> 0.60f
                                                                targetIndex >= 4 -> 0.80f
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
                                                            saveLayer(context, scaledBitmap, "sticker_$targetIndex.png")
                                                            saveValueConfig("config_emoji_set.txt", nextIndex.toString(), context)
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
                                        private var selfChange = false

                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                            if (isProcessing || selfChange) return

                                            val txt = s?.toString() ?: ""

                                            if (txt.isEmpty()) {
                                                selfChange = true
                                                setText(" ")
                                                setSelection(1)
                                                selfChange = false
                                                return
                                            }

                                            val it = java.text.BreakIterator.getCharacterInstance()
                                            it.setText(txt)
                                            var emojiCount = 0
                                            var cutIndex = 0
                                            while (it.next() != java.text.BreakIterator.DONE) {
                                                emojiCount++
                                                if (emojiCount == 10) {
                                                    cutIndex = it.current()
                                                }
                                            }

                                            if (emojiCount > 10 && cutIndex > 0) {
                                                selfChange = true
                                                val trimmed = txt.substring(0, cutIndex)
                                                setText(trimmed)
                                                setSelection(trimmed.length)
                                                selfChange = false
                                                return
                                            }

                                            if (emojiSet != txt) {
                                                emojiSet = txt
                                                if (txt.toIntOrNull() == null) {
                                                    stickerCountState = 0
                                                    scope.launch(Dispatchers.IO) {
                                                        val cleanText = txt.trim()
                                                        saveValueConfig("config_emoji_set.txt", cleanText, context)
                                                        saveValueConfig("config_emoji_use_sticker.txt", "false", context)
                                                    }
                                                }
                                            }
                                        }
                                        override fun afterTextChanged(s: Editable?) {}
                                    })
                                }
                            },
                            update = { editText ->
                                if (!editText.isFocused && editText.text.toString() != emojiSet) {
                                    editText.setText(emojiSet)
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.DarkGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Switch mode above to unlock input zone", color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.emoji_bg_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEmojiModeActive) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    ColorSlider(label = "R", value = colorRed, enabled = isEmojiModeActive, onValueChange = { colorRed = it; scope.launch(Dispatchers.IO) { saveColorConfig(computedColor, context) } })
                    ColorSlider(label = "G", value = colorGreen, enabled = isEmojiModeActive, onValueChange = { colorGreen = it; scope.launch(Dispatchers.IO) { saveColorConfig(computedColor, context) } })
                    ColorSlider(label = "B", value = colorBlue, enabled = isEmojiModeActive, onValueChange = { colorBlue = it; scope.launch(Dispatchers.IO) { saveColorConfig(computedColor, context) } })
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val process = Runtime.getRuntime().exec("su")
                            process.outputStream.use { os ->
                                os.write("chmod 666 /data/local/tmp/config_emoji_*\n".toByteArray())
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
}

@Composable
fun ColorSlider(label: String, value: Float, enabled: Boolean, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp), color = if (enabled) Color.Unspecified else Color.Gray)
        Slider(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text("${value.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp), color = if (enabled) Color.Unspecified else Color.Gray, textAlign = TextAlign.End)
    }
}

suspend fun saveColorConfig(color: Color, context: Context) {
    val hexString = String.format("#%08X", color.toArgb())
    saveValueConfig("config_emoji_bg.txt", hexString, context)
}

private fun clearSavedStickers(context: Context) {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { os ->
            // Delete all sticker images using wildcard matching
            os.write("rm -f /data/local/tmp/sticker_*\n".toByteArray())
            // Reset config parameters to safe baseline states
            os.write("echo '0' > /data/local/tmp/config_emoji_set.txt\n".toByteArray())
            os.write("echo 'false' > /data/local/tmp/config_emoji_use_sticker.txt\n".toByteArray())
            os.write("chmod 666 /data/local/tmp/config_*\n".toByteArray())
            os.flush()
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
