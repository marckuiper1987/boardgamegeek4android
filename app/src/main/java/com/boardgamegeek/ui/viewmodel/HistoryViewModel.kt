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
import com.boardgamegeek.extensions.monthsToNow
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

    private val firstPlay = MutableLiveData<PlayEntity>()
    private var allPlays: RefreshableResource<List<PlayEntity>>? = null
    private val playsByMonth = mutableMapOf<YearMonth, MutableLiveData<List<PlayEntity>>>()
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    val selectedDate = MutableLiveData<LocalDate?>()
    val selectedMonth = MutableLiveData<YearMonth?>()
    val selectedMonthStats = Transformations.map(selectedMonth) { it?.let { getStatsForMonth(it) } }

    init {
        playRepository.getOldestPlayDate().observe(viewLifecycleOwner, Observer { plays ->

            allPlays = plays

            plays.data?.firstOrNull()?.let {

                val yearMonth = YearMonth.now()
                val numMonths = YearMonth.from(it.dateInMillis.toLocalDate()).monthsToNow()

                for (i in 0 until numMonths) {
                    playsByMonth[yearMonth.minusMonths(i)] = MutableLiveData()
                }

                firstPlay.value = it
            }
        })

        playRepository.getPlays().observe(viewLifecycleOwner, Observer { resource ->

            val plays = resource.data ?: return@Observer

            plays
                .groupBy { YearMonth.from(it.dateInMillis.toLocalDate()) }
                .forEach { playsByMonth[it.key]?.value = it.value }

            plays
                .distinctBy { it.gameId }
                .forEach { putGameLiveData(it.gameId) }
        })
    }

    val numberOfMonthsBetweenFirstPlayAndNow =
        Transformations.map(firstPlay) {
            if (it != null)
                30L
//                YearMonth.from(it.dateInMillis.toLocalDate()).monthsToNow()
            else
                0
        }

    private fun getMonthLiveData(yearMonth: YearMonth) =
        playsByMonth.getOrPut(yearMonth) { MutableLiveData() }

    private fun putGameLiveData(gameId: Int) =
        games.putIfAbsent(gameId,
            MutableLiveData<CollectionItemEntity>().also {
                gameCollectionRepository
                    .getCollectionItems(gameId, allowRefresh = false)
                    .observe(viewLifecycleOwner, Observer { items ->
                        if (items.data != null) {
                            games[gameId]?.value = items.data.firstOrNull()
                        }
                    })
            }
        )

    fun getPlaysByDayForMonth(yearMonth: YearMonth) =
        Transformations.map(getMonthLiveData(yearMonth)) { plays ->
            plays.groupBy { it.dateInMillis.toLocalDate() }
        }

    private fun getPlaysForDay(date: LocalDate) =
        Transformations.map(getMonthLiveData(YearMonth.from(date))) { plays ->
            plays.filter {
                val playDate = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                playDate == date
            }
        }

    fun getGamesForDay(date: LocalDate): LiveData<Set<LiveData<CollectionItemEntity>>> =
        Transformations.map(getPlaysForDay(date)) { plays ->
            plays
                .map { games.getOrDefault(it.gameId, MutableLiveData()) }
                .toSet()
        }

    fun getStatsForMonth(yearMonth: YearMonth): LiveData<PlayStatsForMonth> =
        Transformations.map(getMonthLiveData(yearMonth)) { refreshable ->
            refreshable.let { plays ->
                PlayStatsForMonth(
                    gamesPlayed = plays.distinctBy { it.gameId }.count(),
                    numberOfPlays = plays.sumBy { it.quantity },
                    hoursPlayed = plays.sumBy { it.length }
                )
            }
        }

    fun getNameForMonth(yearMonth: YearMonth): String = monthNameFormatter.format(yearMonth)
}

data class PlayStatsForMonth(
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int
)
