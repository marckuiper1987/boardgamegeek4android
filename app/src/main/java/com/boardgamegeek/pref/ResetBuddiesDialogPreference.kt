package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet

import com.boardgamegeek.tasks.ResetBuddiesTask
import com.boardgamegeek.tasks.ToastingAsyncTask

class ResetBuddiesDialogPreference(context: Context, attrs: AttributeSet) : AsyncDialogPreference(context, attrs) {

    override val task: ToastingAsyncTask
        get() = ResetBuddiesTask(context)
}
