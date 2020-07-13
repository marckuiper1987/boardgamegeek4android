package com.boardgamegeek.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import kotlinx.android.synthetic.main.fragment_history.history_calendar_frame
import kotlinx.android.synthetic.main.fragment_history.history_detail
import kotlinx.android.synthetic.main.fragment_history.history_overview_frame
import java.time.LocalDate
import java.time.YearMonth

interface CalendarViewListener {
    fun onNavigateToOverview()
}

class HistoryFragment :
    Fragment(),
    CalendarViewListener,
    HistoryOverviewAdapter.Listener,
    HistoryCalendarFragment.Listener,
    DayViewContainer.Listener {

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    private val playsViewModel by activityViewModels<PlaysViewModel>()

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
            summaryList = history_overview_frame,
            detail = history_detail,
            calendar = history_calendar_frame,
            animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)
        )

        setupOverview()
        setupCalendar()
        setupPlaysList()
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
        selectedMonth = yearMonth
        navigator.navigateToCalendar(yearMonth)
        navigator.markMonthLoaded(yearMonth)
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

    private fun setupOverview() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_overview_frame, HistoryOverviewFragment(this))
            .commit()
    }

    private fun setupCalendar() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.history_calendar_frame, HistoryCalendarFragment(this, this))
            .commit()
    }

    private fun setupPlaysList() {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.playsList, PlaysFragment.newInstanceForDay())
            .commit()
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
