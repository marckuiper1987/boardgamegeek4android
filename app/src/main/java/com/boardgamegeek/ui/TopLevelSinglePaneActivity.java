package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

public abstract class TopLevelSinglePaneActivity extends TopLevelActivity {
	private static final String TAG_SINGLE_PANE = "single_pane";
	private Fragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			parseIntent(getIntent());
		} else {
			fragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		parseIntent(intent);
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane();

	public Fragment getFragment() {
		return fragment;
	}

	private void parseIntent(Intent intent) {
		fragment = onCreatePane();
		fragment.setArguments(UIUtils.intentToFragmentArguments(intent));
		getSupportFragmentManager().beginTransaction().add(R.id.root_container, fragment, TAG_SINGLE_PANE).commit();
	}
}
