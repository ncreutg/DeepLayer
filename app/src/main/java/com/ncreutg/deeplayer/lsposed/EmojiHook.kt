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
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import kotlin.random.Random

class EmojiHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedHelpers.findAndHookMethod(
            "android.view.View",
            lpparam.classLoader,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View
                    if (!view.javaClass.name.contains("NotificationShadeWindowView")) return

                    val container = view as ViewGroup
                    if (container.findViewWithTag<View>("deeplayer_root_layer") != null) return

                    val modeFile = File("/data/local/tmp/config_mode.txt")
                    val currentMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                    if (currentMode != "emoji") return

                    val context = container.context

                    val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
                    val bgFile = File("/data/local/tmp/config_emoji_bg.txt")
                    val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")

                    val rawConfigString = if (emojiFile.exists()) emojiFile.readText().trim() else "👾"
                    val bgColorString = if (bgFile.exists()) bgFile.readText().trim() else "#121214"
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

                    val rootLayer = FrameLayout(context).apply {
                        tag = "deeplayer_root_layer"
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        translationZ = -50f
                        try {
                            setBackgroundColor(Color.parseColor(bgColorString))
                        } catch (e: Exception) {
                            setBackgroundColor(Color.parseColor("#121214"))
                        }
                    }

                    val density = context.resources.displayMetrics.density
                    val emojiView = EmojiPatternView(context, emojis, stickerBitmapsList, density).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                    }

                    rootLayer.addView(emojiView)
                    container.addView(rootLayer, 0)
                }
            }
        )
    }
}

class EmojiPatternView(
    context: Context,
    private val uniqueEmojis: List<String>,
    private val uniqueStickers: List<Bitmap>,
    private val density: Float
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 54f * density
        textAlign = Paint.Align.CENTER
    }

    private val layoutRandom = Random(seed = 42)
    private val dynamicDuplicationRandom = Random(seed = 42)

    // Grid coordinate nodes: Pair of Center X, Center Y
    private var gridTargetPositions: List<Pair<Float, Float>>? = null

    private val finalDrawnEmojisPool = mutableListOf<String>()
    private val finalDrawnStickersPool = mutableListOf<Bitmap>()

    init {
        val targetTotalLimit = 10

        if (uniqueStickers.isNotEmpty()) {
            var currentAssetIdx = 0
            while (finalDrawnStickersPool.size < targetTotalLimit && uniqueStickers.isNotEmpty()) {
                val currentSticker = uniqueStickers[currentAssetIdx % uniqueStickers.size]

                // NEW LIMITS: Minimum 3, maximum 4 copies per unique asset
                val targetCopiesCount = dynamicDuplicationRandom.nextInt(3, 5) // Returns either 3 or 4
                val safeCopiesCount = targetCopiesCount.coerceAtMost(targetTotalLimit - finalDrawnStickersPool.size)

                for (c in 0 until safeCopiesCount) {
                    finalDrawnStickersPool.add(currentSticker)
                }
                currentAssetIdx++
            }
            finalDrawnStickersPool.shuffle(dynamicDuplicationRandom)

        } else if (uniqueEmojis.isNotEmpty()) {
            var currentAssetIdx = 0
            while (finalDrawnEmojisPool.size < targetTotalLimit && uniqueEmojis.isNotEmpty()) {
                val currentEmoji = uniqueEmojis[currentAssetIdx % uniqueEmojis.size]

                // NEW LIMITS: Minimum 3, maximum 4 copies per unique emoji
                val targetCopiesCount = dynamicDuplicationRandom.nextInt(3, 5) // Returns either 3 or 4
                val safeCopiesCount = targetCopiesCount.coerceAtMost(targetTotalLimit - finalDrawnEmojisPool.size)

                for (c in 0 until safeCopiesCount) {
                    finalDrawnEmojisPool.add(currentEmoji)
                }
                currentAssetIdx++
            }
            finalDrawnEmojisPool.shuffle(dynamicDuplicationRandom)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        // Configure virtual mathematical standard grid parameters coordinates mapping
        val columns = 3
        val rows = 4
        val cellWidth = w / columns.toFloat()
        val cellHeight = h / rows.toFloat()

        val allAvailableCells = mutableListOf<Pair<Float, Float>>()

        // Map center coordinates point for each bounding node box inside the viewport scope
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val centerX = (col * cellWidth) + (cellWidth / 2f)
                val centerY = (row * cellHeight) + (cellHeight / 2f)
                allAvailableCells.add(Pair(centerX, centerY))
            }
        }

        // Chaotic scatter selection: Shuffle all grid nodes randomly using dynamic persistent seed logic
        val gridRandomizer = Random(42)
        allAvailableCells.shuffle(gridRandomizer)

        // Grab exactly 10 unique target cell coordinates and lock them down persistently
        gridTargetPositions = allAvailableCells.take(10)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val isStickerMode = finalDrawnStickersPool.isNotEmpty()
        if (!isStickerMode && finalDrawnEmojisPool.isEmpty()) return

        val targets = gridTargetPositions ?: return
        val dynamicSize = (54f * density).toInt() // Crystal-sharp asset allocation boundary size matrix
        val half = dynamicSize / 2

        targets.forEachIndexed { index, (gridX, gridY) ->
            if (isStickerMode) {
                if (index < finalDrawnStickersPool.size) {
                    val stickerBitmap = finalDrawnStickersPool[index]
                    val rect = Rect(
                        (gridX - half).toInt(),
                        (gridY - half).toInt(),
                        (gridX + half).toInt(),
                        (gridY + half).toInt()
                    )
                    canvas.drawBitmap(stickerBitmap, null, rect, paint)
                }
            } else {
                if (index < finalDrawnEmojisPool.size) {
                    val emoji = finalDrawnEmojisPool[index]
                    canvas.drawText(emoji, gridX, gridY + (paint.textSize / 3f), paint)
                }
            }
        }
    }
}
