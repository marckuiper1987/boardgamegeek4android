package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R

class CalendarActivity : TopLevelSinglePaneActivity() {

    override fun onCreatePane(): Fragment = CalendarFragment()

    override val navigationItemId: Int = R.id.calendar
}