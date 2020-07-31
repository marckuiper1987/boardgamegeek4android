package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.android.synthetic.main.fragment_history_overview.history_overview_list
import kotlinx.android.synthetic.main.fragment_history_overview_item.view.background_image
import java.time.YearMonth


class HistoryOverviewFragment : Fragment() {

    interface Listener {
        fun onNavigateToMonth(yearMonth: YearMonth)
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
        inflater.inflate(R.layout.fragment_history_overview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
    }

    private fun setup() {
        history_overview_list.let {

            val adapter = HistoryOverviewAdapter(viewModel, viewLifecycleOwner, requireContext(), listener)

            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
            it.setHasFixedSize(true)
            it.setItemViewCacheSize(20)

            val itemDecoration = RecyclerSectionItemDecoration(
                headerOffset = resources.getDimensionPixelSize(R.dimen.recycler_section_header_height_large),
                sectionCallback = adapter,
                layout = R.layout.row_header_large
            )
            while (it.itemDecorationCount > 0) {
                it.removeItemDecorationAt(0)
            }
            it.addItemDecoration(itemDecoration)
        }
    }
}

class HistoryOverviewAdapter(
    private val viewModel: HistoryViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val listener: HistoryOverviewFragment.Listener?
) : RecyclerView.Adapter<HistoryOverviewAdapter.ViewHolder>(),
    RecyclerSectionItemDecoration.SectionCallback {

    private var monthCount = 0L

    init {
//        setHasStableIds(true)
        viewModel
            .numberOfMonthsBetweenFirstPlayAndNow
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
        holder.bind(yearMonth)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
    }

    override fun getItemCount(): Int = monthCount.toInt()

    inner class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(yearMonth: YearMonth) {
            binding.apply {
                setVariable(BR.viewModel, viewModel)
                setVariable(BR.listener, listener)
                setVariable(BR.yearMonth, yearMonth)
            }

            viewModel.getStatsForMonth(yearMonth).observe(viewLifecycleOwner, Observer { stats ->
                stats.mostPlayedGame?.observe(viewLifecycleOwner, Observer { game ->

                    Picasso
                        .with(context)
                        .apply {
                            setIndicatorsEnabled(true)
                            isLoggingEnabled = true
                        }
                        .load(game.imageUrl.ensureHttpsScheme())
                        .transform(object : Transformation {
                            override fun key(): String = "test()"

                            override fun transform(source: Bitmap): Bitmap {

                                val result = Bitmap.createBitmap(source.width, source.height, source.config)

                                val palette = Palette.from(source).generate()

                                val rgb = palette.getDarkVibrantColor(palette.getDarkMutedColor(context.resources.getColor(R.color.black_overlay)))

                                val canvas = Canvas(result)

                                canvas.drawBitmap(source, 0f, 0f, null)

                                canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), Paint().apply {
                                    color = rgb
                                    alpha = (255 * 70) / 100
                                    style = Paint.Style.FILL
                                })

                                if (result != source) {
                                    source.recycle()
                                }

                                return result
                            }

                        })
                        .placeholder(R.color.black_overlay_light)
                        .fit()
                        .centerCrop()
                        .into(binding.root.background_image)
                })
            })
        }
        fun unbind() {
            binding.apply {
                setVariable(BR.viewModel, null)
                setVariable(BR.listener, null)
                setVariable(BR.yearMonth, null)
            }
        }
    }

    override fun isSection(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        if (itemCount == 0) return false
        if (position < 0 || position >= itemCount) return false
        if (position == 0) return true
        val thisLetter = getSectionHeader(position)
        val lastLetter = getSectionHeader(position - 1)
        return thisLetter != lastLetter
    }

    override fun getSectionHeader(position: Int): CharSequence {
        val yearMonth = YearMonth.now().minusMonths(position.toLong())
        return yearMonth.year.toString()
    }
}
