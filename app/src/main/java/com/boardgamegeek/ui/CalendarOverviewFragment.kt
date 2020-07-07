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

/**
 * A fragment representing a list of Items.
 */
class CalendarOverviewFragment : Fragment() {

    private val viewModel by activityViewModels<CalendarViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_calendar_overview, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = ListAdapter(viewModel, viewLifecycleOwner)
            }
        }
        return view
    }
}

private class ListAdapter(
    viewModel: CalendarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    var monthCount = 0
    var statsPerMonth = mapOf<Int, LiveData<PlayStatsForMonth>>()

    init {
        viewModel
            .getNumberOfMonthsBetweenFirstPlayAndNow()
            .observe(viewLifecycleOwner, Observer { monthCount = it })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int) = R.layout.fragment_calendar_overview_item

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(statsPerMonth[position])
    }

    override fun getItemCount(): Int = monthCount

    inner class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: LiveData<PlayStatsForMonth>?) {
            binding.setVariable(BR.stats, stats)
        }
    }
}
