package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import kotlinx.android.synthetic.main.row_collection.view.year
import java.time.LocalDate
import java.time.YearMonth

private const val weekDays = 7
private const val numWeeks = 6
private const val margin = 1

class HistoryCalendarThumbnail(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val clearPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = context.resources.getColor(R.color.light_blue)
        style = Paint.Style.FILL
    }

    private val filledPaint = Paint(ANTI_ALIAS_FLAG).apply {
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
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val h = (w / weekDays) * numWeeks

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            val yearMonth = yearMonth ?: return

            val w = width.toFloat()
            val cellW = (w - (margin * (weekDays - 1))) / weekDays

            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value

            for (week in 1 until numWeeks + 1) {
                for (dayOfWeek in 1 until weekDays + 1) {

                    val day = (((week - 1) * weekDays) + dayOfWeek - firstDayOfWeek) + 1
                    val dayInOtherMonth = day <= 0 || day > yearMonth.lengthOfMonth()

                    val plays =
                        if (!dayInOtherMonth)
                            playsByDate?.get(yearMonth.atDay(day)) ?: emptyList()
                        else emptyList()

                    val paint = when {
                        dayInOtherMonth ->
                            filledPaint.apply { alpha = 40 }
                        plays.isNotEmpty() ->
                            filledPaint.apply {
                                alpha = (155 + (plays.size * 20)).coerceAtMost(255)
                            }
                        else ->
                            filledPaint.apply { alpha = 100 }
                    }

                    val left = (dayOfWeek - 1) * (cellW + margin)
                    val right = left + cellW
                    val top = (week - 1) * (cellW + margin)
                    val bottom = top + cellW

                    drawRect(left, top, right, bottom, paint)
                }
            }
        }
    }
}