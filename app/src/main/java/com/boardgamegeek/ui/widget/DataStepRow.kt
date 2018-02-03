package com.boardgamegeek.ui.widget

import android.content.Context
import android.support.annotation.StringRes
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.boardgamegeek.R
import com.boardgamegeek.util.AnimationUtils
import com.boardgamegeek.util.FileUtils
import kotlinx.android.synthetic.main.widget_data_step_row.view.*
import org.jetbrains.anko.dimen
import org.jetbrains.anko.textResource

class DataStepRow(context: Context?) : LinearLayout(context) {
    private var type = ""
    private var listener: Listener? = null

    interface Listener {
        fun onExportClicked(type: String)

        fun onImportClicked(type: String)
    }

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.widget_data_step_row, this, true)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.VERTICAL
        minimumHeight = dimen(R.dimen.view_row_height)
        val verticalPadding = dimen(R.dimen.padding_half)
        setPadding(0, verticalPadding, 0, verticalPadding)

        exportButton.setOnClickListener { listener?.onExportClicked(type) }
        importButton.setOnClickListener { listener?.onImportClicked(type) }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun bind(type: String, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        this.type = type
        typeView.textResource = typeResId
        descriptionView.textResource = descriptionResId
        if (FileUtils.shouldUseDefaultFolders()) {
            fileNameView.text = FileUtils.getExportFile(type).toString()
            fileNameView.visibility = View.VISIBLE
        } else {
            fileNameView.visibility = View.GONE
        }
    }

    fun initProgressBar() {
        progressBar?.isIndeterminate = false
        AnimationUtils.fadeIn(progressBar)
        exportButton?.isEnabled = false
        importButton?.isEnabled = false
    }

    fun updateProgressBar(max: Int, progress: Int) {
        if (progressBar == null) return
        with(progressBar) {
            if (max < 0) {
                isIndeterminate = true
            } else {
                isIndeterminate = false
                this.max = max
                this.progress = progress
            }
        }
    }

    fun hideProgressBar() {
        AnimationUtils.fadeOutToInvisible(progressBar)
        exportButton?.isEnabled = true
        importButton?.isEnabled = true
    }
}
