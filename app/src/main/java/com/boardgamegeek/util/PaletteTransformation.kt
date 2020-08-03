package com.boardgamegeek.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

import com.squareup.picasso.Transformation
import java.util.WeakHashMap

import androidx.palette.graphics.Palette
import com.boardgamegeek.R

/**
 * Used to get Picasso and Palette to play well with each other.
 */
class PaletteTransformation private constructor() : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val palette = Palette.from(source).generate()
        CACHE[source] = palette
        return source
    }

    override fun key(): String {
        return "" // Stable key for all requests. An unfortunate requirement.
    }

    companion object {
        private val INSTANCE = PaletteTransformation()
        private val CACHE = WeakHashMap<Bitmap, Palette>()

        fun instance(): PaletteTransformation {
            return INSTANCE
        }

        fun getPalette(bitmap: Bitmap): Palette? {
            return CACHE[bitmap]
        }
    }
}

class PaletteOverlayTransformation(
    private var defaultColor: Int
) : Transformation {

    override fun key(): String = "palette_overlay"

    override fun transform(source: Bitmap): Bitmap {

        val result = Bitmap.createBitmap(source.width, source.height, source.config)

        val rgb = Palette.from(source).generate()
            .let { it.getDarkVibrantColor(it.getDarkMutedColor(defaultColor)) }

        Canvas(result).apply {
            drawBitmap(source, 0f, 0f, null)
            drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), Paint().apply {
                color = rgb
                alpha = 180
                style = Paint.Style.FILL
            })
        }

        if (result != source) {
            source.recycle()
        }

        return result
    }
}