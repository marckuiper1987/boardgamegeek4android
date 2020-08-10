package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.BR
import com.boardgamegeek.R
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.ui.viewmodel.HistoryViewModel
import com.boardgamegeek.ui.viewmodel.HistoryViewModelFactory
import com.boardgamegeek.util.PaletteOverlayTransformation
import kotlinx.android.synthetic.main.fragment_history_play_stats.numberOfPlays
import kotlinx.android.synthetic.main.fragment_history_play_stats_item.view.background_image

class HistoryPlayStatsFragment : Fragment() {

    private val viewModel by activityViewModels<HistoryViewModel> {
        HistoryViewModelFactory(requireActivity().application, viewLifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        DataBindingUtil
            .inflate<ViewDataBinding>(layoutInflater, R.layout.fragment_history_play_stats, container, false)
            .also {
                it.lifecycleOwner = this
                it.setVariable(BR.viewModel, viewModel)
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resources = context?.resources ?: return

        viewModel.selectedMonthStats.observe(viewLifecycleOwner, Observer { liveStats ->
            // FIXME
            liveStats?.observe(viewLifecycleOwner, Observer { stats ->
                stats?.mostPlayedGame?.observe(viewLifecycleOwner, Observer { game ->
                    numberOfPlays.background_image.loadThumbnail(
                        imageUrl = game.imageUrl,
                        showPlaceholder = false,
                        transformation = PaletteOverlayTransformation(
                            defaultColor = resources.getColor(R.color.black_overlay)
                        )
                    )
                })
            })
        })
    }
}
