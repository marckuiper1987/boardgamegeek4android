package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

class CalendarActivity : TopLevelSinglePaneActivity(), CalendarFragment.Listener {

    override fun onCreatePane(): Fragment = CalendarFragment().apply { listener = this@CalendarActivity }

    override val navigationItemId: Int = R.id.calendar

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")

    override fun onChangeMonth(yearMonth: YearMonth?) {
        supportActionBar?.title =
            if (yearMonth != null)
                "${monthTitleFormatter.format(yearMonth)} ${yearMonth.year}"
            else
                "Calendar"
    }
}
