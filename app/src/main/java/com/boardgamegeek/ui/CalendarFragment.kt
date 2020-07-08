package com.boardgamegeek.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.karumi.weak.weak
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
import kotlinx.android.synthetic.main.fragment_calendar.calendar_fragment
import kotlinx.android.synthetic.main.fragment_calendar.calendar_history_list
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.Locale

class CalendarFragment : Fragment(), HistoryListAdapter.Listener, DayViewContainer.Listener {

    private val viewModel by activityViewModels<CalendarViewModel>()
    private var binding: ViewDataBinding? = null
    private var selectedDate: LocalDate? = null

    private var animationDuration: Int = 0

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
                              savedInstanceState: Bundle?): View {

        animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)

        return DataBindingUtil
            .inflate<ViewDataBinding>(layoutInflater, R.layout.fragment_calendar, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
//                setVariable(BR.listener, listener)

                setVariable(BR.listener, object : Listener {
                    override fun onChangeMonth(yearMonth: YearMonth) {
                        TODO("Not yet implemented")
                    }
                    override fun onNavigateToOverview() {
                        crossfade(calendar_fragment, calendar_history_list, false)
                    }
                })
            }
            .also { binding = it }
            .root
    }

    override fun onSelectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            calendarView.notifyDateChanged(date)
            oldDate?.let { calendarView.notifyDateChanged(it) }
            //updateAdapterForDate(day.date)
        }
    }

    private val monthsLoaded = mutableSetOf<YearMonth>()

    override fun onSelectMonth(yearMonth: YearMonth) {
        selectedMonth = yearMonth
        crossfade(calendar_history_list, calendar_fragment, true, yearMonth)
    }

    private fun crossfade(fromView: View, toView: View, up: Boolean, yearMonth: YearMonth? = null) {

        val scaleBy = if (up) .3f else -.3f

        toView.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0f
            visibility = View.VISIBLE

            scaleX = 1 - scaleBy
            scaleY = 1 - scaleBy

            if (yearMonth != null && monthsLoaded.contains(yearMonth)) {
                calendarView.visibility = View.VISIBLE
            }

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                .scaleXBy(scaleBy)
                .scaleYBy(scaleBy)
                .setDuration(animationDuration.toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (up) {
                            calendarView.visibility = View.VISIBLE
                            if (yearMonth != null) {
                                monthsLoaded.add(yearMonth)
                            }
                        }
                    }
                })
        }

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        fromView.animate()
            .alpha(0f)
            .scaleXBy(scaleBy)
            .scaleYBy(scaleBy)
            .setDuration(animationDuration.toLong())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fromView.visibility = View.GONE
                    if (!up) {
                        calendarView.visibility = View.GONE
                    }
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initialize(viewLifecycleOwner)

        calendar_fragment.visibility = View.GONE
        calendarView.visibility = View.GONE

        setupHistoryList()
        setupCalendar()
        setupPlaysList()
    }

    private fun setupHistoryList() {
        with(calendar_history_list) {
            layoutManager = LinearLayoutManager(this@CalendarFragment.context)
            adapter = HistoryListAdapter(viewModel, viewLifecycleOwner, this@CalendarFragment)
        }
    }

    private fun setupCalendar() {

        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()

        calendarView.setup(
            startMonth = currentMonth.minusMonths(10), //  TODO
            endMonth = currentMonth.plusMonths(0),
            firstDayOfWeek = daysOfWeek.first()
        )

        calendarView.scrollToMonth(selectedMonth)

        setCalendarDayDimensions()

        context?.also { context ->
            calendarView.dayBinder = CalendarDayBinder(context, viewLifecycleOwner, viewModel, this)
            calendarView.monthHeaderBinder = CalendarMonthHeaderBinder(daysOfWeek)
        }

        calendarView.monthScrollListener = { month ->
            selectedMonth = month.yearMonth
            monthsLoaded.add(month.yearMonth)
        }
    }

    private fun setCalendarDayDimensions() {

        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        (displayMetrics.widthPixels / 7).let {
            calendarView.dayWidth = it
            calendarView.dayHeight = it + 20
        }
    }

    private fun setupPlaysList() {
//        recyclerView.apply {
//
//        }
    }
}

class HistoryListAdapter(
    private val viewModel: CalendarViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val listener: Listener?
) : RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {

    interface Listener {
        fun onSelectMonth(yearMonth: YearMonth)
    }

    private var monthCount = 0
    private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

    init {
        viewModel
            .getNumberOfMonthsBetweenFirstPlayAndNow()
            .observe(viewLifecycleOwner, Observer {
                monthCount = it
                notifyDataSetChanged()
            })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int) = R.layout.fragment_calendar_overview_item

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val yearMonth = YearMonth.now().minusMonths(position.toLong())
        holder.bind(
            yearMonth = yearMonth,
            stats = viewModel.getStatsForMonth(yearMonth)
        )
    }

    override fun getItemCount(): Int = monthCount

    inner class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(yearMonth: YearMonth, stats: LiveData<PlayStatsForMonth>?) {
            binding.apply {
                setVariable(BR.listener, listener)
                setVariable(BR.yearMonth, yearMonth)
                setVariable(BR.monthString, monthTitleFormatter.format(yearMonth))
                setVariable(BR.stats, stats)
            }
        }
    }
}

class DayViewContainer(view: View, listener: Listener?) : ViewContainer(view) {
    lateinit var day: CalendarDay

    val textView: TextView = view.calendarDayText
    val frame: FrameLayout = view.calendarDayFrame

    interface Listener {
        fun onSelectDate(date: LocalDate)
    }

    init {
        view.setOnClickListener {
            if (day.owner == DayOwner.THIS_MONTH) {
                listener?.onSelectDate(day.date)
            }
        }
    }
}

class CalendarDayBinder(
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    viewModel: CalendarViewModel,
    listener: DayViewContainer.Listener
): DayBinder<DayViewContainer> {

    private val context by weak(context)
    private val viewLifecycleOwner by weak(viewLifecycleOwner)
    private val viewModel by weak(viewModel)
    private val listener by weak(listener)

    override fun create(view: View) = DayViewContainer(view, listener)
    override fun bind(container: DayViewContainer, day: CalendarDay) {

        container.day = day
        container.textView.text = day.date.dayOfMonth.toString()

        val context = context ?: return
        val viewLifecycleOwner = viewLifecycleOwner ?: return
        val viewModel = viewModel ?: return

        // TODO: data binding
        if (day.owner == DayOwner.THIS_MONTH) {
            container.textView.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
        }
        else {
            container.textView.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
        }

//        if (day.date == LocalDate.of(2020, 5, 17)) {
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
//        }
    }
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val legendLayout: LinearLayout = view.legendLayout
}

class CalendarMonthHeaderBinder(
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
