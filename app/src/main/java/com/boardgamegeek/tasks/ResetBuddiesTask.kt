package com.boardgamegeek.tasks

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.service.SyncService

import timber.log.Timber

/**
 * Clears the GeekBuddies sync timestamps and requests a full GeekBuddies sync be performed.
 */
class ResetBuddiesTask(context: Context) : ToastingAsyncTask(context) {

    override val successMessageResource: Int
        get() = R.string.pref_sync_reset_success

    override val failureMessageResource: Int
        get() = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void): Boolean {
        if (context == null) return false
        if (SyncService.clearBuddies(context)) {
            val count = context.contentResolver.delete(Buddies.CONTENT_URI, null, null)
            //TODO remove buddy colors
            Timber.i("Removed %d GeekBuddies", count)
            SyncService.sync(context, SyncService.FLAG_SYNC_BUDDIES)
            return true
        }
        return false
    }
}
