package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.kizitonwose.calendarview.model.CalendarMonth
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

class CalendarActivity :
    TopLevelSinglePaneActivity(),
    CalendarFragment.Listener,
    CalendarOverviewFragment.Listener {

    private val overviewFragment by lazy { CalendarOverviewFragment(this) }
    private val calendarFragment by lazy { CalendarFragment(this) }

    override fun onCreatePane(): Fragment = overviewFragment

    override val navigationItemId: Int = R.id.calendar

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")

    override fun onChangeMonth(yearMonth: YearMonth) {
        supportActionBar?.title = "${monthTitleFormatter.format(yearMonth)} ${yearMonth.year}"
    }

    override fun onSelectMonth(yearMonth: YearMonth) {
        calendarFragment.selectedMonth = yearMonth
        replaceFragment(calendarFragment)
    }
}
