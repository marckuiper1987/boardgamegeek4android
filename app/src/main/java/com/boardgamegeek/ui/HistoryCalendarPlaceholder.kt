package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import com.boardgamegeek.R

private const val weekDays = 7
private const val numWeeks = 6
private const val titleHeight = 44
private const val widthMargin = 2

class HistoryCalendarPlaceholder(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val linePaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = context.resources.getColor(R.color.light_blue)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.line)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val h = (((w - widthMargin) / weekDays) + titleHeight) * numWeeks

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            val w = width.toFloat()
            val h = height.toFloat()

            val cellW = (w - widthMargin) / weekDays
            val cellH = cellW + titleHeight

            // Vertical lines
            for (i in 1 until weekDays) {
                (i*cellW + (widthMargin / 2)).let { x ->
                    drawLine(x, 0f, x, h, linePaint)
                }
            }

            // Horizontal lines
            for (i in 1 until numWeeks) {
                (i*cellH).let { y ->
                    drawLine(0f, y, w, y, linePaint)
                }
            }
        }
    }
}