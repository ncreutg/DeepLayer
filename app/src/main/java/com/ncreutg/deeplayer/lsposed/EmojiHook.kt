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



package io.ncreutg.deeplayer.lsposed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import kotlin.random.Random

class EmojiHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        // Динамически ищем класс, проверяя оба возможных пути в Android 14
        val targetClass: Class<*> = try {
            XposedHelpers.findClass("com.android.systemui.shade.NotificationShadeWindowView", lpparam.classLoader)
        } catch (e: XposedHelpers.ClassNotFoundError) {
            try {
                XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowView", lpparam.classLoader)
            } catch (ex: XposedHelpers.ClassNotFoundError) {
                XposedBridge.log("DEEPLAYER ERROR: Не удалось найти класс NotificationShadeWindowView")
                return
            }
        }

        // Хукаем метод onAttachedToWindow у конкретного найденного класса
        XposedHelpers.findAndHookMethod(
            targetClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val container = param.thisObject as ViewGroup
                    if (container.findViewWithTag<View>("deeplayer_root_layer") != null) return

                    val modeFile = File("/data/local/tmp/config_mode.txt")
                    val currentMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                    if (currentMode != "emoji") return

                    val context = container.context

                    val bgFile = File("/data/local/tmp/config_emoji_bg.txt")
                    val bgColorString = if (bgFile.exists()) bgFile.readText().trim() else "#121214"

                    val rootLayer = FrameLayout(context).apply {
                        tag = "deeplayer_root_layer"
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        translationZ = -50f
                        try {
                            setBackgroundColor(Color.parseColor(bgColorString))
                        } catch (e: Exception) {
                            setBackgroundColor(Color.parseColor("#121214"))
                        }
                    }
                    container.addView(rootLayer, 0)

                    // Асинхронный поток для работы с диском, чтобы SystemUI не фризил при чтении PNG
                    Thread {
                        val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
                        val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")
                        val rawConfigString = if (emojiFile.exists()) emojiFile.readText().trim() else "👾"
                        val useStickerImage = if (stickerToggleFile.exists()) stickerToggleFile.readText().trim() == "true" else false

                        val emojis = mutableListOf<String>()
                        val stickerBitmapsList = mutableListOf<Bitmap>()

                        if (useStickerImage) {
                            val totalStickersCount = rawConfigString.toIntOrNull() ?: 0
                            for (idx in 0 until totalStickersCount) {
                                val targetFile = File("/data/local/tmp/sticker_$idx.png")
                                if (targetFile.exists() && targetFile.canRead()) {
                                    BitmapFactory.decodeFile(targetFile.absolutePath)?.let {
                                        stickerBitmapsList.add(it)
                                    }
                                }
                            }
                        } else {
                            var i = 0
                            while (i < rawConfigString.length) {
                                val codePoint = rawConfigString.codePointAt(i)
                                emojis.add(String(Character.toChars(codePoint)))
                                i += Character.charCount(codePoint)
                            }
                        }

                        // Возвращаемся в UI-поток для отрисовки паттерна
                        container.post {
                            val density = context.resources.displayMetrics.density
                            val emojiView = EmojiPatternView(context, emojis, stickerBitmapsList, density).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            }
                            rootLayer.addView(emojiView)
                        }
                    }.start()
                }
            }
        )
    }
}

class EmojiPatternView(
    context: Context,
    uniqueEmojis: List<String>,
    uniqueStickers: List<Bitmap>,
    private val density: Float
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        textSize = 48f * density
        textAlign = Paint.Align.CENTER
    }

    private val dynamicDuplicationRandom = Random(seed = 42)
    private val renderElements = mutableListOf<RenderItem>()

    private val finalDrawnEmojisPool = mutableListOf<String>()
    private val finalDrawnStickersPool = mutableListOf<Bitmap>()

    sealed class RenderItem(
        val x: Float,
        val y: Float,
        val scaleFactor: Float = 1.0f,
        val rotation: Float = 0f,
        val alpha: Float = 1.0f
    ) {
        class TextItem(x: Float, y: Float, val text: String, scale: Float = 1.0f, rot: Float = 0f, alp: Float = 1.0f) : RenderItem(x, y, scale, rot, alp)
        class ImageItem(x: Float, y: Float, val bitmap: Bitmap, scale: Float = 1.0f, rot: Float = 0f, alp: Float = 1.0f) : RenderItem(x, y, scale, rot, alp)
    }

    init {
        val targetDenseLimit = 80 // Safe bound limit for dense patterns

        if (uniqueStickers.isNotEmpty()) {
            var currentAssetIdx = 0
            // Loop until the dense pool is fully saturated
            while (finalDrawnStickersPool.size < targetDenseLimit) {
                // Safely extract the sticker based on current sequential index pointer
                val currentSticker = uniqueStickers[currentAssetIdx % uniqueStickers.size]

                // Randomize duplicates count per item to create visual variance
                val targetCopiesCount = dynamicDuplicationRandom.nextInt(3, 5)
                val safeCopiesCount = targetCopiesCount.coerceAtMost(targetDenseLimit - finalDrawnStickersPool.size)

                for (c in 0 until safeCopiesCount) {
                    finalDrawnStickersPool.add(currentSticker)
                }
                // Move to the next unique sticker in the array
                currentAssetIdx++
            }
            // Shuffle the complete pool using dynamic timestamp seed
            finalDrawnStickersPool.shuffle(Random(seed = System.currentTimeMillis()))

        } else if (uniqueEmojis.isNotEmpty()) {
            var currentAssetIdx = 0
            while (finalDrawnEmojisPool.size < targetDenseLimit) {
                val currentEmoji = uniqueEmojis[currentAssetIdx % uniqueEmojis.size]

                val targetCopiesCount = dynamicDuplicationRandom.nextInt(3, 5)
                val safeCopiesCount = targetCopiesCount.coerceAtMost(targetDenseLimit - finalDrawnEmojisPool.size)

                for (c in 0 until safeCopiesCount) {
                    finalDrawnEmojisPool.add(currentEmoji)
                }
                currentAssetIdx++
            }
            finalDrawnEmojisPool.shuffle(Random(seed = System.currentTimeMillis()))
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        renderElements.clear()
        val isStickerMode = finalDrawnStickersPool.isNotEmpty()

        // Оптимизация: Читаем файл сетки динамически прямо при инициализации размеров/смене экрана
        val layoutTypeFile = File("/data/local/tmp/config_emoji_layout.txt")
        val currentLayout = if (layoutTypeFile.exists()) layoutTypeFile.readText().trim() else "dense_grid"

        when (currentLayout) {
            "grid" -> calculateAdaptiveGrid(w, h, isStickerMode)
            "dense_grid" -> calculateDenseFullGrid(w, h, isStickerMode)
            "mosaic" -> calculateStaggeredMosaic(w, h, isStickerMode)
            "spiral" -> calculateSpiralLotus(w, h, isStickerMode)
            "matrix" -> calculateMatrixDepth(w, h, isStickerMode)
            "chaos" -> calculateChaosScatter(w, h, isStickerMode)
            "diamond" -> calculateDiamondPattern(w, h, isStickerMode)
            "wave" -> calculateWavePattern(w, h, isStickerMode)
            else -> calculateDenseFullGrid(w, h, isStickerMode)
        }
    }

    private fun calculateAdaptiveGrid(w: Int, h: Int, isStickerMode: Boolean) {
        val columns = 3
        val rows = 5
        val cellWidth = w / columns.toFloat()
        val cellHeight = h / rows.toFloat()

        var index = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (index >= 10) break
                val centerX = (col * cellWidth) + (cellWidth / 2f)
                val centerY = (row * cellHeight) + (cellHeight / 2f)
                addRenderItem(index, centerX, centerY, isStickerMode)
                index++
            }
        }
    }

    private fun calculateDenseFullGrid(w: Int, h: Int, isStickerMode: Boolean) {
        val columns = 4
        val rows = 7
        val cellWidth = w / columns.toFloat()
        val cellHeight = h / rows.toFloat()

        var index = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val centerX = (col * cellWidth) + (cellWidth / 2f)
                val centerY = (row * cellHeight) + (cellHeight / 2f)
                addRenderItem(index, centerX, centerY, isStickerMode)
                index++
            }
        }
    }

    private fun calculateStaggeredMosaic(w: Int, h: Int, isStickerMode: Boolean) {
        val rows = 6
        val columns = 3
        val stepX = w / (columns + 1).toFloat()
        val stepY = h / (rows + 1).toFloat()

        var index = 0
        for (r in 1..rows) {
            val rowOffset = if (r % 2 != 0) stepX / 2f else 0f
            for (c in 1..columns) {
                val centerX = (c * stepX) - (stepX / 4f) + rowOffset
                val centerY = r * stepY

                if (centerX > 0 && centerX < w) {
                    addRenderItem(index, centerX, centerY, isStickerMode)
                    index++
                }
            }
        }
    }

    private fun calculateSpiralLotus(w: Int, h: Int, isStickerMode: Boolean) {
        val centerX = w / 2f
        val centerY = h / 2f
        addRenderItem(0, centerX, centerY, isStickerMode)

        val remaining = 9
        val radius = w * 0.38f

        for (i in 0 until remaining) {
            val angle = (2 * Math.PI * i) / remaining
            val x = centerX + (radius * Math.cos(angle)).toFloat()
            val y = centerY + (radius * Math.sin(angle)).toFloat()
            addRenderItem(i + 1, x, y, isStickerMode)
        }
    }

    private fun calculateMatrixDepth(w: Int, h: Int, isStickerMode: Boolean) {
        val columns = 4
        val rows = 6
        val cellWidth = w / columns.toFloat()
        val cellHeight = h / rows.toFloat()
        val randomizer = Random(1337)

        var index = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val centerX = (col * cellWidth) + (cellWidth / 2f)
                val centerY = (row * cellHeight) + (cellHeight / 2f)

                val depthSelector = randomizer.nextInt(0, 3)
                val scale = when(depthSelector) {
                    0 -> 0.45f
                    1 -> 0.85f
                    else -> 1.35f
                }
                val alpha = when(depthSelector) {
                    0 -> 0.35f
                    1 -> 0.75f
                    else -> 1.0f
                }

                addRenderItem(index, centerX, centerY, isStickerMode, scale, 0f, alpha)
                index++
            }
        }
    }

    private fun calculateChaosScatter(w: Int, h: Int, isStickerMode: Boolean) {
        val randomizer = Random(777)
        // Increased element count limit from 18 to 45 for maximum viewport density
        val targetChaosLimit = 45

        for (index in 0 until targetChaosLimit) {
            // Distribute across full screen space leaving small boundary safety padding
            val x = randomizer.nextFloat() * (w * 0.88f) + (w * 0.06f)
            val y = randomizer.nextFloat() * (h * 0.88f) + (h * 0.06f)

            // Random rotation mapping bounds
            val rotation = randomizer.nextFloat() * 50f - 25f

            // Slightly reduced scale factor bounds to accommodate higher element volume beautifully
            val scale = randomizer.nextFloat() * 0.35f + 0.65f // Scale ranges between 0.65x and 1.0x

            addRenderItem(index, x, y, isStickerMode, scale, rotation, 1.0f)
        }
    }

    private fun calculateDiamondPattern(w: Int, h: Int, isStickerMode: Boolean) {
        val centerX = w / 2f
        val centerY = h / 2f
        val stepX = w * 0.22f
        val stepY = h * 0.12f

        val diamondNodes = listOf(
            Pair(0f, 0f),
            Pair(0f, -stepY), Pair(0f, stepY),
            Pair(-stepX, 0f), Pair(stepX, 0f),
            Pair(-stepX, -stepY), Pair(stepX, -stepY),
            Pair(-stepX, stepY), Pair(stepX, stepY),
            Pair(0f, -2 * stepY), Pair(0f, 2 * stepY),
            Pair(-2 * stepX, 0f), Pair(2 * stepX, 0f)
        )

        diamondNodes.forEachIndexed { index, node ->
            val finalX = centerX + node.first
            val finalY = centerY + node.second
            if (finalX > 0 && finalX < w && finalY > 0 && finalY < h) {
                addRenderItem(index, finalX, finalY, isStickerMode)
            }
        }
    }

    private fun calculateWavePattern(w: Int, h: Int, isStickerMode: Boolean) {
        // Reduced row density parameter to provide structural padding bounds
        val rows = 6
        val startY = h * 0.12f
        val endY = h * 0.88f
        val stepY = (endY - startY) / (rows - 1)

        for (i in 0 until rows) {
            val currentY = startY + (i * stepY)
            val angle = (i * Math.PI / 1.5).toFloat() // Adjusted frequency to smooth the curve trajectory

            // Expanded horizontal safe amplitude shift multiplier preventing center screen overlapping
            val horizontalAmplitude = w * 0.28f
            val currentX = (w / 2f) - (w * 0.12f) + (horizontalAmplitude * Math.sin(angle.toDouble())).toFloat()

            // Draw primary sine trajectory element nodes safely inside horizontal viewbounds
            addRenderItem(i * 2, currentX, currentY, isStickerMode, 0.9f, 0f, 1.0f)

            // Mirror horizontal trace element shifted safely to the opposite side perimeter
            val mirrorX = w - currentX
            addRenderItem(i * 2 + 1, mirrorX, currentY, isStickerMode, 0.75f, 0f, 0.75f)
        }
    }

    private fun addRenderItem(
        index: Int, x: Float, y: Float, isStickerMode: Boolean,
        scale: Float = 1.0f, rotation: Float = 0f, alpha: Float = 1.0f
    ) {
        if (isStickerMode) {
            if (finalDrawnStickersPool.isNotEmpty()) {
                val sticker = finalDrawnStickersPool[index % finalDrawnStickersPool.size]
                renderElements.add(RenderItem.ImageItem(x, y, sticker, scale, rotation, alpha))
            }
        } else {
            if (finalDrawnEmojisPool.isNotEmpty()) {
                val emoji = finalDrawnEmojisPool[index % finalDrawnEmojisPool.size]
                renderElements.add(RenderItem.TextItem(x, y, emoji, scale, rotation, alpha))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val baseSize = (54f * density).toInt()
        val fontMetrics = paint.fontMetrics
        val textOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f

        renderElements.forEach { item ->
            val dynamicSize = (baseSize * item.scaleFactor).toInt()
            paint.alpha = (item.alpha * 255).toInt()

            canvas.save()
            canvas.translate(item.x, item.y)
            if (item.rotation != 0f) {
                canvas.rotate(item.rotation)
            }

            when (item) {
                is RenderItem.ImageItem -> {
                    val srcWidth = item.bitmap.width
                    val srcHeight = item.bitmap.height
                    val scale = Math.min(dynamicSize / srcWidth.toFloat(), dynamicSize / srcHeight.toFloat())

                    val dstWidth = srcWidth * scale
                    val dstHeight = srcHeight * scale

                    val left = -(dstWidth / 2f)
                    val top = -(dstHeight / 2f)

                    val dstRect = RectF(left, top, left + dstWidth, top + dstHeight)
                    canvas.drawBitmap(item.bitmap, null, dstRect, paint)
                }
                is RenderItem.TextItem -> {
                    paint.textSize = 48f * density * item.scaleFactor
                    canvas.drawText(item.text, 0f, 0f - textOffset, paint)
                }
            }
            canvas.restore()
        }
        paint.alpha = 255
    }
}
