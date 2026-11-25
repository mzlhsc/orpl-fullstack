package com.mzlhsc.orpl.admin.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.BusinessData
import com.mzlhsc.orpl.admin.data.model.GoodsOverview
import com.mzlhsc.orpl.admin.data.model.Order
import com.mzlhsc.orpl.admin.data.model.OrderOverview
import com.mzlhsc.orpl.admin.data.model.PageResult
import com.mzlhsc.orpl.admin.data.model.TurnoverReport
import com.mzlhsc.orpl.admin.databinding.FragmentDashboardBinding
import com.mzlhsc.orpl.admin.databinding.ItemPendingBinding
import com.mzlhsc.orpl.admin.ui.main.MainActivity
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var shopOn = false
    private var shopBusy = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDashboardBinding.bind(view)

        binding.tvDate.text = "${Fmt.isoDate()} ${Fmt.weekdayToday()}"
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { load(true) }
        binding.shopPill.setOnClickListener { toggleShop() }
        binding.tvAllOrders.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.tab_orders)
        }

        paintPlaceholders()
        paintShop()
        load(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun paintPlaceholders() {
        binding.heroTurnover.text = "¥ 0.00"
        binding.heroValidOrders.text = "—"
        binding.heroRate.text = "—"
        binding.heroUnitPrice.text = "—"
        binding.statOrders.bind(R.drawable.ic_nav_orders, com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.GREEN, "有效订单", "—", "单", "今日已完成订单数")
        binding.statRate.bind(R.drawable.ic_rate, com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.BLUE, "订单完成率", "—", "", "有效订单 / 总订单")
        binding.statPrice.bind(R.drawable.ic_price, com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.AMBER, "平均客单价", "—", "", "营业额 / 有效订单")
        binding.statUsers.bind(R.drawable.ic_user, com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.SLATE, "今日新增用户", "—", "人", "注册用户数")
        binding.chartRange.text = "${Fmt.mdLabel(Fmt.isoDate(-6))} - ${Fmt.mdLabel(Fmt.isoDate())}"
        binding.pendingContainer.removeAllViews()
    }

    private fun load(fromPull: Boolean) {
        safeLaunch {
            if (fromPull) binding.refresh.isRefreshing = true
            try {
                coroutineScope {
                    val business = async { runCatching { AdminApi.businessData() } }
                    val overview = async { runCatching { AdminApi.overviewOrders() } }
                    val dish = async { runCatching { AdminApi.overviewDishes() } }
                    val setmeal = async { runCatching { AdminApi.overviewSetmeals() } }
                    val shop = async { runCatching { AdminApi.shopStatus() } }
                    val turnover = async { runCatching { AdminApi.reportTurnover(Fmt.isoDate(-6), Fmt.isoDate()) } }
                    val pending = async { runCatching { AdminApi.orderPage(1, 5, status = 2) } }

                    bindBusiness(business.await().getOrNull())
                    bindOrderOverview(overview.await().getOrNull())
                    bindGoods(dish.await().getOrNull(), setmeal.await().getOrNull())
                    shop.await().getOrNull()?.let {
                        shopOn = it == 1
                        paintShop()
                    }
                    bindTurnover(turnover.await().getOrNull())
                    bindPending(pending.await().getOrNull())
                }
            } finally {
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun bindBusiness(data: BusinessData?) {
        if (data == null) return
        binding.heroTurnover.text = Fmt.money(data.turnover)
        binding.heroValidOrders.text = Fmt.count(data.validOrderCount)
        binding.heroRate.text = Fmt.percent(data.orderCompletionRate)
        binding.heroUnitPrice.text = Fmt.money(data.unitPrice)

        binding.statOrders.bind(
            R.drawable.ic_nav_orders,
            com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.GREEN,
            "有效订单",
            Fmt.count(data.validOrderCount),
            "单",
            "今日已完成订单数",
        )
        binding.statRate.bind(
            R.drawable.ic_rate,
            com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.BLUE,
            "订单完成率",
            Fmt.percent(data.orderCompletionRate),
            "",
            "有效订单 / 总订单",
        )
        binding.statPrice.bind(
            R.drawable.ic_price,
            com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.AMBER,
            "平均客单价",
            Fmt.money(data.unitPrice),
            "",
            "营业额 / 有效订单",
        )
        binding.statUsers.bind(
            R.drawable.ic_user,
            com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.SLATE,
            "今日新增用户",
            Fmt.count(data.newUsers),
            "人",
            "注册用户数",
        )
    }

    /** 待接单 / 已接单数量：概览接口 waitingOrders + deliveredOrders（原派送中字段已改为已接单） */
    private fun bindOrderOverview(data: OrderOverview?) {
        if (data == null) return
        val waiting = data.waitingOrders ?: 0
        val confirmed = data.deliveredOrders ?: 0
        binding.tvAllOrders.text = when {
            waiting > 0 || confirmed > 0 -> "待接单 $waiting · 已接单 $confirmed · 全部 ›"
            else -> "全部订单 ›"
        }
    }

    private fun bindGoods(dish: GoodsOverview?, setmeal: GoodsOverview?) {
        dish?.let {
            binding.dishSold.text = Fmt.count(it.sold)
            binding.dishStop.text = Fmt.count(it.discontinued)
        }
        setmeal?.let {
            binding.setmealSold.text = Fmt.count(it.sold)
            binding.setmealStop.text = Fmt.count(it.discontinued)
        }
    }

    private fun bindTurnover(report: TurnoverReport?) {
        if (report == null) return
        val dates = Fmt.splitCsv(report.dateList)
        val values = Fmt.splitCsv(report.turnoverList).map { it.toFloatOrNull() ?: 0f }
        binding.chartTurnover.setData(dates.map { Fmt.mdLabel(it) }, values)
        if (dates.isNotEmpty()) {
            binding.chartRange.text = "${Fmt.mdLabel(dates.first())} - ${Fmt.mdLabel(dates.last())}"
        }
    }

    private fun bindPending(page: PageResult<Order>?) {
        val container = binding.pendingContainer
        container.removeAllViews()
        val list = page?.items ?: emptyList()
        if (list.isEmpty()) {
            container.addView(emptyHint("没有待接单订单"))
            return
        }
        list.forEach { order ->
            val row = ItemPendingBinding.inflate(layoutInflater, container, false)
            row.pendingNumber.text = order.number ?: "#${order.id}"
            row.pendingSub.text = listOf(
                Fmt.orderTypeLabel(order.orderType),
                order.consignee ?: order.userName ?: "顾客",
                Fmt.dateTime(order.orderTime),
            ).joinToString(" · ")
            row.pendingAmount.text = Fmt.money(order.amount)
            row.btnAccept.setOnClickListener { accept(order) }
            container.addView(row.root)
        }
    }

    private fun emptyHint(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(requireContext(), R.color.muted))
        textSize = 12.5f
        setPadding(0, requireContext().resources.getDimensionPixelSize(R.dimen.gap), 0, requireContext().resources.getDimensionPixelSize(R.dimen.gap))
    }

    private fun accept(order: Order) {
        val id = order.id ?: return
        safeLaunch {
            AdminApi.orderConfirm(id)
            toast("接单成功")
            (activity as? MainActivity)?.refreshOrderBadge()
            load(false)
        }
    }

    private fun paintShop() {
        binding.shopDot.setBackgroundResource(if (shopOn) R.drawable.bg_dot_ok else R.drawable.bg_dot_off)
        binding.shopText.text = if (shopOn) "营业中" else "打烊中"
        binding.shopText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (shopOn) R.color.success else R.color.danger,
            )
        )
        binding.shopPill.setBackgroundResource(R.drawable.bg_pill)
    }

    private fun toggleShop() {
        if (shopBusy) return
        shopBusy = true
        safeLaunch {
            try {
                val next = if (shopOn) 0 else 1
                AdminApi.shopSet(next)
                shopOn = next == 1
                paintShop()
                toast(if (shopOn) "店铺已开始营业" else "店铺已打烊")
            } finally {
                shopBusy = false
            }
        }
    }
}
