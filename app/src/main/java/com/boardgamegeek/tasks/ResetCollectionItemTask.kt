package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.events.CollectionItemResetEvent
import com.boardgamegeek.provider.BggContract.Collection
import org.greenrobot.eventbus.EventBus

class ResetCollectionItemTask(context: Context?, private val internalId: Long) : AsyncTask<Void, Void, Boolean>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext

    override fun doInBackground(vararg params: Void): Boolean {
        if (context == null) return false

        val values = ContentValues(9)
        values.put(Collection.COLLECTION_DIRTY_TIMESTAMP, 0)
        values.put(Collection.STATUS_DIRTY_TIMESTAMP, 0)
        values.put(Collection.COMMENT_DIRTY_TIMESTAMP, 0)
        values.put(Collection.RATING_DIRTY_TIMESTAMP, 0)
        values.put(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, 0)
        values.put(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, 0)
        values.put(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, 0)
        values.put(Collection.WANT_PARTS_DIRTY_TIMESTAMP, 0)
        values.put(Collection.HAS_PARTS_DIRTY_TIMESTAMP, 0)

        val rows = context.contentResolver.update(Collection.buildUri(internalId), values, null, null)
        return rows > 0
    }

    override fun onPostExecute(result: Boolean) {
        if (result) EventBus.getDefault().post(CollectionItemResetEvent(internalId))
    }
}
