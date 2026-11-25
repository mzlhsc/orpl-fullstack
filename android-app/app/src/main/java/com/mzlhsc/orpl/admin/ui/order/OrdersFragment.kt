package com.mzlhsc.orpl.admin.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Order
import com.mzlhsc.orpl.admin.databinding.FragmentOrdersBinding
import com.mzlhsc.orpl.admin.ui.common.ChipItem
import com.mzlhsc.orpl.admin.ui.common.Chips
import com.mzlhsc.orpl.admin.ui.common.Dialogs
import com.mzlhsc.orpl.admin.ui.main.MainActivity
import com.mzlhsc.orpl.admin.util.hide
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.show
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf
import kotlinx.coroutines.launch

class OrdersFragment : androidx.fragment.app.Fragment(R.layout.fragment_orders) {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val adapter = OrderAdapter(onOpen = ::openDetail, onAction = ::handleAction)

    private val statusChips = listOf(
        ChipItem("全部", null),
        ChipItem("待付款", 1),
        ChipItem("待接单", 2),
        ChipItem("已接单", 3),
        ChipItem("已完成", 5),
        ChipItem("已取消", 6),
    )
    private var selectedChip = 0
    private var status: Int? = null
    private var number: String? = null
    private var phone: String? = null

    private var page = 1
    private var pageCount = 0
    private var loading = false
    private var loaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrdersBinding.bind(view)

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        binding.refresh.setColorSchemeResources(R.color.brand)
        binding.refresh.setOnRefreshListener { reload() }

        renderChips()

        binding.btnSearch.setOnClickListener { applyKeyword() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyKeyword()
                true
            } else {
                false
            }
        }

        binding.rvOrders.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading) return
                if (!recyclerView.canScrollVertically(1)) loadMore()
            }
        })

        reload()
    }

    private fun renderChips() {
        Chips.render(binding.chipRow, statusChips, selectedChip) { index ->
            selectedChip = index
            status = statusChips[index].value as Int?
            renderChips()
            reload()
        }
    }

    private fun applyKeyword() {
        val keyword = binding.etSearch.text.toString().trim()
        number = null
        phone = null
        if (keyword.isNotEmpty()) {
            // 后端把订单号和手机号分成两个参数：11 位纯数字按手机号查，其它按订单号查
            if (keyword.length == 11 && keyword.all { it.isDigit() }) phone = keyword else number = keyword
        }
        hideKeyboard()
        reload()
    }

    private fun reload() {
        page = 1
        pageCount = 0
        loaded = false
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
                val result = AdminApi.orderPage(
                    page = page,
                    pageSize = PAGE_SIZE,
                    status = status,
                    number = number,
                    phone = phone,
                )
                pageCount = result.pageCount.toInt()
                loaded = true
                adapter.submit(result.items, append)
                if (!append) {
                    binding.empty.root.visibleIf(result.items.isEmpty())
                }
            } catch (e: Exception) {
                if (append) page -= 1
                throw e
            } finally {
                loading = false
                binding.refresh.isRefreshing = false
                binding.loadingMore.hide()
            }
        }
    }

    private fun openDetail(order: Order) {
        val id = order.id ?: return
        OrderDetailActivity.start(requireContext(), id)
    }

    private fun handleAction(order: Order, action: OrderAction) {
        val id = order.id ?: return
        when (action) {
            OrderAction.CONFIRM -> runAction("接单成功") { AdminApi.orderConfirm(id) }
            OrderAction.COMPLETE -> confirmComplete(order)
            OrderAction.REJECT -> Dialogs.askReason(
                requireContext(),
                "拒单原因",
                "确认拒单后订单会退回用户端",
            ) { reason -> runAction("已拒单") { AdminApi.orderReject(id, reason) } }
            OrderAction.CANCEL -> Dialogs.askReason(
                requireContext(),
                "取消原因",
                "取消后该订单不再流转",
            ) { reason -> runAction("订单已取消") { AdminApi.orderCancel(id, reason) } }
            OrderAction.MARK_PAID -> confirmPay(order, 1)
            OrderAction.MARK_UNPAID -> confirmPay(order, 0)
        }
    }

    private fun confirmComplete(order: Order) {
        val id = order.id ?: return
        Dialogs.confirm(
            requireContext(),
            "完成订单",
            "完成后订单状态为「已完成」，支付状态会自动置为「已支付」。",
            "完成订单",
        ) {
            runAction("订单已完成，支付状态已置为已支付") { AdminApi.orderComplete(id) }
        }
    }

    private fun confirmPay(order: Order, payStatus: Int) {
        val id = order.id ?: return
        val label = if (payStatus == 1) "已支付" else "未支付"
        Dialogs.confirm(
            requireContext(),
            "标记$label",
            "确定把订单 ${order.number ?: "#$id"} 的支付状态改为「$label」吗？",
            "标记$label",
        ) {
            runAction("已标记为$label") { AdminApi.orderPayStatus(id, payStatus) }
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Unit) {
        safeLaunch {
            block()
            toast(successMessage)
            (activity as? MainActivity)?.refreshOrderBadge()
            reload()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        // 从详情页返回时订单状态可能已变化，重新拉取第一页
        if (loaded && isVisible) reload()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
