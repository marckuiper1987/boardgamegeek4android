package com.boardgamegeek.tasks

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.service.SyncService

/**
 * Clears the collection sync timestamps and requests a full collection sync be performed.
 */
class ResetCollectionTask(context: Context) : ToastingAsyncTask(context) {

    override val successMessageResource: Int
        get() = R.string.pref_sync_reset_success

    override val failureMessageResource: Int
        get() = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void): Boolean {
        if (context == null) return false
        if (SyncService.clearCollection(context)) {
            SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
            return true
        }
        return false
    }
}
