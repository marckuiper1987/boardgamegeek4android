package com.boardgamegeek.pref

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import com.boardgamegeek.ui.BuddyActivity

class BuddyPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = BuddyActivity.createIntent(context, summary.toString(), title.toString())
    }
}
