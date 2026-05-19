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
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPreviewScreen(
    stickerUri: Uri?,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    var emojiSet by remember { mutableStateOf("👾🔥🚀") }
    var useStickerImage by remember { mutableStateOf(false) }
    var emojiBgColor by remember { mutableStateOf("#121214") }
    var selectedLayout by remember { mutableStateOf("dense_grid") }

    // Array to hold all decoded bitmaps from internal storage
    val cachedStickerBitmaps = remember { mutableStateListOf<Bitmap>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
            val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")
            val bgFile = File("/data/local/tmp/config_emoji_bg.txt")
            val layoutTypeFile = File("/data/local/tmp/config_emoji_layout.txt")

            if (emojiFile.exists()) emojiSet = emojiFile.readText().trim()
            if (stickerToggleFile.exists()) useStickerImage = stickerToggleFile.readText().trim() == "true"
            if (bgFile.exists()) emojiBgColor = bgFile.readText().trim()
            if (layoutTypeFile.exists()) selectedLayout = layoutTypeFile.readText().trim()

            // Read all sequentially stored stickers from disk to mirror lockscreen behavior
            if (useStickerImage) {
                cachedStickerBitmaps.clear()
                val totalStickersCount = emojiSet.toIntOrNull() ?: 0
                for (idx in 0 until totalStickersCount) {
                    val targetFile = File("/data/local/tmp/sticker_$idx.png")
                    if (targetFile.exists() && targetFile.canRead()) {
                        BitmapFactory.decodeFile(targetFile.absolutePath)?.let { decodedBitmap ->
                            cachedStickerBitmaps.add(decodedBitmap)
                        }
                    }
                }
            }
        }
    }

    val parsedEmojisList = remember(emojiSet) {
        val list = mutableListOf<String>()
        var idx = 0
        while (idx < emojiSet.length) {
            val cp = emojiSet.codePointAt(idx)
            list.add(String(Character.toChars(cp)))
            idx += Character.charCount(cp)
        }
        list
    }

    val parsedBgColor = remember(emojiBgColor) {
        try { Color(android.graphics.Color.parseColor(emojiBgColor)) } catch (e: Exception) { Color(0xFF121214) }
    }

    val renderElements = remember { mutableListOf<PreviewRenderItem>() }

    Box(modifier = Modifier.fillMaxSize().background(parsedBgColor)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width.toInt()
            val h = size.height.toInt()
            if (w == 0 || h == 0) return@Canvas

            renderElements.clear()

            val finalDrawnEmojisPool = mutableListOf<String>()
            val finalDrawnStickersPool = mutableListOf<Bitmap>()
            val isStickerMode = useStickerImage && cachedStickerBitmaps.isNotEmpty()

            if (isStickerMode) {
                var currentAssetIdx = 0
                val copiesRandom = Random(seed = 42)
                while (finalDrawnStickersPool.size < 80) {
                    val currentSticker = cachedStickerBitmaps[currentAssetIdx % cachedStickerBitmaps.size]

                    val targetCopiesCount = copiesRandom.nextInt(3, 5)
                    val safeCopiesCount = targetCopiesCount.coerceAtMost(80 - finalDrawnStickersPool.size)

                    for (c in 0 until safeCopiesCount) {
                        finalDrawnStickersPool.add(currentSticker)
                    }
                    currentAssetIdx++
                }
                finalDrawnStickersPool.shuffle(Random(seed = 1337))

            } else if (parsedEmojisList.isNotEmpty()) {
                var currentAssetIdx = 0
                val copiesRandom = Random(seed = 42)
                while (finalDrawnEmojisPool.size < 80) {
                    val currentEmoji = parsedEmojisList[currentAssetIdx % parsedEmojisList.size]

                    val targetCopiesCount = copiesRandom.nextInt(3, 5)
                    val safeCopiesCount = targetCopiesCount.coerceAtMost(80 - finalDrawnEmojisPool.size)

                    for (c in 0 until safeCopiesCount) {
                        finalDrawnEmojisPool.add(currentEmoji)
                    }
                    currentAssetIdx++
                }
                finalDrawnEmojisPool.shuffle(Random(seed = 1337))
            }

            fun addPreviewItem(index: Int, x: Float, y: Float, scale: Float = 1.0f, rot: Float = 0f, alp: Float = 1.0f) {
                if (isStickerMode) {
                    if (finalDrawnStickersPool.isNotEmpty()) {
                        val sticker = finalDrawnStickersPool[index % finalDrawnStickersPool.size]
                        renderElements.add(PreviewRenderItem.ImageItem(x, y, sticker, scale, rot, alp))
                    }
                } else {
                    if (finalDrawnEmojisPool.isNotEmpty()) {
                        val emoji = finalDrawnEmojisPool[index % finalDrawnEmojisPool.size]
                        renderElements.add(PreviewRenderItem.TextItem(x, y, emoji, scale, rot, alp))
                    }
                }
            }

            when (selectedLayout) {
                "grid" -> {
                    val columns = 3; val rows = 5
                    val cellWidth = w / columns.toFloat(); val cellHeight = h / rows.toFloat()
                    var index = 0
                    for (row in 0 until rows) {
                        for (col in 0 until columns) {
                            if (index >= 10) break
                            addPreviewItem(index, (col * cellWidth) + (cellWidth / 2f), (row * cellHeight) + (cellHeight / 2f))
                            index++
                        }
                    }
                }
                "dense_grid" -> {
                    val columns = 4; val rows = 7
                    val cellWidth = w / columns.toFloat(); val cellHeight = h / rows.toFloat()
                    var index = 0
                    for (row in 0 until rows) {
                        for (col in 0 until columns) {
                            addPreviewItem(index, (col * cellWidth) + (cellWidth / 2f), (row * cellHeight) + (cellHeight / 2f))
                            index++
                        }
                    }
                }
                "mosaic" -> {
                    val rows = 6; val columns = 3
                    val stepX = w / (columns + 1).toFloat(); val stepY = h / (rows + 1).toFloat()
                    var index = 0
                    for (r in 1..rows) {
                        val rowOffset = if (r % 2 != 0) stepX / 2f else 0f
                        for (c in 1..columns) {
                            val centerX = (c * stepX) - (stepX / 4f) + rowOffset
                            val centerY = r * stepY
                            if (centerX > 0 && centerX < w) {
                                addPreviewItem(index, centerX, centerY)
                                index++
                            }
                        }
                    }
                }
                "spiral" -> {
                    val centerX = w / 2f; val centerY = h / 2f
                    addPreviewItem(0, centerX, centerY)
                    val radius = w * 0.38f
                    for (i in 0 until 9) {
                        val angle = (2 * Math.PI * i) / 9
                        addPreviewItem(i + 1, centerX + (radius * Math.cos(angle)).toFloat(), centerY + (radius * Math.sin(angle)).toFloat())
                    }
                }
                "matrix" -> {
                    val columns = 4; val rows = 6
                    val cellWidth = w / columns.toFloat(); val cellHeight = h / rows.toFloat()
                    val randomizer = Random(1337)
                    var index = 0
                    for (row in 0 until rows) {
                        for (col in 0 until columns) {
                            val depthSelector = randomizer.nextInt(0, 3)
                            val scale = when(depthSelector) { 0 -> 0.45f; 1 -> 0.85f; else -> 1.35f }
                            val alpha = when(depthSelector) { 0 -> 0.35f; 1 -> 0.75f; else -> 1.0f }
                            addPreviewItem(index, (col * cellWidth) + (cellWidth / 2f), (row * cellHeight) + (cellHeight / 2f), scale, 0f, alpha)
                            index++
                        }
                    }
                }
                "chaos" -> {
                    val randomizer = Random(777)
                    val targetChaosLimit = 45
                    for (index in 0 until targetChaosLimit) {
                        val x = randomizer.nextFloat() * (w * 0.88f) + (w * 0.06f)
                        val y = randomizer.nextFloat() * (h * 0.88f) + (h * 0.06f)
                        val scale = randomizer.nextFloat() * 0.35f + 0.65f
                        val rotation = randomizer.nextFloat() * 50f - 25f
                        addPreviewItem(index, x, y, scale, rotation, 1.0f)
                    }
                }
                "diamond" -> {
                    val centerX = w / 2f; val centerY = h / 2f
                    val stepX = w * 0.22f; val stepY = h * 0.12f
                    val nodes = listOf(
                        Pair(0f, 0f), Pair(0f, -stepY), Pair(0f, stepY), Pair(-stepX, 0f), Pair(stepX, 0f),
                        Pair(-stepX, -stepY), Pair(stepX, -stepY), Pair(-stepX, stepY), Pair(stepX, stepY),
                        Pair(0f, -2 * stepY), Pair(0f, 2 * stepY), Pair(-2 * stepX, 0f), Pair(2 * stepX, 0f)
                    )
                    nodes.forEachIndexed { index, node ->
                        val finalX = centerX + node.first; val finalY = centerY + node.second
                        if (finalX > 0 && finalX < w && finalY > 0 && finalY < h) addPreviewItem(index, finalX, finalY)
                    }
                }
                "wave" -> {
                    val rows = 6
                    val startY = h * 0.12f
                    val endY = h * 0.88f
                    val stepY = (endY - startY) / (rows - 1)
                    for (i in 0 until rows) {
                        val currentY = startY + (i * stepY)
                        val angle = (i * Math.PI / 1.5).toFloat()
                        val horizontalAmplitude = w * 0.28f
                        val currentX = (w / 2f) - (w * 0.12f) + (horizontalAmplitude * Math.sin(angle.toDouble())).toFloat()

                        addPreviewItem(i * 2, currentX, currentY, 0.9f, 0f, 1.0f)
                        addPreviewItem(i * 2 + 1, w - currentX, currentY, 0.75f, 0f, 0.75f)
                    }
                }
            }

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                val density = context.resources.displayMetrics.density

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    textSize = 48f * density
                    textAlign = Paint.Align.CENTER
                }

                val baseSize = (54f * density).toInt()
                val fontMetrics = paint.fontMetrics
                val textOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f

                renderElements.forEach { item ->
                    val dynamicSize = (baseSize * item.scaleFactor).toInt()
                    paint.alpha = (item.alpha * 255).toInt()

                    nativeCanvas.save()
                    nativeCanvas.translate(item.x, item.y)
                    if (item.rotation != 0f) {
                        nativeCanvas.rotate(item.rotation)
                    }

                    when (item) {
                        is PreviewRenderItem.ImageItem -> {
                            val srcWidth = item.bitmap.width
                            val srcHeight = item.bitmap.height
                            val scale = Math.min(dynamicSize / srcWidth.toFloat(), dynamicSize / srcHeight.toFloat())
                            val dstWidth = srcWidth * scale
                            val dstHeight = srcHeight * scale

                            val rect = RectF(-(dstWidth / 2f), -(dstHeight / 2f), dstWidth / 2f, dstHeight / 2f)
                            nativeCanvas.drawBitmap(item.bitmap, null, rect, paint)
                        }
                        is PreviewRenderItem.TextItem -> {
                            paint.textSize = 48f * density * item.scaleFactor
                            nativeCanvas.drawText(item.text, 0f, 0f - textOffset, paint)
                        }
                    }
                    nativeCanvas.restore()
                }
            }
        }

        // TIME SIMULATOR OVERLAY
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("10:00", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light, color = Color.White.copy(alpha = 0.95f))
            Text("Friday, May 15", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f))
        }

        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Close Preview", tint = Color.White)
        }
    }
}

sealed class PreviewRenderItem(
    val x: Float,
    val y: Float,
    val scaleFactor: Float = 1.0f,
    val rotation: Float = 0f,
    val alpha: Float = 1.0f
) {
    class TextItem(x: Float, y: Float, val text: String, scale: Float = 1.0f, rot: Float = 0f, alp: Float = 1.0f) : PreviewRenderItem(x, y, scale, rot, alp)
    class ImageItem(x: Float, y: Float, val bitmap: Bitmap, scale: Float = 1.0f, rot: Float = 0f, alp: Float = 1.0f) : PreviewRenderItem(x, y, scale, rot, alp)
}
