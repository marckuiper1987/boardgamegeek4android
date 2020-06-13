package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.kizitonwose.calendarview.model.CalendarMonth
import org.threeten.bp.format.DateTimeFormatter

class CalendarActivity : TopLevelSinglePaneActivity(), CalendarFragment.Listener {

    override fun onCreatePane(): Fragment = CalendarFragment(this)

    override val navigationItemId: Int = R.id.calendar

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")

    override fun onChangeMonth(month: CalendarMonth) {
        supportActionBar?.title = "${monthTitleFormatter.format(month.yearMonth)} ${month.yearMonth.year}"
    }
}
