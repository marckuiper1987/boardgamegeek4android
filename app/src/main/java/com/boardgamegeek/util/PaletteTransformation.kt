package com.boardgamegeek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.palette.graphics.Palette
import com.boardgamegeek.extensions.dpToPx
import com.squareup.picasso.Transformation
import org.jetbrains.anko.withAlpha
import java.util.WeakHashMap

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

    override fun key(): String = "palette_overlay_${defaultColor}}"

    override fun transform(source: Bitmap): Bitmap {

        val result = Bitmap.createBitmap(source.width, source.height, source.config)

//        val rgb = Palette.from(source).generate().getDarkVibrantColor(defaultColor)
        val rgb = Palette.from(source).generate().getDarkMutedColor(defaultColor)

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

class PaletteCornerOverlayTransformation(
    private var radius: Float,
    private var defaultColor: Int
) : Transformation {

    override fun key(): String = "palette_overlay_corner_${radius}_${defaultColor}"

    override fun transform(source: Bitmap): Bitmap {

        val result = Bitmap.createBitmap(source.width, source.height, source.config)

//        val rgb = Palette.from(source).generate().getDarkVibrantColor(defaultColor)
        val rgb = Palette.from(source).generate().getDarkMutedColor(defaultColor)

        Canvas(result).apply {
            drawBitmap(source, 0f, 0f, null)
            drawCircle(0f, 0f, radius, Paint().apply {
                shader = RadialGradient(
                    0f,
                    0f,
                    radius,
                    intArrayOf(rgb, rgb.withAlpha(0)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            })
        }

        if (result != source) {
            source.recycle()
        }

        return result
    }
}