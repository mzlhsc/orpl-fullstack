package com.mzlhsc.orpl.admin.ui.goods

import android.os.Bundle
import android.app.Activity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.data.model.Setmeal
import com.mzlhsc.orpl.admin.databinding.FragmentListPageBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.util.hide
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.show
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf

class SetmealListFragment : Fragment(R.layout.fragment_list_page) {

    private var _binding: FragmentListPageBinding? = null
    private val binding get() = _binding!!

    private val adapter = SetmealAdapter(
        onToggle = ::toggleStatus,
        onEdit = ::openEdit,
        onDelete = ::confirmDelete,
    )

    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) reload()
    }

    private var chipItems: List<ChipItem> = listOf(ChipItem("全部", null))
    private var selectedChip = 0
    private var categoryId: Long? = null
    private var keyword: String? = null

    private var page = 1
    private var pageCount = 0
    private var loading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListPageBinding.bind(view)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { reload() }

        renderChips()

        binding.btnAdd.setOnClickListener {
            editLauncher.launch(SetmealEditActivity.intent(requireActivity()))
        }

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

        loadCategories()
        reload()
    }

    private fun loadCategories() {
        safeLaunch {
            val categories: List<Category> = runCatching { AdminApi.categoryList(2) }.getOrDefault(emptyList())
            chipItems = listOf(ChipItem("全部", null)) + categories.map {
                ChipItem(it.name ?: "未命名", it.id)
            }
            renderChips()
        }
    }

    private fun renderChips() {
        Chips.render(binding.chipRow, chipItems, selectedChip) { index ->
            selectedChip = index
            categoryId = chipItems[index].value as Long?
            renderChips()
            reload()
        }
    }

    private fun applyKeyword() {
        keyword = binding.etSearch.text.toString().trim().takeIf { it.isNotEmpty() }
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
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
        if (append) binding.loadingMore.show() else binding.refresh.isRefreshing = true

        safeLaunch {
            try {
                val result = AdminApi.setmealPage(
                    page = page,
                    pageSize = PAGE_SIZE,
                    name = keyword,
                    categoryId = categoryId,
                )
                pageCount = result.pageCount.toInt()
                adapter.submit(result.items, append)
                if (!append) {
                    binding.empty.root.visibleIf(result.items.isEmpty())
                    binding.empty.emptyTitle.text = "暂无套餐"
                    binding.empty.emptyDesc.text = "换个分类或名称试试"
                }
            } finally {
                loading = false
                binding.refresh.isRefreshing = false
                binding.loadingMore.hide()
            }
        }
    }

    private fun openEdit(setmeal: Setmeal) {
        editLauncher.launch(SetmealEditActivity.intent(requireActivity(), setmeal.id))
    }

    private fun confirmDelete(setmeal: Setmeal) {
        val id = setmeal.id ?: return
        Dialogs.confirm(
            requireContext(),
            "删除套餐",
            "确定删除「${setmeal.name ?: "该套餐"}」吗？删除后不可恢复。",
            "删除",
        ) {
            safeLaunch {
                AdminApi.setmealDelete(listOf(id))
                toast("套餐已删除")
                reload()
            }
        }
    }

    private fun toggleStatus(setmeal: Setmeal, enabled: Boolean) {
        val id = setmeal.id ?: return
        val next = if (enabled) 1 else 0
        safeLaunch {
            try {
                AdminApi.setmealStatus(next, id)
                toast(if (enabled) "已起售" else "已停售")
                adapter.replace(setmeal.copy(status = next))
            } catch (e: Exception) {
                adapter.notifyDataSetChanged()
                throw e
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
