package com.mzlhsc.orpl.admin.ui.goods

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.databinding.FragmentGoodsBinding

class GoodsFragment : Fragment(R.layout.fragment_goods) {

    private var _binding: FragmentGoodsBinding? = null
    private val binding get() = _binding!!
    private var mediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGoodsBinding.bind(view)

        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) DishListFragment() else SetmealListFragment()
        }

        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = if (position == 0) "菜品" else "套餐"
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
