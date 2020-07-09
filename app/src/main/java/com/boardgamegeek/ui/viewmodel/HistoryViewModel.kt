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
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

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
    private val lifecycleOwner: LifecycleOwner
) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val playsByMonth = mutableMapOf<YearMonth, MutableLiveData<List<PlayEntity>>>()
    private var firstMonth = MutableLiveData<YearMonth?>()

    private val games = mutableMapOf<Int, MutableLiveData<CollectionItemEntity>>()

    val selectedMonth = MutableLiveData<YearMonth?>()
    val selectedMonthStats = Transformations.map(selectedMonth) { it?.let { getStatsForMonth(it) } }

    init {

        val gameIds = mutableSetOf<Int>()

        playRepository.getPlays().observe(lifecycleOwner, Observer { plays ->

            if (plays.data == null) {
                return@Observer
            }

            plays.data
                .groupBy {
                    val date = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    YearMonth.from(date)
                }
                .forEach {
                    getMonthLiveData(it.key).value = it.value
                }

            firstMonth.value = playsByMonth.keys.min()

            gameIds.addAll(plays.data.map { it.gameId })

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

    fun getPlaysForDay(date: LocalDate) =
        Transformations.map(getMonthLiveData(YearMonth.from(date))) { plays ->
            plays.filter {
                val playDate = Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                playDate == date
            }
        }

    fun getGamesForDay(date: LocalDate) =
        Transformations.map(getPlaysForDay(date)) { list ->
            list
                .map { games.getOrPut(it.gameId) { MutableLiveData() } }
                .toSet()
        }

    fun getNumberOfMonthsBetweenFirstPlayAndNow() =
        Transformations.map(firstMonth) {
            if (it != null)
                ChronoUnit.MONTHS.between(
                    YearMonth.from(it),
                    YearMonth.from(LocalDate.now())
                ).toInt()
            else 0
        }

    fun getStatsForMonth(yearMonth: YearMonth): LiveData<PlayStatsForMonth> =
        Transformations.map(getMonthLiveData(yearMonth)) { plays ->
            PlayStatsForMonth(
                gamesPlayed = plays.distinctBy { it.gameId }.count(),
                numberOfPlays = plays.sumBy { it.quantity },
                hoursPlayed = plays.sumBy { it.length }
            )
        }

    private fun getMonthLiveData(yearMonth: YearMonth) =
        playsByMonth.getOrPut(yearMonth) { MutableLiveData() }
}

data class PlayStatsForMonth(
    val gamesPlayed: Int,
    val numberOfPlays: Int,
    val hoursPlayed: Int
)
