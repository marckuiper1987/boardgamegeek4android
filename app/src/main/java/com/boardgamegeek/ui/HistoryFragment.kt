package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.fragment_history.history_play_stats_frame
import kotlinx.android.synthetic.main.fragment_history.history_plays_frame
import kotlinx.android.synthetic.main.fragment_history.scroll_view
import kotlinx.android.synthetic.main.fragment_history.sliding_layout
import java.time.LocalDate
import java.time.YearMonth

class HistoryFragment :
    Fragment(),
    HistoryOverviewFragment.Listener,
    HistoryCalendarFragment.Listener {

    interface Listener {
        fun onChangeMonth(yearMonth: YearMonth?)
    }

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    private val playsViewModel by activityViewModels<PlaysViewModel>()

    private var calendarFragment: HistoryCalendarFragment? = null

    private var listener: Listener? = null
        set(value) {
            field = value
            value?.onChangeMonth(selectedMonth)
        }

    // ----------------------------
    // Fragment events
    // ----------------------------

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as Listener
    }

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
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedMonth.value = YearMonth.now()

        setupOverview()
        setupCalendar()
        setupPlayStats()
        setupPlaysList()
        setupNavigation()
    }

    // ----------------------------
    // View events
    // ----------------------------

    private var selectedDate: LocalDate? = null
        set(date) {
            val oldDate = field
            field = date
            if (date != oldDate) {
                viewModel.selectedDate.value = date
                playsViewModel.setDate(date.toString())
            }
            switchPlaysListVisibility(oldDate, date)
        }

    private var selectedMonth: YearMonth? = null
        set(yearMonth) {
            field = yearMonth
            viewModel.selectedMonth.value = yearMonth
            selectedDate = null
            listener?.onChangeMonth(yearMonth)
        }

    override fun onSelectDate(date: LocalDate) {
        selectedDate = if (date != selectedDate) date else null
    }

    override fun onNavigateToMonth(yearMonth: YearMonth) {
        navigateToCalendar(yearMonth)
        selectedMonth = yearMonth
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            navigateToOverview()
            selectedMonth = null
        }
    }

    // ----------------------------
    // Setup
    // ----------------------------

    private fun setupOverview() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_overview_frame, HistoryOverviewFragment())
            .commit()
    }

    private fun setupCalendar() {
        calendarFragment = HistoryCalendarFragment().also {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.history_calendar_frame, it)
                .commit()
        }
    }

    private fun setupPlayStats() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_play_stats_frame, HistoryPlayStatsFragment())
            .commit()
    }

    private fun setupPlaysList() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_plays_frame, PlaysFragment.newInstanceForDay())
            .commit()
    }

    // ----------------------------
    // Navigation
    // ----------------------------

    private val monthsLoaded = mutableSetOf<YearMonth>()

    private fun setupNavigation() {

        viewModel.selectedMonth.observe(viewLifecycleOwner, Observer {
            it?.let { monthsLoaded.add(it) }
        })

        history_plays_frame.isVisible = false

        sliding_layout.setScrollableView(scroll_view)
        sliding_layout.isTouchEnabled = false

        sliding_layout.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) { }
            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
                when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        calendarFragment?.showCalendar()
                        viewModel.selectedMonth.value?.let { monthsLoaded.add(it) }
                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        calendarFragment?.hideCalendar()
                        selectedMonth = null
                    }
                    else -> { }
                }
            }
        })
    }

    private fun navigateToOverview() {
        backPressedCallback.isEnabled = false
        sliding_layout.isTouchEnabled = false
        sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    private fun navigateToCalendar(yearMonth: YearMonth) {
        backPressedCallback.isEnabled = true
        sliding_layout.isTouchEnabled = true
        sliding_layout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED

        // If this month was shown before, make the calendar visible immediately.
        // Otherwise, it is made visible after panel has finished sliding up.
        if (monthsLoaded.contains(yearMonth)) {
            calendarFragment?.showCalendar()
        }
    }

    private fun switchPlaysListVisibility(oldDate: LocalDate?, date: LocalDate?) {
        if (oldDate == null && date != null) {
            history_play_stats_frame.fadeOut()
            history_plays_frame.fadeIn()
        }
        else if (date == null && oldDate != null) {
            history_plays_frame.fadeOut()
            history_play_stats_frame.fadeIn()
        }
    }
}
