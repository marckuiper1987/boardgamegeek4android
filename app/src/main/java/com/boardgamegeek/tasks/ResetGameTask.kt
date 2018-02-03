package com.boardgamegeek.tasks


import android.content.ContentValues
import android.content.Context

import com.boardgamegeek.provider.BggContract.Games

class ResetGameTask(context: Context) : ToastingAsyncTask(context) {

    override fun doInBackground(vararg params: Void): Boolean {
        if (context == null) return false
        val cv = ContentValues(3)
        cv.put(Games.UPDATED_LIST, 0)
        cv.put(Games.UPDATED, 0)
        cv.put(Games.UPDATED_PLAYS, 0)
        val rows = context.contentResolver.update(Games.CONTENT_URI, cv, null, null)
        return rows > 0
    }
}
