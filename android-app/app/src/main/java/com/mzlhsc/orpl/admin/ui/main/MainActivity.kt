package com.mzlhsc.orpl.admin.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.Prefs
import com.mzlhsc.orpl.admin.databinding.ActivityMainBinding
import com.mzlhsc.orpl.admin.ui.dashboard.DashboardFragment
import com.mzlhsc.orpl.admin.ui.dining.DiningFragment
import com.mzlhsc.orpl.admin.ui.goods.GoodsFragment
import com.mzlhsc.orpl.admin.ui.login.LoginActivity
import com.mzlhsc.orpl.admin.ui.more.MoreFragment
import com.mzlhsc.orpl.admin.ui.order.OrdersFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Prefs.loggedIn) {
            goLogin()
            return
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.tab_dashboard
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.loggedIn) {
            goLogin()
            return
        }
        refreshOrderBadge()
    }

    /** 供页面内跳转：例如工作台「全部订单」 */
    fun selectTab(itemId: Int) {
        if (binding.bottomNav.selectedItemId == itemId) {
            showTab(itemId)
        } else {
            binding.bottomNav.selectedItemId = itemId
        }
    }

    private fun showTab(itemId: Int): Boolean {
        val tag = when (itemId) {
            R.id.tab_dashboard -> TAG_DASHBOARD
            R.id.tab_orders -> TAG_ORDERS
            R.id.tab_dining -> TAG_DINING
            R.id.tab_goods -> TAG_GOODS
            R.id.tab_more -> TAG_MORE
            else -> return false
        }
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        fm.fragments.forEach { transaction.hide(it) }
        val existing = fm.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.fragment_container, createFragment(tag), tag)
        } else {
            transaction.show(existing)
        }
        transaction.commit()
        return true
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        TAG_ORDERS -> OrdersFragment()
        TAG_DINING -> DiningFragment()
        TAG_GOODS -> GoodsFragment()
        TAG_MORE -> MoreFragment()
        else -> DashboardFragment()
    }

    fun refreshOrderBadge() {
        lifecycleScope.launch {
            val stats = runCatching { AdminApi.orderStatistics() }.getOrNull() ?: return@launch
            val waiting = stats.toBeConfirmed ?: 0
            if (waiting > 0) {
                val badge = binding.bottomNav.getOrCreateBadge(R.id.tab_orders)
                badge.isVisible = true
                badge.number = waiting
                badge.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.brand)
                badge.badgeTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)
            } else {
                binding.bottomNav.removeBadge(R.id.tab_orders)
            }
        }
    }

    private fun goLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    companion object {
        private const val TAG_DASHBOARD = "dashboard"
        private const val TAG_ORDERS = "orders"
        private const val TAG_DINING = "dining"
        private const val TAG_GOODS = "goods"
        private const val TAG_MORE = "more"
    }
}
