package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.provider.BggContract.Games

class FavoriteGameTask(context: Context?, private val gameId: Int, private val isFavorite: Boolean) : AsyncTask<Void, Void, Boolean>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext

    override fun doInBackground(vararg params: Void): Boolean? {
        if (context == null) return false
        val values = ContentValues()
        values.put(Games.STARRED, if (isFavorite) 1 else 0)
        return context.contentResolver.update(Games.buildGameUri(gameId), values, null, null) > 0
    }
}
