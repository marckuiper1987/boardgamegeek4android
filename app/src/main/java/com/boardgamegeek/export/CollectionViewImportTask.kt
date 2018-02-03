package com.boardgamegeek.export


import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.util.ResolverUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import timber.log.Timber
import java.util.*

class CollectionViewImportTask(context: Context, uri: Uri) : JsonImportTask<CollectionView>(context, TYPE_COLLECTION_VIEWS, uri) {

    override fun initializeImport() {
        if (context == null) {
            Timber.w("Null context")
            return
        }

        context.contentResolver.delete(CollectionViews.CONTENT_URI, null, null)
    }

    override fun parseItem(gson: Gson, reader: JsonReader): CollectionView {
        return gson.fromJson(reader, CollectionView::class.java)
    }

    override fun importRecord(item: CollectionView, version: Int) {
        if (context == null) {
            Timber.w("Null context")
            return
        }

        val values = ContentValues()
        values.put(CollectionViews.NAME, item.name)
        values.put(CollectionViews.STARRED, item.isStarred)
        values.put(CollectionViews.SORT_TYPE, item.sortType)
        val uri = context.contentResolver.insert(CollectionViews.CONTENT_URI, values)

        if (item.filters.isEmpty()) return

        val viewId = CollectionViews.getViewId(uri)
        val filterUri = CollectionViews.buildViewFilterUri(viewId.toLong())

        val batch = ArrayList<ContentProviderOperation>()
        item.filters.forEach { filter ->
            val builder = ContentProviderOperation.newInsert(filterUri)
            builder.withValue(CollectionViewFilters.TYPE, filter.type)
            builder.withValue(CollectionViewFilters.DATA, filter.data)
            batch.add(builder.build())
        }
        ResolverUtils.applyBatch(context, batch)
    }
}
