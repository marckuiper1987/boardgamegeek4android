package com.boardgamegeek.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.kizitonwose.calendarview.model.CalendarDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import kotlin.collections.set

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val playsByDay = MutableLiveData(mutableMapOf<LocalDate, List<PlayEntity>>())
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()
    private var firstDate: LocalDate? = null

    fun initialize(lifecycleOwner: LifecycleOwner) {

        val playsByDay = mutableMapOf<LocalDate, List<PlayEntity>>()
        val gameIds = mutableSetOf<Int>()

        playRepository.getPlays().observe(lifecycleOwner, Observer { plays ->

            if (plays.data == null) {
                this.playsByDay.value = mutableMapOf()
                return@Observer
            }

            plays.data
                .groupBy { Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
                .forEach {
                    playsByDay[it.key] = it.value
                    if (firstDate == null || it.key.isBefore(firstDate)) {
                        firstDate = it.key
                    }
                }

            gameIds.addAll(plays.data.map { it.gameId })

            this.playsByDay.value = playsByDay

            gameIds.forEach { gameId ->
                // TODO: fetch games in a single query
                gameCollectionRepository
                    .getCollectionItems(gameId, allowRefresh = false)
                    .observe(lifecycleOwner, Observer { game ->
                        if (game.data != null) {
                            games.getOrPut(gameId) { MutableLiveData() }.value = game.data.firstOrNull() // TODO: List?
                        }
                    })
            }
        })
    }

    // TODO: move to PlayRepository?
    fun getPlaysByDay(day: CalendarDay): LiveData<List<PlayEntity>> =
        Transformations.map(playsByDay) { playsByDay ->
            playsByDay[day.date] ?: emptyList()
        }

    fun getGamesFromPlays(plays: List<PlayEntity>): LiveData<Set<MutableLiveData<CollectionItemEntity>>> =
        // FIXME: note great, duplication with above
        Transformations.map(playsByDay) { _ ->
            plays
                .map { games.getOrPut(it.gameId) { MutableLiveData() } }
                .toSet()
        }

    fun getNumberOfMonthsBetweenFirstPlayAndNow() =
        Transformations.map(playsByDay) {
            if (firstDate != null)
                ChronoUnit.MONTHS.between(
                    YearMonth.from(firstDate),
                    YearMonth.from(LocalDate.now())
                ).toInt()
            else 0
        }

    fun getStatsForMonth(month: YearMonth): LiveData<PlayStatsForMonth> {

        val fromDay = month.atDay(0)
        val toDay = month.atEndOfMonth()

        return Transformations.map(playsByDay) { playsByDay ->

            val plays = playsByDay.flatMap { day ->
                if (day.key.isAfter(fromDay) && day.key.isBefore(toDay))
                    day.value
                else
                    emptyList()
            }

            PlayStatsForMonth(
                plays.distinctBy { it.gameId }.count(),
                plays.sumBy { it.quantity },
                plays.sumBy { it.length }
            )
        }
    }
}

data class PlayStatsForMonth(
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int
)
