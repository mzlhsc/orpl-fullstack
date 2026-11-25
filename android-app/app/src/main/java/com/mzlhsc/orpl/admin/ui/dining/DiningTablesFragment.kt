package com.mzlhsc.orpl.admin.ui.dining

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.DiningTable
import com.mzlhsc.orpl.admin.databinding.FragmentDiningTablesBinding
import com.mzlhsc.orpl.admin.databinding.ItemDiningTableBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.Tags
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf
import kotlinx.coroutines.launch

class DiningTablesFragment : androidx.fragment.app.Fragment(R.layout.fragment_dining_tables) {

    private var _binding: FragmentDiningTablesBinding? = null
    private val binding get() = _binding!!

    private val adapter = TableAdapter(::onOpen, ::onCloseSession, ::onMore)

    private val chips = listOf(
        ChipItem("全部", null),
        ChipItem("空闲", 0),
        ChipItem("就餐中", 1),
        ChipItem("待结账", 2),
    )
    private var selectedChip = 0
    private var status: Int? = null
    private var loaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDiningTablesBinding.bind(view)

        binding.rvTables.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTables.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { load() }
        binding.btnAdd.setOnClickListener { createTable() }

        renderChips()
        load()
    }

    private fun renderChips() {
        Chips.render(binding.chipRow, chips, selectedChip) { index ->
            selectedChip = index
            status = chips[index].value as Int?
            renderChips()
            load()
        }
    }

    private fun load() {
        binding.refresh.isRefreshing = true
        safeLaunch {
            try {
                val all = AdminApi.diningTables()
                val filtered = if (status == null) all else all.filter { it.status == status }
                adapter.submit(filtered)
                binding.empty.root.visibleIf(filtered.isEmpty())
                binding.empty.emptyTitle.text = "没有符合条件的桌台"
                binding.empty.emptyDesc.text = "可调整筛选条件或新增桌台"
                binding.tvSummary.text = buildString {
                    append("空闲 ${all.count { it.status == 0 }}")
                    append(" · 就餐中 ${all.count { it.status == 1 }}")
                    append(" · 待结账 ${all.count { it.status == 2 }}")
                    append(" · 共 ${all.size} 张")
                }
                loaded = true
            } finally {
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun onOpen(table: DiningTable) {
        if (table.status == 0) {
            openTable(table)
        } else {
            val sessionId = table.currentSessionId ?: run {
                toast("该桌台暂无就餐会话")
                return
            }
            DiningSessionActivity.start(requireContext(), sessionId)
        }
    }

    private fun openTable(table: DiningTable) {
        DiningDialogs.openTable(requireContext(), table) { guests, remark ->
            safeLaunch {
                AdminApi.diningOpen(table.id ?: return@safeLaunch, guests, remark)
                toast("已开台：${table.name}")
                load()
            }
        }
    }

    private fun onCloseSession(table: DiningTable) {
        val sessionId = table.currentSessionId ?: return
        Dialogs.confirm(
            requireContext(),
            "关闭会话",
            "将结束这桌就餐：未完成订单取消、共享购物车清空、桌台释放；已支付/已完成订单保留。",
            "关闭会话",
        ) {
            safeLaunch {
                AdminApi.diningClose(sessionId)
                toast("会话已关闭，桌台已释放")
                load()
            }
        }
    }

    private fun onMore(table: DiningTable) {
        val items = arrayOf("编辑桌台", "二维码", "删除桌台")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(table.name ?: "桌台")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> editTable(table)
                    1 -> showQr(table)
                    else -> deleteTable(table)
                }
            }
            .show()
    }

    private fun createTable() {
        DiningDialogs.editTable(requireContext(), null) { name, seats, area, reservable ->
            safeLaunch {
                AdminApi.diningTableSave(
                    null,
                    mapOf(
                        "name" to name,
                        "seats" to seats,
                        "area" to area,
                        "reservationEnabled" to if (reservable) 1 else 0,
                    ),
                )
                toast("桌台已新增")
                load()
            }
        }
    }

    private fun editTable(table: DiningTable) {
        DiningDialogs.editTable(requireContext(), table) { name, seats, area, reservable ->
            safeLaunch {
                AdminApi.diningTableSave(
                    table.id,
                    mapOf(
                        "name" to name,
                        "seats" to seats,
                        "area" to area,
                        "reservationEnabled" to if (reservable) 1 else 0,
                    ),
                )
                toast("桌台已保存")
                load()
            }
        }
    }

    private fun showQr(table: DiningTable) {
        DiningDialogs.qrCode(requireContext(), table) { onDone ->
            viewLifecycleOwner.lifecycleScope.launch {
                val content = runCatching {
                    AdminApi.diningTableRefreshQr(table.id ?: return@runCatching null)?.qrContent
                }.getOrNull()
                onDone(content)
                load()
            }
        }
    }

    private fun deleteTable(table: DiningTable) {
        if ((table.status ?: 0) != 0) {
            toast("该桌台正在使用，请先关闭会话")
            return
        }
        Dialogs.confirm(requireContext(), "删除桌台", "确定删除 ${table.name} 吗？删除后不可恢复。", "删除") {
            safeLaunch {
                AdminApi.diningTableDelete(table.id ?: return@safeLaunch)
                toast("桌台已删除")
                load()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loaded && isVisible) load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class TableAdapter(
        private val onOpen: (DiningTable) -> Unit,
        private val onClose: (DiningTable) -> Unit,
        private val onMore: (DiningTable) -> Unit,
    ) : RecyclerView.Adapter<TableAdapter.VH>() {

        private val items = mutableListOf<DiningTable>()

        fun submit(list: List<DiningTable>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemDiningTableBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val binding: ItemDiningTableBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(table: DiningTable) {
                binding.tvName.text = table.name ?: "桌台"
                val (text, tone) = Tags.table(table.status)
                Tags.apply(binding.tvStatus, text, tone)

                binding.tvMeta.text = listOfNotNull(
                    table.area ?: "未分区",
                    "${table.seats ?: 0} 座",
                    if (table.reservationEnabled == 1) "可预约" else "不可预约",
                ).joinToString(" · ")

                binding.tvSession.text = if (table.currentSessionId != null) {
                    "当前会话  #${table.currentSessionId} · ${table.currentSessionNo ?: ""}"
                } else {
                    "当前无就餐会话"
                }

                binding.btnPrimary.text = if (table.status == 0) "开台" else "查看会话"
                binding.btnPrimary.setOnClickListener { onOpen(table) }

                binding.btnClose.visibleIf(table.currentSessionId != null)
                binding.btnClose.setOnClickListener { onClose(table) }

                binding.btnMore.setOnClickListener { onMore(table) }
                binding.root.setOnClickListener { onOpen(table) }
            }
        }
    }
}
