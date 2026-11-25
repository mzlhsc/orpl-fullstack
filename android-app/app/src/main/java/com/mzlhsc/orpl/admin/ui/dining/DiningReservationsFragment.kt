package com.mzlhsc.orpl.admin.ui.dining

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.DiningReservation
import com.mzlhsc.orpl.admin.databinding.FragmentDiningReservationsBinding
import com.mzlhsc.orpl.admin.databinding.ItemDiningReservationBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.Tags
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf

class DiningReservationsFragment : androidx.fragment.app.Fragment(R.layout.fragment_dining_reservations) {

    private var _binding: FragmentDiningReservationsBinding? = null
    private val binding get() = _binding!!

    private val adapter = ReservationAdapter(::confirm, ::arrive, ::cancel)

    private val chips = listOf(
        ChipItem("全部", null),
        ChipItem("待确认", 1),
        ChipItem("已确认", 2),
        ChipItem("已到店", 3),
        ChipItem("已取消", 4),
    )
    private var selectedChip = 0
    private var status: Int? = null
    private var loaded = false
    private val tableNames = mutableMapOf<Long, String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDiningReservationsBinding.bind(view)

        binding.rvReservations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservations.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { load() }
        binding.empty.emptyTitle.text = "暂无预约"
        binding.empty.emptyDesc.text = "顾客在小程序提交预约后会出现在这里"

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
                if (tableNames.isEmpty()) {
                    runCatching { AdminApi.diningTables() }.getOrNull()?.forEach { table ->
                        table.id?.let { tableNames[it] = table.name ?: "桌台 $it" }
                    }
                }
                val list = AdminApi.diningReservations(status)
                adapter.submit(list, tableNames)
                binding.empty.root.visibleIf(list.isEmpty())
                loaded = true
            } finally {
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun confirm(item: DiningReservation) {
        val id = item.id ?: return
        safeLaunch {
            AdminApi.diningReservationConfirm(id)
            toast("预约已确认")
            load()
        }
    }

    private fun arrive(item: DiningReservation) {
        val id = item.id ?: return
        safeLaunch {
            AdminApi.diningReservationArrive(id)
            toast("已标记到店，可直接在桌台页开台")
            load()
        }
    }

    private fun cancel(item: DiningReservation) {
        val id = item.id ?: return
        Dialogs.confirm(requireContext(), "取消预约", "确定取消 ${item.contactName ?: ""} 的预约吗？", "取消预约") {
            safeLaunch {
                AdminApi.diningReservationCancel(id)
                toast("预约已取消")
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

    private class ReservationAdapter(
        private val onConfirm: (DiningReservation) -> Unit,
        private val onArrive: (DiningReservation) -> Unit,
        private val onCancel: (DiningReservation) -> Unit,
    ) : RecyclerView.Adapter<ReservationAdapter.VH>() {

        private val items = mutableListOf<DiningReservation>()
        private var names: Map<Long, String> = emptyMap()

        fun submit(list: List<DiningReservation>, tableNames: Map<Long, String>) {
            items.clear()
            items.addAll(list)
            names = tableNames
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemDiningReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val binding: ItemDiningReservationBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: DiningReservation) {
                binding.tvContact.text = item.contactName ?: "顾客"
                val (text, tone) = Tags.reservation(item.status)
                Tags.apply(binding.tvStatus, text, tone)

                val tableName = item.tableId?.let { names[it] } ?: "桌台 ${item.tableId ?: "-"}"
                binding.tvMeta.text = listOfNotNull(
                    item.contactPhone,
                    tableName,
                    "${item.guestCount ?: 0} 人",
                ).joinToString(" · ")

                binding.tvTime.text = "预约时间：${Fmt.dateTime(item.reserveTime)}"

                if (item.remark.isNullOrBlank()) {
                    binding.tvRemark.visibility = View.GONE
                } else {
                    binding.tvRemark.visibility = View.VISIBLE
                    binding.tvRemark.text = "备注：${item.remark}"
                }

                val status = item.status ?: 0
                binding.btnConfirm.visibleIf(status == 1)
                binding.btnArrive.visibleIf(status == 1 || status == 2)
                binding.btnCancel.visibleIf(status == 1 || status == 2)
                binding.btnConfirm.setOnClickListener { onConfirm(item) }
                binding.btnArrive.setOnClickListener { onArrive(item) }
                binding.btnCancel.setOnClickListener { onCancel(item) }

                val anyAction = status == 1 || status == 2
                binding.actionRow.visibleIf(anyAction)
            }
        }
    }
}
