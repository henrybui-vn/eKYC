package com.android.master.kyc.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.master.kyc.R
import com.android.master.kyc.model.Category
import com.android.master.kyc.ui.adapter.MainListAdapter
import com.android.master.kyc.utils.EXTRA_1
import com.android.master.kyc.utils.EXTRA_2
import kotlinx.android.synthetic.main.main_fragment.*

var animationPlaybackSpeed: Double = 0.8

class MainFragment : Fragment() {

    lateinit var viewModel: MainViewModel

    private lateinit var mainListAdapter: MainListAdapter

    private val loadingDuration: Long
        get() = (resources.getInteger(R.integer.loadingAnimDuration) / animationPlaybackSpeed).toLong()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        initUI()
        observeChanges()
    }

    private fun initUI() {
        //Init UI
        val data = listOf(Category("Chứng minh thư, Thẻ căn cước"),
            Category("Hộ chiếu"),
            Category("Chứng minh thư quân đội"),
            Category("Bằng lái xe")
        )
        mainListAdapter = MainListAdapter(
            requireContext(),
            data,
            viewModel::clickItem
        )
        recyclerView.adapter = mainListAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        updateRecyclerViewAnimDuration()
    }

    private fun observeChanges() {
        viewModel.typeData.observe(viewLifecycleOwner, Observer {
            val intent = Intent(requireActivity(), GetPhotoActivity::class.java)
            intent.putExtra(EXTRA_1, it)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity()).toBundle())
        })
    }

    /**
     * Update RecyclerView Item Animation Durations
     */
    private fun updateRecyclerViewAnimDuration() = recyclerView.itemAnimator?.run {
        removeDuration = loadingDuration * 60 / 100
        addDuration = loadingDuration
    }
}
