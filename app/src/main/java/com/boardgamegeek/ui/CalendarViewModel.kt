package com.boardgamegeek.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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

    private val playsByDay = mutableMapOf<LocalDate, MutableLiveData<List<PlayEntity>>>()
    private val games = MutableLiveData(mutableMapOf<Int, GameEntity>())

    init {

        val gameIds = mutableSetOf<Int>()

        playRepository.getPlays().observeForever { plays ->

            playsByDay.clear()

            if (plays.data != null) {

                plays.data
                    .groupBy {
                        Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    .forEach {
                        if (playsByDay.containsKey(it.key)) {
                            playsByDay[it.key]?.value = it.value
                        }
                        else {
                            playsByDay[it.key] = MutableLiveData(it.value)
                        }
                    }

                gameIds.addAll(plays.data.map { it.gameId })
            }

            gameIds.forEach { gameId ->
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

    fun getPlaysByDay(day: CalendarDay): LiveData<List<PlayEntity>> {
        if (!playsByDay.containsKey(day.date)) {
            playsByDay[day.date] = MutableLiveData(emptyList())
        }
        return playsByDay[day.date]!!
    }

    // FIXME: doesn't observe updates to game data, only plays
    fun getPlayedGamesByDay(day: CalendarDay) =
        Transformations.map(getPlaysByDay(day)) { plays ->
            plays.mapNotNull { games.value?.get(it.gameId) }.toSet()
        }
}
