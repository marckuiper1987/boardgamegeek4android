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
import com.boardgamegeek.extensions.toLocalDate
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import java.time.LocalDate
import java.time.YearMonth
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

    private val oldestPlay = MutableLiveData<PlayEntity>()
    private val newestPlay = MutableLiveData<PlayEntity>()

    val playStatsByMonth = MutableLiveData<Map<YearMonth, PlayStatsForMonth>>()

    private val playsByMonth = mutableMapOf<YearMonth, MutableLiveData<List<PlayEntity>>>()
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    val selectedDate = MutableLiveData<LocalDate?>()
    val selectedMonth = MutableLiveData<YearMonth?>()
    val selectedMonthStats = Transformations.map(selectedMonth) { it?.let { playStatsByMonth.value?.get(it) } } // FIXME

    init {
        playRepository.getOldestPlayDate().observe(viewLifecycleOwner, Observer { plays ->
            plays.data?.firstOrNull()?.let {
                oldestPlay.value = it
            }
        })

        var initialObserve = true
        playRepository.getPlays().observe(viewLifecycleOwner, Observer { resource ->

            val plays = resource.data ?: return@Observer

            playStatsByMonth.value =
                plays
                    .groupBy { YearMonth.from(it.dateInMillis.toLocalDate()) }
                    .mapValues { makeStatsForMonth(it.value) }

            plays
                .distinctBy { it.gameId }
                .forEach { putGameLiveData(it.gameId, onlyIfAbsent = !initialObserve) }

            initialObserve = false
        })
    }

    val numberOfMonthsBetweenFirstPlayAndNow =
        Transformations.map(oldestPlay) {
            if (it != null)
                30L
//                YearMonth.from(it.dateInMillis.toLocalDate()).monthsToNow()
            else
                0
        }

//    private fun getMonthLiveData(yearMonth: YearMonth) =
//        playsByMonth.getOrPut(yearMonth) { MutableLiveData() }

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

//    fun getPlaysByDayForMonth(yearMonth: YearMonth) =
//        Transformations.map(getMonthLiveData(yearMonth)) { plays ->
//            plays.groupBy { it.dateInMillis.toLocalDate() }
//        }

//    private fun getPlaysForDay(date: LocalDate) =
//        Transformations.map(getMonthLiveData(YearMonth.from(date))) { plays ->
//            plays.filter {
//                val playDate = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
//                playDate == date
//            }
//        }

    fun getGamesForDay(date: LocalDate): LiveData<Set<LiveData<CollectionItemEntity>>> =
        MutableLiveData()
//        Transformations.map(getPlaysForDay(date)) { plays ->
//            plays
//                .map { getGameLiveData(it.gameId) }
//                .toSet()
//        }

//    fun getStatsForMonth(yearMonth: YearMonth): LiveData<PlayStatsForMonth> =
//        Transformations.map(getMonthLiveData(yearMonth)) { makeStatsForMonth(it) }

    fun getNameForMonth(yearMonth: YearMonth): String = monthNameFormatter.format(yearMonth)

    private fun makeStatsForMonth(plays: List<PlayEntity>) =
        PlayStatsForMonth(
            gamesPlayed = plays.distinctBy { it.gameId }.count(),
            numberOfPlays = plays.sumBy { it.quantity },
            hoursPlayed = plays.sumBy { it.length },
            mostPlayedGame = plays
                .groupBy { it.gameId }
                .maxBy { it.value.size } // TODO: max by play time
                ?.let { getGameLiveData(it.key).value }, // FIXME
            playsByDate = plays.groupBy { it.dateInMillis.toLocalDate() }
        )
}

data class PlayStatsForMonth(
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int,
    val mostPlayedGame: CollectionItemEntity?,
    val playsByDate: Map<LocalDate, List<PlayEntity>>
)
