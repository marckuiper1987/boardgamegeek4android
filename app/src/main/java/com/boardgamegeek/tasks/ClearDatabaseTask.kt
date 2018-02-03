package com.boardgamegeek.tasks

import android.content.Context
import android.net.Uri
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.service.SyncService
import timber.log.Timber

/**
 * Deletes all data in the local database.
 */
class ClearDatabaseTask(context: Context) : ToastingAsyncTask(context) {
    override val successMessageResource: Int
        get() = R.string.pref_sync_clear_success

    override val failureMessageResource: Int
        get() = R.string.pref_sync_clear_failure

    override fun doInBackground(vararg params: Void): Boolean? {
        if (context == null) return false

        var success = SyncService.clearCollection(context)
        success = success and SyncService.clearBuddies(context)
        success = success and SyncService.clearPlays(context)

        var count = 0
        count += delete(Games.CONTENT_URI)
        count += delete(Artists.CONTENT_URI)
        count += delete(Designers.CONTENT_URI)
        count += delete(Publishers.CONTENT_URI)
        count += delete(Categories.CONTENT_URI)
        count += delete(Mechanics.CONTENT_URI)
        count += delete(Buddies.CONTENT_URI)
        count += delete(Plays.CONTENT_URI)
        count += delete(CollectionViews.CONTENT_URI)
        Timber.i("Removed %,d records", count)

        if (context.contentResolver != null) {
            count = 0
            count += context.contentResolver.delete(Thumbnails.CONTENT_URI, null, null)
            count += context.contentResolver.delete(Avatars.CONTENT_URI, null, null)
            Timber.i("Removed %,d files", count)
        }

        return success
    }

    private fun delete(uri: Uri): Int {
        if (context.contentResolver == null) return 0
        val count = context.contentResolver.delete(uri, null, null)
        Timber.i("Removed %1\$,d %2\$s", count, uri.lastPathSegment)
        return count
    }
}
