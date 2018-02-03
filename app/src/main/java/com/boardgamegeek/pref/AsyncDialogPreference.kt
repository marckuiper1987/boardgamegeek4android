package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet

import com.boardgamegeek.tasks.ToastingAsyncTask
import com.boardgamegeek.util.TaskUtils

abstract class AsyncDialogPreference(context: Context, attrs: AttributeSet) : ConfirmDialogPreference(context, attrs) {
    protected abstract val task: ToastingAsyncTask

    override fun execute() = TaskUtils.executeAsyncTask(task)
}
