package com.boardgamegeek.export.model

import android.database.Cursor

import com.boardgamegeek.provider.BggContract.GameColors
import com.google.gson.annotations.Expose

class Color {

    @Expose
    var color = ""
        private set

    companion object {
        val PROJECTION = arrayOf(GameColors.COLOR)

        private val COLOR = 0

        fun fromCursor(cursor: Cursor): Color {
            val color = Color()
            color.color = cursor.getString(COLOR)
            return color
        }
    }
}
