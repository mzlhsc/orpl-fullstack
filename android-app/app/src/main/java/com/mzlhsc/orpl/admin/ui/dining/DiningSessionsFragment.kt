package com.mzlhsc.orpl.admin.ui.dining

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.DiningSession
import com.mzlhsc.orpl.admin.databinding.FragmentDiningSessionsBinding
import com.mzlhsc.orpl.admin.databinding.ItemDiningSessionBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.Tags
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf

class DiningSessionsFragment : androidx.fragment.app.Fragment(R.layout.fragment_dining_sessions) {

    private var _binding: FragmentDiningSessionsBinding? = null
    private val binding get() = _binding!!

    private val adapter = SessionAdapter(::openDetail, ::checkout, ::closeSession)

    private val chips = listOf(
        ChipItem("全部", null),
        ChipItem("就餐中", 1),
        ChipItem("待结账", 2),
        ChipItem("已结账", 3),
        ChipItem("已关闭", 4),
    )
    private var selectedChip = 0
    private var status: Int? = null
    private var loaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDiningSessionsBinding.bind(view)

        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { load() }
        binding.empty.emptyTitle.text = "暂无就餐会话"
        binding.empty.emptyDesc.text = "顾客扫码开台后会出现在这里"

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
                val list = AdminApi.diningSessions(status)
                adapter.submit(list)
                binding.empty.root.visibleIf(list.isEmpty())
                loaded = true
            } finally {
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun openDetail(session: DiningSession) {
        val id = session.id ?: return
        DiningSessionActivity.start(requireContext(), id)
    }

    private fun checkout(session: DiningSession) {
        val id = session.id ?: return
        safeLaunch {
            val detail = runCatching { AdminApi.diningSession(id) }.getOrNull() ?: session
            DiningDialogs.checkout(requireContext(), detail) { payMethod, settleUnpaid, remark ->
                safeLaunch {
                    AdminApi.diningCheckout(id, payMethod, settleUnpaid, remark)
                    toast("结账成功，桌台已释放")
                    load()
                }
            }
        }
    }

    private fun closeSession(session: DiningSession) {
        val id = session.id ?: return
        Dialogs.confirm(
            requireContext(),
            "关闭会话",
            "将结束这桌就餐：未完成订单取消、共享购物车清空、桌台释放；已支付/已完成订单保留。",
            "关闭会话",
        ) {
            safeLaunch {
                AdminApi.diningClose(id)
                toast("会话已关闭")
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

    private class SessionAdapter(
        private val onOpen: (DiningSession) -> Unit,
        private val onCheckout: (DiningSession) -> Unit,
        private val onClose: (DiningSession) -> Unit,
    ) : RecyclerView.Adapter<SessionAdapter.VH>() {

        private val items = mutableListOf<DiningSession>()

        fun submit(list: List<DiningSession>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemDiningSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val binding: ItemDiningSessionBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(session: DiningSession) {
                binding.tvNo.text = session.sessionNo ?: "#${session.id}"
                val (text, tone) = Tags.session(session.status)
                Tags.apply(binding.tvStatus, text, tone)

                binding.tvMeta.text = listOfNotNull(
                    session.tableName ?: "未知桌台",
                    "${session.guestCount ?: 0} 人 / ${session.memberCount ?: 0} 成员",
                    Fmt.dateTime(session.openTime),
                    if (session.aaEnabled == 1) "AA 已开" else null,
                ).joinToString(" · ")

                binding.tvTotal.text = Fmt.money(session.totalAmount)
                binding.tvPaid.text = Fmt.money(session.paidAmount)
                binding.tvUnpaid.text = Fmt.money(session.unpaidAmount)

                val active = session.status == 1 || session.status == 2
                binding.actionRow.visibleIf(active)
                binding.btnCheckout.visibleIf(active)
                binding.btnClose.visibleIf(active)
                binding.btnCheckout.setOnClickListener { onCheckout(session) }
                binding.btnClose.setOnClickListener { onClose(session) }
                binding.root.setOnClickListener { onOpen(session) }
            }
        }
    }
}
