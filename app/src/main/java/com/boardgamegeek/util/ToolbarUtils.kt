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
        setActionBarText(id, text, null)
    }

    fun Menu.setActionBarText(id: Int, text1: String, text2: String?) {
        val item = findItem(id) ?: return
        val actionView = item.actionView
        if (actionView != null) {
            actionView.findViewById<TextView>(android.R.id.text1).text = text1
            actionView.findViewById<TextView>(android.R.id.text2).text = text2
        }
    }

    fun AppCompatActivity.setDoneCancelActionBarView(listener: View.OnClickListener) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_done_cancel) ?: return
        toolbar.setContentInsetsAbsolute(0, 0)
        toolbar.findViewById<View>(R.id.menu_cancel).setOnClickListener(listener)
        toolbar.findViewById<View>(R.id.menu_done).setOnClickListener(listener)
        setSupportActionBar(toolbar)
    }
}
