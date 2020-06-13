package com.boardgamegeek.ui

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.boardgamegeek.R
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.calendar_day.view.*
import kotlinx.android.synthetic.main.calendar_header.*
import kotlinx.android.synthetic.main.calendar_header.view.*
import kotlinx.android.synthetic.main.fragment_calendar.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.*

class CalendarFragment(
    private val listener: Listener
) : Fragment() {

    private lateinit var viewModel: CalendarViewModel
    private var selectedDate: LocalDate? = null

    interface Listener {
        fun onChangeMonth(month: CalendarMonth)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        recyclerView.apply {
//
//        }

        val daysOfWeek = daysOfWeekFromLocale()

        val currentMonth = YearMonth.now()
        calendarView.setup(
            startMonth = currentMonth.minusMonths(10),
            endMonth = currentMonth.plusMonths(10),
            firstDayOfWeek = daysOfWeek.first()
        )
        calendarView.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay

            val textView = view.calendarDayText

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        if (selectedDate != day.date) {
                            val oldDate = selectedDate
                            selectedDate = day.date
                            calendarView.notifyDateChanged(day.date)
                            oldDate?.let { calendarView.notifyDateChanged(it) }
                            //updateAdapterForDate(day.date)
                        }
                    }
                }
            }
        }

        calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                container.textView.text = day.date.dayOfMonth.toString()

                if (day.owner == DayOwner.THIS_MONTH) {
                    container.textView.setTextColor(resources.getColor(R.color.primary_text))
                }
                else {
                    container.textView.setTextColor(resources.getColor(R.color.primary_dark))
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = view.legendLayout
        }
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                        tv.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                                .toUpperCase(Locale.ENGLISH)
                        tv.setTextColor(resources.getColor(R.color.primary_dark))
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    month.yearMonth
                }
            }
        }

        calendarView.monthScrollListener = { month ->
            listener.onChangeMonth(month)

            selectedDate?.let {
                // Clear selection if we scroll to a new month.
                selectedDate = null
                calendarView.notifyDateChanged(it)
//                updateAdapterForDate(null)
            }
        }
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProviders.of(this).get(CalendarViewModel::class.java)
//
//        calendarView.dayBinder = object : DayBinder<DayViewContainer> {
//            // Called only when a new container is needed.
//            override fun create(view: View) = DayViewContainer(view)
//
//            // Called every time we need to reuse a container.
//            override fun bind(container: DayViewContainer, day: CalendarDay) {
//                container.textView.text = day.date.dayOfMonth.toString()
//            }
//        }
//
//        val currentMonth = YearMonth.now()
//        val firstMonth = currentMonth.minusMonths(10)
//        val lastMonth = currentMonth.plusMonths(10)
//        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
//        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
//        calendarView.scrollToMonth(currentMonth)
//    }
}

fun daysOfWeekFromLocale(): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var daysOfWeek = DayOfWeek.values()
    // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
    // Only necessary if firstDayOfWeek != DayOfWeek.MONDAY which has ordinal 0.
    if (firstDayOfWeek != DayOfWeek.MONDAY) {
        val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
        val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
        daysOfWeek = rhs + lhs
    }
    return daysOfWeek
}
