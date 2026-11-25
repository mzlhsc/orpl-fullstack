package com.mzlhsc.orpl.admin.ui.report

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.SalesTop10Report
import com.mzlhsc.orpl.admin.databinding.ActivityReportBinding
import com.mzlhsc.orpl.admin.databinding.ItemTopBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding

    private val presets = listOf(
        ChipItem("今日", 0),
        ChipItem("近 7 天", 6),
        ChipItem("近 30 天", 29),
    )
    private var selectedPreset = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.toolbarTitle.text = "数据统计"
        binding.toolbar.toolbarAction.text = "刷新"
        binding.toolbar.toolbarAction.visibility = View.VISIBLE
        binding.toolbar.toolbarAction.setOnClickListener { load() }
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        renderChips()
        load()
    }

    private fun renderChips() {
        Chips.render(binding.chipRow, presets, selectedPreset) { index ->
            selectedPreset = index
            renderChips()
            load()
        }
    }

    private fun load() {
        val days = presets[selectedPreset].value as Int
        val begin = Fmt.isoDate(-days)
        val end = Fmt.isoDate()

        lifecycleScope.launch {
            try {
                coroutineScope {
                    val turnover = async { runCatching { AdminApi.reportTurnover(begin, end) } }
                    val orders = async { runCatching { AdminApi.reportOrders(begin, end) } }
                    val users = async { runCatching { AdminApi.reportUsers(begin, end) } }
                    val top10 = async { runCatching { AdminApi.reportTop10(begin, end) } }

                    val turnoverData = turnover.await().getOrNull()
                    val orderData = orders.await().getOrNull()
                    val userData = users.await().getOrNull()

                    val dates = Fmt.splitCsv(turnoverData?.dateList)
                    val turnovers = Fmt.splitCsv(turnoverData?.turnoverList).map { it.toFloatOrNull() ?: 0f }
                    val totalTurnover = turnovers.sumOf { it.toDouble() }
                    val newUsers = Fmt.splitCsv(userData?.newUserList).sumOf { it.toIntOrNull() ?: 0 }

                    binding.statTurnover.bind(
                        R.drawable.ic_money,
                        com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.BRAND,
                        "营业额合计",
                        Fmt.money(totalTurnover),
                        "",
                        "$begin ~ $end",
                    )
                    binding.statOrders.bind(
                        R.drawable.ic_nav_orders,
                        com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.GREEN,
                        "订单总数",
                        Fmt.count(orderData?.totalOrderCount),
                        "单",
                        "有效 ${Fmt.count(orderData?.validOrderCount)} 单",
                    )
                    binding.statRate.bind(
                        R.drawable.ic_rate,
                        com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.BLUE,
                        "订单完成率",
                        Fmt.percent(orderData?.orderCompletionRate),
                        "",
                        "有效订单 / 总订单",
                    )
                    binding.statUsers.bind(
                        R.drawable.ic_user,
                        com.mzlhsc.orpl.admin.widget.StatCardView.IconTone.AMBER,
                        "新增用户",
                        Fmt.count(newUsers),
                        "人",
                        "$begin ~ $end",
                    )

                    binding.chartRange.text = "${Fmt.mdLabel(begin)} - ${Fmt.mdLabel(end)}"
                    binding.chartTurnover.setData(dates.map { Fmt.mdLabel(it) }, turnovers)

                    // 订单趋势：与 Web 报表的「订单总数 / 有效订单」对比图对应
                    val orderDates = Fmt.splitCsv(orderData?.dateList).ifEmpty { dates }
                    val totalOrders = Fmt.splitCsv(orderData?.orderCountList).map { it.toFloatOrNull() ?: 0f }
                    val validOrders = Fmt.splitCsv(orderData?.validOrderCountList).map { it.toFloatOrNull() ?: 0f }
                    binding.ordersHint.text = "总数 ${Fmt.count(orderData?.totalOrderCount)} · 有效 ${Fmt.count(orderData?.validOrderCount)}"
                    binding.chartOrders.setData(orderDates.map { Fmt.mdLabel(it) }, validOrders.ifEmpty { totalOrders })

                    // 用户增长：每日新增用户
                    val userDates = Fmt.splitCsv(userData?.dateList).ifEmpty { dates }
                    val dailyNew = Fmt.splitCsv(userData?.newUserList).map { it.toFloatOrNull() ?: 0f }
                    val totalUser = Fmt.splitCsv(userData?.totalUserList).lastOrNull()?.toIntOrNull()
                        ?: Fmt.splitCsv(userData?.totalUserList).sumOf { it.toIntOrNull() ?: 0 }
                    binding.usersHint.text = "总用户 ${Fmt.count(totalUser)} · 新增 ${Fmt.count(newUsers)}"
                    binding.chartUsers.setData(userDates.map { Fmt.mdLabel(it) }, dailyNew)

                    bindTop10(top10.await().getOrNull())
                }
            } catch (e: Exception) {
                toast(e.message ?: "统计数据加载失败")
            }
        }
    }

    private fun bindTop10(report: SalesTop10Report?) {
        val container = binding.topContainer
        container.removeAllViews()

        val names = Fmt.splitCsv(report?.nameList)
        val numbers = Fmt.splitCsv(report?.numberList).map { it.toIntOrNull() ?: 0 }
        if (names.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "统计区间内暂无销量数据"
                setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.muted))
                textSize = 12.5f
                setPadding(0, resources.getDimensionPixelSize(R.dimen.gap), 0, resources.getDimensionPixelSize(R.dimen.gap))
            })
            return
        }

        val max = numbers.maxOrNull()?.coerceAtLeast(1) ?: 1
        names.forEachIndexed { index, name ->
            val row = ItemTopBinding.inflate(layoutInflater, container, false)
            val count = numbers.getOrNull(index) ?: 0
            row.tvRank.text = (index + 1).toString()
            if (index < 3) {
                row.tvRank.setBackgroundResource(R.drawable.bg_tag_brand)
                row.tvRank.setTextColor(ContextCompat.getColor(this, R.color.brand_strong))
            }
            row.tvName.text = name
            row.tvCount.text = "$count 份"
            row.bar.progress = (count * 100 / max).coerceIn(0, 100)
            container.addView(row.root)
        }
    }
}
