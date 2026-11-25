package com.mzlhsc.orpl.admin.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.Prefs
import com.mzlhsc.orpl.admin.databinding.DialogPasswordBinding
import com.mzlhsc.orpl.admin.databinding.DialogServerBinding
import com.mzlhsc.orpl.admin.databinding.FragmentMoreBinding
import com.mzlhsc.orpl.admin.databinding.ItemActionBinding
import com.mzlhsc.orpl.admin.ui.category.CategoryActivity
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.ui.employee.EmployeeActivity
import com.mzlhsc.orpl.admin.ui.login.LoginActivity
import com.mzlhsc.orpl.admin.ui.main.MainActivity
import com.mzlhsc.orpl.admin.ui.report.ReportActivity
import com.mzlhsc.orpl.admin.util.hide
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.show
import com.mzlhsc.orpl.admin.util.toast

class MoreFragment : Fragment(R.layout.fragment_more) {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private var shopOn = false
    private var shopBusy = false
    private var takeoutAuto = false
    private var diningAuto = false
    private var autoBusy = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMoreBinding.bind(view)

        bindProfile()
        buildActionRows()

        binding.swShop.setOnCheckedChangeListener(null)
        binding.swShop.setOnCheckedChangeListener { _, checked ->
            if (shopBusy) return@setOnCheckedChangeListener
            setShop(checked)
        }

        binding.swTakeoutAuto.setOnCheckedChangeListener(null)
        binding.swTakeoutAuto.setOnCheckedChangeListener { _, checked ->
            if (autoBusy) return@setOnCheckedChangeListener
            setAutoAccept(dining = false, checked = checked)
        }
        binding.swDiningAuto.setOnCheckedChangeListener(null)
        binding.swDiningAuto.setOnCheckedChangeListener { _, checked ->
            if (autoBusy) return@setOnCheckedChangeListener
            setAutoAccept(dining = true, checked = checked)
        }

        binding.btnLogout.setOnClickListener {
            Dialogs.confirm(requireContext(), "退出登录", "退出后需要重新输入账号密码", "退出") {
                doLogout()
            }
        }

        loadShop()
    }

    override fun onResume() {
        super.onResume()
        loadShop()
        loadAutoAccept()
    }

    private fun bindProfile() {
        val name = Prefs.empName ?: "管理员"
        binding.tvName.text = name
        binding.tvUsername.text = Prefs.empUserName ?: "—"
        binding.tvAvatar.text = name.trim().take(1).ifEmpty { "管" }
        binding.tvRoleTag.text = "管理员"
    }

    private fun buildActionRows() {
        val container = binding.containerActions
        container.removeAllViews()

        addRow(container, R.drawable.ic_category, R.drawable.bg_icon_brand, "分类管理", "菜品分类与套餐分类") {
            startActivity(Intent(requireContext(), CategoryActivity::class.java))
        }
        addRow(container, R.drawable.ic_employees, R.drawable.bg_icon_blue, "员工管理", "账号状态与权限") {
            startActivity(Intent(requireContext(), EmployeeActivity::class.java))
        }
        addRow(container, R.drawable.ic_report, R.drawable.bg_icon_green, "数据统计", "营业额、订单与销量排行") {
            startActivity(Intent(requireContext(), ReportActivity::class.java))
        }
        addRow(
            container,
            R.drawable.ic_server,
            R.drawable.bg_icon_amber,
            "服务器地址",
            null,
            Prefs.baseUrl,
        ) { showServerDialog() }
        addRow(container, R.drawable.ic_lock, R.drawable.bg_icon_slate, "修改密码", "修改后下次登录生效") {
            showPasswordDialog()
        }
    }

    private fun addRow(
        container: LinearLayout,
        icon: Int,
        iconBackground: Int,
        title: String,
        sub: String? = null,
        value: String? = null,
        onClick: () -> Unit,
    ) {
        val row = ItemActionBinding.inflate(LayoutInflater.from(requireContext()), container, false)
        row.actionIcon.setImageResource(icon)
        row.actionIcon.setBackgroundResource(iconBackground)
        row.actionTitle.text = title
        if (sub.isNullOrBlank()) row.actionSub.hide() else {
            row.actionSub.show()
            row.actionSub.text = sub
        }
        row.actionValue.text = value.orEmpty()
        row.root.setOnClickListener { onClick() }
        container.addView(row.root)
    }

    private fun loadShop() {
        if (!isAdded) return
        safeLaunch {
            val status = runCatching { AdminApi.shopStatus() }.getOrNull() ?: return@safeLaunch
            shopOn = status == 1
            paintShop()
        }
    }

    private fun paintShop() {
        binding.swShop.setOnCheckedChangeListener(null)
        binding.swShop.isChecked = shopOn
        binding.tvShopDesc.text = if (shopOn) "营业中，用户端可以正常下单" else "打烊中，用户端提示门店休息"
        binding.swShop.setOnCheckedChangeListener { _, checked ->
            if (shopBusy) return@setOnCheckedChangeListener
            setShop(checked)
        }
    }

    private fun setShop(checked: Boolean) {
        shopBusy = true
        safeLaunch {
            try {
                AdminApi.shopSet(if (checked) 1 else 0)
                shopOn = checked
                paintShop()
                toast(if (checked) "店铺已开始营业" else "店铺已打烊")
            } catch (e: Exception) {
                paintShop()
                throw e
            } finally {
                shopBusy = false
            }
        }
    }

    private fun loadAutoAccept() {
        if (!isAdded) return
        safeLaunch {
            val data = runCatching { AdminApi.shopAutoAccept() }.getOrNull() ?: return@safeLaunch
            takeoutAuto = data.takeoutAutoAccept == true
            diningAuto = data.diningAutoAccept == true
            paintAutoAccept()
        }
    }

    private fun paintAutoAccept() {
        autoBusy = true
        binding.swTakeoutAuto.isChecked = takeoutAuto
        binding.swDiningAuto.isChecked = diningAuto
        autoBusy = false
    }

    private fun setAutoAccept(dining: Boolean, checked: Boolean) {
        autoBusy = true
        val label = if (dining) "堂食" else "外卖"
        safeLaunch {
            try {
                val body = if (dining) {
                    mapOf("diningAutoAccept" to checked)
                } else {
                    mapOf("takeoutAutoAccept" to checked)
                }
                val data = AdminApi.shopAutoAcceptSet(body)
                if (data != null) {
                    takeoutAuto = data.takeoutAutoAccept == true
                    diningAuto = data.diningAutoAccept == true
                } else if (dining) {
                    diningAuto = checked
                } else {
                    takeoutAuto = checked
                }
                toast("${label}自动接单已${if (checked) "开启" else "关闭"}")
            } finally {
                autoBusy = false
                paintAutoAccept()
            }
        }
    }

    private fun showServerDialog() {        val dialogBinding = DialogServerBinding.inflate(layoutInflater)
        dialogBinding.etServerUrl.setText(Prefs.baseUrl)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("服务器地址")
            .setView(dialogBinding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val input = dialogBinding.etServerUrl.text.toString().trim()
                if (input.isEmpty()) {
                    toast("请输入接口地址")
                    return@setOnClickListener
                }
                Prefs.baseUrl = input
                toast("已保存：${Prefs.baseUrl}")
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showPasswordDialog() {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("修改登录密码")
            .setView(dialogBinding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val old = dialogBinding.etOld.text.toString()
                val new = dialogBinding.etNew.text.toString()
                val confirm = dialogBinding.etConfirm.text.toString()
                val error = when {
                    old.isEmpty() || new.isEmpty() -> "请填写当前密码与新密码"
                    new.length < 6 -> "新密码至少 6 位"
                    new != confirm -> "两次输入的新密码不一致"
                    new == old -> "新密码不能与当前密码相同"
                    else -> null
                }
                if (error != null) {
                    dialogBinding.tvPwdError.show()
                    dialogBinding.tvPwdError.text = error
                    return@setOnClickListener
                }
                dialog.dismiss()
                safeLaunch {
                    AdminApi.editPassword(Prefs.empId, old, new)
                    toast("密码修改成功，下次登录请使用新密码")
                }
            }
        }
        dialog.show()
    }

    private fun doLogout() {
        safeLaunch {
            runCatching { AdminApi.logout() }
            Prefs.clearSession()
            toast("已退出登录")
            startActivity(
                Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
