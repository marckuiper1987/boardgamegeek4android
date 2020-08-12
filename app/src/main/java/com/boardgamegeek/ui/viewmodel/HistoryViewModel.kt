package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.monthsBetween
import com.boardgamegeek.extensions.toLocalDate
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryViewModelFactory(
    private val application: Application,
    private val viewLifecycleOwner: LifecycleOwner
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(application, viewLifecycleOwner) as T
    }
}

class HistoryViewModel(
    application: Application,
    private val viewLifecycleOwner: LifecycleOwner
) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val monthNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

    private val plays: LiveData<RefreshableResource<List<PlayEntity>>>
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    val selectedDate = MutableLiveData<LocalDate?>()
    val selectedMonth = MutableLiveData<YearMonth?>()

    init {
        plays = playRepository.getPlays()

        var initialObserve = true
        plays.observe(viewLifecycleOwner, Observer { refreshable ->
            refreshable.data?.let { plays ->
                plays
                    .distinctBy { it.gameId }
                    .forEach { putGameLiveData(it.gameId, onlyIfAbsent = !initialObserve) }
                initialObserve = false
            }
        })
    }

    // --------------------------
    // Game data
    // --------------------------

    private fun putGameLiveData(gameId: Int, onlyIfAbsent: Boolean = true) {

        if (onlyIfAbsent && games.containsKey(gameId)) {
            return
        }

        getGameLiveData(gameId).also {
            gameCollectionRepository
                .getCollectionItems(gameId, allowRefresh = false)
                .observe(viewLifecycleOwner, Observer { items ->
                    if (items.data != null) {
                        games[gameId]?.value = items.data.firstOrNull()
                    }
                })
        }
    }

    private fun getGameLiveData(gameId: Int) =
        games.getOrPut(gameId) { MutableLiveData() }

    // --------------------------
    // Play organisation
    // --------------------------

    private val playsByMonth = Transformations.map(plays) { refreshable ->
        refreshable.data?.let { plays ->
            plays.groupBy { YearMonth.from(it.dateInMillis.toLocalDate()) }
        } ?: emptyMap()
    }

    private val playStatsByMonth = Transformations.map(playsByMonth) { playsByMonth ->
        playsByMonth
            ?.mapValues { (yearMonth, plays) ->
                PlayStatsForMonth(
                    yearMonth = yearMonth,
                    gamesPlayed = plays.distinctBy { it.gameId }.count(),
                    numberOfPlays = plays.sumBy { it.quantity },
                    hoursPlayed = plays.sumBy { it.length },
                    mostPlayedGame = plays
                        .groupBy { it.gameId }
                        .maxBy { it.value.size } // TODO: max by play time
                        ?.let { getGameLiveData(it.key) },
                    playsByDate = plays.groupBy { it.dateInMillis.toLocalDate() }
                )
            }
            ?: emptyMap()
    }

    // --------------------------
    // Play data
    // --------------------------

    private fun getMonthLiveData(yearMonth: YearMonth) =
        Transformations.map(playsByMonth) { it[yearMonth] ?: emptyList() }

    private fun getMonthStatsLiveData(yearMonth: YearMonth) =
        Transformations.map(playStatsByMonth) { it[yearMonth] }

    private fun getPlaysForDay(date: LocalDate) =
        Transformations.map(getMonthLiveData(YearMonth.from(date))) { plays ->
            plays.filter {
                val playDate = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                playDate == date
            }
        }

    // --------------------------
    // Adapter lists
    // --------------------------

    val playStatsByMonthList: LiveData<List<PlayStatsForMonth>> = Transformations.map(playStatsByMonth) { statsByMonth ->

        val oldestYearMonth = statsByMonth
            .keys.min() ?: return@map emptyList<PlayStatsForMonth>()

        val newestYearMonth = statsByMonth
            .keys.max() ?: YearMonth.now()
            .coerceAtLeast(YearMonth.now())

        val monthsBetween = oldestYearMonth.monthsBetween(newestYearMonth)

        mutableListOf<PlayStatsForMonth>().apply {
            for (position in 0 until monthsBetween) {
                val yearMonth = newestYearMonth.minusMonths(position)
                add(statsByMonth[yearMonth] ?: PlayStatsForMonth.empty(yearMonth))
            }
        }
    }

    // --------------------------
    // UI data
    // --------------------------

    val selectedMonthStats = Transformations.map(selectedMonth) {
        yearMonth -> yearMonth?.let { getMonthStatsLiveData(it) }
    }

    fun getGamesForDay(date: LocalDate): LiveData<Set<LiveData<CollectionItemEntity>>> =
        Transformations.map(getPlaysForDay(date)) { plays ->
            plays
                .map { getGameLiveData(it.gameId) }
                .toSet()
        }

    fun getNameForMonth(yearMonth: YearMonth): String = monthNameFormatter.format(yearMonth)

    fun getStatsForMonth(yearMonth: YearMonth) = getMonthStatsLiveData(yearMonth)
}

data class PlayStatsForMonth(
    val yearMonth: YearMonth,
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int,
    val mostPlayedGame: LiveData<CollectionItemEntity>?,
    val playsByDate: Map<LocalDate, List<PlayEntity>>
) {
    companion object {
        fun empty(yearMonth: YearMonth) = PlayStatsForMonth(
            yearMonth = yearMonth,
            gamesPlayed = 0,
            numberOfPlays = 0,
            hoursPlayed = 0,
            mostPlayedGame = null,
            playsByDate = emptyMap()
        )
    }
}
