package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.karumi.weak.weak
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.calendar_day.view.calendarDay
import kotlinx.android.synthetic.main.calendar_day.view.calendarDayFrame
import kotlinx.android.synthetic.main.calendar_day.view.calendarDayText
import kotlinx.android.synthetic.main.calendar_header.view.legendLayout
import kotlinx.android.synthetic.main.fragment_history_calendar.history_calendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

class HistoryCalendarFragment : Fragment() {

    interface Listener {
        fun onNavigateToMonth(yearMonth: YearMonth)
        fun onSelectDate(date: LocalDate)
    }

    private lateinit var listener: Listener

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_history_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
    }

    private fun setup() {

        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()
        val selectedMonth = viewModel.selectedMonth.value

        // Hide calendar initially. This will speed up navigation to this fragment,
        // and a placeholder is shown while the calendar is initializing.
        hideCalendar()

        history_calendar.setup(
            startMonth = currentMonth.minusMonths(10), //  TODO
            endMonth = currentMonth.plusMonths(0),
            firstDayOfWeek = daysOfWeek.first()
        )

        history_calendar.scrollToMonth(selectedMonth ?: currentMonth)

        setCalendarDayDimensions()

        context?.let { context ->
            history_calendar.dayBinder = CalendarDayBinder(context, viewLifecycleOwner, viewModel, listener)
            history_calendar.monthHeaderBinder = CalendarMonthHeaderBinder(daysOfWeek)
        }

        var lastSelectedMonth = selectedMonth
        var monthListenerEnabled = true

        history_calendar.monthScrollListener = { month ->
            // When the calendar is opened on a different month than what it was
            // displaying previously, it will trigger this listener with the
            // previous month, causing us to switch back to it here. So that's
            // why we ignore this right after opening the calendar.
            if (monthListenerEnabled) {
                listener.onNavigateToMonth(month.yearMonth)
            }
            monthListenerEnabled = true
        }

        // View model listeners

        var previousDate: LocalDate? = null
        viewModel.selectedDate.observe(viewLifecycleOwner, Observer { date ->
            previousDate?.let { history_calendar?.notifyCalendarChanged() }
            previousDate = date
            date?.let { history_calendar?.notifyDateChanged(it) }
        })

        viewModel.selectedMonth.observe(viewLifecycleOwner, Observer { yearMonth ->
            // When a different month is opened, ignore the month listener
            // next time (this is a bit of a hack)
            if (yearMonth != lastSelectedMonth) {
                monthListenerEnabled = false
            }
            lastSelectedMonth = yearMonth
            yearMonth?.let { history_calendar.scrollToMonth(it) }
        })
    }

    private fun setCalendarDayDimensions() {

        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        (displayMetrics.widthPixels / 7).let {
            history_calendar.dayWidth = it
            history_calendar.dayHeight = it + 44 // add height of text box containing day number
        }
    }

    fun showCalendar() {
        history_calendar.visibility = View.VISIBLE
    }

    fun hideCalendar() {
        history_calendar.visibility = View.GONE
    }
}

private class DayViewContainer(view: View, listener: HistoryCalendarFragment.Listener?) : ViewContainer(view) {
    lateinit var day: CalendarDay

    val layout: LinearLayout = view.calendarDay
    val textView: TextView = view.calendarDayText
    val frame: FrameLayout = view.calendarDayFrame

    init {
        view.setOnClickListener {
            listener?.onSelectDate(day.date)
        }
    }
}

private class CalendarDayBinder(
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    viewModel: HistoryViewModel,
    listener: HistoryCalendarFragment.Listener?
): DayBinder<DayViewContainer> {

    private val context by weak(context)
    private val viewLifecycleOwner by weak(viewLifecycleOwner)
    private val viewModel by weak(viewModel)
    private val listener by weak(listener)

    private val today = LocalDate.now()

    override fun create(view: View) = DayViewContainer(view, listener)
    override fun bind(container: DayViewContainer, day: CalendarDay) {

        container.day = day
        container.textView.text = day.date.dayOfMonth.toString()

        val context = context ?: return
        val viewLifecycleOwner = viewLifecycleOwner ?: return
        val viewModel = viewModel ?: return

//        container.textView.setTextColor(ContextCompat.getColor(context,
//            if (day.owner == DayOwner.THIS_MONTH)
//                R.color.primary_text
//            else
//                R.color.subtle_text
//        ))
//
//        container.frame.alpha = if (day.owner == DayOwner.THIS_MONTH) 1f else .3f

        when (day.date) {
            viewModel.selectedDate.value -> {
                container.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.light_blue))
            }
            today -> {

            }
            else -> {
                container.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
            }
        }

        viewModel.getGamesForDay(day.date).observe(viewLifecycleOwner, Observer { games ->
            container.frame.removeAllViews()
            container.frame.addView(
                CalendarDayView(context).apply {
                    setGames(games, viewLifecycleOwner)
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        })
    }
}

private class MonthViewContainer(view: View) : ViewContainer(view) {
    val legendLayout: LinearLayout = view.legendLayout
}

private class CalendarMonthHeaderBinder(
    private val daysOfWeek: Array<DayOfWeek>
) : MonthHeaderFooterBinder<MonthViewContainer> {
    override fun create(view: View) = MonthViewContainer(view)
    override fun bind(container: MonthViewContainer, month: CalendarMonth) {

        // Setup each header day text if we have not done that already.
        if (container.legendLayout.tag == null) {
            container.legendLayout.tag = month.yearMonth
            container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                tv.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase(Locale.ENGLISH)
            }
            month.yearMonth
        }
    }
}

private fun daysOfWeekFromLocale(): Array<DayOfWeek> {
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
