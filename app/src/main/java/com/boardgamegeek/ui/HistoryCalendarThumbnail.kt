package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import java.time.LocalDate
import java.time.YearMonth


private const val weekDays = 7
private const val numWeeks = 6

private const val padding = 8
private const val rectMargin = 1
private const val rectRadius = 5f

class HistoryCalendarThumbnail(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint(ANTI_ALIAS_FLAG).apply {
        color = context.resources.getColor(R.color.background)
        style = Paint.Style.FILL
    }

    var yearMonth: YearMonth? = null
        set(value) {
            field = value
            requestLayout()
        }

    var playsByDate: Map<LocalDate, List<PlayEntity>>? = null
        set(value) {
            field = value
            setup()
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val h = (w / weekDays) * numWeeks

        setMeasuredDimension(w, h)
    }

    private val rects = mutableListOf<Pair<RectF,DayProps>>()

    data class DayProps(
        val inOtherMonth: Boolean,
        val playNum: Int
    )

    private fun setup() {

        rects.clear()

        val yearMonth = yearMonth ?: return

        val w = width.toFloat() - (padding * 2)
        val cellW = (w - (rectMargin * (weekDays - 1))) / weekDays

        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value

        for (week in 1 until numWeeks + 1) {
            for (dayOfWeek in 1 until weekDays + 1) {

                val day = (((week - 1) * weekDays) + dayOfWeek - firstDayOfWeek) + 1
                val dayInOtherMonth = day <= 0 || day > yearMonth.lengthOfMonth()

                val plays =
                    if (!dayInOtherMonth)
                        playsByDate?.get(yearMonth.atDay(day)) ?: emptyList()
                    else emptyList()

                val props = DayProps(
                    inOtherMonth = dayInOtherMonth,
                    playNum = plays.size
                )

                val left = padding + (dayOfWeek - 1) * (cellW + rectMargin)
                val right = left + cellW
                val top = padding + (week - 1) * (cellW + rectMargin)
                val bottom = top + cellW

                rects.add(RectF(left, top, right, bottom) to props)
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            rects.forEach {
                drawRoundRect(
                    it.first,
                    rectRadius,
                    rectRadius,
                    it.second.let { (inOtherMonth, playNum) ->
                        when {
                            inOtherMonth ->
                                paint.apply { alpha = 20 }
                            playNum != 0 ->
                                paint.apply {
                                    alpha = (155 + (playNum * 20)).coerceAtMost(255)
                                }
                            else ->
                                paint.apply { alpha = 60 }
                        }
                    }
                )
            }
        }
    }
}