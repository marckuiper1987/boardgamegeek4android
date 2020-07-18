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
import com.boardgamegeek.extensions.toLocalDate
import com.boardgamegeek.extensions.toZonedDateTime
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

class HistoryViewModelFactory(
    private val application: Application,
    private val lifecycleOwner: LifecycleOwner
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(application, lifecycleOwner) as T
    }
}

class HistoryViewModel(
    application: Application,
    private val viewLifecycleOwner: LifecycleOwner
) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val monthNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

    private val firstPlay: LiveData<RefreshableResource<List<PlayEntity>>>
    private val playsByMonth = mutableMapOf<YearMonth, LiveData<RefreshableResource<List<PlayEntity>>>>()
    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    val selectedDate = MutableLiveData<LocalDate?>()
    val selectedMonth = MutableLiveData<YearMonth?>()
    val selectedMonthStats = Transformations.map(selectedMonth) { it?.let { getStatsForMonth(it) } }

    init {
        firstPlay = playRepository.getOldestPlayDate()
    }

    private val firstMonth = Transformations.map(firstPlay) { refreshable ->
        refreshable.data?.firstOrNull()?.let {
            YearMonth.from(it.dateInMillis.toZonedDateTime())
        }
    }

    private fun getMonthLiveData(yearMonth: YearMonth) =
        playsByMonth.getOrPut(yearMonth) {
            playRepository
                .loadPlaysByYearMonth(yearMonth)
                .also { data ->
                    data.observe(viewLifecycleOwner, Observer { plays ->
                        plays.data?.forEach { play ->
                            putGameLiveData(play.gameId)
                        }
                    })
                }
        }

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
            plays.data?.groupBy { it.dateInMillis.toLocalDate() }
        }

    fun getPlaysForDay(date: LocalDate) =
        Transformations.map(getMonthLiveData(YearMonth.from(date))) { plays ->
            plays.data?.filter {
                val playDate = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                playDate == date
            } ?: emptyList()
        }

    fun getGamesForDay(date: LocalDate): LiveData<Set<LiveData<CollectionItemEntity>>> =
        Transformations.map(getPlaysForDay(date)) { plays ->
            plays
                .map { games.getOrDefault(it.gameId, MutableLiveData()) }
                .toSet()
        }

    fun getNumberOfMonthsBetweenFirstPlayAndNow() =
        Transformations.map(firstMonth) {
            if (it != null)
                ChronoUnit.MONTHS.between(
                    YearMonth.from(it),
                    YearMonth.from(LocalDate.now())
                ).toInt() + 1
            else 0
        }

    fun getStatsForMonth(yearMonth: YearMonth): LiveData<PlayStatsForMonth> =
        Transformations.map(getMonthLiveData(yearMonth)) { refreshable ->
            refreshable.data?.let { plays ->
                PlayStatsForMonth(
                    gamesPlayed = plays.distinctBy { it.gameId }.count(),
                    numberOfPlays = plays.sumBy { it.quantity },
                    hoursPlayed = plays.sumBy { it.length }
                )
            }
        }

    fun getNameForMonth(yearMonth: YearMonth) = monthNameFormatter.format(yearMonth)
}

data class PlayStatsForMonth(
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int
)
