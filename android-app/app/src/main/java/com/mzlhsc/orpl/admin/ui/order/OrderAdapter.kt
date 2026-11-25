package com.mzlhsc.orpl.admin.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.data.model.Order
import com.mzlhsc.orpl.admin.databinding.ItemOrderBinding
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.Tags

enum class OrderAction { CONFIRM, REJECT, COMPLETE, CANCEL, MARK_PAID, MARK_UNPAID }

class OrderAdapter(
    private val onOpen: (Order) -> Unit,
    private val onAction: (Order, OrderAction) -> Unit,
) : RecyclerView.Adapter<OrderAdapter.VH>() {

    private val items = mutableListOf<Order>()

    fun submit(list: List<Order>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {

        private var current: Order? = null

        fun bind(order: Order) {
            current = order

            binding.tvNumber.text = order.number ?: "#${order.id}"
            val (statusText, tone) = Tags.order(order.status)
            Tags.apply(binding.tvStatus, statusText, tone)

            val (payText, payTone) = Tags.pay(order.payStatus)
            Tags.apply(binding.tvPay, payText, payTone)

            binding.tvMeta.text = listOf(
                Fmt.orderTypeLabel(order.orderType),
                Fmt.dateTime(order.orderTime),
            ).joinToString(" · ")

            binding.tvDishes.text = order.orderDishes
                ?.trimEnd(';')
                ?.replace(";", "、")
                ?.takeIf { it.isNotBlank() } ?: "暂无菜品明细"

            binding.tvConsignee.text = listOfNotNull(
                order.consignee ?: order.userName,
                order.phone,
            ).joinToString(" · ").ifBlank { "顾客信息待补充" }

            binding.tvAmount.text = Fmt.money(order.amount)

            applyActions(order)

            binding.root.setOnClickListener { onOpen(order) }
        }

        /** 动作规则对齐 Web 端：已接单可直接完成；支付状态按 payStatus 给对应标记动作 */
        private fun applyActions(order: Order) {
            binding.actionRow.visibility = View.GONE
            binding.btnPrimary.visibility = View.GONE
            binding.btnDanger.visibility = View.GONE
            binding.btnGhost.visibility = View.GONE
            binding.btnPay.visibility = View.GONE

            val payStatus = order.payStatus ?: 0
            when (order.status) {
                2 -> {
                    button(binding.btnPrimary, "接单", OrderAction.CONFIRM)
                    button(binding.btnDanger, "拒单", OrderAction.REJECT)
                    button(binding.btnGhost, "取消", OrderAction.CANCEL)
                }
                3 -> {
                    // 堂食订单通过整桌结账完成，不给「完成」按钮
                    if (order.orderType != 2) button(binding.btnPrimary, "完成", OrderAction.COMPLETE)
                    if (payStatus == 0) {
                        button(binding.btnPay, "标记已支付", OrderAction.MARK_PAID)
                    } else {
                        button(binding.btnPay, "标记未支付", OrderAction.MARK_UNPAID)
                    }
                    button(binding.btnGhost, "取消", OrderAction.CANCEL)
                }
                5 -> {
                    if (payStatus == 0) button(binding.btnPay, "补记已支付", OrderAction.MARK_PAID)
                }
            }

            val visible = listOf(binding.btnPrimary, binding.btnDanger, binding.btnGhost, binding.btnPay)
                .any { it.visibility == View.VISIBLE }
            binding.actionRow.visibility = if (visible) View.VISIBLE else View.GONE
        }

        private fun button(view: android.widget.Button, text: String, action: OrderAction) {
            view.visibility = View.VISIBLE
            view.text = text
            view.setOnClickListener {
                current?.let { onAction(it, action) }
            }
        }
    }
}
