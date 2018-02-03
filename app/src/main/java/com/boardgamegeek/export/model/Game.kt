package com.boardgamegeek.export.model

import android.content.Context
import android.database.Cursor
import com.boardgamegeek.provider.BggContract.Games
import com.google.gson.annotations.Expose

class Game : Model() {

    @Expose
    var gameId = 0
        private set
    @Expose
    var colors = mutableListOf<Color>()
        private set

    fun addColors(context: Context) {
        colors.clear()

        val cursor = context.contentResolver.query(
                Games.buildColorsUri(gameId),
                Color.PROJECTION,
                null,
                null,
                null) ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                colors.add(Color.fromCursor(c))
            }
        }
    }

    override fun toString(): String {
        return "Game{id=$gameId, colors=$colors}"
    }

    companion object {
        val PROJECTION = arrayOf(Games.GAME_ID)

        private val GAME_ID = 0

        fun fromCursor(cursor: Cursor): Game {
            val game = Game()
            game.gameId = cursor.getInt(GAME_ID)
            return game
        }
    }
}
