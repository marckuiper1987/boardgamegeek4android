package com.boardgamegeek.ui

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.kizitonwose.calendarview.model.CalendarDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())

    private val playsByDay = MutableLiveData(mutableMapOf<LocalDate, List<PlayEntity>>())
    private val games = MutableLiveData(mutableMapOf<Int, GameEntity>())

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
                gameRepository.getGame(gameId).observeForever { game ->
                    if (game.data != null) {
                        games.value = (games.value ?: mutableMapOf()).apply {
                            this[game.data.id] = game.data
                        }
                    }
                }
            }
        }
    }

    fun getPlaysByDay(day: CalendarDay) =
        Transformations.map(playsByDay) { playsByDay ->
            playsByDay[day.date] ?: emptyList()
        }

    fun getPlayedGamesByDay(day: CalendarDay) =
        // TODO: this triggers on each individual game being added to games list
        getPlaysByDay(day).combineWith(games) { plays, games ->
            if (plays == null || games == null)
                emptySet()
            else
                plays.mapNotNull { games[it.gameId] }.toSet()
        }
}

// TODO: put in extension file
// https://stackoverflow.com/questions/50599830/how-to-combine-two-live-data-one-after-the-other
fun <T, K, R> LiveData<T>.combineWith(
        liveData: LiveData<K>,
        block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block.invoke(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block.invoke(this.value, liveData.value)
    }
    return result
}
