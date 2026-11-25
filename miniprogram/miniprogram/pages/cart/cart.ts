import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { dateTime, diningStatus, foodFallback, foodImage, money } from '../../utils/format'
import type {
  DiningOrderRound,
  DiningSession,
  OrderDetailItem,
  ShoppingCartItem,
} from '../../types/api'
import { confirm, showError, toast } from '../../utils/ui'

interface CartView extends ShoppingCartItem {
  imageSrc: string
  unitText: string
  lineText: string
  flavorText: string
}

interface HistoryItemView extends OrderDetailItem {
  imageSrc: string
  lineText: string
  flavorText: string
}

interface OrderRoundView extends DiningOrderRound {
  label: string
  timeText: string
  amountText: string
  items: HistoryItemView[]
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

function numberData(event: TapEvent, key: string): number {
  return Number(event.currentTarget.dataset[key] || 0)
}

function buildViews(items: ShoppingCartItem[]): CartView[] {
  return items.map((item) => ({
    ...item,
    imageSrc: foodImage(item.name, item.image),
    unitText: money(item.amount),
    lineText: money(Number(item.amount || 0) * Number(item.number || 0)),
    flavorText: item.dishFlavor ? item.dishFlavor.replace(/,/g, ' · ') : '',
  }))
}

function roundLabel(round: number): string {
  const names = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  return round >= 1 && round <= names.length
    ? `第${names[round - 1]}次下单`
    : `第${round}次下单`
}

function buildOrderRounds(rounds?: DiningOrderRound[] | null): OrderRoundView[] {
  return (rounds || []).map((round) => ({
    ...round,
    label: roundLabel(round.round),
    timeText: dateTime(round.orderTime),
    amountText: money(round.amount),
    items: (round.items || []).map((item) => ({
      ...item,
      imageSrc: foodImage(item.name, item.image),
      lineText: money(Number(item.amount || 0) * Number(item.number || 0)),
      flavorText: item.dishFlavor ? item.dishFlavor.replace(/,/g, ' · ') : '',
    })),
  }))
}

Page({
  data: {
    initialized: false,
    loading: true,
    shopOpen: true,
    mode: 'takeaway' as 'takeaway' | 'dining',
    session: null as DiningSession | null,
    sessionStatusText: '',
    sessionTotalText: '0.00',
    items: [] as CartView[],
    count: 0,
    subtotalText: '0.00',
    packText: '0.00',
    totalText: '0.00',
    canSubmit: false,
    checkoutText: '去结算',
    busyId: 0,
    orderRounds: [] as OrderRoundView[],
    orderRoundCount: 0,
    currentOrderNo: '',
    hasSubmittedOrder: false,
  },

  onLoad() {
    void this.loadData()
  },

  onShow() {
    if (this.data.initialized) {
      void this.loadData()
    }
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [status, session] = await Promise.all([
        api.shopStatus().catch(() => 1),
        api.diningMine().catch(() => null),
      ])
      const items = session ? await api.diningCart(session.id) : await api.cart()
      const views = buildViews(items)
      const count = views.reduce((sum, item) => sum + Number(item.number || 0), 0)
      const subtotal = views.reduce(
        (sum, item) => sum + Number(item.amount || 0) * Number(item.number || 0),
        0,
      )
      const open = status === 1
      const sessionMeta = session ? diningStatus(session.status) : null
      const canSubmit = views.length > 0
        && open
        && (!session || session.status === 1)
      const orderRounds = session ? buildOrderRounds(session.orderRounds) : []
      const hasSubmittedOrder = orderRounds.length > 0
      const currentOrderNo = session
        ? (session.currentOrderNo || (session.orders && session.orders.length > 0
          ? session.orders[session.orders.length - 1].number
          : ''))
        : ''
      this.setData({
        initialized: true,
        loading: false,
        shopOpen: open,
        mode: session ? 'dining' : 'takeaway',
        session,
        sessionStatusText: sessionMeta ? sessionMeta.label : '',
        sessionTotalText: money(session ? session.totalAmount : 0),
        items: views,
        count,
        subtotalText: money(subtotal),
        totalText: money(subtotal),
        canSubmit,
        checkoutText: session
          ? (session.status === 1
            ? (hasSubmittedOrder ? '确认加餐' : '确认下单')
            : '当前不可下单')
          : '去结算',
        orderRounds,
        orderRoundCount: orderRounds.length,
        currentOrderNo,
        hasSubmittedOrder,
      })
    } catch (error) {
      this.setData({ loading: false, initialized: true })
      showError(error)
    }
  },

  onItemImageError(event: TapEvent) {
    const id = numberData(event, 'id')
    const target = this.data.items.find((item) => item.id === id)
    if (!target) {
      return
    }
    const imageSrc = foodFallback(target.name)
    const items = this.data.items.map((item) => (
      item.id === id ? { ...item, imageSrc } : item
    ))
    this.setData({ items })
  },

  async onPlus(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.items.find((entry) => entry.id === id)
    if (!item || this.data.busyId) {
      return
    }
    if (!this.data.canSubmit) {
      toast(this.data.mode === 'dining' ? '当前桌台不可继续加菜' : '门店打烊中')
      return
    }
    this.setData({ busyId: id })
    try {
      if (this.data.mode === 'dining' && this.data.session) {
        await api.diningCartAdd({
          sessionId: this.data.session.id,
          dishId: Number(item.dishId || 0),
          dishFlavor: item.dishFlavor || undefined,
          number: 1,
        })
      } else if (item.dishId) {
        await api.addCart({ dishId: item.dishId, dishFlavor: item.dishFlavor || undefined })
      } else if (item.setmealId) {
        await api.addCart({ setmealId: item.setmealId })
      }
      await this.loadData()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ busyId: 0 })
    }
  },

  async onMinus(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.items.find((entry) => entry.id === id)
    if (!item || this.data.busyId) {
      return
    }
    this.setData({ busyId: id })
    try {
      if (this.data.mode === 'dining' && this.data.session) {
        await api.diningCartAdd({
          sessionId: this.data.session.id,
          dishId: Number(item.dishId || 0),
          dishFlavor: item.dishFlavor || undefined,
          number: -1,
        })
      } else if (item.dishId) {
        await api.subCart({ dishId: item.dishId, dishFlavor: item.dishFlavor || undefined })
      } else if (item.setmealId) {
        await api.subCart({ setmealId: item.setmealId })
      }
      await this.loadData()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ busyId: 0 })
    }
  },

  async onRemoveItem(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.items.find((entry) => entry.id === id)
    if (!item || !this.data.session && this.data.mode === 'dining') {
      return
    }
    const ok = await confirm(`把「${item.name}」从购物车移出？`, '移出商品')
    if (!ok) {
      return
    }
    try {
      if (this.data.mode === 'dining' && this.data.session) {
        await api.diningCartRemove(this.data.session.id, item.id)
      } else {
        for (let i = 0; i < Number(item.number || 0); i += 1) {
          if (item.dishId) {
            await api.subCart({ dishId: item.dishId, dishFlavor: item.dishFlavor || undefined })
          } else if (item.setmealId) {
            await api.subCart({ setmealId: item.setmealId })
          }
        }
      }
      await this.loadData()
      toast('已移出')
    } catch (error) {
      showError(error)
    }
  },

  async onClear() {
    if (this.data.items.length === 0) {
      return
    }
    const ok = await confirm('清空当前购物车？', '清空购物车')
    if (!ok) {
      return
    }
    try {
      if (this.data.mode === 'dining' && this.data.session) {
        await api.diningCartClear(this.data.session.id)
      } else {
        await api.cleanCart()
      }
      await this.loadData()
      toast('购物车已清空')
    } catch (error) {
      showError(error)
    }
  },

  async onCheckout() {
    if (!this.data.canSubmit) {
      toast(this.data.mode === 'dining' ? '当前桌台不可下单' : '门店打烊中')
      return
    }
    if (this.data.mode === 'dining' && this.data.session) {
      const hasSubmittedOrder = this.data.hasSubmittedOrder
      const actionText = hasSubmittedOrder ? '确认加餐' : '确认下单'
      const ok = await confirm(
        hasSubmittedOrder
          ? `本次 ${this.data.count} 件菜将并入当前订单 #${this.data.currentOrderNo}，不会生成新订单。是否继续？`
          : `确认提交本桌 ${this.data.count} 件菜？将生成当前堂食订单。`,
        actionText,
        hasSubmittedOrder ? '加餐' : '下单',
      )
      if (!ok) {
        return
      }
      try {
        await api.diningSubmit(this.data.session.id)
        await this.loadData()
        toast(hasSubmittedOrder ? '加餐成功，已并入当前订单' : '下单成功，已生成堂食订单')
      } catch (error) {
        showError(error)
      }
      return
    }
    wx.navigateTo({ url: '/pages/order-confirm/order-confirm' })
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  goSession() {
    if (!this.data.session) {
      return
    }
    wx.navigateTo({ url: `/pages/dining-session/dining-session?id=${this.data.session.id}` })
  },

  onRetry() {
    void this.loadData()
  },
})
