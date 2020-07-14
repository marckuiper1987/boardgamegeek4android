package com.boardgamegeek.extensions

import android.view.View
import android.view.ViewGroup
import android.widget.ListView

// https://stackoverflow.com/questions/1778485/android-listview-display-all-available-items-without-scroll-with-static-header
fun ListView.setHeightBasedOnItems(): Boolean =
    if (adapter == null)
        false
    else {
        val numberOfItems: Int = adapter.count

        // Get total height of all items.
        var totalItemsHeight = 0
        for (itemPos in 0 until numberOfItems) {
            val item: View = adapter.getView(itemPos, null, this)
            item.measure(0, 0)
            totalItemsHeight += item.getMeasuredHeight()
        }

        // Get total height of all item dividers.
        val totalDividersHeight: Int = dividerHeight *
            (numberOfItems - 1)

        // Set list height.
        val params: ViewGroup.LayoutParams = layoutParams
        params.height = totalItemsHeight + totalDividersHeight
        layoutParams = params
        requestLayout()
        true
    }
