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
import androidx.activity.OnBackPressedCallback
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
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.viewmodel.PlayStatsForMonth
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
import kotlinx.android.synthetic.main.fragment_history.history_calendar
import kotlinx.android.synthetic.main.fragment_history.history_detail
import kotlinx.android.synthetic.main.fragment_history.history_summary_list
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.Locale

interface CalendarViewListener {
    fun onNavigateToOverview()
}

class CalendarFragment :
    Fragment(),
    CalendarViewListener,
    HistoryListAdapter.Listener,
    DayViewContainer.Listener {

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }
    private var binding: ViewDataBinding? = null
    private lateinit var navigator: HistoryFragmentNavigator

    // ----------------------------
    // Fragment events
    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, backPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
        DataBindingUtil
            .inflate<ViewDataBinding>(layoutInflater, R.layout.fragment_history, container, false)
            .also {
                it.lifecycleOwner = this
                it.setVariable(BR.viewModel, viewModel)
                it.setVariable(BR.listener, this)
                binding = it
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigator = HistoryFragmentNavigator(
            summaryList = history_summary_list,
            detail = history_detail,
            calendar = history_calendar,
            animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)
        )

        setupHistoryList()
        setupCalendar()
        setupPlaysList()
    }

    // ----------------------------
    // View events
    // ----------------------------

    private var selectedDate: LocalDate? = null
        set(date) {
            field?.let {
                history_calendar?.notifyDateChanged(it)
            }
            field = date
            viewModel.selectedDate.value = date
            date?.let {
                history_calendar?.notifyDateChanged(it)
            }
//                updateAdapterForDate(null)
        }

    private var selectedMonth: YearMonth? = null
        set(yearMonth) {
            field = yearMonth
            viewModel.selectedMonth.value = yearMonth

            selectedDate = null
            listener?.onChangeMonth(yearMonth)
            yearMonth?.let { history_calendar?.scrollToMonth(it) }
        }

    override fun onSelectDate(date: LocalDate) {
        selectedDate = date
    }

    override fun onNavigateToMonth(yearMonth: YearMonth) {
        backPressedCallback.isEnabled = true
        selectedMonth = yearMonth
        navigator.navigateToCalendar(yearMonth)
    }

    override fun onNavigateToOverview() {
        backPressedCallback.isEnabled = false
        selectedMonth = null
        navigator.navigateToOverview()
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            onNavigateToOverview()
        }
    }

    // ----------------------------
    // Setup
    // ----------------------------

    private fun setupHistoryList() {
        history_summary_list.also {
            it.layoutManager = LinearLayoutManager(this.context)
            it.adapter = HistoryListAdapter(viewModel, viewLifecycleOwner, this)
        }
    }

    private fun setupCalendar() {

        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()

        history_calendar.setup(
            startMonth = currentMonth.minusMonths(10), //  TODO
            endMonth = currentMonth.plusMonths(0),
            firstDayOfWeek = daysOfWeek.first()
        )

        history_calendar.scrollToMonth(selectedMonth ?: currentMonth)

        setCalendarDayDimensions()

        context?.also { context ->
            history_calendar.dayBinder = CalendarDayBinder(context, viewLifecycleOwner, viewModel, this)
            history_calendar.monthHeaderBinder = CalendarMonthHeaderBinder(daysOfWeek)
        }

        history_calendar.monthScrollListener = { month ->
            selectedMonth = month.yearMonth
            navigator.markMonthLoaded(month.yearMonth)
        }
    }

    private fun setCalendarDayDimensions() {

        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        (displayMetrics.widthPixels / 7).let {
            history_calendar.dayWidth = it
            history_calendar.dayHeight = it + 44 // add height of text box containing day number
        }
    }

    private fun setupPlaysList() {
//        recyclerView.apply {
//
//        }
    }

    // ----------------------------
    // Listener
    // ----------------------------

    var listener: Listener? = null
        set(value) {
            field = value
            value?.onChangeMonth(selectedMonth)
        }

    interface Listener {
        fun onChangeMonth(yearMonth: YearMonth?)
    }
}

class HistoryFragmentNavigator(
    private val summaryList: View,
    private val detail: View,
    private val calendar: View,
    private val animationDuration: Int
) {
    private val monthsLoaded = mutableSetOf<YearMonth>()

    init {
        detail.visibility = View.GONE
        calendar.visibility = View.GONE
    }

    fun navigateToOverview() {
        fadeIn(summaryList, towardScreen = false, yearMonth = null)
        fadeOut(detail, towardScreen = false)
    }

    fun navigateToCalendar(yearMonth: YearMonth) {
        fadeIn(detail, towardScreen = true, yearMonth = yearMonth)
        fadeOut(summaryList, towardScreen = true)
    }

    fun markMonthLoaded(yearMonth: YearMonth) {
        monthsLoaded.add(yearMonth)
    }

    private fun scaleBy(towardScreen: Boolean) =
        if (towardScreen) .3f else -.3f

    private fun fadeIn(view: View, towardScreen: Boolean, yearMonth: YearMonth?) {
        val scaleBy = scaleBy(towardScreen)
        view.apply {
            alpha = 0f
            visibility = View.VISIBLE

            scaleX = 1 - scaleBy
            scaleY = 1 - scaleBy

            if (yearMonth != null && monthsLoaded.contains(yearMonth)) {
                calendar.visibility = View.VISIBLE
            }

            animate()
                .alpha(1f)
                .scaleXBy(scaleBy)
                .scaleYBy(scaleBy)
                .setDuration(animationDuration.toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (towardScreen) {
                            calendar.visibility = View.VISIBLE
                            if (yearMonth != null) {
                                monthsLoaded.add(yearMonth)
                            }
                        }
                    }
                })
        }
    }

    private fun fadeOut(view: View, towardScreen: Boolean) {
        val scaleBy = scaleBy(towardScreen)
        view.animate()
            .alpha(0f)
            .scaleXBy(scaleBy)
            .scaleYBy(scaleBy)
            .setDuration(animationDuration.toLong())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    if (!towardScreen) {
                        calendar.visibility = View.GONE
                    }
                }
            })
    }
}

class HistoryListAdapter(
    private val viewModel: HistoryViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val listener: Listener?
) : RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {

    interface Listener {
        fun onNavigateToMonth(yearMonth: YearMonth)
    }

    private var monthCount = 0
    private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

    init {
        viewModel
            .getNumberOfMonthsBetweenFirstPlayAndNow()
            .observe(viewLifecycleOwner, Observer {
                if (it != monthCount) {
                    monthCount = it
                    notifyDataSetChanged()
                }
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

    val layout: LinearLayout = view.calendarDay
    val textView: TextView = view.calendarDayText
    val frame: FrameLayout = view.calendarDayFrame

    interface Listener {
        fun onSelectDate(date: LocalDate)
    }

    init {
        view.setOnClickListener {
            listener?.onSelectDate(day.date)
        }
    }
}

class CalendarDayBinder(
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    viewModel: HistoryViewModel,
    listener: DayViewContainer.Listener
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
                container.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.border))
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
