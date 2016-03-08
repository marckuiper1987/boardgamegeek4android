package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

import butterknife.InjectView;

/**
 * A navigation drawer activity that displays a hero image.
 */
public abstract class HeroActivity extends DrawerActivity implements OnRefreshListener {
	private static final String TAG_SINGLE_PANE = "single_pane";
	private Fragment fragment;
	private boolean isRefreshing;

	@SuppressWarnings("unused") @InjectView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@SuppressWarnings("unused") @InjectView(R.id.coordinator) CoordinatorLayout coordinator;
	@SuppressWarnings("unused") @InjectView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;
	@SuppressWarnings("unused") @InjectView(R.id.toolbar_image) ImageView toolbarImage;
	@SuppressWarnings("unused") @InjectView(R.id.scrim) View scrimView;
	@SuppressWarnings("unused") @InjectView(R.id.fab) FloatingActionButton fab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		if (savedInstanceState == null) {
			createFragment();
		} else {
			fragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}
	}

	@Override
	protected void onPostInject() {
		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);
	}

	protected void createFragment() {
		fragment = onCreatePane(getIntent());
		if (fragment != null) {
			fragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.root_container, fragment, TAG_SINGLE_PANE)
				.commit();
		}
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane(Intent intent);

	protected Fragment getFragment() {
		return fragment;
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.activity_hero;
	}

	protected void safelySetTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
			collapsingToolbar.setTitle(title);
		}
	}

	@Override
	public void onRefresh() {
		//No-op; just here for the children
	}

	protected void updateRefreshStatus(boolean refreshing) {
		this.isRefreshing = refreshing;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}
}