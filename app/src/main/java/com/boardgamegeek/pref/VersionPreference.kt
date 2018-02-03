package com.boardgamegeek.pref

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet

import com.boardgamegeek.util.HelpUtils

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    override fun getSummary(): CharSequence = HelpUtils.getVersionName(context)
}