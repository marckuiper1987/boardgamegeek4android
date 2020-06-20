package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.load
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.io.model.Image
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.ImageUtils.getImageId
import com.boardgamegeek.util.RemoteConfig
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameCollectionRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getCollectionItems(
        gameId: Int,
        subType: String = BggService.THING_SUBTYPE_BOARDGAME,
        allowRefresh: Boolean = true // FIXME: this is a hack
    ): LiveData<RefreshableResource<List<CollectionItemEntity>>> {
        return object : RefreshableResourceLoader<List<CollectionItemEntity>, CollectionResponse>(application) {
            private var timestamp = 0L

            override val typeDescriptionResId = R.string.title_collection

            override fun loadFromDatabase() = dao.load(gameId)

            override fun shouldRefresh(data: List<CollectionItemEntity>?): Boolean {
                if (gameId == BggContract.INVALID_ID || username == null) return false
                val syncTimestamp = data?.minBy { it.syncTimestamp }?.syncTimestamp ?: 0L
                return allowRefresh && syncTimestamp.isOlderThan(refreshMinutes, TimeUnit.MINUTES)
            }

            override fun createCall(page: Int): Call<CollectionResponse> {
                timestamp = System.currentTimeMillis()
                val options = mutableMapOf(
                        BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                        BggService.COLLECTION_QUERY_KEY_STATS to "1",
                        BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                        BggService.COLLECTION_QUERY_KEY_SUBTYPE to subType
                )
                return Adapter.createForXmlWithAuth(application).collection(username, options)
            }

            override fun saveCallResult(result: CollectionResponse) {
                val mapper = CollectionItemMapper()
                val collectionIds = arrayListOf<Int>()

                result.items?.forEach { item ->
                    val pair = mapper.map(item)
                    val collectionId = dao.saveItem(pair.first, pair.second, timestamp)
                    collectionIds.add(collectionId)
                }
                Timber.i("Synced %,d collection item(s) for game '%s'", result.items?.size ?: 0, gameId)

                val deleteCount = dao.delete(gameId, collectionIds)
                Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)
            }
        }.asLiveData()
    }

    fun addCollectionItem(gameId: Int, statuses: List<String>, wishListPriority: Int?) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = contentValuesOf()
            values.put(BggContract.Collection.GAME_ID, gameId)
            putValue(statuses, values, BggContract.Collection.STATUS_OWN)
            putValue(statuses, values, BggContract.Collection.STATUS_PREORDERED)
            putValue(statuses, values, BggContract.Collection.STATUS_FOR_TRADE)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT_TO_PLAY)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT_TO_BUY)
            putValue(statuses, values, BggContract.Collection.STATUS_WISHLIST)
            putValue(statuses, values, BggContract.Collection.STATUS_PREVIOUSLY_OWNED)
            putWishList(statuses, wishListPriority, values)
            values.put(BggContract.Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis())

            application.contentResolver.load(BggContract.Games.buildGameUri(gameId),
                    arrayOf(BggContract.Games.GAME_NAME,
                            BggContract.Games.GAME_SORT_NAME,
                            BggContract.Games.YEAR_PUBLISHED,
                            BggContract.Games.IMAGE_URL,
                            BggContract.Games.THUMBNAIL_URL,
                            BggContract.Games.HERO_IMAGE_URL)
            )?.use {
                if (it.moveToFirst()) {
                    values.put(BggContract.Collection.COLLECTION_NAME, it.getString(0))
                    values.put(BggContract.Collection.COLLECTION_SORT_NAME, it.getString(1))
                    values.put(BggContract.Collection.COLLECTION_YEAR_PUBLISHED, it.getInt(2))
                    values.put(BggContract.Collection.COLLECTION_IMAGE_URL, it.getString(3))
                    values.put(BggContract.Collection.COLLECTION_THUMBNAIL_URL, it.getString(4))
                    values.put(BggContract.Collection.COLLECTION_HERO_IMAGE_URL, it.getString(5))
                    values.put(BggContract.Collection.COLLECTION_DIRTY_TIMESTAMP, System.currentTimeMillis())
                }
            }

            val gameName = values.getAsString(BggContract.Collection.COLLECTION_NAME)
            val response = application.contentResolver.insert(BggContract.Collection.CONTENT_URI, values)
            val internalId = response?.lastPathSegment?.toLongOrNull() ?: BggContract.INVALID_ID.toLong()
            if (internalId == BggContract.INVALID_ID.toLong()) {
                Timber.d("Collection item for game %s (%s) not added", gameName, gameId)
            } else {
                Timber.d("Collection item added for game %s (%s) (internal ID = %s)", gameName, gameId, internalId)
                SyncService.sync(application, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            }
        }
    }

    private fun putValue(statuses: List<String>, values: ContentValues, statusColumn: String) {
        values.put(statusColumn, if (statuses.contains(statusColumn)) 1 else 0)
    }

    private fun putWishList(statuses: List<String>, wishListPriority: Int?, values: ContentValues) {
        if (statuses.contains(BggContract.Collection.STATUS_WISHLIST)) {
            values.put(BggContract.Collection.STATUS_WISHLIST, 1)
            values.put(BggContract.Collection.STATUS_WISHLIST_PRIORITY, wishListPriority ?: 3) // like to have
            return
        } else {
            values.put(BggContract.Collection.STATUS_WISHLIST, 0)
        }
    }

    /**
     * If the hero image is different from the thumbnail, attempt to update it. This will be true if the hero image has
     * never been fetched, or if the collection's image has changed.
     */
    fun maybeRefreshHeroImageUrl(internalId: Long, thumbnailUrl: String?, heroImageUrl: String?, started: AtomicBoolean) {
        val heroImageId = heroImageUrl?.getImageId() ?: 0
        val thumbnailId = thumbnailUrl?.getImageId() ?: 0
        if (heroImageId != thumbnailId && started.compareAndSet(false, true)) {
            val call = Adapter.createGeekdoApi().image(thumbnailId)
            call.enqueue(object : retrofit2.Callback<Image> {
                override fun onResponse(call: Call<Image>?, response: Response<Image>?) {
                    if (response?.isSuccessful == true) {
                        val body = response.body()
                        if (body != null) {
                            application.appExecutors.diskIO.execute {
                                val values = ContentValues()
                                values.put(BggContract.Collection.COLLECTION_HERO_IMAGE_URL, body.images.medium.url)
                                dao.update(internalId, values)
                            }
                        } else {
                            Timber.w("Empty body while fetching image $thumbnailId for collection $internalId")
                        }
                    } else {
                        val message = response?.message() ?: response?.code().toString()
                        Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for collection $internalId")
                    }
                    started.set(false)
                }

                override fun onFailure(call: Call<Image>?, t: Throwable?) {
                    val message = t?.localizedMessage ?: "Unknown error"
                    Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for collection $internalId")
                    started.set(false)
                }
            })
        }
    }
}
