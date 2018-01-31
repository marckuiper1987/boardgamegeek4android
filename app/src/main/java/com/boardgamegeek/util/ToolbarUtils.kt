package com.boardgamegeek.util

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import com.boardgamegeek.R

/**
 * Helps creating and populating toolbars (and older action bars).
 */
object ToolbarUtils {

    fun setActionBarText(menu: Menu, id: Int, text: String) {
        setActionBarText(menu, id, text, null)
    }

    fun setActionBarText(menu: Menu, id: Int, text1: String, text2: String?) {
        val item = menu.findItem(id)
        if (item != null) {
            val actionView = item.actionView
            if (actionView != null) {
                val tv1 = actionView.findViewById<TextView>(android.R.id.text1)
                if (tv1 != null) {
                    tv1.text = text1
                }
                val tv2 = actionView.findViewById<TextView>(android.R.id.text2)
                if (tv2 != null) {
                    tv2.text = text2
                }
            }
        }
    }

    fun setDoneCancelActionBarView(activity: AppCompatActivity, listener: View.OnClickListener) {
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar_done_cancel) ?: return
        toolbar.setContentInsetsAbsolute(0, 0)
        val cancelActionView = toolbar.findViewById<View>(R.id.menu_cancel)
        cancelActionView.setOnClickListener(listener)
        val doneActionView = toolbar.findViewById<View>(R.id.menu_done)
        doneActionView.setOnClickListener(listener)
        activity.setSupportActionBar(toolbar)
    }
}
