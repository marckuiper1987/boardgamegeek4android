package com.boardgamegeek.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.calendar_day.view.calendarDayFrame
import kotlinx.android.synthetic.main.calendar_day.view.calendarDayText
import kotlinx.android.synthetic.main.calendar_header.view.legendLayout
import kotlinx.android.synthetic.main.fragment_calendar.calendarView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.Locale

class CalendarFragment() : Fragment() {

    private val viewModel by activityViewModels<CalendarViewModel>()
    private var binding: ViewDataBinding? = null
    private var selectedDate: LocalDate? = null

    var selectedMonth: YearMonth = YearMonth.now()
        set(value) {
            field = value
            listener?.onChangeMonth(value)

            // TODO: bind viewModel instead?
            binding?.setVariable(BR.stats, viewModel.getStatsForMonth(value))
            if (calendarView != null) {
                // TODO: data binding?
                calendarView.scrollToMonth(value)
            }

            selectedDate?.let {
                // Clear selection if we scroll to a new month.
                selectedDate = null
                calendarView.notifyDateChanged(it)
//                updateAdapterForDate(null)
            }
        }

    var listener: Listener? = null
        set(value) {
            field = value
            value?.onChangeMonth(selectedMonth)
        }

    interface Listener {
        fun onChangeMonth(yearMonth: YearMonth)
        fun onNavigateToOverview()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
        DataBindingUtil
            .inflate<ViewDataBinding>(layoutInflater, R.layout.fragment_calendar, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                setVariable(BR.listener, listener)
            }
            .also { binding = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initialize(viewLifecycleOwner)

//        recyclerView.apply {
//
//        }

        val daysOfWeek = daysOfWeekFromLocale()

        val currentMonth = YearMonth.now()
        calendarView.setup(
            startMonth = currentMonth.minusMonths(10), //  TODO
            endMonth = currentMonth.plusMonths(0),
            firstDayOfWeek = daysOfWeek.first()
        )

        // TODO: make dynamic
        calendarView.dayWidth = 1080 / 7
        calendarView.dayHeight = (1314 - 83) / 6

//        calendarView.addOnLayoutChangeListener { it: View, i: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int ->
//            calendarView.dayWidth = it.width / 7
//            calendarView.dayHeight = it.height / 6
//        }

        calendarView.scrollToMonth(selectedMonth ?: currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay

            val textView = view.calendarDayText
            val frame = view.calendarDayFrame

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

                context?.let { context ->
//                    if (day.date == LocalDate.of(2020, 5, 17)) {
                        viewModel.getPlaysByDay(day).observe(viewLifecycleOwner, Observer { plays ->
                            container.frame.removeAllViews()
                            container.frame.addView(
                                CalendarDayView(context).apply {
                                    viewModel.getGamesFromPlays(plays).observe(viewLifecycleOwner, Observer { games ->
                                        setGames(games, viewLifecycleOwner)
                                    })
                                },
                                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            )
                        })
//                    }
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
            selectedMonth = month.yearMonth
        }
    }
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
