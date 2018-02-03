package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri

import com.boardgamegeek.export.model.Game
import com.boardgamegeek.provider.BggContract.Games
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class GameExportTask(context: Context, uri: Uri) : JsonExportTask<Game>(context, TYPE_GAMES, uri) {

    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                Games.CONTENT_URI,
                Game.PROJECTION,
                null,
                null,
                null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        with(Game.fromCursor(cursor)) {
            addColors(context)
            if (hasColors()) {
                gson.toJson(this, Game::class.java, writer)
            }
        }
    }
}
