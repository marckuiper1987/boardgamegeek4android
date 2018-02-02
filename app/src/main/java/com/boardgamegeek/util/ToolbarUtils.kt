package com.boardgamegeek.util

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.TextView
import com.boardgamegeek.R

/**
 * Helps creating and populating toolbars (and older action bars).
 */
object ToolbarUtils {
    fun Menu.setActionBarText(id: Int, text: String) {
        setActionBarText(id, text, "")
    }

    fun Menu.setActionBarText(id: Int, text1: String, text2: String) {
        with(findItem(id)?.actionView ?: return) {
            findViewById<TextView>(android.R.id.text1)?.text = text1
            findViewById<TextView>(android.R.id.text2)?.text = text2
        }
    }

    fun AppCompatActivity.setDoneCancelActionBarView(listener: View.OnClickListener) {
        with(findViewById<Toolbar>(R.id.toolbar_done_cancel) ?: return) {
            setContentInsetsAbsolute(0, 0)
            findViewById<View>(R.id.menu_cancel)?.setOnClickListener(listener)
            findViewById<View>(R.id.menu_done)?.setOnClickListener(listener)
            setSupportActionBar(this)
        }
    }
}
