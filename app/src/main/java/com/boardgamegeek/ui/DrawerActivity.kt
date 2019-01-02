package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.ButterKnife
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.Status
import com.boardgamegeek.events.SignInEvent
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.activity_drawer_base.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
abstract class DrawerActivity : BaseActivity() {
    private var viewModel: SelfUserViewModel? = null

    protected open fun getDrawerResId(): Int {
        return 0
    }

    @LayoutRes
    protected open fun getLayoutResId(): Int {
        return R.layout.activity_drawer_base
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())
        ButterKnife.bind(this)
        setSupportActionBar(toolbar)
        if (drawerLayout != null) {
            drawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
            drawerLayout!!.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark))
        }
        viewModel = ViewModelProviders.of(this).get(SelfUserViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        if (!PreferencesUtils.hasSeenNavDrawer(this)) {
            drawerLayout!!.openDrawer(GravityCompat.START)
            PreferencesUtils.sawNavDrawer(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDrawer()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SignInEvent) {
        if (!TextUtils.isEmpty(event.username))
            onSignInSuccess()
    }

    protected fun onSignInSuccess() {
        refreshDrawer()
    }

    private fun refreshDrawer() {
        if (drawerList == null) return

        drawerList!!.removeAllViews()
        drawerList!!.addView(makeNavDrawerBuffer(drawerList))
        if (!Authenticator.isSignedIn(this)) {
            drawerList!!.addView(makeNavDrawerSpacer(drawerList))
            drawerList!!.addView(makeNavDrawerItem(R.string.title_signin, R.drawable.ic_account_circle_black_24dp, drawerList))
        } else {
            val view = makeNavDrawerHeader(drawerList)
            if (view != null) drawerList!!.addView(view)
            drawerList!!.addView(makeNavDrawerSpacer(drawerList))
            drawerList!!.addView(makeNavDrawerItem(R.string.title_collection, R.drawable.ic_collection, drawerList))
            drawerList!!.addView(makeNavDrawerItem(R.string.title_plays, R.drawable.ic_log_play, drawerList))
            drawerList!!.addView(makeNavDrawerItem(R.string.title_buddies, R.drawable.ic_user, drawerList))
        }
        drawerList!!.addView(makeNavDrawerSpacerWithDivider(drawerList))

        drawerList!!.addView(makeNavDrawerSpacer(drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_search, R.drawable.ic_action_search, drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_hotness, R.drawable.ic_hotness, drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_top_games, R.drawable.ic_top_games, drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_geeklists, R.drawable.ic_geek_list, drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_forums, R.drawable.ic_forums, drawerList))
        drawerList!!.addView(makeNavDrawerSpacerWithDivider(drawerList))

        drawerList!!.addView(makeNavDrawerSpacer(drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_backup, R.drawable.ic_data, drawerList))
        drawerList!!.addView(makeNavDrawerItem(R.string.title_settings, R.drawable.ic_settings, drawerList))
        drawerList!!.addView(makeNavDrawerSpacer(drawerList))
    }

    private fun selectItem(titleResId: Int) {
        if (titleResId != getDrawerResId()) {
            var intent: Intent? = null
            var shouldFinish = true
            when (titleResId) {
                R.string.title_collection -> intent = Intent(this, CollectionActivity::class.java)
                R.string.title_search -> {
                    intent = Intent(this, SearchResultsActivity::class.java)
                    shouldFinish = false
                }
                R.string.title_hotness -> intent = Intent(this, HotnessActivity::class.java)
                R.string.title_top_games -> intent = Intent(this, TopGamesActivity::class.java)
                R.string.title_geeklists -> intent = Intent(this, GeekListsActivity::class.java)
                R.string.title_plays -> intent = Intent(this, PlaysSummaryActivity::class.java)
                R.string.title_buddies -> intent = Intent(this, BuddiesActivity::class.java)
                R.string.title_forums -> intent = Intent(this, ForumsActivity::class.java)
                R.string.title_signin -> startActivity(Intent(this, LoginActivity::class.java))
                R.string.title_backup -> startActivity(Intent(this, DataActivity::class.java))
                R.string.title_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            if (intent != null) {
                startActivity(intent)
                if (shouldFinish) {
                    finish()
                }
            }
        }
        drawerLayout!!.closeDrawer(drawerListContainer)
    }

    private fun makeNavDrawerHeader(container: ViewGroup): View? {
        val view = layoutInflater.inflate(R.layout.row_header_drawer, container, false)

        val fullName = AccountUtils.getFullName(this)
        val username = AccountUtils.getUsername(this)
        if (TextUtils.isEmpty(fullName)) {
            if (TextUtils.isEmpty(username)) {
                val account = Authenticator.getAccount(this)
                if (account != null) {
                    viewModel!!.user.observe(this, Observer { (status) ->
                        if (status === Status.SUCCESS) {
                            refreshDrawer()
                        }
                    })
                }
                return null
            } else {
                (view.findViewById<View>(R.id.account_info_primary) as TextView).text = username
            }
        } else {
            (view.findViewById<View>(R.id.account_info_primary) as TextView).text = fullName
            (view.findViewById<View>(R.id.account_info_secondary) as TextView).text = username
        }

        val avatarUrl = AccountUtils.getAvatarUrl(this)
        val imageView = view.findViewById<ImageView>(R.id.account_image)
        if (avatarUrl == null || avatarUrl.isBlank()) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            imageView.loadThumbnail(avatarUrl, R.drawable.person_image_empty)
        }

        return view
    }

    private fun makeNavDrawerBuffer(container: ViewGroup): View {
        return layoutInflater.inflate(R.layout.row_buffer_drawer, container, false)
    }

    private fun makeNavDrawerSpacer(container: ViewGroup): View {
        return layoutInflater.inflate(R.layout.row_spacer_drawer, container, false)
    }

    private fun makeNavDrawerSpacerWithDivider(container: ViewGroup): View {
        val view = makeNavDrawerSpacer(container)
        view.findViewById<View>(R.id.divider).visibility = View.VISIBLE
        return view
    }

    private fun makeNavDrawerItem(titleId: Int, iconId: Int, container: ViewGroup): View {
        val view = layoutInflater.inflate(R.layout.row_drawer, container, false)

        val titleView = view.findViewById<TextView>(android.R.id.title)
        val iconView = view.findViewById<ImageView>(android.R.id.icon)

        titleView.setText(titleId)
        if (iconId != 0) {
            iconView.setImageResource(iconId)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
        if (titleId == getDrawerResId()) {
            view.setBackgroundResource(R.color.navdrawer_selected_row)
            titleView.setTextColor(ContextCompat.getColor(this, R.color.primary))
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        } else {
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.navdrawer_icon_tint))
        }

        view.setOnClickListener { selectItem(titleId) }

        return view
    }
}
