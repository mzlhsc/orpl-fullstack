package com.mzlhsc.orpl.admin.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Order
import com.mzlhsc.orpl.admin.databinding.ActivityOrderDetailBinding
import com.mzlhsc.orpl.admin.databinding.ItemOrderDishBinding
import com.mzlhsc.orpl.admin.databinding.ViewDetailRowBinding
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.toast
import kotlinx.coroutines.launch

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private var orderId: Long = 0L
    private var order: Order? = null
    private var changed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getLongExtra(EXTRA_ID, 0L)
        binding.toolbar.toolbarTitle.text = "订单详情"
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        hideAllActions()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val data = AdminApi.orderDetail(orderId)
                if (data == null) {
                    toast("订单不存在")
                    finish()
                    return@launch
                }
                order = data
                render(data)
            } catch (e: Exception) {
                toast(e.message ?: "订单详情加载失败")
            }
        }
    }

    private fun render(data: Order) {
        val (statusText, hint) = statusOf(data.status)
        binding.tvStatusBig.text = statusText
        binding.tvStatusHint.text = hint
        binding.tvNumberBig.text = data.number ?: "#${data.id}"
        binding.tvAmountBig.text = Fmt.money(data.amount)

        binding.consigneeBlock.removeAllViews()
        addRow(binding.consigneeBlock, "收货人", data.consignee ?: data.userName.orEmpty())
        addRow(binding.consigneeBlock, "手机号", data.phone.orEmpty())
        addRow(binding.consigneeBlock, "地址", data.address.orEmpty())
        if (!data.remark.isNullOrBlank()) {
            addRow(binding.consigneeBlock, "备注", data.remark)
        }

        binding.dishContainer.removeAllViews()
        val dishes = data.orderDetailList.orEmpty()
        if (dishes.isEmpty()) {
            addRow(binding.dishContainer, "菜品", "暂无明细")
        } else {
            dishes.forEach { detail ->
                val row = ItemOrderDishBinding.inflate(layoutInflater, binding.dishContainer, false)
                row.dishName.text = detail.name ?: "菜品"
                row.dishCount.text = "x${detail.number ?: 1}"
                row.dishAmount.text = Fmt.money(detail.amount)
                if (detail.dishFlavor.isNullOrBlank()) {
                    row.dishFlavor.visibility = View.GONE
                } else {
                    row.dishFlavor.visibility = View.VISIBLE
                    row.dishFlavor.text = detail.dishFlavor
                }
                binding.dishContainer.addView(row.root)
            }
        }

        binding.infoContainer.removeAllViews()
        addRow(binding.infoContainer, "订单号", data.number.orEmpty())
        addRow(binding.infoContainer, "下单时间", Fmt.dateTime(data.orderTime))
        addRow(
            binding.infoContainer,
            "支付方式",
            "${Fmt.payMethodLabel(data.payMethod)} · ${Fmt.payStatusLabel(data.payStatus)}",
        )
        addRow(binding.infoContainer, "订单类型", Fmt.orderTypeLabel(data.orderType))
        addRow(binding.infoContainer, "预计送达", Fmt.dateTime(data.estimatedDeliveryTime))
        data.packAmount?.let { addRow(binding.infoContainer, "打包费", "¥$it") }

        val reason = data.rejectionReason?.takeIf { it.isNotBlank() }
            ?: data.cancelReason?.takeIf { it.isNotBlank() }
        if (reason != null) {
            binding.tvReason.visibility = View.VISIBLE
            binding.tvReason.text = if (!data.rejectionReason.isNullOrBlank()) "拒单原因：$reason" else "取消原因：$reason"
        } else {
            binding.tvReason.visibility = View.GONE
        }

        bindActions(data)
    }

    private fun bindActions(data: Order) {
        hideAllActions()
        val id = data.id ?: return
        val payStatus = data.payStatus ?: 0
        when (data.status) {
            2 -> {
                binding.btnPrimary.visibility = View.VISIBLE
                binding.btnPrimary.text = "接单"
                binding.btnPrimary.setOnClickListener { perform("接单成功") { AdminApi.orderConfirm(id) } }

                binding.btnDanger.visibility = View.VISIBLE
                binding.btnDanger.text = "拒单"
                binding.btnDanger.setOnClickListener {
                    Dialogs.askReason(this, "拒单原因", "确认拒单后订单会退回用户端") { reason ->
                        perform("已拒单") { AdminApi.orderReject(id, reason) }
                    }
                }

                binding.btnGhost.visibility = View.VISIBLE
                binding.btnGhost.text = "取消订单"
                binding.btnGhost.setOnClickListener {
                    Dialogs.askReason(this, "取消原因", "取消后该订单不再流转") { reason ->
                        perform("订单已取消") { AdminApi.orderCancel(id, reason) }
                    }
                }
            }
            3 -> {
                // 已接单可直接完成，不再经过派送中；堂食订单走整桌结账
                if (data.orderType != 2) {
                    binding.btnPrimary.visibility = View.VISIBLE
                    binding.btnPrimary.text = "完成订单"
                    binding.btnPrimary.setOnClickListener { confirmComplete(id) }
                }

                binding.btnPay.visibility = View.VISIBLE
                binding.btnPay.text = if (payStatus == 0) "标记已支付" else "标记未支付"
                binding.btnPay.setOnClickListener { confirmPay(id, if (payStatus == 0) 1 else 0) }

                binding.btnGhost.visibility = View.VISIBLE
                binding.btnGhost.text = "取消订单"
                binding.btnGhost.setOnClickListener {
                    Dialogs.askReason(this, "取消原因", "取消后该订单不再流转") { reason ->
                        perform("订单已取消") { AdminApi.orderCancel(id, reason) }
                    }
                }
            }
            5 -> {
                if (payStatus == 0) {
                    binding.btnPay.visibility = View.VISIBLE
                    binding.btnPay.text = "补记已支付"
                    binding.btnPay.setOnClickListener { confirmPay(id, 1) }
                }
            }
        }
    }

    private fun confirmComplete(id: Long) {
        Dialogs.confirm(
            this,
            "完成订单",
            "完成后订单状态为「已完成」，支付状态会自动置为「已支付」。",
            "完成订单",
        ) {
            perform("订单已完成，支付状态已置为已支付") { AdminApi.orderComplete(id) }
        }
    }

    private fun confirmPay(id: Long, payStatus: Int) {
        val label = if (payStatus == 1) "已支付" else "未支付"
        Dialogs.confirm(this, "标记$label", "确定把该订单的支付状态改为「$label」吗？", "标记$label") {
            perform("已标记为$label") { AdminApi.orderPayStatus(id, payStatus) }
        }
    }

    private fun hideAllActions() {
        binding.btnPrimary.visibility = View.GONE
        binding.btnDanger.visibility = View.GONE
        binding.btnGhost.visibility = View.GONE
        binding.btnPay.visibility = View.GONE
    }

    private fun perform(successMessage: String, block: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                block()
                changed = true
                toast(successMessage)
                load()
            } catch (e: Exception) {
                toast(e.message ?: "操作失败")
            }
        }
    }

    private fun addRow(container: LinearLayout, label: String, value: String) {
        val row = ViewDetailRowBinding.inflate(layoutInflater, container, false)
        row.rowLabel.text = label
        row.rowValue.text = value.ifBlank { "—" }
        container.addView(row.root)
    }

    private fun statusOf(status: Int?): Pair<String, String> = when (status) {
        1 -> "待付款" to "等待用户完成支付"
        2 -> "待接单" to "等待商家接单"
        3 -> "已接单" to "已接单，可直接完成订单"
        4 -> "已接单" to "历史派送中状态，已统一为已接单"
        5 -> "已完成" to "订单已完成，款项已结算"
        6 -> "已取消" to "订单已取消"
        7 -> "退款" to "订单已退款"
        else -> "未知状态" to ""
    }

    override fun finish() {
        setResult(if (changed) RESULT_OK else RESULT_CANCELED)
        super.finish()
    }

    companion object {
        private const val EXTRA_ID = "order_id"

        fun start(context: Context, id: Long) {
            context.startActivity(
                Intent(context, OrderDetailActivity::class.java).putExtra(EXTRA_ID, id)
            )
        }
    }
}
