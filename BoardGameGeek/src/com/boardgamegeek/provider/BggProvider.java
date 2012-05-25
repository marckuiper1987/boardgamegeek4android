package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.SelectionBuilder;

public class BggProvider extends ContentProvider {
	private static final String TAG = "BggProvider";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private BggDatabase mOpenHelper;

	private static UriMatcher sUriMatcher = buildUriMatcher();
	private static HashMap<Integer, BaseProvider> providers = buildProviderMap();
	private static final HashMap<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();

	private static final int GAMES = 100;
	private static final int GAMES_ID = 101;
	private static final int GAMES_RANKS = 102;
	private static final int GAMES_RANKS_ID = 103;
	private static final int GAMES_ID_RANKS = 104;
	private static final int GAMES_ID_RANKS_ID = 1041;
	private static final int GAMES_ID_DESIGNERS = 105;
	private static final int GAMES_ID_DESIGNERS_ID = 1051;
	private static final int GAMES_ID_ARTISTS = 106;
	private static final int GAMES_ID_ARTISTS_ID = 1061;
	private static final int GAMES_ID_PUBLISHERS = 107;
	private static final int GAMES_ID_PUBLISHERS_ID = 1071;
	private static final int GAMES_ID_MECHANICS = 108;
	private static final int GAMES_ID_MECHANICS_ID = 1081;
	private static final int GAMES_ID_CATEGORIES = 109;
	private static final int GAMES_ID_CATEGORIES_ID = 1091;
	private static final int GAMES_ID_EXPANSIONS = 112;
	private static final int GAMES_ID_EXPANSIONS_ID = 1121;
	private static final int GAMES_DESIGNERS_ID = 401;
	private static final int GAMES_ARTISTS_ID = 403;
	private static final int GAMES_PUBLISHERS_ID = 405;
	private static final int GAMES_MECHANICS_ID = 407;
	private static final int GAMES_CATEGORIES_ID = 409;
	private static final int BUDDIES = 1000;
	private static final int BUDDIES_ID = 1001;
	private static final int SEARCH_SUGGEST = 9998;
	private static final int SHORTCUT_REFRESH = 9999;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = BggContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, "games", GAMES);
		matcher.addURI(authority, "games/#", GAMES_ID);
		matcher.addURI(authority, "games/ranks", GAMES_RANKS);
		matcher.addURI(authority, "games/ranks/#", GAMES_RANKS_ID);
		matcher.addURI(authority, "games/#/ranks", GAMES_ID_RANKS);
		matcher.addURI(authority, "games/#/ranks/#", GAMES_ID_RANKS_ID);
		matcher.addURI(authority, "games/#/designers", GAMES_ID_DESIGNERS);
		matcher.addURI(authority, "games/#/designers/#", GAMES_ID_DESIGNERS_ID);
		matcher.addURI(authority, "games/#/artists", GAMES_ID_ARTISTS);
		matcher.addURI(authority, "games/#/artists/#", GAMES_ID_ARTISTS_ID);
		matcher.addURI(authority, "games/#/publishers", GAMES_ID_PUBLISHERS);
		matcher.addURI(authority, "games/#/publishers/#", GAMES_ID_PUBLISHERS_ID);
		matcher.addURI(authority, "games/#/mechanics", GAMES_ID_MECHANICS);
		matcher.addURI(authority, "games/#/mechanics/#", GAMES_ID_MECHANICS_ID);
		matcher.addURI(authority, "games/#/categories", GAMES_ID_CATEGORIES);
		matcher.addURI(authority, "games/#/categories/#", GAMES_ID_CATEGORIES_ID);
		matcher.addURI(authority, "games/#/expansions", GAMES_ID_EXPANSIONS);
		matcher.addURI(authority, "games/#/expansions/#", GAMES_ID_EXPANSIONS_ID);
		matcher.addURI(authority, "games/designers/#", GAMES_DESIGNERS_ID);
		matcher.addURI(authority, "games/artists/#", GAMES_ARTISTS_ID);
		matcher.addURI(authority, "games/publishers/#", GAMES_PUBLISHERS_ID);
		matcher.addURI(authority, "games/mechanics/#", GAMES_MECHANICS_ID);
		matcher.addURI(authority, "games/categories/#", GAMES_CATEGORIES_ID);
		matcher.addURI(authority, "buddies", BUDDIES);
		matcher.addURI(authority, "buddies/#", BUDDIES_ID);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#", SHORTCUT_REFRESH);

		return matcher;
	}

	private static int code = 4000;

	private static void addProvider(HashMap<Integer, BaseProvider> map, UriMatcher matcher, BaseProvider provider) {
		code++;
		matcher.addURI(BggContract.CONTENT_AUTHORITY, provider.getPath(), code);
		map.put(code, provider);
	}

	@SuppressLint("UseSparseArrays")
	private static HashMap<Integer, BaseProvider> buildProviderMap() {
		HashMap<Integer, BaseProvider> map = new HashMap<Integer, BaseProvider>();

		addProvider(map, sUriMatcher, new GamesIdPollsProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyResultProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyResultKeyProvider());

		addProvider(map, sUriMatcher, new GamesIdColorsProvider());
		addProvider(map, sUriMatcher, new GamesIdColorsNameProvider());

		addProvider(map, sUriMatcher, new DesignersProvider());
		addProvider(map, sUriMatcher, new DesignersIdProvider());
		addProvider(map, sUriMatcher, new ArtistsProvider());
		addProvider(map, sUriMatcher, new ArtistsIdProvider());
		addProvider(map, sUriMatcher, new PublishersProvider());
		addProvider(map, sUriMatcher, new PublishersIdProvider());
		addProvider(map, sUriMatcher, new MechanicsProvider());
		addProvider(map, sUriMatcher, new MechanicsIdProvider());
		addProvider(map, sUriMatcher, new CategoriesProvider());
		addProvider(map, sUriMatcher, new CategoriesIdProvider());

		addProvider(map, sUriMatcher, new CollectionProvider());
		addProvider(map, sUriMatcher, new CollectionIdProvider());

		addProvider(map, sUriMatcher, new PlaysProvider());
		addProvider(map, sUriMatcher, new PlaysIdProvider());
		addProvider(map, sUriMatcher, new PlaysIdItemsProvider());
		addProvider(map, sUriMatcher, new PlaysIdItemsIdProvider());
		addProvider(map, sUriMatcher, new PlaysIdPlayersProvider());
		addProvider(map, sUriMatcher, new PlaysIdPlayersIdProvider());
		addProvider(map, sUriMatcher, new PlaysGamesId());
		addProvider(map, sUriMatcher, new PlaysLocationsProvider());

		addProvider(map, sUriMatcher, new CollectionFiltersProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdDetailsProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdDetailsIdProvider());

		return map;
	}

	private static HashMap<String, String> buildSuggestionProjectionMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Games._ID, Games._ID);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, Games.GAME_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, Games.YEAR_PUBLISHED + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_1, "0 AS " + SearchManager.SUGGEST_COLUMN_ICON_1);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_2, "'" + Thumbnails.CONTENT_URI + "/' || " + Tables.GAMES + "."
				+ Games.THUMBNAIL_URL + " AS " + SearchManager.SUGGEST_COLUMN_ICON_2);
		map.put(Games.GAME_SORT_NAME, "(CASE WHEN " + Games.GAME_SORT_NAME + " IS NULL THEN " + Games.GAME_SORT_NAME
				+ " ELSE " + Games.GAME_SORT_NAME + " END) AS " + Games.GAME_SORT_NAME);
		return map;
	}

	private interface Qualified {
		String GAMES_DESIGNERS_GAME_ID = Tables.GAMES_DESIGNERS + "." + GamesDesigners.GAME_ID;
		String GAMES_ARTISTS_GAME_ID = Tables.GAMES_ARTISTS + "." + GamesArtists.GAME_ID;
		String GAMES_PUBLISHERS_GAME_ID = Tables.GAMES_PUBLISHERS + "." + GamesPublishers.GAME_ID;
		String GAMES_MECHANICS_GAME_ID = Tables.GAMES_MECHANICS + "." + GamesMechanics.GAME_ID;
		String GAMES_CATEGORIES_GAME_ID = Tables.GAMES_CATEGORIES + "." + GamesCategories.GAME_ID;
		String GAMES_EXPANSIONS_GAME_ID = Tables.GAMES_EXPANSIONS + "." + Games.GAME_ID;
	}

	@Override
	public boolean onCreate() {
		final Context context = getContext();
		mOpenHelper = new BggDatabase(context);
		return true;
	}

	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);

		if (providers.containsKey(match)) {
			return providers.get(match).getType(uri);
		}

		switch (match) {
			case GAMES:
				return Games.CONTENT_TYPE;
			case GAMES_ID:
				return Games.CONTENT_ITEM_TYPE;
			case GAMES_RANKS:
				return GameRanks.CONTENT_TYPE;
			case GAMES_RANKS_ID:
				return GameRanks.CONTENT_ITEM_TYPE;
			case GAMES_ID_RANKS:
				return GameRanks.CONTENT_TYPE;
			case GAMES_ID_RANKS_ID:
				return GameRanks.CONTENT_ITEM_TYPE;
			case GAMES_ID_DESIGNERS:
				return Designers.CONTENT_TYPE;
			case GAMES_ID_DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
			case GAMES_ID_ARTISTS:
				return Artists.CONTENT_TYPE;
			case GAMES_ID_ARTISTS_ID:
				return Artists.CONTENT_ITEM_TYPE;
			case GAMES_ID_PUBLISHERS:
				return Publishers.CONTENT_TYPE;
			case GAMES_ID_PUBLISHERS_ID:
				return Publishers.CONTENT_ITEM_TYPE;
			case GAMES_ID_MECHANICS:
				return Mechanics.CONTENT_TYPE;
			case GAMES_ID_MECHANICS_ID:
				return Mechanics.CONTENT_ITEM_TYPE;
			case GAMES_ID_CATEGORIES:
				return Categories.CONTENT_TYPE;
			case GAMES_ID_CATEGORIES_ID:
				return Categories.CONTENT_ITEM_TYPE;
			case GAMES_ID_EXPANSIONS:
				return GamesExpansions.CONTENT_TYPE;
			case GAMES_ID_EXPANSIONS_ID:
				return GamesExpansions.CONTENT_ITEM_TYPE;
			case GAMES_DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
			case GAMES_ARTISTS_ID:
				return Artists.CONTENT_ITEM_TYPE;
			case GAMES_PUBLISHERS_ID:
				return Publishers.CONTENT_ITEM_TYPE;
			case GAMES_MECHANICS_ID:
				return Mechanics.CONTENT_ITEM_TYPE;
			case GAMES_CATEGORIES_ID:
				return Categories.CONTENT_ITEM_TYPE;
			case BUDDIES:
				return Buddies.CONTENT_TYPE;
			case BUDDIES_ID:
				return Buddies.CONTENT_ITEM_TYPE;
			case SEARCH_SUGGEST:
				return SearchManager.SUGGEST_MIME_TYPE;
			case SHORTCUT_REFRESH:
				return SearchManager.SHORTCUT_MIME_TYPE;
			default:
				throw new UnsupportedOperationException("Unknown uri getting type: " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (LOGV) {
			Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
		}
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		final int match = sUriMatcher.match(uri);
		switch (match) {
			case SEARCH_SUGGEST: {
				String query = null;
				if (uri.getPathSegments().size() > 1) {
					query = uri.getLastPathSegment().toLowerCase();
				}
				if (query == null) {
					return null;
				} else {
					SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
					qb.setTables(Tables.GAMES);
					qb.setProjectionMap(sSuggestionProjectionMap);
					qb.appendWhere("(" + Tables.GAMES + "." + Games.GAME_NAME + " like '" + query + "%' OR "
							+ Tables.GAMES + "." + Games.GAME_NAME + " like '% " + query + "%')");
					String orderBy;
					if (TextUtils.isEmpty(sortOrder)) {
						orderBy = Games.DEFAULT_SORT;
					} else {
						orderBy = sortOrder;
					}
					Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
					c.setNotificationUri(getContext().getContentResolver(), uri);
					Log.d(TAG, "Queried URI " + uri);
					return c;
				}
			}
			case SHORTCUT_REFRESH: {
				String shortcutId = null;
				if (uri.getPathSegments().size() > 1) {
					shortcutId = uri.getLastPathSegment();
				}
				if (TextUtils.isEmpty(shortcutId)) {
					return null;
				} else {
					final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
					qb.setTables(Tables.GAMES);
					qb.setProjectionMap(sSuggestionProjectionMap);
					qb.appendWhere(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + "=" + uri.getPathSegments().get(1));
					Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
					c.setNotificationUri(getContext().getContentResolver(), uri);
					Log.d(TAG, "Queried URI " + uri);
					return c;
				}
			}
			default: {
				final SelectionBuilder builder = buildExpandedSelection(uri, match);
				return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
			}
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGV) {
			Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		Uri newUri = null;

		if (providers.containsKey(match)) {
			newUri = providers.get(match).insert(db, uri, values);
			if (newUri != null) {
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			return newUri;
		}
		long rowId = -1;

		switch (match) {
			case GAMES: {
				rowId = db.insertOrThrow(Tables.GAMES, null, values);
				newUri = Games.buildGameUri(values.getAsInteger(Games.GAME_ID));
				break;
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				values.put(GameRanks.GAME_ID, gameId);
				rowId = db.insertOrThrow(Tables.GAME_RANKS, null, values);
				newUri = GameRanks.buildGameRankUri((int) rowId);
				break;
			}
			case GAMES_ID_DESIGNERS: {
				rowId = db.insertOrThrow(Tables.GAMES_DESIGNERS, null, values);
				newUri = Games.buildDesignersUri(rowId);
				break;
			}
			case GAMES_ID_ARTISTS: {
				rowId = db.insertOrThrow(Tables.GAMES_ARTISTS, null, values);
				newUri = Games.buildArtistUri(rowId);
				break;
			}
			case GAMES_ID_PUBLISHERS: {
				rowId = db.insertOrThrow(Tables.GAMES_PUBLISHERS, null, values);
				newUri = Games.buildPublisherUri(rowId);
				break;
			}
			case GAMES_ID_MECHANICS: {
				rowId = db.insertOrThrow(Tables.GAMES_MECHANICS, null, values);
				newUri = Games.buildMechanicUri(rowId);
				break;
			}
			case GAMES_ID_CATEGORIES: {
				rowId = db.insertOrThrow(Tables.GAMES_CATEGORIES, null, values);
				newUri = Games.buildCategoryUri(rowId);
				break;
			}
			case GAMES_ID_EXPANSIONS: {
				rowId = db.insertOrThrow(Tables.GAMES_EXPANSIONS, null, values);
				newUri = Games.buildExpansionUri(rowId);
				break;
			}
			case BUDDIES: {
				rowId = db.insertOrThrow(Tables.BUDDIES, null, values);
				newUri = Buddies.buildBuddyUri(values.getAsInteger(Buddies.BUDDY_ID));
				break;
			}
			default: {
				throw new UnsupportedOperationException("Unknown uri inserting: " + uri);
			}
		}

		if (newUri != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);

		int rowCount = 0;
		if (providers.containsKey(match)) {
			rowCount = providers.get(match).buildSimpleSelection(uri).where(selection, selectionArgs)
					.update(db, values);
		} else {
			rowCount = buildSimpleSelection(uri, match).where(selection, selectionArgs).update(db, values);
		}

		if (LOGV) {
			Log.v(TAG, "updated " + rowCount + " rows");
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "delete(uri=" + uri + ")");
		}

		int rowCount = 0;
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		final SelectionBuilder builder = buildSimpleSelection(uri, match).where(selection, selectionArgs);

		if (providers.containsKey(match)) {
			providers.get(match).deleteChildren(db, builder);
		}

		switch (match) {
			case GAMES:
			case GAMES_ID:
				deleteGameChildren(db, builder);
				break;
			default:
				break;
		}
		rowCount = builder.delete(db);

		if (LOGV) {
			Log.v(TAG, "deleted " + rowCount + " rows");
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		// TODO: fix this to not include the entire thumbnail URL in the URI
		// TODO: test for a URI match
		String fileName = uri.getLastPathSegment();
		final File file = ImageCache.getExistingImageFile(fileName);
		if (file != null) {
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		return null;
	}

	private SelectionBuilder buildSimpleSelection(Uri uri, int match) {
		if (providers.containsKey(match)) {
			return providers.get(match).buildSimpleSelection(uri);
		}

		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
			case GAMES:
				return builder.table(Tables.GAMES);
			case GAMES_ID: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES).where(Games.GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_RANKS:
				return builder.table(Tables.GAME_RANKS);
			case GAMES_RANKS_ID: {
				final int rankId = GameRanks.getRankId(uri);
				return builder.table(Tables.GAME_RANKS).where(GameRanks.GAME_RANK_ID + "=?", String.valueOf(rankId));
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAME_RANKS).where(GameRanks.GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_RANKS_ID: {
				int gameId = Games.getGameId(uri);
				int rankId = GameRanks.getRankId(uri);
				return builder.table(Tables.GAME_RANKS).where(GameRanks.GAME_ID + "=?", String.valueOf(gameId))
						.where(GameRanks.GAME_RANK_ID + "=?", String.valueOf(rankId));
			}
			case GAMES_ID_DESIGNERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_DESIGNERS).where(GamesDesigners.GAME_ID + "=?",
						String.valueOf(gameId));
			}
			case GAMES_ID_DESIGNERS_ID: {
				final int gameId = Games.getGameId(uri);
				final long designerId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_DESIGNERS)
						.where(GamesDesigners.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesDesigners.DESIGNER_ID + "=?", String.valueOf(designerId));
			}
			case GAMES_ID_ARTISTS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(GamesArtists.GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_ARTISTS_ID: {
				final int gameId = Games.getGameId(uri);
				final long artistId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(GamesArtists.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesArtists.ARTIST_ID + "=?", String.valueOf(artistId));
			}
			case GAMES_ID_PUBLISHERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS).where(GamesPublishers.GAME_ID + "=?",
						String.valueOf(gameId));
			}
			case GAMES_ID_PUBLISHERS_ID: {
				final int gameId = Games.getGameId(uri);
				final long publisherId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS)
						.where(GamesPublishers.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesPublishers.PUBLISHER_ID + "=?", String.valueOf(publisherId));
			}
			case GAMES_ID_MECHANICS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_MECHANICS).where(GamesMechanics.GAME_ID + "=?",
						String.valueOf(gameId));
			}
			case GAMES_ID_MECHANICS_ID: {
				final int gameId = Games.getGameId(uri);
				final long mechanicId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_MECHANICS)
						.where(GamesMechanics.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesMechanics.MECHANIC_ID + "=?", String.valueOf(mechanicId));
			}
			case GAMES_ID_CATEGORIES: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_CATEGORIES).where(GamesCategories.GAME_ID + "=?",
						String.valueOf(gameId));
			}
			case GAMES_ID_CATEGORIES_ID: {
				final int gameId = Games.getGameId(uri);
				final long categoryId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_CATEGORIES)
						.where(GamesCategories.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesCategories.CATEGORY_ID + "=?", String.valueOf(categoryId));
			}
			case GAMES_ID_EXPANSIONS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_EXPANSIONS).where(GamesExpansions.GAME_ID + "=?",
						String.valueOf(gameId));
			}
			case GAMES_ID_EXPANSIONS_ID: {
				final int gameId = Games.getGameId(uri);
				final long expansionId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_EXPANSIONS)
						.where(GamesExpansions.GAME_ID + "=?", String.valueOf(gameId))
						.where(GamesExpansions.EXPANSION_ID + "=?", String.valueOf(expansionId));
			}
			case GAMES_DESIGNERS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_DESIGNERS).where(BaseColumns._ID + "=?", String.valueOf(id));
			}
			case GAMES_ARTISTS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(BaseColumns._ID + "=?", String.valueOf(id));
			}
			case GAMES_PUBLISHERS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS).where(BaseColumns._ID + "=?", String.valueOf(id));
			}
			case GAMES_MECHANICS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_MECHANICS).where(BaseColumns._ID + "=?", String.valueOf(id));
			}
			case GAMES_CATEGORIES_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_CATEGORIES).where(BaseColumns._ID + "=?", String.valueOf(id));
			}
			case BUDDIES:
				return builder.table(Tables.BUDDIES);
			case BUDDIES_ID:
				final int buddyId = Buddies.getBuddyId(uri);
				return builder.table(Tables.BUDDIES).where(Buddies.BUDDY_ID + "=?", String.valueOf(buddyId));
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
		if (providers.containsKey(match)) {
			return providers.get(match).buildExpandedSelection(uri);
		}

		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
			case GAMES_ID_DESIGNERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_DESIGNERS_JOIN_DESIGNERS).mapToTable(Designers._ID, Tables.DESIGNERS)
						.mapToTable(Designers.DESIGNER_ID, Tables.DESIGNERS)
						.mapToTable(SyncColumns.UPDATED, Tables.DESIGNERS)
						.where(Qualified.GAMES_DESIGNERS_GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_ARTISTS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_ARTISTS_JOIN_ARTISTS).mapToTable(Artists._ID, Tables.ARTISTS)
						.mapToTable(Artists.ARTIST_ID, Tables.ARTISTS).mapToTable(SyncColumns.UPDATED, Tables.ARTISTS)
						.where(Qualified.GAMES_ARTISTS_GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_PUBLISHERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS_JOIN_PUBLISHERS)
						.mapToTable(Publishers._ID, Tables.PUBLISHERS)
						.mapToTable(Publishers.PUBLISHER_ID, Tables.PUBLISHERS)
						.mapToTable(SyncColumns.UPDATED, Tables.PUBLISHERS)
						.where(Qualified.GAMES_PUBLISHERS_GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_MECHANICS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_MECHANICS_JOIN_MECHANICS).mapToTable(Mechanics._ID, Tables.MECHANICS)
						.mapToTable(Mechanics.MECHANIC_ID, Tables.MECHANICS)
						.where(Qualified.GAMES_MECHANICS_GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_CATEGORIES: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_CATEGORIES_JOIN_CATEGORIES)
						.mapToTable(Categories._ID, Tables.CATEGORIES)
						.mapToTable(Categories.CATEGORY_ID, Tables.CATEGORIES)
						.where(Qualified.GAMES_CATEGORIES_GAME_ID + "=?", String.valueOf(gameId));
			}
			case GAMES_ID_EXPANSIONS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_EXPANSIONS_JOIN_EXPANSIONS)
						.mapToTable(GamesExpansions._ID, Tables.GAMES_EXPANSIONS)
						.mapToTable(GamesExpansions.GAME_ID, Tables.GAMES_EXPANSIONS)
						.where(Qualified.GAMES_EXPANSIONS_GAME_ID + "=?", String.valueOf(gameId));
			}

			default:
				return buildSimpleSelection(uri, match);
		}
	}

	private void deleteGameChildren(final SQLiteDatabase db, final SelectionBuilder builder) {
		// TODO after upgrading to API 8, use cascading deletes (http://stackoverflow.com/questions/2545558)
		Cursor c = builder.query(db, new String[] { Games.GAME_ID }, null);
		try {
			while (c.moveToNext()) {
				int gameId = c.getInt(0);
				String[] gameArg = new String[] { String.valueOf(gameId) };
				db.delete(Tables.GAME_RANKS, GameRanks.GAME_ID + "=?", gameArg);
				db.delete(Tables.COLLECTION, Collection.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_DESIGNERS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_ARTISTS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_PUBLISHERS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_MECHANICS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_CATEGORIES, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_EXPANSIONS, Games.GAME_ID + "=?", gameArg);
				db.delete(
						Tables.GAME_POLL_RESULTS_RESULT,
						"pollresults_id IN (SELECT game_poll_results._id from game_poll_results WHERE game_poll_results.poll_id IN (SELECT game_polls._id FROM game_polls WHERE game_id=?))",
						gameArg);
				db.delete(Tables.GAME_POLL_RESULTS,
						"game_poll_results.poll_id IN (SELECT game_polls._id FROM game_polls WHERE game_id=?)", gameArg);
				db.delete(Tables.GAME_POLLS, GamePolls.GAME_ID + "=?", gameArg);
			}
		} finally {
			c.close();
		}
	}
}
// TODO: improve the magical WHERE clauses with table and column constants - this should improve performance