package com.boardgamegeek.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.boardgamegeek.export.model.Game
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.ResolverUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import timber.log.Timber

class GameImportTask(context: Context, uri: Uri) : JsonImportTask<Game>(context, TYPE_GAMES, uri) {

    override fun parseItem(gson: Gson, reader: JsonReader): Game {
        return gson.fromJson(reader, Game::class.java)
    }

    override fun importRecord(game: Game, version: Int) {
        if (context == null) {
            Timber.w("Null context")
            return
        }

        if (ResolverUtils.rowExists(context.contentResolver, Games.buildGameUri(game.gameId))) {
            val gameColorsUri = Games.buildColorsUri(game.gameId)

            context.contentResolver.delete(gameColorsUri, null, null)

            val values = mutableListOf<ContentValues>()
            game.colors.forEach { color ->
                if (!TextUtils.isEmpty(color.color)) {
                    val cv = ContentValues()
                    cv.put(GameColors.COLOR, color.color)
                    values.add(cv)
                }
            }

            if (values.isNotEmpty()) {
                context.contentResolver.bulkInsert(gameColorsUri, values.toTypedArray())
            }
        }
    }
}
