package com.boardgamegeek.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.kizitonwose.calendarview.model.CalendarDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.groupBy
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.set
import kotlin.collections.toSet

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val playsByDay = MutableLiveData(mutableMapOf<LocalDate, List<PlayEntity>>())
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    init {

        val playsByDay = mutableMapOf<LocalDate, List<PlayEntity>>()
        val gameIds = mutableSetOf<Int>()

        playRepository.getPlays().observeForever { plays ->

            if (plays.data == null) {
                this.playsByDay.value = mutableMapOf()
                return@observeForever
            }

            plays.data
                .groupBy { Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
                .forEach { playsByDay[it.key] = it.value }

            gameIds.addAll(plays.data.map { it.gameId })

            this.playsByDay.value = playsByDay

            gameIds.forEach { gameId ->
                // FIXME: does this keep refreshing game fetch from BGG?
                gameCollectionRepository.getCollectionItems(gameId).observeForever { game ->
                    if (game.data != null) {
                        games.getOrPut(gameId) { MutableLiveData() }.value = game.data.firstOrNull() // TODO: List?
                    }
                }
            }
        }
    }

    fun getPlaysByDay(day: CalendarDay): LiveData<List<PlayEntity>> =
        Transformations.map(playsByDay) { playsByDay ->
            playsByDay[day.date] ?: emptyList()
        }

    fun getGamesFromPlays(plays: List<PlayEntity>): Set<LiveData<CollectionItemEntity>> =
        plays
            .map { games.getOrPut(it.gameId) { MutableLiveData() } }
            .toSet()

//    fun getPlayedGamesByDay(day: CalendarDay): Set<LiveData<GameEntity>> =
//        getPlaysByDay(day)
//            .value
//            ?.map { games.getOrPut(it.gameId) { MutableLiveData() } }
//            ?.toSet()
//            ?: emptySet()
}
