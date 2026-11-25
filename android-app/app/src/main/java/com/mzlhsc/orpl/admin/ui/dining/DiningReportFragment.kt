package com.mzlhsc.orpl.admin.ui.dining

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.DiningDaily
import com.mzlhsc.orpl.admin.data.model.DiningReport
import com.mzlhsc.orpl.admin.databinding.FragmentDiningReportBinding
import com.mzlhsc.orpl.admin.databinding.ViewDetailRowBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.visibleIf
import com.mzlhsc.orpl.admin.widget.StatCardView

class DiningReportFragment : androidx.fragment.app.Fragment(R.layout.fragment_dining_report) {

    private var _binding: FragmentDiningReportBinding? = null
    private val binding get() = _binding!!

    private val chips = listOf(
        ChipItem("今日", 0),
        ChipItem("近 7 天", 6),
        ChipItem("近 30 天", 29),
    )
    private var selectedChip = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDiningReportBinding.bind(view)

        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { load() }
        binding.cardTurnover.bind(R.drawable.ic_report, StatCardView.IconTone.GREEN, "桌台周转率", "—", "", "已结账会话 / 桌台数")
        binding.cardTables.bind(R.drawable.ic_nav_dining, StatCardView.IconTone.BLUE, "桌台总数", "—", "张", "含所有区域")

        renderChips()
        load()
    }

    private fun renderChips() {
        Chips.render(binding.chipRow, chips, selectedChip) { index ->
            selectedChip = index
            renderChips()
            load()
        }
    }

    private fun load() {
        val offset = chips[selectedChip].value as Int
        val beginDate = Fmt.isoDate(-offset)
        val endDate = Fmt.isoDate()
        binding.refresh.isRefreshing = true
        safeLaunch {
            try {
                val report = AdminApi.diningReport("$beginDate 00:00:00", "$endDate 23:59:59")
                bind(report, beginDate, endDate)
            } finally {
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun bind(report: DiningReport?, begin: String, end: String) {
        binding.tvRange.text = "$begin 至 $end"
        if (report == null) {
            binding.empty.root.visibleIf(true)
            binding.empty.emptyTitle.text = "暂无堂食数据"
            binding.empty.emptyDesc.text = "换个时间范围再试试"
            return
        }
        binding.tvRevenue.text = Fmt.money(report.totalRevenue)
        binding.tvSessions.text = Fmt.count(report.sessionCount)
        binding.tvGuests.text = Fmt.count(report.totalGuests)
        binding.tvAvg.text = Fmt.money(report.avgAmount)
        binding.cardTurnover.bind(
            R.drawable.ic_report,
            StatCardView.IconTone.GREEN,
            "桌台周转率",
            String.format(java.util.Locale.CHINA, "%.2f", report.turnoverRate ?: 0.0),
            "次",
            "已结账会话 / 桌台数",
        )
        binding.cardTables.bind(
            R.drawable.ic_nav_dining,
            StatCardView.IconTone.BLUE,
            "桌台总数",
            Fmt.count(report.tableCount),
            "张",
            "含所有区域",
        )

        val daily = report.daily.orEmpty()
        binding.tvDays.text = "${daily.size} 天"
        binding.chart.setData(
            daily.map { Fmt.mdLabel(it.day ?: "") },
            daily.map { (it.revenue ?: 0.0).toFloat() },
        )
        bindDaily(binding.dailyContainer, daily)
        binding.empty.root.visibleIf(daily.isEmpty())
        binding.empty.emptyTitle.text = "该区间暂无已结账会话"
        binding.empty.emptyDesc.text = "换到「近 7 天」或「近 30 天」看看"
    }

    private fun bindDaily(container: LinearLayout, daily: List<DiningDaily>) {
        container.removeAllViews()
        if (daily.isEmpty()) return
        daily.reversed().forEach { item ->
            val row = ViewDetailRowBinding.inflate(layoutInflater, container, false)
            row.rowLabel.text = item.day ?: "—"
            row.rowValue.text = "${Fmt.count(item.sessionCount)} 桌 · ${Fmt.count(item.guests)} 人 · ${Fmt.money(item.revenue)}"
            container.addView(row.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
