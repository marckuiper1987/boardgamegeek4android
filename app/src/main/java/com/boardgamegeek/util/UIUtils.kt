package com.boardgamegeek.util

import android.content.Context
import android.os.SystemClock
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView

/**
 * Various static methods for use on views and fragments.
 */
object UIUtils {
    /**
     * Populate the given [TextView] with the requested text, formatting through [Html.fromHtml]
     * when applicable. Also sets [TextView.setMovementMethod] so inline links are handled.
     */
    fun setTextMaybeHtml(view: TextView, text: String) {
        var text = text
        if (TextUtils.isEmpty(text)) {
            view.text = ""
            return
        }
        if (text.contains("<") && text.contains(">") || text.contains("&") && text.contains(";")) {
            // Fix up problematic HTML
            // replace DIVs with BR
            text = text.replace("[<]div[^>]*[>]".toRegex(), "")
            text = text.replace("[<]/div[>]".toRegex(), "<br/>")
            // remove all P tags
            text = text.replace("[<](/)?p[>]".toRegex(), "")
            // remove trailing BRs
            text = text.replace("(<br\\s?/>)+$".toRegex(), "")
            // replace 3+ BRs with a double
            text = text.replace("(<br\\s?/>){3,}".toRegex(), "<br/><br/>")
            // use BRs instead of new line character
            text = text.replace("\n".toRegex(), "<br/>")
            text = fixInternalLinks(text)

            val spanned = Html.fromHtml(text)
            view.text = spanned
            view.movementMethod = LinkMovementMethod.getInstance()
        } else {
            view.text = text
        }
    }

    fun setWebViewText(view: WebView, text: String) {
        view.loadDataWithBaseURL(null, fixInternalLinks(text), "text/html", "UTF-8", null)
    }

    private fun fixInternalLinks(text: String): String {
        // ensure internal, path-only links are complete with the hostname
        if (TextUtils.isEmpty(text)) return ""
        var fixedText = text.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
        fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
        return fixedText
    }

    fun startTimerWithSystemTime(timer: Chronometer, time: Long) {
        timer.base = time - System.currentTimeMillis() + SystemClock.elapsedRealtime()
        timer.start()
    }

    fun finishingEditing(editText: EditText) {
        editText.setSelection(0, editText.text.length)
        editText.requestFocus()
        val inputMethodManager = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun showMenuItem(menu: Menu, itemId: Int, visible: Boolean) {
        val menuItem = menu.findItem(itemId) ?: return
        menuItem.isVisible = visible
    }

    fun enableMenuItem(menu: Menu, itemId: Int, enabled: Boolean) {
        val menuItem = menu.findItem(itemId) ?: return
        menuItem.isEnabled = enabled
    }

    fun checkMenuItem(menu: Menu, itemId: Int) {
        val menuItem = menu.findItem(itemId) ?: return
        menuItem.isChecked = true
    }
}
