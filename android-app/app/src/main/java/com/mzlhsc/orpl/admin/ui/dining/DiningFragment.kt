package com.mzlhsc.orpl.admin.ui.dining

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.databinding.FragmentDiningBinding

/**
 * 堂食管理：桌台 / 就餐会话 / 预约 / 经营报表。
 * 对应 Web 端 html/admin/js/pages/dining.js。
 */
class DiningFragment : Fragment(R.layout.fragment_dining) {

    private var _binding: FragmentDiningBinding? = null
    private val binding get() = _binding!!
    private var mediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDiningBinding.bind(view)

        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> DiningTablesFragment()
                1 -> DiningSessionsFragment()
                2 -> DiningReservationsFragment()
                else -> DiningReportFragment()
            }
        }
        binding.pager.offscreenPageLimit = 3

        val titles = listOf("桌台", "会话", "预约", "报表")
        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = titles[position]
        }.also { it.attach() }
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        binding.pager.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
