package com.boardgamegeek.tasks

import android.content.ContentValues
import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.service.SyncService

import timber.log.Timber

/**
 * Clears the plays sync timestamps and requests a full plays sync be performed.
 */
class ResetPlaysTask(context: Context) : ToastingAsyncTask(context) {

    override val successMessageResource: Int
        get() = R.string.pref_sync_reset_success

    override val failureMessageResource: Int
        get() = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void): Boolean {
        if (context == null) return false
        if (SyncService.clearPlays(context)) {
            val values = ContentValues(1)
            values.put(Plays.SYNC_HASH_CODE, 0)
            val count = context.contentResolver.update(Plays.CONTENT_URI, values, null, null)
            Timber.d("Cleared the hashcode from %,d plays.", count)
            SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS)
            return true
        }
        return false
    }
}