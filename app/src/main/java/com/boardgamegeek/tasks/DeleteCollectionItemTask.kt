package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.events.CollectionItemDeletedEvent
import com.boardgamegeek.provider.BggContract.Collection
import org.greenrobot.eventbus.EventBus

class DeleteCollectionItemTask(context: Context?, private val internalId: Long) : AsyncTask<Void, Void, Boolean>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext

    override fun doInBackground(vararg params: Void): Boolean? {
        if (context == null) return false
        val values = ContentValues()
        values.put(Collection.COLLECTION_DELETE_TIMESTAMP, System.currentTimeMillis())
        return context.contentResolver.update(Collection.buildUri(internalId), values, null, null) > 0
    }

    override fun onPostExecute(result: Boolean) {
        if (result) EventBus.getDefault().post(CollectionItemDeletedEvent(internalId))
    }
}
