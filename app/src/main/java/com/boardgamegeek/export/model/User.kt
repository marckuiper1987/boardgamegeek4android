package com.boardgamegeek.export.model

import android.content.Context
import android.database.Cursor
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.google.gson.annotations.Expose

class User : Model() {
    @Expose
    var name = ""
        private set

    @Expose
    var colors = mutableListOf<PlayerColor>()
        private set

    fun addColors(context: Context) {
        colors.clear()

        val cursor = context.contentResolver.query(
                PlayerColors.buildUserUri(name),
                PlayerColor.PROJECTION,
                null,
                null,
                null) ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                colors.add(PlayerColor.fromCursor(c))
            }
        }
    }

    companion object {
        val PROJECTION = arrayOf(Buddies.BUDDY_NAME)

        private val BUDDY_NAME = 0

        fun fromCursor(cursor: Cursor): User {
            with(User()) {
                name = cursor.getString(BUDDY_NAME)
                return this
            }
        }
    }
}
