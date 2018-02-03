package com.boardgamegeek.export.model

import android.database.Cursor

import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.google.gson.annotations.Expose

class Filter {

    @Expose
    var type = 0
        private set
    @Expose
    var data = ""
        private set

    companion object {
        val PROJECTION = arrayOf(
                CollectionViewFilters._ID,
                CollectionViewFilters.TYPE,
                CollectionViewFilters.DATA
        )

        private val TYPE = 1
        private val DATA = 2

        fun fromCursor(cursor: Cursor): Filter {
            val filter = Filter()
            filter.type = cursor.getInt(TYPE)
            filter.data = cursor.getString(DATA)
            return filter
        }
    }
}
