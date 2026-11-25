import { API_BASE, SHOP_PHONE, STORAGE_KEYS, USE_MOCK_LOGIN } from '../../config'
import { api } from '../../utils/api'
import { ensureLogin, getStoredUser, logout } from '../../utils/auth'
import { diningStatus, maskOpenid } from '../../utils/format'
import type { UserLogin } from '../../types/api'
import { confirm, copyText, showError, toast } from '../../utils/ui'

interface ProfileState {
  initialized: boolean
  loading: boolean
  shopOpen: boolean
  user: UserLogin | null
  displayName: string
  avatarText: string
  openidMasked: string
  userIdText: string
  addressCount: number
  orderCount: number
  reservationCount: number
  activeSessionId: number
  activeSessionText: string
  hasActiveSession: boolean
  loginModeText: string
  apiBase: string
  shopPhone: string
  version: string
}

Page({
  data: {
    initialized: false,
    loading: true,
    shopOpen: true,
    user: null as UserLogin | null,
    displayName: '微信顾客',
    avatarText: 'OP',
    openidMasked: '',
    userIdText: '',
    addressCount: 0,
    orderCount: 0,
    reservationCount: 0,
    activeSessionId: 0,
    activeSessionText: '',
    hasActiveSession: false,
    loginModeText: USE_MOCK_LOGIN ? '本地联调登录' : '微信登录',
    apiBase: API_BASE,
    shopPhone: SHOP_PHONE,
    version: 'v1.0.0',
  } as ProfileState,

  onLoad() {
    void this.loadProfile()
  },

  onShow() {
    if (this.data.initialized) {
      void this.loadProfile()
    }
  },

  async loadProfile() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const user = getStoredUser()
      const [addresses, orders, reservations, session, status] = await Promise.all([
        api.addresses().catch(() => []),
        api.orders(1, 1).catch(() => ({ total: 0, pages: 0, list: [] })),
        api.reservations().catch(() => []),
        api.diningMine().catch(() => null),
        api.shopStatus().catch(() => 1),
      ])
      const localNickname = (wx.getStorageSync(STORAGE_KEYS.localNickname) as string) || ''
      const displayName = localNickname && localNickname !== '我' ? localNickname : '微信顾客'
      const sessionMeta = session ? diningStatus(session.status) : null
      this.setData({
        initialized: true,
        loading: false,
        shopOpen: status === 1,
        user,
        displayName,
        avatarText: 'OP',
        openidMasked: maskOpenid(user ? user.openid : ''),
        userIdText: user ? `#${user.id}` : '',
        addressCount: addresses.length,
        orderCount: Number(orders.total || 0),
        reservationCount: reservations.filter((item) => item.status === 1 || item.status === 2).length,
        activeSessionId: session ? session.id : 0,
        activeSessionText: session ? `${session.tableName} · ${sessionMeta ? sessionMeta.label : ''}` : '',
        hasActiveSession: !!session,
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  onAddresses() {
    wx.navigateTo({ url: '/pages/address/list' })
  },

  onOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  onReservations() {
    wx.switchTab({ url: '/pages/dining/dining' })
  },

  onReserve() {
    wx.navigateTo({ url: '/pages/reservation/reservation' })
  },

  onActiveSession() {
    if (this.data.activeSessionId) {
      wx.navigateTo({ url: `/pages/dining-session/dining-session?id=${this.data.activeSessionId}` })
      return
    }
    wx.switchTab({ url: '/pages/dining/dining' })
  },

  onContact() {
    copyText(SHOP_PHONE, '门店电话已复制')
  },

  onAbout() {
    wx.showModal({
      title: 'Order Platform 顾客端',
      content: `版本 ${this.data.version}\n接口：${API_BASE}\n开发联调环境，支付与配送为模拟流程。`,
      showCancel: false,
      confirmText: '知道了',
      confirmColor: '#e8552d',
    })
  },

  async onRelogin() {
    const ok = await confirm('重新获取登录态？当前本地购物车和地址不会受影响。', '重新登录', '重新登录')
    if (!ok) {
      return
    }
    try {
      await ensureLogin(true)
      toast('登录态已刷新')
      await this.loadProfile()
    } catch (error) {
      showError(error)
    }
  },

  async onLogout() {
    const ok = await confirm('清除本地登录态？下次进入页面会自动重新登录。', '退出登录', '退出')
    if (!ok) {
      return
    }
    logout()
    wx.removeStorageSync(STORAGE_KEYS.selectedAddress)
    toast('已清除，本地将自动登录')
    setTimeout(() => wx.reLaunch({ url: '/pages/menu/menu' }), 700)
  },

  onRetry() {
    void this.loadProfile()
  },

  onPullDownRefresh() {
    void this.loadProfile().finally(() => wx.stopPullDownRefresh())
  },
})
