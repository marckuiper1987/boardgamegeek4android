package com.boardgamegeek.util.shortcut

import android.content.Context
import android.content.Intent

import com.boardgamegeek.R
import com.boardgamegeek.ui.CollectionActivity

class CollectionShortcutTask(context: Context, private val viewId: Long, private val viewName: String) : ShortcutTask(context) {

    override val shortcutName: String
        get() = viewName

    override val id: String
        get() = "collection_view-" + viewId

    override val shortcutIconResId: Int
        get() = R.drawable.ic_shortcut_ic_collection

    override fun createIntent(): Intent? = CollectionActivity.createIntentAsShortcut(context, viewId)
}
