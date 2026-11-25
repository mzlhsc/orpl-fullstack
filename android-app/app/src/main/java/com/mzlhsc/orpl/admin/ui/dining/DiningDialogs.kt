package com.mzlhsc.orpl.admin.ui.dining

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.data.model.DiningSession
import com.mzlhsc.orpl.admin.data.model.DiningTable
import com.mzlhsc.orpl.admin.databinding.DialogCheckoutBinding
import com.mzlhsc.orpl.admin.databinding.DialogOpenTableBinding
import com.mzlhsc.orpl.admin.databinding.DialogTableEditBinding
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.toast

/** 堂食相关弹窗，样式沿用 MaterialAlertDialog + bg_input 输入框 */
object DiningDialogs {

    fun openTable(context: Context, table: DiningTable, onConfirm: (Int, String?) -> Unit) {
        val binding = DialogOpenTableBinding.inflate(LayoutInflater.from(context))
        binding.tvTable.text = "${table.name ?: "桌台"} · ${table.seats ?: 0} 座 ${table.area ?: ""}"
        binding.etGuests.setText((table.seats ?: 2).toString())
        binding.etGuests.setSelection(binding.etGuests.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("开台")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认开台", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val guests = binding.etGuests.text.toString().trim().toIntOrNull()
                if (guests == null || guests < 1) {
                    context.toast("请输入有效的就餐人数")
                    return@setOnClickListener
                }
                val remark = binding.etRemark.text.toString().trim().takeIf { it.isNotEmpty() }
                dialog.dismiss()
                onConfirm(guests, remark)
            }
        }
        dialog.show()
    }

    fun editTable(context: Context, table: DiningTable?, onConfirm: (String, Int, String?, Boolean) -> Unit) {
        val binding = DialogTableEditBinding.inflate(LayoutInflater.from(context))
        binding.etName.setText(table?.name.orEmpty())
        binding.etSeats.setText((table?.seats ?: 4).toString())
        binding.etArea.setText(table?.area.orEmpty())
        binding.swReservation.isChecked = (table?.reservationEnabled ?: 0) == 1

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (table == null) "新增桌台" else "编辑桌台")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.etName.text.toString().trim()
                val seats = binding.etSeats.text.toString().trim().toIntOrNull()
                if (name.isEmpty()) {
                    context.toast("请填写桌台名称")
                    return@setOnClickListener
                }
                if (seats == null || seats < 1) {
                    context.toast("请输入有效的座位数")
                    return@setOnClickListener
                }
                val area = binding.etArea.text.toString().trim().takeIf { it.isNotEmpty() }
                dialog.dismiss()
                onConfirm(name, seats, area, binding.swReservation.isChecked)
            }
        }
        dialog.show()
    }

    fun checkout(context: Context, session: DiningSession?, onConfirm: (Int, Boolean, String?) -> Unit) {
        val binding = DialogCheckoutBinding.inflate(LayoutInflater.from(context))
        binding.tvSummary.text = session?.let {
            "${it.sessionNo ?: "#${it.id}"} · ${it.tableName ?: ""}\n应收 ${Fmt.money(it.totalAmount)} · 待付 ${Fmt.money(it.unpaidAmount)}"
        } ?: "会话信息读取中"

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("整单结账")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认结账", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val payMethod = when (binding.rgPay.checkedRadioButtonId) {
                    binding.rbWechat.id -> 1
                    binding.rbAlipay.id -> 2
                    else -> 3
                }
                val remark = binding.etRemark.text.toString().trim().takeIf { it.isNotEmpty() }
                dialog.dismiss()
                onConfirm(payMethod, binding.swSettle.isChecked, remark)
            }
        }
        dialog.show()
    }

    /** 桌台二维码：展示扫码内容，可复制或刷新令牌 */
    fun qrCode(
        context: Context,
        table: DiningTable,
        onRefresh: (onDone: (String?) -> Unit) -> Unit,
    ) {
        fun text(qr: String?) = "桌台：${table.name ?: ""}\n扫码内容：\n${qr ?: "—"}"

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("桌台二维码")
            .setMessage(text(table.qrContent))
            .setNegativeButton("关闭", null)
            .setNeutralButton("复制内容", null)
            .setPositiveButton("刷新二维码", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("桌台二维码", table.qrContent.orEmpty()))
                context.toast("扫码内容已复制")
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onRefresh { newContent ->
                    if (newContent == null) {
                        context.toast("刷新失败，请重试")
                    } else {
                        context.toast("二维码已刷新，旧码立即失效")
                        dialog.setMessage(text(newContent))
                    }
                }
            }
        }
        dialog.show()
    }
}
