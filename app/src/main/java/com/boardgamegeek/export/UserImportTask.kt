package com.boardgamegeek.export

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.Builder
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.boardgamegeek.export.model.User
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.util.ResolverUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import timber.log.Timber
import java.util.*

class UserImportTask(context: Context, uri: Uri) : JsonImportTask<User>(context, TYPE_USERS, uri) {

    override fun parseItem(gson: Gson, reader: JsonReader): User {
        return gson.fromJson(reader, User::class.java)
    }

    override fun importRecord(item: User, version: Int) {
        if (context == null) {
            Timber.w("Null context")
            return
        }

        if (ResolverUtils.rowExists(context.contentResolver, Buddies.buildBuddyUri(item.name))) {
            val batch = ArrayList<ContentProviderOperation>(item.colors.size)
            item.colors.forEach { color ->
                if (!TextUtils.isEmpty(color.color)) {
                    val builder: Builder = if (ResolverUtils.rowExists(context.contentResolver, PlayerColors.buildUserUri(item.name, color.sort))) {
                        ContentProviderOperation.newUpdate(PlayerColors.buildUserUri(item.name, color.sort))
                    } else {
                        ContentProviderOperation.newInsert(PlayerColors.buildUserUri(item.name))
                                .withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, color.sort)
                    }
                    batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color.color).build())
                }
            }

            ResolverUtils.applyBatch(context, batch)
        }
    }
}
