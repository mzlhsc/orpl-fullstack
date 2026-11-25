import { api } from '../../utils/api'
import { ensureLogin, getStoredUser } from '../../utils/auth'
import {
  dateTime,
  diningStatus,
  foodImage,
  money,
} from '../../utils/format'
import type {
  DiningOrderRound,
  DiningSession,
  DiningSessionMember,
  OrderDetailItem,
  ShoppingCartItem,
} from '../../types/api'
import { confirm, copyText, showError, showLoading, hideLoading, toast } from '../../utils/ui'

interface SessionView extends DiningSession {
  statusText: string
  statusTone: string
  totalText: string
  paidText: string
  unpaidText: string
  perPersonText: string
  cartAmountText: string
  openTimeText: string
}

interface MemberView extends DiningSessionMember {
  nameText: string
  initial: string
  isMe: boolean
  isOwnerFlag: boolean
  payText: string
  paidText: string
}

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

function toSessionView(session: DiningSession): SessionView {
  const meta = diningStatus(session.status)
  return {
    ...session,
    statusText: meta.label,
    statusTone: meta.tone,
    totalText: money(session.totalAmount),
    paidText: money(session.paidAmount),
    unpaidText: money(session.unpaidAmount),
    perPersonText: money(session.perPersonAmount),
    cartAmountText: money(session.cartAmount),
    openTimeText: dateTime(session.openTime),
  }
}

function toMemberView(member: DiningSessionMember, currentUserId: number): MemberView {
  const nameText = member.nickname || `用户${member.userId}`
  return {
    ...member,
    nameText,
    initial: nameText.slice(0, 1),
    isMe: member.userId === currentUserId,
    isOwnerFlag: member.isOwner === 1,
    payText: member.payStatus === 1 ? '已支付' : '未支付',
    paidText: money(member.paidAmount),
  }
}

function toCartView(item: ShoppingCartItem): CartView {
  return {
    ...item,
    imageSrc: foodImage(item.name, item.image),
    unitText: money(item.amount),
    lineText: money(Number(item.amount || 0) * Number(item.number || 0)),
    flavorText: item.dishFlavor ? item.dishFlavor.replace(/,/g, ' · ') : '',
  }
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

let sessionStatusTimer: ReturnType<typeof setInterval> | null = null

Page({
  data: {
    initialized: false,
    loading: true,
    error: '',
    sessionId: 0,
    currentUserId: 0,
    session: null as SessionView | null,
    members: [] as MemberView[],
    carts: [] as CartView[],
    orderRounds: [] as OrderRoundView[],
    hasSubmittedOrder: false,
    currentOrderNo: '',
    submitButtonText: '确认下单',
    cartCount: 0,
    canSubmit: false,
    canEditCart: false,
    canAaPay: false,
    aaShareText: '0.00',
    busyId: 0,
    actionBusy: false,
    closedHandled: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    const sessionId = Number(options.id || 0)
    this.setData({ sessionId })
    if (!sessionId) {
      toast('会话参数有误')
      setTimeout(() => wx.navigateBack(), 800)
      return
    }
    void this.loadSession()
  },

  onShow() {
    this.startStatusPoll()
    if (this.data.initialized) {
      void this.loadSession()
    }
  },

  onHide() {
    this.stopStatusPoll()
  },

  onUnload() {
    this.stopStatusPoll()
  },

  startStatusPoll() {
    this.stopStatusPoll()
    if (this.data.sessionId && !this.data.closedHandled) {
      sessionStatusTimer = setInterval(() => {
        void this.checkSessionClosed()
      }, 5000)
    }
  },

  stopStatusPoll() {
    if (sessionStatusTimer) {
      clearInterval(sessionStatusTimer)
      sessionStatusTimer = null
    }
  },

  async checkSessionClosed() {
    if (!this.data.sessionId || this.data.closedHandled) {
      return
    }
    try {
      const session = await api.diningDetail(this.data.sessionId)
      if (session.status === 3 || session.status === 4) {
        this.exitClosedSession()
      }
    } catch {
      // 轮询失败不打断当前页面，下一次继续检查
    }
  },

  exitClosedSession() {
    if (this.data.closedHandled) {
      return
    }
    this.stopStatusPoll()
    this.setData({ closedHandled: true, loading: false, error: '' })
    toast('本桌会话已结束')
    setTimeout(() => {
      wx.switchTab({ url: '/pages/dining/dining' })
    }, 1200)
  },

  onPullDownRefresh() {
    void this.loadSession().finally(() => wx.stopPullDownRefresh())
  },

  async loadSession() {
    this.setData({ loading: true, error: '' })
    try {
      await ensureLogin()
      const session = await api.diningDetail(this.data.sessionId)
      if (session.status === 3 || session.status === 4) {
        this.exitClosedSession()
        return
      }
      const currentUser = getStoredUser()
      const currentUserId = currentUser ? currentUser.id : 0
      const members = (session.members || []).map((member) => toMemberView(member, currentUserId))
      const carts = (session.cartItems || []).map(toCartView)
      const orderRounds = buildOrderRounds(session.orderRounds)
      const hasSubmittedOrder = orderRounds.length > 0
      const cartCount = carts.reduce((sum, item) => sum + Number(item.number || 0), 0)
      const myMember = members.find((member) => member.isMe)
      this.setData({
        initialized: true,
        loading: false,
        currentUserId,
        session: toSessionView(session),
        members,
        carts,
        orderRounds,
        hasSubmittedOrder,
        currentOrderNo: session.currentOrderNo || '',
        submitButtonText: hasSubmittedOrder ? '确认加餐' : '确认下单',
        cartCount,
        canSubmit: session.status === 1 && carts.length > 0,
        canEditCart: session.status === 1,
        canAaPay: session.status === 2
          && session.aaEnabled === 1
          && !!myMember
          && myMember.payStatus !== 1,
        aaShareText: money(session.perPersonAmount),
      })
    } catch (error) {
      this.setData({
        initialized: true,
        loading: false,
        error: error instanceof Error ? error.message : '堂食会话加载失败',
      })
    }
  },

  onCartImageError(event: TapEvent) {
    const id = numberData(event, 'id')
    const target = this.data.carts.find((item) => item.id === id)
    if (!target) {
      return
    }
    const imageSrc = foodImage(target.name, undefined)
    const carts = this.data.carts.map((item) => (
      item.id === id ? { ...item, imageSrc } : item
    ))
    this.setData({ carts })
  },

  async onPlusCart(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.carts.find((entry) => entry.id === id)
    const session = this.data.session
    if (!item || !session || !this.data.canEditCart || this.data.busyId) {
      return
    }
    if (!item.dishId) {
      toast('该商品暂不支持堂食加菜')
      return
    }
    this.setData({ busyId: id })
    try {
      await api.diningCartAdd({
        sessionId: session.id,
        dishId: item.dishId,
        dishFlavor: item.dishFlavor || undefined,
        number: 1,
      })
      await this.loadSession()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ busyId: 0 })
    }
  },

  async onMinusCart(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.carts.find((entry) => entry.id === id)
    const session = this.data.session
    if (!item || !session || !this.data.canEditCart || this.data.busyId) {
      return
    }
    if (!item.dishId) {
      toast('该商品暂不支持堂食加减')
      return
    }
    this.setData({ busyId: id })
    try {
      await api.diningCartAdd({
        sessionId: session.id,
        dishId: item.dishId,
        dishFlavor: item.dishFlavor || undefined,
        number: -1,
      })
      await this.loadSession()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ busyId: 0 })
    }
  },

  async onRemoveCart(event: TapEvent) {
    const id = numberData(event, 'id')
    const session = this.data.session
    if (!session || !this.data.canEditCart) {
      return
    }
    const ok = await confirm('把这件菜从本桌购物车移出？', '移出菜品')
    if (!ok) {
      return
    }
    try {
      await api.diningCartRemove(session.id, id)
      await this.loadSession()
      toast('已移出')
    } catch (error) {
      showError(error)
    }
  },

  async onClearCart() {
    const session = this.data.session
    if (!session || !this.data.canEditCart) {
      return
    }
    const ok = await confirm('清空本桌共享购物车？', '清空本桌购物车')
    if (!ok) {
      return
    }
    try {
      await api.diningCartClear(session.id)
      await this.loadSession()
      toast('本桌购物车已清空')
    } catch (error) {
      showError(error)
    }
  },

  onAddDish() {
    if (!this.data.session || this.data.session.status !== 1) {
      toast('当前会话不能继续加菜')
      return
    }
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  async onSubmitOrder() {
    const session = this.data.session
    if (!session || !this.data.canSubmit || this.data.actionBusy) {
      return
    }
    const hasSubmittedOrder = this.data.hasSubmittedOrder
    const actionText = hasSubmittedOrder ? '确认加餐' : '确认下单'
    const ok = await confirm(
      hasSubmittedOrder
        ? `本次 ${this.data.cartCount} 件菜将并入当前订单 #${this.data.currentOrderNo}，不会生成新订单。是否继续？`
        : `确认提交本桌 ${this.data.cartCount} 件菜？将生成当前堂食订单。`,
      actionText,
      hasSubmittedOrder ? '加餐' : '下单',
    )
    if (!ok) {
      return
    }
    this.setData({ actionBusy: true })
    showLoading(hasSubmittedOrder ? '加餐提交中' : '下单提交中')
    try {
      await api.diningSubmit(session.id)
      hideLoading()
      await this.loadSession()
      toast(hasSubmittedOrder ? '加餐成功，已并入当前订单' : '下单成功，已生成堂食订单')
    } catch (error) {
      hideLoading()
      showError(error)
    } finally {
      this.setData({ actionBusy: false })
    }
  },

  async onRequestCheckout() {
    const session = this.data.session
    if (!session || this.data.actionBusy) {
      return
    }
    if (Number(session.totalAmount || 0) <= 0) {
      toast('本桌还没有已提交的消费')
      return
    }
    const ok = await confirm('申请结账后本桌将不能继续加菜，确定申请？', '申请结账', '申请')
    if (!ok) {
      return
    }
    this.setData({ actionBusy: true })
    try {
      await api.diningRequestCheckout(session.id)
      await this.loadSession()
      toast('已申请结账，请等待服务员处理')
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ actionBusy: false })
    }
  },

  async onEnableAa() {
    const session = this.data.session
    if (!session || this.data.actionBusy) {
      return
    }
    if (Number(session.totalAmount || 0) <= 0) {
      toast('先提交本桌订单，有消费金额后才能开启 AA')
      return
    }
    const ok = await confirm(
      '开启 AA 后本桌进入待结账状态，每位未支付成员按剩余金额平摊。',
      '开启 AA 结账',
      '开启',
    )
    if (!ok) {
      return
    }
    this.setData({ actionBusy: true })
    try {
      await api.diningEnableAa(session.id)
      await this.loadSession()
      toast('已开启 AA 结账')
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ actionBusy: false })
    }
  },

  async onAaPay() {
    const session = this.data.session
    if (!session || this.data.actionBusy) {
      return
    }
    if (!this.data.canAaPay) {
      toast('当前无需支付，或你已完成 AA 支付')
      return
    }
    const ok = await confirm(
      `本次 AA 支付约 ¥${this.data.aaShareText}，确认支付？`,
      'AA 支付',
      '支付',
    )
    if (!ok) {
      return
    }
    this.setData({ actionBusy: true })
    showLoading('AA 支付中')
    try {
      const result = await api.diningAaPay(session.id)
      hideLoading()
      await this.loadSession()
      if (result.allPaid) {
        toast('本桌已全部结清')
      } else {
        toast(`已支付 ¥${money(result.share)}，等待其他成员`)
      }
    } catch (error) {
      hideLoading()
      showError(error)
    } finally {
      this.setData({ actionBusy: false })
    }
  },

  onCopySessionNo() {
    if (this.data.session) {
      copyText(this.data.session.sessionNo, '会话号已复制')
    }
  },

  goDining() {
    wx.switchTab({ url: '/pages/dining/dining' })
  },

  onRetry() {
    void this.loadSession()
  },

  noop() {
    // 阻止底部弹层点击冒泡到遮罩
  },
})
