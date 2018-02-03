package com.boardgamegeek.export.model

import android.database.Cursor

import com.boardgamegeek.provider.BggContract.PlayerColors
import com.google.gson.annotations.Expose

class PlayerColor {

    private val id: Int = 0
    @Expose
    var sort = 0
        private set
    @Expose
    var color = ""
        private set

    companion object {
        val PROJECTION = arrayOf(
                PlayerColors._ID,
                PlayerColors.PLAYER_COLOR_SORT_ORDER,
                PlayerColors.PLAYER_COLOR
        )

        private val SORT = 1
        private val COLOR = 2

        fun fromCursor(cursor: Cursor): PlayerColor {
            with(PlayerColor()) {
                sort = cursor.getInt(SORT)
                color = cursor.getString(COLOR)
                return this
            }
        }
    }
}
