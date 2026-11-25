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
import type { Order, OrderDetailItem } from '../../types/api'
import { confirm, copyText, showError, toast } from '../../utils/ui'

interface DetailItemView extends OrderDetailItem {
  imageSrc: string
  unitText: string
  lineText: string
  flavorText: string
}

interface StepView {
  label: string
  state: 'done' | 'active' | 'idle'
}

interface ActionTapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

interface OrderView extends Order {
  statusText: string
  statusTone: string
  statusHint: string
  amountText: string
  goodsAmountText: string
  packText: string
  timeText: string
  estimatedText: string
  deliveryText: string
  checkoutText: string
  tablewareText: string
  addressLine: string
  payMethodText: string
  payStatusText: string
  canCancel: boolean
  canRemind: boolean
  canRepeat: boolean
  isDining: boolean
}

function statusHint(status: number): string {
  switch (status) {
    case 1:
      return '订单待付款，请完成支付'
    case 2:
      return '订单已提交，门店正在确认'
    case 3:
    case 4:
      return '门店已接单，正在为你备餐'
    case 5:
      return '订单已完成，谢谢光临'
    case 6:
      return '订单已取消，库存已释放'
    case 7:
      return '退款处理中，请稍后查看'
    default:
      return '订单正在处理中'
  }
}

function buildSteps(status: number): StepView[] {
  const stages = [
    { label: '已下单', value: 2 },
    { label: '已接单', value: 3 },
    { label: '已完成', value: 5 },
  ]
  return stages.map((stage) => {
    if (status > 5 || status < 2 || status < stage.value) {
      return { label: stage.label, state: 'idle' } as StepView
    }
    const isCurrent = status === stage.value && stage.value < 5
    return { label: stage.label, state: isCurrent ? 'active' : 'done' } as StepView
  })
}

function toView(order: Order): OrderView {
  const meta = orderStatus(order.status)
  const isDining = order.orderType === 2
  const packAmount = Number(order.packAmount || 0)
  const tablewareText = order.tablewareStatus === 1
    ? '按餐量提供'
    : (Number(order.tablewareNumber || 0) > 0 ? `${order.tablewareNumber} 份` : '无需餐具')
  return {
    ...order,
    statusText: meta.label,
    statusTone: meta.tone,
    statusHint: statusHint(order.status),
    amountText: money(order.amount),
    goodsAmountText: money(Number(order.amount || 0) - packAmount),
    packText: money(packAmount),
    timeText: dateTime(order.orderTime),
    estimatedText: dateTime(order.estimatedDeliveryTime),
    deliveryText: dateTime(order.deliveryTime),
    checkoutText: dateTime(order.checkoutTime),
    tablewareText,
    addressLine: isDining
      ? `堂食订单${order.remark ? ` · ${order.remark}` : ''}`
      : [order.address, order.consignee, order.phone].filter((item) => !!item).join(' · '),
    payMethodText: order.payMethod === 2 ? '支付宝' : '微信支付',
    payStatusText: order.payStatus === 1 ? '已支付' : (order.payStatus === 2 ? '已退款' : '未支付'),
    canCancel: orderCanCancel(order.status) && order.status === 2,
    canRemind: orderCanRemind(order.status),
    canRepeat: orderCanRepeat(order.status),
    isDining,
  }
}

Page({
  data: {
    loading: true,
    error: '',
    orderId: 0,
    isNew: false,
    order: null as OrderView | null,
    details: [] as DetailItemView[],
    count: 0,
    steps: [] as StepView[],
  },

  onLoad(options: Record<string, string | undefined>) {
    const id = Number(options.id || 0)
    this.setData({
      orderId: id,
      isNew: options.new === '1',
    })
    if (!id) {
      toast('订单参数有误')
      setTimeout(() => wx.navigateBack(), 800)
      return
    }
    void this.loadOrder()
  },

  onPullDownRefresh() {
    void this.loadOrder().finally(() => wx.stopPullDownRefresh())
  },

  async loadOrder() {
    this.setData({ loading: true, error: '' })
    try {
      await ensureLogin()
      const order = await api.orderDetail(this.data.orderId)
      const view = toView(order)
      const details = (order.orderDetailList || []).map((item): DetailItemView => ({
        ...item,
        imageSrc: foodImage(item.name, item.image),
        unitText: money(item.amount),
        lineText: money(Number(item.amount || 0) * Number(item.number || 0)),
        flavorText: item.dishFlavor ? item.dishFlavor.replace(/,/g, ' · ') : '',
      }))
      const count = details.reduce((sum, item) => sum + Number(item.number || 0), 0)
      this.setData({
        loading: false,
        order: view,
        details,
        count,
        steps: order.status === 6 || order.status === 7 ? [] : buildSteps(order.status),
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '订单详情加载失败',
      })
    }
  },

  onCopyNumber() {
    if (this.data.order) {
      copyText(this.data.order.number, '订单号已复制')
    }
  },

  onCopyAddress() {
    if (this.data.order) {
      copyText(this.data.order.addressLine, '地址信息已复制')
    }
  },

  onItemImageError(event: ActionTapEvent) {
    const index = Number(event.currentTarget.dataset.index || -1)
    if (index < 0) {
      return
    }
    const target = this.data.details[index]
    if (!target) {
      return
    }
    const imageSrc = foodFallback(target.name)
    const details = this.data.details.map((item, i) => (
      i === index ? { ...item, imageSrc } : item
    ))
    this.setData({ details })
  },

  async onCancel() {
    if (!this.data.order) {
      return
    }
    const ok = await confirm('确定取消这笔订单？取消后不可恢复。', '取消订单', '取消订单')
    if (!ok) {
      return
    }
    try {
      await api.cancelOrder(this.data.order.id, '用户取消订单')
      toast('订单已取消')
      await this.loadOrder()
    } catch (error) {
      showError(error)
    }
  },

  async onRemind() {
    if (!this.data.order) {
      return
    }
    try {
      await api.reminder(this.data.order.id)
      toast('已提醒门店，请稍等')
    } catch (error) {
      showError(error)
    }
  },

  async onRepeat() {
    if (!this.data.order) {
      return
    }
    try {
      await api.repetition(this.data.order.id)
      toast('已按原单加入购物车')
      setTimeout(() => wx.switchTab({ url: '/pages/menu/menu' }), 700)
    } catch (error) {
      showError(error)
    }
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  onRetry() {
    void this.loadOrder()
  },
})
