package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.Preference
import android.util.AttributeSet

import com.boardgamegeek.BuildConfig

class ChangeLogPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ccomeaux/boardgamegeek4android/blob/${BuildConfig.GIT_BRANCH}/CHANGELOG.md"))
    }
}
