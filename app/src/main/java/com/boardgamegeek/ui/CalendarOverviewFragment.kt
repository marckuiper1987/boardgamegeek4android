package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

/**
 * A fragment representing a list of Items.
 */
class CalendarOverviewFragment(
    private val listener: Listener
) : Fragment() {

    private val viewModel by activityViewModels<CalendarViewModel>()

    interface Listener {
        fun onSelectMonth(yearMonth: YearMonth)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_calendar_overview, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = ListAdapter(viewModel, viewLifecycleOwner, listener)
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initialize(viewLifecycleOwner)
    }
}

private class ListAdapter(
    private val viewModel: CalendarViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val listener: CalendarOverviewFragment.Listener
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    var monthCount = 0
    val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

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

    inner class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
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
