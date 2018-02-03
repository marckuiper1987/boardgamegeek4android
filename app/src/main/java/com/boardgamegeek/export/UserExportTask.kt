package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri

import com.boardgamegeek.export.model.User
import com.boardgamegeek.provider.BggContract.Buddies
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class UserExportTask(context: Context, uri: Uri) : JsonExportTask<User>(context, TYPE_USERS, uri) {

    override val version = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                Buddies.CONTENT_URI,
                User.PROJECTION,
                null,
                null,
                null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        with(User.fromCursor(cursor)) {
            addColors(context)
            if (colors.isNotEmpty()) {
                gson.toJson(this, User::class.java, writer)
            }
        }
    }
}
