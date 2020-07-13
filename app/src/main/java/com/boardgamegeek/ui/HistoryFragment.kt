package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.fragment_history.sliding_layout
import java.time.LocalDate
import java.time.YearMonth

interface CalendarViewListener {
    fun onNavigateToOverview()
}

class HistoryFragment :
    Fragment(),
    CalendarViewListener,
    HistoryOverviewAdapter.Listener,
    HistoryCalendarFragment.Listener {

    interface Listener {
        fun onChangeMonth(yearMonth: YearMonth?)
    }

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    private val playsViewModel by activityViewModels<PlaysViewModel>()

    private var binding: ViewDataBinding? = null

    private var calendarFragment: HistoryCalendarFragment? = null

    var listener: Listener? = null
        set(value) {
            field = value
            value?.onChangeMonth(selectedMonth)
        }

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

        setupOverview()
        setupCalendar()
        setupPlaysList()
        setupNavigation()
    }

    // ----------------------------
    // View events
    // ----------------------------

    private var selectedDate: LocalDate? = null
        set(date) {
            field = date
            viewModel.selectedDate.value = date
            playsViewModel.setDate(date.toString())
        }

    private var selectedMonth: YearMonth? = null
        set(yearMonth) {
            field = yearMonth
            viewModel.selectedMonth.value = yearMonth

            selectedDate = null
            listener?.onChangeMonth(yearMonth)
        }

    override fun onSelectDate(date: LocalDate) {
        selectedDate = date
    }

    override fun onNavigateToMonth(yearMonth: YearMonth) {
        backPressedCallback.isEnabled = true
        navigateToCalendar(yearMonth)
        selectedMonth = yearMonth
    }

    override fun onNavigateToOverview() {
        backPressedCallback.isEnabled = false
        navigateToOverview()
        selectedMonth = null
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            onNavigateToOverview()
        }
    }

    // ----------------------------
    // Setup
    // ----------------------------

    private fun setupOverview() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_overview_frame, HistoryOverviewFragment(this))
            .commit()
    }

    private fun setupCalendar() {
        calendarFragment = HistoryCalendarFragment(this).also {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.history_calendar_frame, it)
                .commit()
        }
    }

    private fun setupPlaysList() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.playsList, PlaysFragment.newInstanceForDay())
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

        sliding_layout.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) { }
            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        calendarFragment?.showCalendar()
                        viewModel.selectedMonth.value?.let { monthsLoaded.add(it) }
                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        calendarFragment?.hideCalendar()
                    }
                    else -> { }
                }
            }
        })
    }

    private fun navigateToOverview() {
        sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    private fun navigateToCalendar(yearMonth: YearMonth) {
        sliding_layout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED

        // If this month was shown before, make the calendar visible immediately.
        // Otherwise, it is made visible after panel has finished sliding up.
        if (monthsLoaded.contains(yearMonth)) {
            calendarFragment?.showCalendar()
        }
    }
}
