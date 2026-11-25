package com.mzlhsc.orpl.admin.ui.employee

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Employee
import com.mzlhsc.orpl.admin.databinding.ActivityEmployeeBinding
import com.mzlhsc.orpl.admin.databinding.DialogEmployeeEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf
import kotlinx.coroutines.launch

class EmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeBinding
    private val adapter = EmployeeAdapter(onToggle = ::toggleStatus, onEdit = ::showEditDialog)

    private var keyword: String? = null
    private var page = 1
    private var pageCount = 0
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.toolbarTitle.text = "员工管理"
        binding.toolbar.toolbarAction.text = "＋ 新增"
        binding.toolbar.toolbarAction.visibleIf(true)
        binding.toolbar.toolbarAction.setOnClickListener { showEditDialog(null) }
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { reload() }

        binding.btnSearch.setOnClickListener { applyKeyword() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyKeyword()
                true
            } else {
                false
            }
        }

        binding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading) return
                if (!recyclerView.canScrollVertically(1)) loadMore()
            }
        })

        reload()
    }

    private fun applyKeyword() {
        keyword = binding.etSearch.text.toString().trim().takeIf { it.isNotEmpty() }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        reload()
    }

    private fun reload() {
        page = 1
        pageCount = 0
        fetch(append = false)
    }

    private fun loadMore() {
        if (page >= pageCount) return
        page += 1
        fetch(append = true)
    }

    private fun fetch(append: Boolean) {
        if (loading) return
        loading = true
        if (!append) binding.refresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = AdminApi.employeePage(page, PAGE_SIZE, keyword)
                pageCount = result.pageCount.toInt()
                adapter.submit(result.items, append)
                if (!append) {
                    binding.empty.root.visibleIf(result.items.isEmpty())
                    binding.empty.emptyTitle.text = "没有员工"
                    binding.empty.emptyDesc.text = "换个姓名试试"
                }
            } catch (e: Exception) {
                toast(e.message ?: "员工列表加载失败")
            } finally {
                loading = false
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun toggleStatus(employee: Employee, enabled: Boolean) {
        val id = employee.id ?: return
        val next = if (enabled) 1 else 0
        lifecycleScope.launch {
            try {
                AdminApi.employeeStatus(next, id)
                adapter.replace(employee.copy(status = next))
                toast(if (enabled) "账号已启用" else "账号已禁用")
            } catch (e: Exception) {
                adapter.notifyDataSetChanged()
                toast(e.message ?: "操作失败")
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }

    /** 新增 / 编辑员工：与 Web 端一致，新增默认密码 123456，编辑不改密码与状态 */
    private fun showEditDialog(employee: Employee?) {
        val form = DialogEmployeeEditBinding.inflate(LayoutInflater.from(this))
        val isEdit = employee != null
        form.tvHint.text = if (isEdit) {
            "编辑不会修改密码与启用状态，账号创建后不可改"
        } else {
            "新增员工默认密码 123456，创建后可在列表里启用/禁用"
        }

        var sex = employee?.sex
        var dialogRef: androidx.appcompat.app.AlertDialog? = null

        fun bindSex() {
            form.tvSex.text = sex?.takeIf { it.isNotBlank() } ?: "请选择"
            form.tvSex.setTextColor(getColor(if (sex.isNullOrBlank()) R.color.muted else R.color.ink))
        }

        form.tvSex.setOnClickListener {
            val options = arrayOf("男", "女")
            val checked = options.indexOf(sex)
            MaterialAlertDialogBuilder(this)
                .setTitle("性别")
                .setSingleChoiceItems(options, checked) { d, which ->
                    sex = options[which]
                    bindSex()
                    d.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        if (isEdit) {
            form.etName.setText(employee?.name.orEmpty())
            form.etUsername.setText(employee?.username.orEmpty())
            form.etUsername.isEnabled = false
            form.etPhone.setText(employee?.phone.orEmpty())
            form.etIdNumber.setText(employee?.idNumber.orEmpty())
            bindSex()
        } else {
            bindSex()
        }

        dialogRef = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "编辑员工" else "新增员工")
            .setView(form.root)
            .setNegativeButton("取消", null)
            .setPositiveButton(if (isEdit) "保存" else "创建", null)
            .create()

        dialogRef.setOnShowListener {
            dialogRef.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = form.etName.text.toString().trim()
                val username = form.etUsername.text.toString().trim()
                val phone = form.etPhone.text.toString().trim()
                val idNumber = form.etIdNumber.text.toString().trim()

                if (name.isEmpty()) {
                    toast("请填写姓名")
                    return@setOnClickListener
                }
                if (!isEdit && username.isEmpty()) {
                    toast("请填写登录账号")
                    return@setOnClickListener
                }
                if (phone.isNotEmpty() && phone.length != 11) {
                    toast("手机号需为 11 位")
                    return@setOnClickListener
                }
                if (idNumber.isNotEmpty() && idNumber.length != 18) {
                    toast("身份证号需为 18 位")
                    return@setOnClickListener
                }

                val body = mutableMapOf<String, Any?>(
                    "name" to name,
                    "phone" to phone,
                    "sex" to sex,
                    "idNumber" to idNumber,
                )
                if (isEdit) {
                    body["id"] = employee?.id
                    body["username"] = employee?.username
                } else {
                    body["username"] = username
                }

                safeLaunch {
                    try {
                        if (isEdit) AdminApi.employeeUpdate(body) else AdminApi.employeeSave(body)
                        toast(if (isEdit) "员工已更新" else "员工已创建，默认密码 123456")
                        dialogRef.dismiss()
                        reload()
                    } catch (e: Exception) {
                        toast(e.message ?: "保存失败")
                    }
                }
            }
        }
        dialogRef.show()
    }
}
