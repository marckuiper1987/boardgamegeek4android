package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteArtistHandler;
import com.boardgamegeek.io.RemoteDesignerHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePublisherHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameListsActivityTab extends ExpandableListActivity implements AsyncQueryListener {
	private static final String TAG = "GameListsActivityTab";

	private static final int ID_DIALOG_RESULTS = 1;

	private static final int TOKEN_DESIGNERS = 1;
	private static final int TOKEN_DESIGNERS_UPDATE = 2;
	private static final int TOKEN_ARTISTS = 3;
	private static final int TOKEN_ARTISTS_UPDATE = 4;
	private static final int TOKEN_PUBLISHERS = 5;
	private static final int TOKEN_PUBLISHERS_UPDATE = 6;
	private static final int TOKEN_MECHANICS = 7;
	private static final int TOKEN_CATEGORIES = 8;
	private static final int TOKEN_EXPANSIONS = 9;
	private static final int TOKEN_BASE_GAMES = 10;

	private static final int GROUP_DESIGNERS = 0;
	private static final int GROUP_ARTISTS = 1;
	private static final int GROUP_PUBLISHERS = 2;
	private static final int GROUP_MECHANICS = 3;
	private static final int GROUP_CATEGORIES = 4;
	private static final int GROUP_EXPANSIONS = 5;
	private static final int GROUP_BASE_GAMES = 6;
	private static final int GROUP_TOTAL_COUNT = 7;

	private static final String KEY_NAME = "NAME";
	private static final String KEY_COUNT = "COUNT";
	private static final String KEY_DESCRIPTION = "DESCRIPTION";

	private int mPadding;
	private Uri mDesignersUri;
	private Uri mArtistsUri;
	private Uri mPublishersUri;
	private Uri mMechanicsUri;
	private Uri mCategoriesUri;
	private Uri mExpansionsUri;
	private NotifyingAsyncQueryHandler mHandler;

	private DesignersObserver mDesignersObserver;
	private ArtistsObserver mArtistsObserver;
	private PublishersObserver mPublishersObserver;
	private MechanicsObserver mMechanicsObserver;
	private CategoriesObserver mCategoriesObserver;
	private ExpansionsObserver mExpansionsObserver;

	private List<Map<String, String>> mGroupData;
	private List<List<Map<String, String>>> mChildData;
	private ExpandableListAdapter mAdapter;
	private ExpandableListView mListView;
	private boolean[] mGroupStatus;

	private String mName;
	private String mDescription;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPadding = (int) getResources().getDimension(R.dimen.padding_standard);
		initializeGroupData();
		setAndObserveUris();
	}

	@Override
	protected void onStart() {
		super.onStart();
		final ContentResolver cr = getContentResolver();
		cr.registerContentObserver(mDesignersUri, true, mDesignersObserver);
		cr.registerContentObserver(mArtistsUri, true, mArtistsObserver);
		cr.registerContentObserver(mPublishersUri, true, mPublishersObserver);
		cr.registerContentObserver(mMechanicsUri, true, mMechanicsObserver);
		cr.registerContentObserver(mCategoriesUri, true, mCategoriesObserver);
		cr.registerContentObserver(mExpansionsUri, true, mExpansionsObserver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startQueries();
	}

	@Override
	protected void onStop() {
		final ContentResolver cr = getContentResolver();
		cr.unregisterContentObserver(mDesignersObserver);
		cr.unregisterContentObserver(mArtistsObserver);
		cr.unregisterContentObserver(mPublishersObserver);
		cr.unregisterContentObserver(mMechanicsObserver);
		cr.unregisterContentObserver(mCategoriesObserver);
		cr.unregisterContentObserver(mExpansionsObserver);
		super.onStop();
	}

	private void setAndObserveUris() {
		final Uri gameUri = getIntent().getData();
		final int gameId = Games.getGameId(gameUri);
		mDesignersUri = Games.buildDesignersUri(gameId);
		mArtistsUri = Games.buildArtistsUri(gameId);
		mPublishersUri = Games.buildPublishersUri(gameId);
		mMechanicsUri = Games.buildMechanicsUri(gameId);
		mCategoriesUri = Games.buildCategoriesUri(gameId);
		mExpansionsUri = Games.buildExpansionsUri(gameId);

		mDesignersObserver = new DesignersObserver(new Handler());
		mArtistsObserver = new ArtistsObserver(new Handler());
		mPublishersObserver = new PublishersObserver(new Handler());
		mMechanicsObserver = new MechanicsObserver(new Handler());
		mCategoriesObserver = new CategoriesObserver(new Handler());
		mExpansionsObserver = new ExpansionsObserver(new Handler());
	}

	private void startQueries() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(TOKEN_DESIGNERS, null, mDesignersUri, DesignerQuery.PROJECTION, null, null,
				Designers.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_ARTISTS, null, mArtistsUri, ArtistQuery.PROJECTION, null, null, Artists.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_PUBLISHERS, null, mPublishersUri, PublisherQuery.PROJECTION, null, null,
				Publishers.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_MECHANICS, null, mMechanicsUri, MechanicQuery.PROJECTION, null, null,
				Mechanics.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_CATEGORIES, null, mCategoriesUri, CategoryQuery.PROJECTION, null, null,
				Categories.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_EXPANSIONS, null, mExpansionsUri, ExpansionQuery.PROJECTION, GamesExpansions.INBOUND
				+ "=?", new String[] { "0" }, GamesExpansions.DEFAULT_SORT);
		mHandler.startQuery(TOKEN_BASE_GAMES, null, mExpansionsUri, ExpansionQuery.PROJECTION, GamesExpansions.INBOUND
				+ "=?", new String[] { "1" }, GamesExpansions.DEFAULT_SORT);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

		removeDialog(ID_DIALOG_RESULTS);

		Map<String, Object> childItem = (Map<String, Object>) mAdapter.getChild(groupPosition, childPosition);
		mName = (String) childItem.get(KEY_NAME);
		mDescription = (String) childItem.get(KEY_DESCRIPTION);
		if (groupPosition == GROUP_EXPANSIONS || groupPosition == GROUP_BASE_GAMES) {
			Uri gameUri = Games.buildGameUri(Integer.valueOf(mDescription).intValue());
			Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
			intent.putExtra(BoardgameActivity.KEY_GAME_NAME, mName);
			startActivity(intent);
		} else {
			if (TextUtils.isEmpty(mDescription)) {
				showToast(R.string.msg_no_description);
			} else {
				showDialog(ID_DIALOG_RESULTS);
			}
		}

		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_RESULTS) {
			Dialog dialog = new Dialog(this);
			dialog.setTitle(mName);
			WebView webView = new WebView(this);
			webView.loadDataWithBaseURL(null, mDescription, "text/html", "UTF-8", null);
			WebSettings webSettings = webView.getSettings();
			webSettings.setDefaultFontSize((int) (getResources().getDimension(R.dimen.text_size_small) / getResources()
					.getDisplayMetrics().density));
			ScrollView scrollView = new ScrollView(this);
			scrollView.setPadding(mPadding, mPadding, mPadding, mPadding);
			scrollView.addView(webView);
			dialog.setContentView(scrollView);
			dialog.setCancelable(true);
			return dialog;
		}
		return super.onCreateDialog(id);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_DESIGNERS || token == TOKEN_DESIGNERS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> designers = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_DESIGNERS) {
						int id = cursor.getInt(DesignerQuery.DESIGNER_ID);
						getContentResolver().registerContentObserver(Designers.buildDesignerUri(id), true,
								new DesignerObserver(new Handler()));
						addId(cursor, ids, id, DesignerQuery.UPDATED);
					}
					addChildItem(cursor, designers, DesignerQuery.DESIGNER_NAME, DesignerQuery.DESIGNER_DESCRIPTION);
				}
				updateGroup(GROUP_DESIGNERS, designers);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new DesignerTask().execute(array);
				}
			} else if (token == TOKEN_ARTISTS || token == TOKEN_ARTISTS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> artists = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_ARTISTS) {
						int id = cursor.getInt(ArtistQuery.ARTIST_ID);
						getContentResolver().registerContentObserver(Artists.buildArtistUri(id), true,
								new ArtistObserver(new Handler()));
						addId(cursor, ids, id, ArtistQuery.UPDATED);
					}
					addChildItem(cursor, artists, ArtistQuery.ARTIST_NAME, ArtistQuery.ARTIST_DESCRIPTION);
				}
				updateGroup(GROUP_ARTISTS, artists);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new ArtistTask().execute(array);
				}
			} else if (token == TOKEN_PUBLISHERS || token == TOKEN_PUBLISHERS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> publishers = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_PUBLISHERS) {
						int id = cursor.getInt(PublisherQuery.PUBLISHER_ID);
						getContentResolver().registerContentObserver(Publishers.buildPublisherUri(id), true,
								new PublisherObserver(new Handler()));
						addId(cursor, ids, id, PublisherQuery.UPDATED);
					}
					addChildItem(cursor, publishers, PublisherQuery.PUBLISHER_NAME,
							PublisherQuery.PUBLISHER_DESCRIPTION);
				}
				updateGroup(GROUP_PUBLISHERS, publishers);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new PublisherTask().execute(array);
				}
			} else if (token == TOKEN_EXPANSIONS) {
				List<ChildItem> expansions = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, expansions, ExpansionQuery.EXPANSION_NAME, ExpansionQuery.EXPANSION_ID);
				}
				updateGroup(GROUP_EXPANSIONS, expansions);
			} else if (token == TOKEN_BASE_GAMES) {
				List<ChildItem> baseGames = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, baseGames, ExpansionQuery.EXPANSION_NAME, ExpansionQuery.EXPANSION_ID);
				}
				updateGroup(GROUP_BASE_GAMES, baseGames);
			} else if (token == TOKEN_MECHANICS) {
				List<ChildItem> mechanics = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, mechanics, MechanicQuery.MECHANIC_NAME);
				}
				updateGroup(GROUP_MECHANICS, mechanics);
			} else if (token == TOKEN_CATEGORIES) {
				List<ChildItem> categories = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, categories, CategoryQuery.CATEGORY_NAME);
				}
				updateGroup(GROUP_CATEGORIES, categories);
			}

			mAdapter = new SimpleExpandableListAdapter(this, mGroupData, R.layout.grouprow, new String[] { KEY_NAME,
					KEY_COUNT }, new int[] { R.id.name, R.id.count }, mChildData, R.layout.childrow,
					new String[] { KEY_NAME }, new int[] { R.id.name });
			setListAdapter(mAdapter);

			restoreListView();
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void restoreListView() {
		ensureListView();
		ensureGroupStatus();
		for (int i = 0; i < mGroupStatus.length; i++) {
			if (mGroupStatus[i]) {
				mListView.expandGroup(i);
			}
		}
	}

	@Override
	public void onGroupExpand(int groupPosition) {
		ensureGroupStatus();
		mGroupStatus[groupPosition] = true;
		super.onGroupExpand(groupPosition);
	}

	@Override
	public void onGroupCollapse(int groupPosition) {
		ensureGroupStatus();
		mGroupStatus[groupPosition] = false;
		super.onGroupCollapse(groupPosition);
	}

	private void ensureListView() {
		if (mListView == null) {
			mListView = (ExpandableListView) findViewById(android.R.id.list);
		}
	}

	private void ensureGroupStatus() {
		if (mGroupStatus == null) {
			mGroupStatus = new boolean[GROUP_TOTAL_COUNT];
		}
	}

	private void addId(Cursor cursor, List<Integer> list, int id, int updatedColumnIndex) {
		long lastUpdated = cursor.getLong(updatedColumnIndex);
		if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > 14) {
			list.add(id);
		}
	}

	private void addChildItem(Cursor cursor, List<ChildItem> list, int nameColumnIndex, int descriptionColumnIndex) {
		final ChildItem childItem = new ChildItem();
		childItem.Name = cursor.getString(nameColumnIndex);
		childItem.Description = cursor.getString(descriptionColumnIndex);
		list.add(childItem);
	}

	private void addChildItem(Cursor cursor, List<ChildItem> list, int nameColumnIndex) {
		final ChildItem childItem = new ChildItem();
		childItem.Name = cursor.getString(nameColumnIndex);
		childItem.Description = null;
		list.add(childItem);
	}

	private void initializeGroupData() {
		mGroupData = new ArrayList<Map<String, String>>();
		mChildData = new ArrayList<List<Map<String, String>>>();

		createGroup(R.string.designers);
		createGroup(R.string.artists);
		createGroup(R.string.publishers);
		createGroup(R.string.mechanics);
		createGroup(R.string.categories);
		createGroup(R.string.expansions);
		createGroup(R.string.base_games);
	}

	private void createGroup(int nameResourceId) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupMap.put(KEY_NAME, getResources().getString(nameResourceId));
		groupMap.put(KEY_COUNT, "0");
		mGroupData.add(groupMap);
		mChildData.add(new ArrayList<Map<String, String>>());
	}

	private void updateGroup(int group, Collection<ChildItem> children) {
		mGroupData.get(group).put(KEY_COUNT, "" + children.size());

		List<Map<String, String>> childList = mChildData.get(group);
		childList.clear();
		for (ChildItem child : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childList.add(childMap);
			childMap.put(KEY_NAME, child.Name);
			childMap.put(KEY_DESCRIPTION, child.Description);
		}
	}

	class ChildItem {
		String Name;
		String Description;
	}

	class DesignersObserver extends ContentObserver {
		public DesignersObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_DESIGNERS, null, mDesignersUri, DesignerQuery.PROJECTION, null, null,
					Designers.DEFAULT_SORT);
		}
	}

	class DesignerObserver extends ContentObserver {
		public DesignerObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_DESIGNERS_UPDATE, null, mDesignersUri, DesignerQuery.PROJECTION, null, null,
					Designers.DEFAULT_SORT);
		}
	}

	class ArtistsObserver extends ContentObserver {
		public ArtistsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_ARTISTS, null, mArtistsUri, ArtistQuery.PROJECTION, null, null,
					Artists.DEFAULT_SORT);
		}
	}

	class ArtistObserver extends ContentObserver {
		public ArtistObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_ARTISTS_UPDATE, null, mArtistsUri, ArtistQuery.PROJECTION, null, null,
					Artists.DEFAULT_SORT);
		}
	}

	class PublishersObserver extends ContentObserver {
		public PublishersObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_PUBLISHERS, null, mPublishersUri, PublisherQuery.PROJECTION, null, null,
					Publishers.DEFAULT_SORT);
		}
	}

	class PublisherObserver extends ContentObserver {
		public PublisherObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_PUBLISHERS_UPDATE, null, mPublishersUri, PublisherQuery.PROJECTION, null, null,
					Publishers.DEFAULT_SORT);
		}
	}

	class MechanicsObserver extends ContentObserver {
		public MechanicsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_MECHANICS, null, mMechanicsUri, MechanicQuery.PROJECTION, null, null,
					Mechanics.DEFAULT_SORT);
		}
	}

	class CategoriesObserver extends ContentObserver {
		public CategoriesObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_CATEGORIES, null, mCategoriesUri, CategoryQuery.PROJECTION, null, null,
					Categories.DEFAULT_SORT);
		}
	}

	class ExpansionsObserver extends ContentObserver {
		public ExpansionsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.startQuery(TOKEN_EXPANSIONS, null, mExpansionsUri, ExpansionQuery.PROJECTION,
					GamesExpansions.INBOUND + "=?", new String[] { "0" }, GamesExpansions.DEFAULT_SORT);
			mHandler.startQuery(TOKEN_BASE_GAMES, null, mExpansionsUri, ExpansionQuery.PROJECTION,
					GamesExpansions.INBOUND + "=?", new String[] { "1" }, GamesExpansions.DEFAULT_SORT);
		}
	}

	class DesignerTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int designerId : params) {
				Log.d(TAG, "Fetching designer ID = " + designerId);
				final String url = HttpUtils.constructDesignerUrl(designerId);
				try {
					mExecutor.executeGet(url, new RemoteDesignerHandler(designerId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch designer ID = " + designerId, e);
					runOnUiThread(new Runnable() {
						public void run() {
							showToast(R.string.msg_error_remote);
						}
					});
				}
			}
			return null;
		}
	}

	class ArtistTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int artistId : params) {
				Log.d(TAG, "Fetching artist ID = " + artistId);
				final String url = HttpUtils.constructArtistUrl(artistId);
				try {
					mExecutor.executeGet(url, new RemoteArtistHandler(artistId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch artist ID = " + artistId, e);
					runOnUiThread(new Runnable() {
						public void run() {
							showToast(R.string.msg_error_remote);
						}
					});
				}
			}
			return null;
		}
	}

	class PublisherTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int publisherId : params) {
				Log.d(TAG, "Fetching publisher ID = " + publisherId);
				final String url = HttpUtils.constructPublisherUrl(publisherId);
				try {
					mExecutor.executeGet(url, new RemotePublisherHandler(publisherId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch publisher ID = " + publisherId, e);
					runOnUiThread(new Runnable() {
						public void run() {
							showToast(R.string.msg_error_remote);
						}
					});
				}
			}
			return null;
		}
	}

	private void showToast(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	private interface DesignerQuery {
		String[] PROJECTION = { GamesDesigners.DESIGNER_ID, Designers.DESIGNER_NAME, Designers.DESIGNER_DESCRIPTION,
				SyncColumns.UPDATED };

		int DESIGNER_ID = 0;
		int DESIGNER_NAME = 1;
		int DESIGNER_DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface ArtistQuery {
		String[] PROJECTION = { GamesArtists.ARTIST_ID, Artists.ARTIST_NAME, Artists.ARTIST_DESCRIPTION,
				SyncColumns.UPDATED };

		int ARTIST_ID = 0;
		int ARTIST_NAME = 1;
		int ARTIST_DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface PublisherQuery {
		String[] PROJECTION = { GamesPublishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME,
				Publishers.PUBLISHER_DESCRIPTION, SyncColumns.UPDATED };

		int PUBLISHER_ID = 0;
		int PUBLISHER_NAME = 1;
		int PUBLISHER_DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface MechanicQuery {
		String[] PROJECTION = { Mechanics.MECHANIC_NAME };

		int MECHANIC_NAME = 0;
	}

	private interface CategoryQuery {
		String[] PROJECTION = { Categories.CATEGORY_NAME };

		int CATEGORY_NAME = 0;
	}

	private interface ExpansionQuery {
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME };

		int EXPANSION_ID = 0;
		int EXPANSION_NAME = 1;
	}
}
