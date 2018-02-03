package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet

import com.boardgamegeek.tasks.ClearDatabaseTask
import com.boardgamegeek.tasks.ToastingAsyncTask

class ClearDialogPreference(context: Context, attrs: AttributeSet) : AsyncDialogPreference(context, attrs) {

    override val task: ToastingAsyncTask
        get() = ClearDatabaseTask(context)
}
