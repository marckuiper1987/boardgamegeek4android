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

    fun Context.createCollectionShortcut(viewId: Long, viewName: String) {
        val task = CollectionShortcutTask(this, viewId, viewName)
        TaskUtils.executeAsyncTask(task)
    }

    fun Context.createGameShortcut(gameId: Int, gameName: String, thumbnailUrl: String) {
        val task = GameShortcutTask(this, gameId, gameName, thumbnailUrl)
        TaskUtils.executeAsyncTask(task)
    }

    fun Context.getThumbnailFile(url: String): File? {
        if (!TextUtils.isEmpty(url)) {
            val filename = FileUtils.getFileNameFromUrl(url)
            if (filename != null) {
                return File(FileUtils.generateContentPath(this, BggContract.PATH_THUMBNAILS), filename)
            }
        }
        return null
    }

    @JvmOverloads
    fun Context.createShortcutIntent(shortcutName: String, intent: Intent, @DrawableRes shortcutIconResId: Int = R.mipmap.ic_launcher_foreground): Intent {
        val iconResource = Intent.ShortcutIconResource.fromContext(this, shortcutIconResId)
        with(Intent("com.android.launcher.action.INSTALL_SHORTCUT")) {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
            return this
        }
    }

    fun createGameShortcutId(gameId: Int): String {
        return "game-" + gameId
    }
}
