package com.mzlhsc.orpl.admin.ui.common

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.databinding.DialogReasonBinding
import com.mzlhsc.orpl.admin.databinding.DialogStockBinding
import com.mzlhsc.orpl.admin.util.toast

object Dialogs {

    /** 拒单 / 取消订单时填写原因 */
    fun askReason(
        context: Context,
        title: String,
        message: String,
        onConfirm: (String) -> Unit,
    ) {
        val binding = DialogReasonBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(binding.root)
            .setNegativeButton("再想想", null)
            .setPositiveButton("确定", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val reason = binding.etReason.text.toString().trim()
                if (reason.isEmpty()) {
                    context.toast("请填写原因")
                    return@setOnClickListener
                }
                dialog.dismiss()
                onConfirm(reason)
            }
        }
        dialog.show()
    }

    /** 设置菜品库存 */
    fun askStock(
        context: Context,
        dishName: String,
        currentStock: Int?,
        onConfirm: (Int) -> Unit,
    ) {
        val binding = DialogStockBinding.inflate(LayoutInflater.from(context))
        binding.etStock.setText((currentStock ?: 0).toString())
        binding.etStock.setSelection(binding.etStock.text?.length ?: 0)
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("设置库存 · $dishName")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val stock = binding.etStock.text.toString().trim().toIntOrNull()
                if (stock == null || stock < 0) {
                    context.toast("请输入有效的库存数量")
                    return@setOnClickListener
                }
                dialog.dismiss()
                onConfirm(stock)
            }
        }
        dialog.show()
    }

    fun confirm(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "确定",
        onConfirm: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton(confirmText) { _, _ -> onConfirm() }
            .show()
    }
}
