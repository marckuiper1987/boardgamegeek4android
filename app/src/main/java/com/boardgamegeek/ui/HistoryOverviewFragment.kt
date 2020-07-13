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
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.viewmodel.PlayStatsForMonth
import kotlinx.android.synthetic.main.fragment_history_overview.history_overview_list
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HistoryOverviewFragment(
    private val listener: HistoryOverviewAdapter.Listener
) : Fragment() {

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_history_overview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        history_overview_list.also {
            it.layoutManager = LinearLayoutManager(this.context)
            it.adapter = HistoryOverviewAdapter(viewModel, viewLifecycleOwner, listener)
        }
    }
}

class HistoryOverviewAdapter(
    private val viewModel: HistoryViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val listener: Listener?
) : RecyclerView.Adapter<HistoryOverviewAdapter.ViewHolder>() {

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

    override fun getItemViewType(position: Int) = R.layout.fragment_history_overview_item

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
