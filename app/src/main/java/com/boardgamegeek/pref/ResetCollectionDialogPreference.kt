package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet

import com.boardgamegeek.tasks.ResetCollectionTask
import com.boardgamegeek.tasks.ToastingAsyncTask

class ResetCollectionDialogPreference(context: Context, attrs: AttributeSet) : AsyncDialogPreference(context, attrs) {

    override val task: ToastingAsyncTask
        get() = ResetCollectionTask(context)
}
