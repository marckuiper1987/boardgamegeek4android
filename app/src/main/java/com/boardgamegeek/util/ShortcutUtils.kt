package com.boardgamegeek.util

import android.content.Context
import android.content.Intent
import android.support.annotation.DrawableRes
import android.text.TextUtils

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.shortcut.CollectionShortcutTask
import com.boardgamegeek.util.shortcut.GameShortcutTask

import java.io.File

/**
 * Helps create shortcuts.
 */
object ShortcutUtils {
    val SHORT_LABEL_LENGTH = 14
    val LONG_LABEL_LENGTH = 25

    fun createCollectionShortcut(context: Context, viewId: Long, viewName: String) {
        val task = CollectionShortcutTask(context, viewId, viewName)
        TaskUtils.executeAsyncTask(task)
    }

    fun createGameShortcut(context: Context, gameId: Int, gameName: String, thumbnailUrl: String) {
        val task = GameShortcutTask(context, gameId, gameName, thumbnailUrl)
        TaskUtils.executeAsyncTask(task)
    }

    fun getThumbnailFile(context: Context, url: String): File? {
        if (!TextUtils.isEmpty(url)) {
            val filename = FileUtils.getFileNameFromUrl(url)
            if (filename != null) {
                return File(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS), filename)
            }
        }
        return null
    }

    @JvmOverloads
    fun createShortcutIntent(context: Context, shortcutName: String, intent: Intent, @DrawableRes shortcutIconResId: Int = R.mipmap.ic_launcher_foreground): Intent {
        val shortcut = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, shortcutIconResId))
        return shortcut
    }

    fun createGameShortcutId(gameId: Int): String {
        return "game-" + gameId
    }
}
