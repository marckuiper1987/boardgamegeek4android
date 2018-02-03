package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.support.annotation.StringRes
import android.widget.Toast

/**
 * A task that requires no input or output other than a context that will show a success or failure message via a Toast
 * when complete.
 */
abstract class ToastingAsyncTask(context: Context?) : AsyncTask<Void, Void, Boolean>() {
    @SuppressLint("StaticFieldLeak") protected val context: Context? = context?.applicationContext

    @get:StringRes
    protected open val successMessageResource: Int
        get() = 0

    @get:StringRes
    protected open val failureMessageResource: Int
        get() = 0

    override fun onPostExecute(result: Boolean) {
        @StringRes val resId = if (result) successMessageResource else failureMessageResource
        if (resId > 0) Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }
}