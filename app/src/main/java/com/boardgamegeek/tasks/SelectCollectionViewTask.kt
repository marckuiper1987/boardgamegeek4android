package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.util.ShortcutUtils
import com.boardgamegeek.util.StringUtils
import java.util.*

class SelectCollectionViewTask(context: Context?, private val viewId: Long) : AsyncTask<Void, Void, Void>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext
    private val shortcutManager: ShortcutManager? = when {
        context != null && VERSION.SDK_INT >= VERSION_CODES.N_MR1 -> context.getSystemService(ShortcutManager::class.java)
        else -> null
    }

    override fun doInBackground(vararg params: Void): Void? {
        if (context == null) return null
        updateSelection()
        if (shortcutManager != null && VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            shortcutManager.reportShortcutUsed(createShortcutName(viewId))
            setShortcuts()
        }
        return null
    }

    private fun updateSelection() {
        if (context == null) return
        val uri = CollectionViews.buildViewUri(viewId)

        val cursor = context.contentResolver.query(uri,
                arrayOf(CollectionViews.SELECTED_COUNT),
                null,
                null,
                null)
        cursor.use { c ->
            {
                val currentCount = c.getInt(0)
                val cv = ContentValues(2)
                cv.put(CollectionViews.SELECTED_COUNT, currentCount + 1)
                cv.put(CollectionViews.SELECTED_TIMESTAMP, System.currentTimeMillis())
                context.contentResolver.update(uri, cv, null, null)
            }
        }
    }

    @RequiresApi(VERSION_CODES.N_MR1)
    private fun setShortcuts() {
        if (context == null || shortcutManager == null) return
        val shortcuts = ArrayList<ShortcutInfo>(SHORTCUT_COUNT)

        val cursor = context.contentResolver.query(CollectionViews.CONTENT_URI,
                arrayOf(CollectionViews._ID, CollectionViews.NAME),
                null,
                null,
                "${CollectionViews.SELECTED_COUNT} DESC, ${CollectionViews.SELECTED_TIMESTAMP} DESC")
        cursor.use { c ->
            {
                while (c.moveToNext()) {
                    val name = c.getString(1)
                    if (name.isNotEmpty()) {
                        shortcuts.add(createShortcutInfo(c.getLong(0), name))
                        if (shortcuts.size >= SHORTCUT_COUNT) break
                    }
                }
            }
        }

        shortcutManager.dynamicShortcuts = shortcuts
    }

    @RequiresApi(VERSION_CODES.N_MR1)
    private fun createShortcutInfo(viewId: Long, viewName: String): ShortcutInfo {
        return ShortcutInfo.Builder(context, createShortcutName(viewId))
                .setShortLabel(StringUtils.limitText(viewName, ShortcutUtils.SHORT_LABEL_LENGTH))
                .setLongLabel(StringUtils.limitText(viewName, ShortcutUtils.LONG_LABEL_LENGTH))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
                .setIntent(CollectionActivity.createIntentAsShortcut(context, viewId))
                .build()
    }

    companion object {
        private val SHORTCUT_COUNT = 3

        private fun createShortcutName(viewId: Long) = "collection-view-" + viewId
    }
}
