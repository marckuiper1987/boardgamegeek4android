package com.boardgamegeek.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.loadThumbnail
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_4
import kotlinx.android.synthetic.main.calendar_day_three.view.calendar_day_1
import kotlinx.android.synthetic.main.calendar_day_three.view.calendar_day_2
import kotlinx.android.synthetic.main.calendar_day_three.view.calendar_day_3

class BoxedGameImages(
    private val games: Set<LiveData<CollectionItemEntity>>
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

    fun gamesForBox(box: Int) =
        if (gamesPerBox.containsKey(box)) {

            val indexValues = gamesPerBox.filterKeys { it < box }
            val fromIndex = if (indexValues.count() > 0) indexValues.values.reduce { acc, i -> acc + i } else 0

            games.toList().subList(
                fromIndex = fromIndex,
                toIndex = fromIndex + gamesPerBox.getOrElse(box) { 0 }
            ).toSet()
        }
        else emptySet()

    fun gamesPerBox() =
        mutableMapOf<Int, Set<LiveData<CollectionItemEntity>>>().apply {
            for (i in 0 until numberOfBoxes) {
                this[i] = gamesForBox(i)
            }
        }

    val numberOfBoxes = gamesPerBox.count()
}


class CalendarDayView(
    context: Context,
    attrs: AttributeSet? = null
): FrameLayout(context, attrs) {

    fun setGames(games: Set<LiveData<CollectionItemEntity>>, owner: LifecycleOwner) {
        removeAllViews()

        val boxes = BoxedGameImages(games)

        when(boxes.numberOfBoxes) {
            1 -> {
                inflate(context, R.layout.calendar_day_one, this)

                boxes.gamesForBox(0).first().observe(owner, Observer { game ->
                    calendar_day_1.loadThumbnail(game.thumbnailUrl)
                })
            }
            2 -> {
                inflate(context, R.layout.calendar_day_two, this)

                boxes.gamesForBox(0).first().observe(owner, Observer { game ->
                    calendar_day_1.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(1).first().observe(owner, Observer { game ->
                    calendar_day_2.loadThumbnail(game.thumbnailUrl)
                })
            }
            3 -> {
                inflate(context, R.layout.calendar_day_three, this)

                boxes.gamesForBox(0).first().observe(owner, Observer { game ->
                    calendar_day_1.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(1).first().observe(owner, Observer { game ->
                    calendar_day_2.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(2).first().observe(owner, Observer { game ->
                    calendar_day_3.loadThumbnail(game.thumbnailUrl)
                })
            }
            4 -> {
                inflate(context, R.layout.calendar_day_four, this)

                boxes.gamesForBox(0).first().observe(owner, Observer { game ->
                    calendar_day_1.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(1).first().observe(owner, Observer { game ->
                    calendar_day_2.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(2).first().observe(owner, Observer { game ->
                    calendar_day_3.loadThumbnail(game.thumbnailUrl)
                })
                boxes.gamesForBox(3).first().observe(owner, Observer { game ->
                    calendar_day_4.loadThumbnail(game.thumbnailUrl)
                })
            }
        }
    }
}