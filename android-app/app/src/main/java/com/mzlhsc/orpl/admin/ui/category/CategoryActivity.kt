package com.mzlhsc.orpl.admin.ui.category

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.databinding.ActivityCategoryBinding
import com.mzlhsc.orpl.admin.databinding.DialogCategoryEditBinding
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private val adapter = CategoryAdapter(onToggle = ::toggleStatus, onEdit = ::showEditDialog)

    private var type = 1
    private var page = 1
    private var pageCount = 0
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.toolbarTitle.text = "分类管理"
        binding.toolbar.toolbarAction.text = "＋ 新增"
        binding.toolbar.toolbarAction.visibleIf(true)
        binding.toolbar.toolbarAction.setOnClickListener { showEditDialog(null) }
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { reload() }

        binding.tabs.addTab(binding.tabs.newTab().setText("菜品分类"), true)
        binding.tabs.addTab(binding.tabs.newTab().setText("套餐分类"))
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                type = tab.position + 1
                reload()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading) return
                if (!recyclerView.canScrollVertically(1)) loadMore()
            }
        })

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
                val result = AdminApi.categoryPage(page, PAGE_SIZE, type)
                pageCount = result.pageCount.toInt()
                val list = if (append) (adapter.currentList() + result.items) else result.items
                adapter.submit(list)
                binding.empty.root.visibleIf(list.isEmpty())
                binding.empty.emptyTitle.text = "还没有分类"
                binding.empty.emptyDesc.text = "点击右上角新增一个分类"
            } catch (e: Exception) {
                toast(e.message ?: "分类加载失败")
            } finally {
                loading = false
                binding.refresh.isRefreshing = false
            }
        }
    }

    private fun toggleStatus(category: Category, enabled: Boolean) {
        val id = category.id ?: return
        val next = if (enabled) 1 else 0
        lifecycleScope.launch {
            try {
                AdminApi.categoryStatus(next, id)
                adapter.replace(category.copy(status = next))
                toast(if (enabled) "已启用" else "已禁用")
            } catch (e: Exception) {
                adapter.notifyDataSetChanged()
                toast(e.message ?: "操作失败")
            }
        }
    }

    private fun showEditDialog(category: Category?) {
        val dialogBinding = DialogCategoryEditBinding.inflate(layoutInflater)
        dialogBinding.etName.setText(category?.name.orEmpty())
        dialogBinding.etSort.setText((category?.sort ?: 0).toString())
        dialogBinding.tvHint.text = if (category == null) {
            "将创建在「${if (type == 1) "菜品分类" else "套餐分类"}」下"
        } else {
            "启用状态可在列表里直接切换"
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (category == null) "新增分类" else "编辑分类")
            .setView(dialogBinding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)

        if (category != null) {
            builder.setNeutralButton("删除", null)
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                if (name.isEmpty()) {
                    toast("请填写分类名称")
                    return@setOnClickListener
                }
                val sort = dialogBinding.etSort.text.toString().trim().toIntOrNull() ?: 0
                dialog.dismiss()
                save(category, name, sort)
            }
            if (category != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.danger)
                )
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    dialog.dismiss()
                    Dialogs.confirm(this, "删除分类", "确认删除「${category.name}」？", "删除") {
                        deleteCategory(category)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun save(category: Category?, name: String, sort: Int) {
        lifecycleScope.launch {
            try {
                if (category == null) {
                    AdminApi.categorySave(
                        mapOf("type" to type, "name" to name, "sort" to sort, "status" to 1)
                    )
                    toast("分类已创建")
                } else {
                    AdminApi.categoryUpdate(
                        mapOf(
                            "id" to category.id,
                            "type" to (category.type ?: type),
                            "name" to name,
                            "sort" to sort,
                            "status" to (category.status ?: 1),
                        )
                    )
                    toast("分类已更新")
                }
                reload()
            } catch (e: Exception) {
                toast(e.message ?: "保存失败")
            }
        }
    }

    private fun deleteCategory(category: Category) {
        val id = category.id ?: return
        lifecycleScope.launch {
            try {
                AdminApi.categoryDelete(id)
                toast("分类已删除")
                reload()
            } catch (e: Exception) {
                toast(e.message ?: "删除失败")
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
