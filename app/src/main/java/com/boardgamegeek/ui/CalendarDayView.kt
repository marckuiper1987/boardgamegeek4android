package com.boardgamegeek.ui

import android.content.Context
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.loadThumbnail
import kotlinx.android.synthetic.main.calendar_day_view.view.*

class BoxedGameImages(
    private val games: Set<GameEntity>
) {

    private val boxes = 4
    private val gamesPerBox = mutableMapOf<Int, Int>()

    init {

        var maxGamesPerBox = 1
        var gamesLeft = games.count()

        if (gamesLeft < 4) {
            for (i in gamesLeft downTo 1) {
                gamesPerBox[i-1] = 1
            }
        }
        else {
            while (gamesLeft > 0) {
                for (i in boxes downTo 1) {
                    gamesPerBox[i-1] = maxGamesPerBox.coerceAtMost(gamesLeft)
                    gamesLeft -= maxGamesPerBox
                    if (gamesLeft <= 0) {
                        break
                    }
                }
                maxGamesPerBox *= 4
            }
        }
    }

    private fun gamesForBox(box: Int) =
        if (gamesPerBox.containsKey(box))
            games.toList().subList(
                fromIndex = gamesPerBox.getOrElse(box-1) { 0 },
                toIndex = gamesPerBox.getOrElse(box) { 0 }
            ).toSet()
        else emptySet()

    fun gamesPerBox() =
        mutableMapOf<Int, Set<GameEntity>>().apply {
            for (i in 0 until numberOfBoxes) {
                this[i] = gamesForBox(i)
            }
        }

    val numberOfBoxes = gamesPerBox.count()
}


class CalendarDayView(
    context: Context,
    games: Set<GameEntity>
): FrameLayout(context) {

    init {
        inflate(context, R.layout.calendar_day_view, this)

        val boxes = BoxedGameImages(games)

        val view = when (boxes.numberOfBoxes) {
            1 -> FrameLayout(context)
            2 -> LinearLayout(context)
            3 -> LinearLayout(context)
            4 -> GridLayout(context)
            else -> null
        }

        if (view != null) {
            boxes.gamesPerBox().forEach { gamesForBox ->
                view.addView(
                        if (gamesForBox.value.count() == 1)
                            ImageView(context).apply {
                                loadThumbnail(gamesForBox.value.first().thumbnailUrl)
                            }
                        else
                            CalendarDayView(context, gamesForBox.value))
            }

            calendar_day_view.addView(view)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }
}