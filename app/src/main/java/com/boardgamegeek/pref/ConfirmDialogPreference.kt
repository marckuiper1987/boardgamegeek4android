package com.boardgamegeek.pref

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet

import com.boardgamegeek.R

abstract class ConfirmDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    init {
        dialogTitle = "$dialogTitle?"
        dialogLayoutResource = R.layout.widget_dialogpreference_textview
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) execute()
    }

    protected abstract fun execute()
}
