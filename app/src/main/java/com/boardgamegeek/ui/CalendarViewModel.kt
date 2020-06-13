package com.boardgamegeek.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.repository.PlayRepository
import com.kizitonwose.calendarview.model.CalendarDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val playRepository = PlayRepository(getApplication())

    private val playsByDay = mutableMapOf<LocalDate, List<PlayEntity>>() // TODO: LiveData

    init {

        playRepository.getPlays().observeForever { plays ->

            playsByDay.clear()

            if (plays.data != null) {
                playsByDay.putAll(plays.data.groupBy {
                    Instant.ofEpochMilli(it.dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                })
            }
        }
    }

    fun getPlaysByDay(day: CalendarDay): List<PlayEntity> = playsByDay[day.date] ?: emptyList()
}
