package com.boardgamegeek.extensions

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// https://stackoverflow.com/questions/1778485/android-listview-display-all-available-items-without-scroll-with-static-header
fun RecyclerView.setHeightBasedOnItemsExcludingDecorations() = adapter?.let { adapter ->

    val numberOfItems = adapter.itemCount

    var totalItemHeight = 0
    for (itemPos in 0 until numberOfItems) {

        val viewHolder = adapter.onCreateViewHolder(this, 1)
        adapter.onBindViewHolder(viewHolder, itemPos)

        viewHolder.itemView.let {
            it.measure(0, 0)
            totalItemHeight += it.measuredHeight
        }
    }

    val params: ViewGroup.LayoutParams = layoutParams
    params.height = totalItemHeight
    layoutParams = params
    setPadding(0,0,0,0)
    requestLayout()
    true
}
