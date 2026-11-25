import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import {
  dateTime,
  foodFallback,
  foodImage,
  money,
  orderCanCancel,
  orderCanRemind,
  orderCanRepeat,
  orderStatus,
} from '../../utils/format'
import type { Order } from '../../types/api'
import { confirm, showError, toast } from '../../utils/ui'

interface OrderView extends Order {
  statusText: string
  statusTone: string
  amountText: string
  timeText: string
  summary: string
  itemCount: number
  firstImage: string
  canCancel: boolean
  canRemind: boolean
  canRepeat: boolean
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

interface StatusTab {
  label: string
  value: number
}

const STATUS_TABS: StatusTab[] = [
  { label: '全部', value: 0 },
  { label: '待接单', value: 2 },
  { label: '已接单', value: 3 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 },
]

function toView(order: Order): OrderView {
  const meta = orderStatus(order.status)
  const details = order.orderDetailList || []
  const itemCount = details.reduce((sum, item) => sum + Number(item.number || 0), 0)
  const summary = details.length > 0
    ? details.map((item) => `${item.name} ×${item.number}`).join('、')
    : (order.orderDishes || (order.orderType === 2 ? '堂食订单' : '订单菜品'))
  const first = details[0]
  return {
    ...order,
    statusText: meta.label,
    statusTone: meta.tone,
    amountText: money(order.amount),
    timeText: dateTime(order.orderTime),
    summary,
    itemCount,
    firstImage: foodImage(first ? first.name : '订单', first ? first.image : null),
    canCancel: orderCanCancel(order.status) && order.status === 2,
    canRemind: orderCanRemind(order.status),
    canRepeat: orderCanRepeat(order.status),
  }
}

Page({
  data: {
    initialized: false,
    loading: true,
    loadingMore: false,
    error: '',
    activeStatus: 0,
    statusTabs: STATUS_TABS,
    orders: [] as OrderView[],
    page: 1,
    pages: 1,
    total: 0,
    hasMore: false,
  },

  onLoad() {
    void this.loadOrders(true)
  },

  onShow() {
    if (this.data.initialized) {
      void this.loadOrders(true)
    }
  },

  onPullDownRefresh() {
    void this.loadOrders(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loadingMore) {
      void this.loadOrders(false)
    }
  },

  async loadOrders(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1
    if (reset) {
      this.setData({ loading: true, error: '' })
    } else {
      this.setData({ loadingMore: true })
    }
    try {
      await ensureLogin()
      const status = this.data.activeStatus === 0 ? undefined : this.data.activeStatus
      const result = await api.orders(page, 10, status)
      const views = (result.list || []).map(toView)
      const orders = reset ? views : this.data.orders.concat(views)
      const pages = Number(result.pages || 0)
      this.setData({
        initialized: true,
        loading: false,
        loadingMore: false,
        orders,
        page,
        pages,
        total: Number(result.total || 0),
        hasMore: page < pages,
      })
    } catch (error) {
      this.setData({
        initialized: true,
        loading: false,
        loadingMore: false,
        error: error instanceof Error ? error.message : '订单加载失败',
      })
      if (!reset) {
        showError(error)
      }
    }
  },

  onStatusTap(event: TapEvent) {
    const value = Number(event.currentTarget.dataset.value || 0)
    if (value === this.data.activeStatus) {
      return
    }
    this.setData({ activeStatus: value, orders: [], page: 1, hasMore: false })
    void this.loadOrders(true)
  },

  onOrderTap(event: TapEvent) {
    const id = Number(event.currentTarget.dataset.id || 0)
    if (!id) {
      return
    }
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` })
  },

  onOrderImageError(event: TapEvent) {
    const index = Number(event.currentTarget.dataset.index || -1)
    if (index < 0) {
      return
    }
    const target = this.data.orders[index]
    if (!target) {
      return
    }
    const orders = this.data.orders.map((order, i) => (
      i === index ? { ...order, firstImage: foodFallback(order.summary) } : order
    ))
    this.setData({ orders })
  },

  async onCancel(event: TapEvent) {
    const id = Number(event.currentTarget.dataset.id || 0)
    const ok = await confirm('取消后菜品库存会释放，确定取消这笔订单？', '取消订单', '取消订单')
    if (!ok) {
      return
    }
    try {
      await api.cancelOrder(id, '用户取消订单')
      toast('订单已取消')
      await this.loadOrders(true)
    } catch (error) {
      showError(error)
    }
  },

  async onRemind(event: TapEvent) {
    const id = Number(event.currentTarget.dataset.id || 0)
    try {
      await api.reminder(id)
      toast('已提醒门店，请稍等')
    } catch (error) {
      showError(error)
    }
  },

  async onRepeat(event: TapEvent) {
    const id = Number(event.currentTarget.dataset.id || 0)
    try {
      await api.repetition(id)
      toast('已按原单加入购物车')
      setTimeout(() => wx.switchTab({ url: '/pages/menu/menu' }), 600)
    } catch (error) {
      showError(error)
    }
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  onRetry() {
    void this.loadOrders(true)
  },
})
