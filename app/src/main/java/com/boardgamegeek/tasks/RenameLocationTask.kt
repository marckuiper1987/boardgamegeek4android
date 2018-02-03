package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.ResolverUtils
import com.boardgamegeek.util.SelectionBuilder
import hugo.weaving.DebugLog
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * Renames a location in all plays, then triggers an update.
 */
class RenameLocationTask @DebugLog
constructor(context: Context?, private val oldLocationName: String, private val newLocationName: String) : AsyncTask<String, Void, String>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext
    private val startTime = System.currentTimeMillis()

    @DebugLog
    override fun doInBackground(vararg params: String): String {
        if (context == null) return "Error."

        val batch = ArrayList<ContentProviderOperation>()

        val values = ContentValues()
        values.put(Plays.LOCATION, newLocationName)
        batch.add(ContentProviderOperation
                .newUpdate(Plays.CONTENT_URI)
                .withValues(values)
                .withSelection(
                        "${Plays.LOCATION}=? AND (${Plays.UPDATE_TIMESTAMP}>0 OR ${Plays.DIRTY_TIMESTAMP}>0)",
                        arrayOf(oldLocationName))
                .build())

        values.put(Plays.UPDATE_TIMESTAMP, startTime)
        batch.add(ContentProviderOperation
                .newUpdate(Plays.CONTENT_URI)
                .withValues(values)
                .withSelection(
                        Plays.LOCATION + "=? AND " +
                                SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
                                SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
                                SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
                        arrayOf(oldLocationName)).build())

        val results = ResolverUtils.applyBatch(context, batch)

        return if (results.isNotEmpty()) {
            SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            val count = results.sumBy { it.count }
            context.resources.getQuantityString(
                    R.plurals.msg_play_location_change,
                    count,
                    count,
                    oldLocationName,
                    newLocationName)
        } else {
            context.getString(R.string.msg_play_location_change, oldLocationName, newLocationName)
        }
    }

    @DebugLog
    override fun onPostExecute(result: String) {
        EventBus.getDefault().post(Event(newLocationName, result))
    }

    inner class Event(val locationName: String, val message: String)
}
