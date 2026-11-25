package com.mzlhsc.orpl.admin.ui.dining

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.DiningSession
import com.mzlhsc.orpl.admin.databinding.ActivityDiningSessionBinding
import com.mzlhsc.orpl.admin.databinding.ViewDetailRowBinding
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.ui.order.OrderDetailActivity
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.Tags
import com.mzlhsc.orpl.admin.util.hide
import com.mzlhsc.orpl.admin.util.show
import com.mzlhsc.orpl.admin.util.toast
import kotlinx.coroutines.launch

class DiningSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiningSessionBinding
    private var sessionId: Long = 0L
    private var session: DiningSession? = null
    private var changed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiningSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getLongExtra(EXTRA_ID, 0L)
        binding.toolbar.toolbarTitle.text = "就餐会话"
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        hideActions()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val data = AdminApi.diningSession(sessionId)
                if (data == null) {
                    toast("会话不存在")
                    finish()
                    return@launch
                }
                session = data
                render(data)
            } catch (e: Exception) {
                toast(e.message ?: "会话详情加载失败")
            }
        }
    }

    private fun render(data: DiningSession) {
        val (text, _) = Tags.session(data.status)
        binding.tvStatus.text = text
        binding.tvNo.text = data.sessionNo ?: "#${data.id}"
        binding.tvTotal.text = Fmt.money(data.totalAmount)
        binding.tvPaid.text = Fmt.money(data.paidAmount)
        binding.tvUnpaid.text = Fmt.money(data.unpaidAmount)
        binding.tvPerPerson.text = Fmt.money(data.perPersonAmount)
        binding.tvHint.text = when (data.status) {
            1 -> "顾客正在用餐，可整单结账或关闭会话"
            2 -> "顾客已发起结账，请前台收款"
            3 -> "会话已结账，桌台已释放"
            4 -> "会话已关闭，未完成订单已取消"
            else -> ""
        }

        binding.infoContainer.removeAllViews()
        val tables = data.tableNames.orEmpty()
        addRow(
            binding.infoContainer,
            "桌台",
            (data.tableName ?: "—") + if (tables.size > 1) "（并桌：${tables.joinToString(" / ")}）" else "",
        )
        addRow(
            binding.infoContainer,
            "就餐人数",
            "${data.guestCount ?: 0} 人 · ${data.memberCount ?: 0} 位成员",
        )
        addRow(binding.infoContainer, "AA 结账", if (data.aaEnabled == 1) "已开启" else "未开启")
        addRow(binding.infoContainer, "开台时间", Fmt.dateTime(data.openTime))
        addRow(binding.infoContainer, "结账时间", Fmt.dateTime(data.checkoutTime))
        addRow(binding.infoContainer, "购物车金额", Fmt.money(data.cartAmount))
        if (!data.remark.isNullOrBlank()) addRow(binding.infoContainer, "备注", data.remark)

        val members = data.members.orEmpty()
        binding.tvMemberTitle.text = "就餐成员（${members.size}）"
        binding.memberContainer.removeAllViews()
        if (members.isEmpty()) {
            addRow(binding.memberContainer, "成员", "暂无成员")
        } else {
            members.forEach { member ->
                val owner = if (member.isOwner == 1) " · 台主" else ""
                val pay = if (member.payStatus == 1) "已支付" else "未支付"
                addRow(
                    binding.memberContainer,
                    (member.nickname ?: "用户 ${member.userId}") + owner,
                    "${Fmt.money(member.paidAmount)} · $pay",
                )
            }
        }

        val cart = data.cartItems.orEmpty()
        binding.tvCartTitle.text = "共享购物车（${cart.size}）"
        binding.cartContainer.removeAllViews()
        if (cart.isEmpty()) {
            addRow(binding.cartContainer, "购物车", "暂无商品")
        } else {
            cart.forEach { item ->
                val flavor = if (item.dishFlavor.isNullOrBlank()) "" else " · ${item.dishFlavor}"
                addRow(binding.cartContainer, "${item.name} ×${item.number ?: 1}$flavor", Fmt.money(item.amount))
            }
        }

        val orders = data.orders.orEmpty()
        binding.tvOrderTitle.text = "关联订单（${orders.size}）"
        binding.orderContainer.removeAllViews()
        if (orders.isEmpty()) {
            addRow(binding.orderContainer, "订单", "暂无订单")
        } else {
            orders.forEach { order ->
                val row = ViewDetailRowBinding.inflate(layoutInflater, binding.orderContainer, false)
                row.rowLabel.text = order.number ?: "#${order.id}"
                val (orderText, _) = Tags.order(order.status)
                row.rowValue.text = "${Fmt.money(order.amount)} · $orderText"
                row.root.setOnClickListener {
                    order.id?.let { OrderDetailActivity.start(this, it) }
                }
                binding.orderContainer.addView(row.root)
            }
        }

        bindActions(data)
    }

    private fun bindActions(data: DiningSession) {
        hideActions()
        val active = data.status == 1 || data.status == 2
        val id = data.id ?: return

        binding.btnTransfer.show()
        binding.btnTransfer.isEnabled = active
        binding.btnTransfer.alpha = if (active) 1f else 0.5f
        binding.btnTransfer.setOnClickListener { if (active) openTransfer(id) }

        binding.btnMerge.show()
        binding.btnMerge.isEnabled = active
        binding.btnMerge.alpha = if (active) 1f else 0.5f
        binding.btnMerge.setOnClickListener { if (active) openMerge(data) }

        binding.btnClose.show()
        binding.btnClose.setOnClickListener {
            if (!active) {
                toast("该会话已结束")
                return@setOnClickListener
            }
            Dialogs.confirm(
                this,
                "关闭会话",
                "将结束这桌就餐：未完成订单取消、共享购物车清空、桌台释放；已支付/已完成订单保留。",
                "关闭会话",
            ) {
                perform("会话已关闭") { AdminApi.diningClose(id) }
            }
        }

        binding.btnCheckout.show()
        binding.btnCheckout.setOnClickListener {
            if (!active) {
                toast("该会话已结束")
                return@setOnClickListener
            }
            DiningDialogs.checkout(this, session) { payMethod, settleUnpaid, remark ->
                perform("结账成功，桌台已释放") {
                    AdminApi.diningCheckout(id, payMethod, settleUnpaid, remark)
                }
            }
        }
    }

    /** 转台：只列空闲桌台 */
    private fun openTransfer(id: Long) {
        lifecycleScope.launch {
            val free = runCatching { AdminApi.diningTables(status = 0) }.getOrNull()
            if (free.isNullOrEmpty()) {
                toast("当前没有空闲桌台可转")
                return@launch
            }
            val labels = free.map { "${it.name} · ${it.seats ?: 0} 座 ${it.area ?: ""}" }.toTypedArray()
            MaterialAlertDialogBuilder(this@DiningSessionActivity)
                .setTitle("转台到")
                .setItems(labels) { _, which ->
                    val target = free[which]
                    target.id?.let { targetId ->
                        perform("转台成功") { AdminApi.diningTransfer(id, targetId) }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /**
     * 并桌：把同一桌其他就餐中的会话并入当前会话（并入后那些会话的订单与购物车归到本桌）。
     * 与 Web 端 dining.js 的并桌一致，只能并入「就餐中 / 待结账」的会话。
     */
    private fun openMerge(current: DiningSession) {
        val tableId = current.tableId ?: return
        lifecycleScope.launch {
            val all = runCatching { AdminApi.diningSessions() }.getOrNull().orEmpty()
            val sources = all.filter {
                it.id != current.id && it.tableId == tableId && (it.status == 1 || it.status == 2)
            }
            if (sources.isEmpty()) {
                toast("这张桌没有其他进行中的会话可并入")
                return@launch
            }
            val labels = sources.map {
                "${it.sessionNo ?: "#${it.id}"} · ${it.memberCount ?: it.members?.size ?: 0} 人 · ${Fmt.money(it.totalAmount)}"
            }.toTypedArray()
            val checked = BooleanArray(sources.size)
            MaterialAlertDialogBuilder(this@DiningSessionActivity)
                .setTitle("并入当前会话")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setNegativeButton("取消", null)
                .setPositiveButton("并桌") { _, _ ->
                    val ids = sources.filterIndexed { index, _ -> checked[index] }.mapNotNull { it.id }
                    if (ids.isEmpty()) {
                        toast("请先选择要并入的会话")
                        return@setPositiveButton
                    }
                    perform("并桌成功") { AdminApi.diningMerge(current.id ?: return@perform, ids) }
                }
                .show()
        }
    }

    private fun hideActions() {
        binding.btnMerge.hide()
        binding.btnTransfer.hide()
        binding.btnClose.hide()
        binding.btnCheckout.hide()
    }

    private fun addRow(container: LinearLayout, label: String, value: String) {
        val row = ViewDetailRowBinding.inflate(layoutInflater, container, false)
        row.rowLabel.text = label
        row.rowValue.text = value.ifBlank { "—" }
        container.addView(row.root)
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

    override fun finish() {
        setResult(if (changed) RESULT_OK else RESULT_CANCELED)
        super.finish()
    }

    companion object {
        private const val EXTRA_ID = "session_id"

        fun start(context: Context, id: Long) {
            context.startActivity(
                Intent(context, DiningSessionActivity::class.java).putExtra(EXTRA_ID, id)
            )
        }
    }
}
