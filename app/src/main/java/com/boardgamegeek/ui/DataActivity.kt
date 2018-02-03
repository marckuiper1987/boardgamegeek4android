package com.boardgamegeek.ui

import com.boardgamegeek.R

class DataActivity : TopLevelSinglePaneActivity() {

    override fun onCreatePane() = DataFragment()

    override fun getDrawerResId() = R.string.title_backup
}
