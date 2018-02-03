package com.boardgamegeek.export.model

import android.content.Context
import android.database.Cursor
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.google.gson.annotations.Expose

class CollectionView : Model() {

    private var id = 0
    @Expose
    var name = ""
        private set
    @Expose
    var sortType = 0
        private set
    @Expose
    var isStarred = false
        private set
    @Expose
    var filters = mutableListOf<Filter>()
        private set

    fun addFilters(context: Context) {
        filters.clear()

        val cursor = context.contentResolver.query(
                CollectionViews.buildViewFilterUri(id.toLong()),
                Filter.PROJECTION,
                null,
                null,
                null) ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                filters.add(Filter.fromCursor(c))
            }
        }
    }

    companion object {
        val PROJECTION = arrayOf(
                CollectionViews._ID,
                CollectionViews.NAME,
                CollectionViews.SORT_TYPE,
                CollectionViews.STARRED
        )

        private val ID = 0
        private val NAME = 1
        private val SORT_TYPE = 2
        private val STARRED = 3

        fun fromCursor(cursor: Cursor): CollectionView {
            val cv = CollectionView()
            cv.id = cursor.getInt(ID)
            cv.name = cursor.getString(NAME)
            cv.sortType = cursor.getInt(SORT_TYPE)
            cv.isStarred = cursor.getInt(STARRED) == 1
            return cv
        }
    }
}
