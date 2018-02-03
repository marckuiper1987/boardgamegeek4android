package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet

import com.boardgamegeek.tasks.ResetPlaysTask
import com.boardgamegeek.tasks.ToastingAsyncTask

class ResetPlaysDialogPreference(context: Context, attrs: AttributeSet) : AsyncDialogPreference(context, attrs) {

    override val task: ToastingAsyncTask
        get() = ResetPlaysTask(context)
}
