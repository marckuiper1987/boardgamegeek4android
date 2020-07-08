package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

class HistoryActivity : TopLevelSinglePaneActivity(), CalendarFragment.Listener {

    override fun onCreatePane(): Fragment = CalendarFragment().also { it.listener = this }

    override val navigationItemId: Int = R.id.history

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")

    override fun onChangeMonth(yearMonth: YearMonth?) {
        supportActionBar?.title =
            if (yearMonth != null)
                "${monthTitleFormatter.format(yearMonth)} ${yearMonth.year}"
            else
                resources.getString(R.string.title_history)
    }
}
