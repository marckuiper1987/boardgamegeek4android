package com.boardgamegeek.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.dpToPx
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.util.PaletteCornerOverlayTransformation
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_1_frame
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_2_frame
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_3_frame
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_4
import kotlinx.android.synthetic.main.calendar_day_four.view.calendar_day_4_frame
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
                    gamesPerBox[i-1] = maxGamesPerBox.coerceAtMost(gamesLeft.plus(gamesPerBox[i-1] ?: 0))
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

    private var nested = false

    private constructor(context: Context, nested: Boolean) : this(context) {
        this.nested = nested
    }

    private var games: Set<LiveData<CollectionItemEntity>>? = null

    private val paletteOverlayTransformation = PaletteCornerOverlayTransformation(
        defaultColor = context.resources.getColor(R.color.black_overlay),
        radius = context.dpToPx(25f)
    )

    fun setGames(games: Set<LiveData<CollectionItemEntity>>, owner: LifecycleOwner) {

        if (games == this.games) return
        this.games = games

        val boxes = BoxedGameImages(games)

        removeAllViews()
        boxes.gamesPerBox()
            .flatMap { it.value }
            .forEach { it.removeObservers(owner) }

        fun loadThumbnail(card: CardView, view: ImageView, box: Int) {

            card.setCardBackgroundColor(context.resources.getColor(R.color.black_overlay_light))

            boxes.gamesForBox(box).first().observe(owner, Observer { game ->
                if (game != null) {
                    view.loadThumbnail(
                        imageUrl = game.thumbnailUrl,
                        showPlaceholder = false,
                        transformation = if (!nested && box == 0) paletteOverlayTransformation else null
                    )
                }
            })
            if (nested) {
                card.radius = 0F
            }
        }

        fun loadThumbnailOrBox(frame: CardView, view: ImageView, box: Int) {

            val boxGames = boxes.gamesForBox(box)

            if (boxGames.size > 1) {
                frame.removeAllViews()
                frame.addView(CalendarDayView(context, nested = true).apply {
                    setGames(boxGames, owner)
                })
            }
            else {
                loadThumbnail(frame, view, box)
            }
        }

        when (boxes.numberOfBoxes) {
            1 -> {
                inflate(context, R.layout.calendar_day_one, this)
                loadThumbnail(calendar_day_1_frame, calendar_day_1, 0)
            }
            2 -> {
                inflate(context, R.layout.calendar_day_two, this)
                loadThumbnail(calendar_day_1_frame, calendar_day_1, 0)
                loadThumbnail(calendar_day_2_frame, calendar_day_2, 1)
            }
            3 -> {
                inflate(context, R.layout.calendar_day_three, this)
                loadThumbnail(calendar_day_1_frame, calendar_day_1, 0)
                loadThumbnail(calendar_day_2_frame, calendar_day_2, 1)
                loadThumbnail(calendar_day_3_frame, calendar_day_3, 2)
            }
            4 -> {
                inflate(context, R.layout.calendar_day_four, this)
                loadThumbnailOrBox(calendar_day_1_frame, calendar_day_1, 0)
                loadThumbnailOrBox(calendar_day_2_frame, calendar_day_2, 1)
                loadThumbnailOrBox(calendar_day_3_frame, calendar_day_3, 2)
                loadThumbnailOrBox(calendar_day_4_frame, calendar_day_4, 3)
            }
        }
    }
}