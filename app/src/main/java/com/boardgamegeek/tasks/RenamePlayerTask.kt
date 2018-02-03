package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.ResolverUtils
import com.boardgamegeek.util.SelectionBuilder
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.*

/**
 * Change a player name (either a GeekBuddy or named player), updating and syncing all plays.
 */
class RenamePlayerTask(context: Context?, private val oldName: String, private val newName: String) : AsyncTask<Void, Void, String>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext
    private val startTime = System.currentTimeMillis()
    private val batch: ArrayList<ContentProviderOperation> = ArrayList()

    override fun doInBackground(vararg params: Void): String {
        if (context == null) return ""

        batch.clear()
        updatePlays()
        updatePlayers()
        updateColors()
        ResolverUtils.applyBatch(context, batch)

        SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD)

        return context.getString(R.string.msg_play_player_change, oldName, newName)
    }

    private fun updatePlays() {
        if (context == null) return
        val internalIds = ResolverUtils.queryLongs(context.contentResolver,
                Plays.buildPlayersByPlayUri(),
                Plays._ID,
                "(" + SELECTION + ") AND " +
                        SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
                        SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
                        SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
                arrayOf(oldName, ""))
        if (internalIds.isNotEmpty()) {
            internalIds
                    .filter { it != BggContract.INVALID_ID.toLong() }
                    .mapTo(batch) {
                        ContentProviderOperation
                                .newUpdate(Plays.buildPlayUri(it!!))
                                .withValue(Plays.UPDATE_TIMESTAMP, startTime)
                                .build()
                    }
        }
    }

    private fun updatePlayers() {
        batch.add(ContentProviderOperation
                .newUpdate(Plays.buildPlayersByPlayUri())
                .withValue(PlayPlayers.NAME, newName)
                .withSelection(SELECTION, arrayOf(oldName, ""))
                .build())
    }

    private fun updateColors() {
        if (context == null) return
        val cursor = context.contentResolver.query(PlayerColors.buildPlayerUri(oldName),
                arrayOf(PlayerColors.PLAYER_COLOR, PlayerColors.PLAYER_COLOR_SORT_ORDER),
                null,
                null,
                null)
        cursor.use { c ->
            if (c != null) {
                while (c.moveToNext()) {
                    batch.add(ContentProviderOperation
                            .newInsert(PlayerColors.buildPlayerUri(newName))
                            .withValue(PlayerColors.PLAYER_COLOR, c.getString(0))
                            .withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, c.getInt(1))
                            .build())
                }
                Timber.i("Updated %,d colors", c.count)
            } else {
                Timber.i("No colors to update")
            }
        }
        batch.add(ContentProviderOperation.newDelete(PlayerColors.buildPlayerUri(oldName)).build())
    }

    override fun onPostExecute(result: String) {
        EventBus.getDefault().post(Event(newName, result))
    }

    inner class Event(val playerName: String, val message: String)

    companion object {
        private const val SELECTION = "play_players.${PlayPlayers.NAME}=? AND (${PlayPlayers.USER_NAME}=? OR ${PlayPlayers.USER_NAME} IS NULL)"
    }
}
