package com.boardgamegeek.util.shortcut

import android.content.Context
import android.content.Intent

import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.ShortcutUtils

class GameShortcutTask(context: Context, private val gameId: Int, private val gameName: String, thumbnailUrl: String) : ShortcutTask(context, thumbnailUrl) {

    override val shortcutName: String
        get() = gameName

    override val id: String
        get() = ShortcutUtils.createGameShortcutId(gameId)

    override fun createIntent(): Intent? {
        return GameActivity.createIntentAsShortcut(gameId, shortcutName)
    }
}
