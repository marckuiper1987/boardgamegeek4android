package com.boardgamegeek.util

import android.content.Context
import android.os.SystemClock
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
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
     * Populate the given [TextView] with the requested htmlText, formatting through [Html.fromHtml]
     * when applicable. Also sets [TextView.setMovementMethod] so inline links are handled.
     */
    fun TextView.setTextMaybeHtml(htmlText: String) {
        if (TextUtils.isEmpty(htmlText)) {
            text = ""
        } else if (htmlText.contains("<") && htmlText.contains(">") || htmlText.contains("&") && htmlText.contains(";")) {
            var modifiedText = htmlText
            // Fix up problematic HTML
            // replace DIVs with BR
            modifiedText = modifiedText.replace("[<]div[^>]*[>]".toRegex(), "")
            modifiedText = modifiedText.replace("[<]/div[>]".toRegex(), "<br/>")
            // remove all P tags
            modifiedText = modifiedText.replace("[<](/)?p[>]".toRegex(), "")
            // remove trailing BRs
            modifiedText = modifiedText.replace("(<br\\s?/>)+$".toRegex(), "")
            // replace 3+ BRs with a double
            modifiedText = modifiedText.replace("(<br\\s?/>){3,}".toRegex(), "<br/><br/>")
            // use BRs instead of new line character
            modifiedText = modifiedText.replace("\n".toRegex(), "<br/>")
            modifiedText = fixInternalLinks(modifiedText)

            val spanned = Html.fromHtml(modifiedText)
            text = spanned
            movementMethod = LinkMovementMethod.getInstance()
        } else {
            text = htmlText
        }
    }

    fun WebView.setWebViewText(text: String) {
        loadDataWithBaseURL(null, fixInternalLinks(text), "text/html", "UTF-8", null)
    }

    private fun fixInternalLinks(text: String): String {
        // ensure internal, path-only links are complete with the hostname
        if (TextUtils.isEmpty(text)) return ""
        var fixedText = text.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
        fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
        return fixedText
    }

    fun Chronometer.startTimerWithSystemTime(time: Long) {
        base = time - System.currentTimeMillis() + SystemClock.elapsedRealtime()
        start()
    }

    fun EditText.finishingEditing() {
        setSelection(0, text.length)
        requestFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun Menu.showMenuItem(itemId: Int, visible: Boolean) {
        findItem(itemId)?.isVisible = visible
    }

    fun Menu.enableMenuItem(itemId: Int, enabled: Boolean) {
        findItem(itemId)?.isEnabled = enabled
    }

    fun Menu.checkMenuItem(itemId: Int) {
        findItem(itemId)?.isChecked = true
    }
}
