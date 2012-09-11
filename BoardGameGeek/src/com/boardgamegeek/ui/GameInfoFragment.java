package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class GameInfoFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(GameInfoFragment.class);

	private Uri mGameUri;
	private ImageFetcher mImageFetcher;
	private ImageView mThumbnailView;
	private TextView mNameView;
	private TextView mUnratedView;
	private RatingBar mRatingBar;
	private TextView mRatingView;
	private TextView mRatingDenomView;
	private TextView mNumberRatingView;
	private TextView mIdView;
	private TextView mUpdatedView;
	private TextView mDescriptionView;
	private TextView mRankView;
	private TextView mYearPublishedView;
	private TextView mPlayersView;
	private TextView mPlayingTimeView;
	private TextView mSuggestedAgesView;
	private View mDesignersRoot;
	private TextView mDesignersView;
	private View mArtistsRoot;
	private TextView mArtistsView;
	private View mPublishersRoot;
	private TextView mPublishersView;
	private View mCategoriesRoot;
	private TextView mCategoriesView;
	private View mMechanicsRoot;
	private TextView mMechanicsView;
	private View mExpansionsRoot;
	private TextView mExpansionsView;
	private LinearLayout mRankRoot;
	private TextView mRatingsCount;
	private StatBar mAverageStatBar;
	private StatBar mBayesAverageBar;
	private StatBar mMedianBar;
	private StatBar mStdDevBar;
	private TextView mWeightCount;
	private StatBar mWeightBar;
	private TextView mUserCount;
	private StatBar mNumOwningBar;
	private StatBar mNumRatingBar;
	private StatBar mNumTradingBar;
	private StatBar mNumWantingBar;
	private StatBar mNumWishingBar;
	private StatBar mNumWeightingBar;

	boolean mIsDescriptionExpanded;
	private NumberFormat mFormat = NumberFormat.getInstance();
	private float mRankTextSize;
	private int mHPadding;
	private int mVPadding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameUri = intent.getData();

		if (mGameUri == null) {
			return;
		}

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setImageFadeIn(false);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_size));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_info, null);

		mNameView = (TextView) rootView.findViewById(R.id.game_info_name);
		mThumbnailView = (ImageView) rootView.findViewById(R.id.game_info_thumbnail);
		mUnratedView = (TextView) rootView.findViewById(R.id.game_info_rating_unrated);
		mRatingBar = (RatingBar) rootView.findViewById(R.id.game_info_rating_stars);
		mRatingView = (TextView) rootView.findViewById(R.id.game_info_rating);
		mRatingDenomView = (TextView) rootView.findViewById(R.id.game_info_rating_denom);
		mNumberRatingView = (TextView) rootView.findViewById(R.id.game_info_rating_count);
		mIdView = (TextView) rootView.findViewById(R.id.game_info_id);
		mUpdatedView = (TextView) rootView.findViewById(R.id.game_info_last_updated);
		mDescriptionView = (TextView) rootView.findViewById(R.id.game_info_description);

		mRankView = (TextView) rootView.findViewById(R.id.game_info_rank);
		mYearPublishedView = (TextView) rootView.findViewById(R.id.game_info_year);
		mPlayersView = (TextView) rootView.findViewById(R.id.game_info_num_of_players);
		mPlayingTimeView = (TextView) rootView.findViewById(R.id.game_info_playing_time);
		mSuggestedAgesView = (TextView) rootView.findViewById(R.id.game_info_suggested_ages);

		mDesignersRoot = rootView.findViewById(R.id.game_info_designers_root);
		mDesignersView = (TextView) rootView.findViewById(R.id.game_info_designers_data);
		mArtistsRoot = rootView.findViewById(R.id.game_info_artists_root);
		mArtistsView = (TextView) rootView.findViewById(R.id.game_info_artists_data);
		mPublishersRoot = rootView.findViewById(R.id.game_info_publishers_root);
		mPublishersView = (TextView) rootView.findViewById(R.id.game_info_publishers_data);
		mCategoriesRoot = rootView.findViewById(R.id.game_info_categories_root);
		mCategoriesView = (TextView) rootView.findViewById(R.id.game_info_categories_data);
		mMechanicsRoot = rootView.findViewById(R.id.game_info_mechanics_root);
		mMechanicsView = (TextView) rootView.findViewById(R.id.game_info_mechanics_data);
		mExpansionsRoot = rootView.findViewById(R.id.game_info_expansions_root);
		mExpansionsView = (TextView) rootView.findViewById(R.id.game_info_expansions_data);

		mRankRoot = (LinearLayout) rootView.findViewById(R.id.game_info_rank_root);

		mRatingsCount = (TextView) rootView.findViewById(R.id.statsRatingCount);
		mAverageStatBar = (StatBar) rootView.findViewById(R.id.average_bar);
		mBayesAverageBar = (StatBar) rootView.findViewById(R.id.bayes_bar);
		mMedianBar = (StatBar) rootView.findViewById(R.id.median_bar);
		mStdDevBar = (StatBar) rootView.findViewById(R.id.stddev_bar);

		mWeightCount = (TextView) rootView.findViewById(R.id.statsWeightCount);
		mWeightBar = (StatBar) rootView.findViewById(R.id.weight_bar);

		mUserCount = (TextView) rootView.findViewById(R.id.game_info_users_count);
		mNumOwningBar = (StatBar) rootView.findViewById(R.id.game_info_owning_bar);
		mNumRatingBar = (StatBar) rootView.findViewById(R.id.game_info_rating_bar);
		mNumTradingBar = (StatBar) rootView.findViewById(R.id.game_info_trading_bar);
		mNumWantingBar = (StatBar) rootView.findViewById(R.id.game_info_wanting_bar);
		mNumWishingBar = (StatBar) rootView.findViewById(R.id.game_info_wishing_bar);
		mNumWeightingBar = (StatBar) rootView.findViewById(R.id.game_info_weighting_bar);

		mRankTextSize = getResources().getDimension(R.dimen.text_size_small);
		mHPadding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		mVPadding = getResources().getDimensionPixelSize(R.dimen.padding_small);
		mVPadding = (int) (getResources().getDimension(R.dimen.padding_small) / getResources().getDisplayMetrics().density);

		mDescriptionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsDescriptionExpanded) {
					mDescriptionView.setMaxLines(3);
				} else {
					mDescriptionView.setMaxLines(Integer.MAX_VALUE);
				}
				mIsDescriptionExpanded = !mIsDescriptionExpanded;
			}
		});

		rootView.findViewById(R.id.game_info_num_of_players_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startPoll(PollActivity.SUGGESTED_NUMPLAYERS);
			}
		});

		rootView.findViewById(R.id.game_info_suggested_ages_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startPoll(PollActivity.SUGGESTED_PLAYERAGE);
			}
		});

		rootView.findViewById(R.id.game_info_languages_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startPoll(PollActivity.LANGUAGE_DEPENDENCE);
			}
		});

		LoaderManager lm = getLoaderManager();
		lm.restartLoader(GameQuery._TOKEN, null, this);
		lm.restartLoader(DesignerQuery._TOKEN, null, this);
		lm.restartLoader(ArtistQuery._TOKEN, null, this);
		lm.restartLoader(PublisherQuery._TOKEN, null, this);
		lm.restartLoader(CategoryQuery._TOKEN, null, this);
		lm.restartLoader(MechanicQuery._TOKEN, null, this);
		lm.restartLoader(ExpansionQuery._TOKEN, null, this);
		lm.restartLoader(RankQuery._TOKEN, null, this);

		return rootView;
	}

	private void startPoll(String type) {
		Intent intent = new Intent(getActivity(), PollActivity.class);
		intent.putExtra(PollActivity.KEY_GAME_ID, Games.getGameId(mGameUri));
		intent.putExtra(PollActivity.KEY_TYPE, type);
		startActivity(intent);
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mGameUri, GameQuery.PROJECTION, null, null, null);
				break;
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildDesignersUri(Games.getGameId(mGameUri)),
					DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildArtistsUri(Games.getGameId(mGameUri)),
					ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildPublishersUri(Games.getGameId(mGameUri)),
					PublisherQuery.PROJECTION, null, null, null);
				break;
			case CategoryQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildCategoriesUri(Games.getGameId(mGameUri)),
					CategoryQuery.PROJECTION, null, null, null);
				break;
			case MechanicQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildMechanicsUri(Games.getGameId(mGameUri)),
					MechanicQuery.PROJECTION, null, null, null);
				break;
			case ExpansionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(Games.getGameId(mGameUri)),
					ExpansionQuery.PROJECTION, null, null, null);
				break;
			case RankQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildRanksUri(Games.getGameId(mGameUri)),
					RankQuery.PROJECTION, null, null, null);
			default:
				LOGW(TAG, "Invalid query token=" + id);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case GameQuery._TOKEN:
				onGameQueryComplete(cursor);
				break;
			case DesignerQuery._TOKEN:
				onListQueryComplete(cursor, mDesignersRoot, mDesignersView, DesignerQuery.DESIGNER_NAME);
				break;
			case ArtistQuery._TOKEN:
				onListQueryComplete(cursor, mArtistsRoot, mArtistsView, ArtistQuery.ARTIST_NAME);
				break;
			case PublisherQuery._TOKEN:
				onListQueryComplete(cursor, mPublishersRoot, mPublishersView, PublisherQuery.PUBLISHER_NAME);
				break;
			case CategoryQuery._TOKEN:
				onListQueryComplete(cursor, mCategoriesRoot, mCategoriesView, CategoryQuery.CATEGORY_NAME);
				break;
			case MechanicQuery._TOKEN:
				onListQueryComplete(cursor, mMechanicsRoot, mMechanicsView, MechanicQuery.MECHANIC_NAME);
				break;
			case ExpansionQuery._TOKEN:
				onListQueryComplete(cursor, mExpansionsRoot, mExpansionsView, ExpansionQuery.EXPANSION_NAME);
				break;
			case RankQuery._TOKEN:
				onRankQueryComplete(cursor);
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	private void onGameQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		Game game = new Game(cursor);

		mNameView.setText(game.Name);
		formatRating(game);
		mIdView.setText(String.valueOf(game.Id));
		mUpdatedView.setText(game.getUpdatedDescription());
		UIUtils.setTextMaybeHtml(mDescriptionView, game.Description);
		mRankView.setText(game.getRankDescription());
		mYearPublishedView.setText(game.getYearPublished());
		mPlayingTimeView.setText(game.getPlayingTimeDescription());
		mPlayersView.setText(game.getPlayerRangeDescription());
		mSuggestedAgesView.setText(game.getAgeDescription());

		mImageFetcher.loadAvatarImage(game.ThumbnailUrl, Games.buildThumbnailUri(game.Id), mThumbnailView,
			R.drawable.thumbnail_image_empty);

		mRatingsCount.setText(String.format(getResources().getString(R.string.rating_count),
			mFormat.format(game.UsersRated)));
		mAverageStatBar.setBar(R.string.average_meter_text, game.Rating);
		mBayesAverageBar.setBar(R.string.bayes_meter_text, game.BayesAverage);
		if (game.Median <= 0) {
			mMedianBar.setVisibility(View.GONE);
		} else {
			mMedianBar.setVisibility(View.VISIBLE);
			mMedianBar.setBar(R.string.median_meter_text, game.Median);
		}
		mStdDevBar.setBar(R.string.stdDev_meter_text, game.StandardDeviation, 5.0);

		mWeightCount.setText(String.format(getResources().getString(R.string.weight_count),
			mFormat.format(game.NumberWeights)));
		mWeightBar.setBar(game.getWeightDescriptionResId(), game.AverageWeight, 5.0, 1.0);

		mUserCount.setText(String.format(getResources().getString(R.string.user_total),
			mFormat.format(game.getMaxUsers())));
		mNumOwningBar.setBar(R.string.owning_meter_text, game.NumberOwned, game.getMaxUsers());
		mNumRatingBar.setBar(R.string.rating_meter_text, game.UsersRated, game.getMaxUsers());
		mNumTradingBar.setBar(R.string.trading_meter_text, game.NumberTrading, game.getMaxUsers());
		mNumWantingBar.setBar(R.string.wanting_meter_text, game.NumberWanting, game.getMaxUsers());
		mNumWishingBar.setBar(R.string.wishing_meter_text, game.NumberWishing, game.getMaxUsers());
		mNumWeightingBar.setBar(R.string.weighting_meter_text, game.NumberWeights, game.getMaxUsers());
	}

	private void onListQueryComplete(Cursor cursor, View root, TextView data, int columnIndex) {
		if (cursor == null || cursor.getCount() == 0) {
			root.setVisibility(View.GONE);
		} else {
			root.setVisibility(View.VISIBLE);

			String list = TextUtils.join(", ", CursorUtils.getStringArray(cursor, columnIndex));
			TextPaint paint = new TextPaint();
			paint.setTextSize(data.getTextSize());
			CharSequence text = TextUtils.commaEllipsize(list, paint, data.getWidth(), "1 more", "%d more");
			if (TextUtils.isEmpty(text)) {
				text = String.format("%d more", cursor.getCount());
			}
			data.setText(text);
		}
	}

	private void formatRating(Game game) {
		mRatingBar.setRating(game.Rating);
		mRatingView.setText(game.getRatingDescription());
		mNumberRatingView.setText(String.valueOf(game.UsersRated));

		mRatingBar.setVisibility(game.UsersRated == 0 ? View.GONE : View.VISIBLE);
		mRatingDenomView.setVisibility(game.UsersRated == 0 ? View.GONE : View.VISIBLE);
		mRatingView.setVisibility(game.UsersRated == 0 ? View.GONE : View.VISIBLE);
		mRatingsCount.setVisibility(game.UsersRated == 0 ? View.GONE : View.VISIBLE);
		mNumberRatingView.setVisibility(game.UsersRated == 0 ? View.GONE : View.VISIBLE);
		mUnratedView.setVisibility(game.UsersRated == 0 ? View.VISIBLE : View.GONE);
	}

	private void onRankQueryComplete(Cursor cursor) {
		mRankRoot.removeAllViews();
		if (cursor == null || cursor.getCount() == 0) {
			mRankRoot.setVisibility(View.GONE);
		} else {
			mRankRoot.setVisibility(View.VISIBLE);

			while (cursor.moveToNext()) {
				Rank rank = new Rank(cursor);
				addRankRow(rank.Name, rank.Rank, "subtype".equals(rank.Type), rank.Rating);
			}
		}
	}

	private void addRankRow(String label, int rank, boolean bold, double rating) {
		// TODO: move this to XML
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView tv = new TextView(getActivity(), null, R.style.StatsHeading);
		tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT));
		tv.setPadding(mHPadding, mVPadding, mVPadding, mVPadding);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRankTextSize);
		setText(tv, label, bold);
		layout.addView(tv);

		tv = new TextView(getActivity());
		tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT));
		// tv.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
		tv.setPadding(mHPadding, mVPadding, mHPadding, mVPadding);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRankTextSize);
		tv.setGravity(Gravity.RIGHT);
		String rankText = (rank == 0) ? getResources().getString(R.string.text_not_available) : String.valueOf(rank);
		setText(tv, rankText, bold);
		layout.addView(tv);

		StatBar sb = new StatBar(getActivity());
		sb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT));
		sb.setPadding(mHPadding, mVPadding, mHPadding, mVPadding);
		sb.setBar(R.string.average_meter_text, rating);

		mRankRoot.addView(layout);
		mRankRoot.addView(sb);
	}

	private void setText(TextView tv, String text, boolean bold) {
		if (bold) {
			SpannableString ss = new SpannableString(text);
			ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tv.setText(ss);
		} else {
			tv.setText(text);
		}
	}

	private interface GameQuery {
		int _TOKEN = 0x11;

		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
			Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED,
			Games.UPDATED, GameRanks.GAME_RANK_VALUE, Games.GAME_NAME, Games.THUMBNAIL_URL, Games.STATS_BAYES_AVERAGE,
			Games.STATS_MEDIAN, Games.STATS_STANDARD_DEVIATION, Games.STATS_NUMBER_WEIGHTS, Games.STATS_AVERAGE_WEIGHT,
			Games.STATS_NUMBER_OWNED, Games.STATS_NUMBER_TRADING, Games.STATS_NUMBER_WANTING,
			Games.STATS_NUMBER_WISHING, Games.STATS_USERS_RATED };

		int GAME_ID = 0;
		int STATS_AVERAGE = 1;
		int YEAR_PUBLISHED = 2;
		int MIN_PLAYERS = 3;
		int MAX_PLAYERS = 4;
		int PLAYING_TIME = 5;
		int MINIMUM_AGE = 6;
		int DESCRIPTION = 7;
		int STATS_USERS_RATED = 8;
		int UPDATED = 9;
		int GAME_RANK_VALUE = 10;
		int GAME_NAME = 11;
		int THUMBNAIL_URL = 12;
		int STATS_BAYES_AVERAGE = 13;
		int STATS_MEDIAN = 14;
		int STATS_STANDARD_DEVIATION = 15;
		int STATS_NUMBER_WEIGHTS = 16;
		int STATS_AVERAGE_WEIGHT = 17;
		int STATS_NUMBER_OWNED = 18;
		int STATS_NUMBER_TRADING = 19;
		int STATS_NUMBER_WANTING = 20;
		int STATS_NUMBER_WISHING = 21;
	}

	private interface DesignerQuery {
		int _TOKEN = 0x12;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME };
		int DESIGNER_NAME = 1;
	}

	private interface ArtistQuery {
		int _TOKEN = 0x13;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME };
		int ARTIST_NAME = 1;
	}

	private interface PublisherQuery {
		int _TOKEN = 0x14;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME };
		int PUBLISHER_NAME = 1;
	}

	private interface CategoryQuery {
		int _TOKEN = 0x15;
		String[] PROJECTION = { Categories.CATEGORY_ID, Categories.CATEGORY_NAME };
		int CATEGORY_NAME = 1;
	}

	private interface MechanicQuery {
		int _TOKEN = 0x16;
		String[] PROJECTION = { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME };
		int MECHANIC_NAME = 1;
	}

	private interface ExpansionQuery {
		int _TOKEN = 0x17;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME };
		int EXPANSION_NAME = 1;
	}

	private interface RankQuery {
		int _TOKEN = 0x18;
		String[] PROJECTION = { GameRanks.GAME_RANK_FRIENDLY_NAME, GameRanks.GAME_RANK_VALUE, GameRanks.GAME_RANK_TYPE,
			GameRanks.GAME_RANK_BAYES_AVERAGE };
		int GAME_RANK_FRIENDLY_NAME = 0;
		int GAME_RANK_VALUE = 1;
		int GAME_RANK_TYPE = 2;
		int GAME_RANK_BAYES_AVERAGE = 3;
	}

	private class Game {
		String Name;
		String ThumbnailUrl;
		int Id;
		float Rating;
		int YearPublished;
		int MinPlayers;
		int MaxPlayers;
		int PlayingTime;
		int MinimumAge;
		String Description;
		int UsersRated;
		long Updated;
		int Rank;
		double BayesAverage;
		double Median;
		double StandardDeviation;
		double AverageWeight;
		int NumberWeights;
		int NumberOwned;
		int NumberTrading;
		int NumberWanting;
		int NumberWishing;

		public Game(Cursor cursor) {
			Name = cursor.getString(GameQuery.GAME_NAME);
			ThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
			Id = cursor.getInt(GameQuery.GAME_ID);
			Rating = (float) cursor.getDouble(GameQuery.STATS_AVERAGE);
			YearPublished = cursor.getInt(GameQuery.YEAR_PUBLISHED);
			MinPlayers = cursor.getInt(GameQuery.MIN_PLAYERS);
			MaxPlayers = cursor.getInt(GameQuery.MAX_PLAYERS);
			PlayingTime = cursor.getInt(GameQuery.PLAYING_TIME);
			MinimumAge = cursor.getInt(GameQuery.MINIMUM_AGE);
			Description = cursor.getString(GameQuery.DESCRIPTION);
			UsersRated = 0;// cursor.getInt(GameQuery.STATS_USERS_RATED);
			Updated = 0;// cursor.getLong(GameQuery.UPDATED);
			Rank = cursor.getInt(GameQuery.GAME_RANK_VALUE);
			BayesAverage = cursor.getDouble(GameQuery.STATS_BAYES_AVERAGE);
			Median = cursor.getDouble(GameQuery.STATS_MEDIAN);
			StandardDeviation = cursor.getDouble(GameQuery.STATS_STANDARD_DEVIATION);
			AverageWeight = cursor.getDouble(GameQuery.STATS_AVERAGE_WEIGHT);
			NumberWeights = cursor.getInt(GameQuery.STATS_NUMBER_WEIGHTS);
			NumberOwned = cursor.getInt(GameQuery.STATS_NUMBER_OWNED);
			NumberTrading = cursor.getInt(GameQuery.STATS_NUMBER_TRADING);
			NumberWanting = cursor.getInt(GameQuery.STATS_NUMBER_WANTING);
			NumberWishing = cursor.getInt(GameQuery.STATS_NUMBER_WISHING);
		}

		public String getAgeDescription() {
			if (MinimumAge > 0) {
				return MinimumAge + " " + getResources().getString(R.string.age_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		public int getMaxUsers() {
			int max = Math.max(UsersRated, NumberOwned);
			max = Math.max(max, NumberTrading);
			max = Math.max(max, NumberWanting);
			max = Math.max(max, NumberWeights);
			max = Math.max(max, NumberWishing);
			return max;
		}

		private String getPlayerRangeDescription() {
			if (MinPlayers == 0 && MaxPlayers == 0) {
				return getResources().getString(R.string.text_unknown);
			} else if (MinPlayers >= MaxPlayers) {
				return String.valueOf(MinPlayers);
			} else {
				return String.valueOf(MinPlayers) + " - " + String.valueOf(MaxPlayers);
			}
		}

		private String getPlayingTimeDescription() {
			if (PlayingTime > 0) {
				return PlayingTime + " " + getResources().getString(R.string.time_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		private String getRankDescription() {
			if (Rank == 0) {
				return getString(R.string.text_not_available);
			} else {
				return String.valueOf(Rank);
			}
		}

		public String getRatingDescription() {
			return new DecimalFormat("#0.00").format(Rating);
		}

		public String getYearPublished() {
			if (YearPublished == 0) {
				return getResources().getString(R.string.text_unknown);
			}
			return String.valueOf(YearPublished);
		}

		public CharSequence getUpdatedDescription() {
			if (Updated == 0) {
				return "Needs updating";
			}
			return DateUtils.getRelativeTimeSpanString(Updated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
		}

		public int getWeightDescriptionResId() {
			int resId = R.string.weight_1_text;
			if (AverageWeight >= 4.5) {
				resId = R.string.weight_5_text;
			} else if (AverageWeight >= 3.5) {
				resId = R.string.weight_4_text;
			} else if (AverageWeight >= 2.5) {
				resId = R.string.weight_3_text;
			} else if (AverageWeight >= 1.5) {
				resId = R.string.weight_2_text;
			}
			return resId;
		}
	}

	private class Rank {
		String Name;
		int Rank;
		double Rating;
		String Type;

		Rank(Cursor cursor) {
			Name = cursor.getString(RankQuery.GAME_RANK_FRIENDLY_NAME);
			Rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
			Rating = cursor.getDouble(RankQuery.GAME_RANK_BAYES_AVERAGE);
			Type = cursor.getString(RankQuery.GAME_RANK_TYPE);
		}
	}
}