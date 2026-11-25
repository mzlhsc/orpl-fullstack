import { STORAGE_KEYS } from '../../config'
import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import {
  dateTime,
  diningStatus,
  money,
  reservationStatus,
  tableStatus,
} from '../../utils/format'
import type { DiningReservation, DiningSession, DiningTable } from '../../types/api'
import { confirm, hideLoading, showError, showLoading, toast } from '../../utils/ui'

interface TableView extends DiningTable {
  statusText: string
  statusTone: string
  actionText: string
  canOpen: boolean
  canJoin: boolean
  seatsText: string
}

interface SessionView extends DiningSession {
  statusText: string
  statusTone: string
  totalText: string
  cartAmountText: string
  openTimeText: string
}

interface ReservationView extends DiningReservation {
  statusText: string
  statusTone: string
  timeText: string
  tableName: string
  canCancel: boolean
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

interface InputEvent {
  detail: { value: string }
}

function numberData(event: TapEvent, key: string): number {
  return Number(event.currentTarget.dataset[key] || 0)
}

function parseQrToken(raw: string): string {
  let token = raw.trim()
  try {
    token = decodeURIComponent(token)
  } catch (error) {
    // Keep the original text when it contains an invalid percent escape.
  }
  if (token.startsWith('DINING_TABLE:')) {
    token = token.slice('DINING_TABLE:'.length)
  } else if (token.indexOf('qrToken=') >= 0) {
    token = token.split('qrToken=')[1].split('&')[0]
  } else if (token.indexOf('?') >= 0) {
    token = token.split('?')[1]
  }
  return decodeURIComponent(token || '')
}

function launchQrToken(options: Record<string, string | undefined>): string {
  const candidates = [options.qrToken, options.scene, options.q]
  for (let index = 0; index < candidates.length; index += 1) {
    const token = parseQrToken(candidates[index] || '')
    if (token) {
      return token
    }
  }
  return ''
}

function isToday(value: string): boolean {
  const date = (value || '').slice(0, 10)
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return date === `${now.getFullYear()}-${month}-${day}`
}

function tableView(table: DiningTable): TableView {
  const meta = tableStatus(table.status)
  const canOpen = table.status === 0
  const canJoin = !!table.currentSessionId && table.status !== 0
  return {
    ...table,
    statusText: meta.label,
    statusTone: meta.tone,
    actionText: canOpen ? '开台' : (canJoin ? '加入本桌' : '使用中'),
    canOpen,
    canJoin,
    seatsText: `${table.seats || 0} 座`,
  }
}

function sessionView(session: DiningSession): SessionView {
  const meta = diningStatus(session.status)
  return {
    ...session,
    statusText: meta.label,
    statusTone: meta.tone,
    totalText: money(session.totalAmount),
    cartAmountText: money(session.cartAmount),
    openTimeText: dateTime(session.openTime),
  }
}

function reservationView(
  reservation: DiningReservation,
  tableNames: Record<number, string>,
): ReservationView {
  const meta = reservationStatus(reservation.status)
  return {
    ...reservation,
    statusText: meta.label,
    statusTone: meta.tone,
    timeText: dateTime(reservation.reserveTime),
    tableName: tableNames[reservation.tableId] || `桌台 ${reservation.tableId}`,
    canCancel: reservation.status === 1 || reservation.status === 2,
  }
}

Page({
  data: {
    initialized: false,
    loading: true,
    error: '',
    shopOpen: true,
    activeSession: null as SessionView | null,
    reservations: [] as ReservationView[],
    showOpen: false,
    selectedTable: null as TableView | null,
    guestCount: 2,
    nickname: '',
    openRemark: '',
    openSubmitting: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    const qrToken = launchQrToken(options || {})
    void this.refreshAll().then(() => {
      if (qrToken) {
        void this.handleQrToken(qrToken)
      }
    })
  },

  onShow() {
    if (this.data.initialized) {
      void this.refreshAll()
    }
  },

  onPullDownRefresh() {
    void this.refreshAll().finally(() => wx.stopPullDownRefresh())
  },

  async refreshAll() {
    this.setData({ loading: true, error: '' })
    try {
      await ensureLogin()
      const [status, session, tables, reservations] = await Promise.all([
        api.shopStatus().catch(() => 1),
        api.diningMine().catch(() => null),
        api.diningTables().catch(() => [] as DiningTable[]),
        api.reservations().catch(() => [] as DiningReservation[]),
      ])
      const tableNames: Record<number, string> = {}
      tables.forEach((table) => {
        tableNames[table.id] = table.name
      })
      const currentReservations = reservations.filter((item) => {
        if (item.status === 1 || item.status === 2) {
          return true
        }
        if (item.status === 3) {
          const activeTableId = session ? session.tableId : 0
          return isToday(item.reserveTime) && item.tableId !== activeTableId
        }
        return false
      })
      this.setData({
        initialized: true,
        loading: false,
        shopOpen: status === 1,
        activeSession: session ? sessionView(session) : null,
        reservations: currentReservations.map((item) => reservationView(item, tableNames)),
      })
    } catch (error) {
      this.setData({
        initialized: true,
        loading: false,
        error: error instanceof Error ? error.message : '堂食信息加载失败',
      })
    }
  },

  onScan() {
    wx.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        const token = parseQrToken(res.result || '')
        if (!token) {
          toast('没有识别到桌台码')
          return
        }
        void this.handleQrToken(token)
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('cancel') < 0) {
          toast('扫码未完成，请重新扫描本桌桌贴二维码')
        }
      },
    })
  },

  async handleQrToken(token: string) {
    showLoading('识别桌台')
    try {
      const result = await api.diningScan(token)
      hideLoading()
      const table = result.table
      const sessionId = result.session && result.session.id ? result.session.id : table.currentSessionId
      if (sessionId) {
        const ok = await confirm(
          `${table.name} 正在就餐，加入本桌后可以和同桌一起点菜。`,
          '加入堂食',
          '加入',
        )
        if (ok) {
          await this.joinAndEnter(sessionId)
        }
        return
      }
      if (table.status === 0) {
        this.openTable(table)
        return
      }
      toast('该桌台暂不可加入，请联系服务员')
    } catch (error) {
      hideLoading()
      showError(error)
    }
  },

  openTable(table: DiningTable) {
    const nickname = (wx.getStorageSync(STORAGE_KEYS.localNickname) as string) || '我'
    this.setData({
      showOpen: true,
      selectedTable: tableView(table),
      guestCount: Math.min(2, table.seats || 2),
      nickname,
      openRemark: '',
    })
  },

  onCloseOpen() {
    if (this.data.openSubmitting) {
      return
    }
    this.setData({ showOpen: false, selectedTable: null })
  },

  onGuestMinus() {
    if (this.data.guestCount > 1) {
      this.setData({ guestCount: this.data.guestCount - 1 })
    }
  },

  onGuestPlus() {
    const max = this.data.selectedTable ? this.data.selectedTable.seats : 2
    if (this.data.guestCount < max) {
      this.setData({ guestCount: this.data.guestCount + 1 })
    } else {
      toast('已超过桌台座位数')
    }
  },

  onNicknameInput(event: InputEvent) {
    this.setData({ nickname: event.detail.value })
  },

  onOpenRemarkInput(event: InputEvent) {
    this.setData({ openRemark: event.detail.value })
  },

  async onOpenConfirm() {
    const table = this.data.selectedTable
    if (!table || this.data.openSubmitting) {
      return
    }
    this.setData({ openSubmitting: true })
    try {
      await api.diningOpen({
        tableId: table.id,
        guestCount: this.data.guestCount,
        nickname: this.data.nickname.trim() || '我',
        remark: this.data.openRemark.trim(),
      })
      wx.setStorageSync(STORAGE_KEYS.localNickname, this.data.nickname.trim() || '我')
      this.setData({ openSubmitting: false, showOpen: false })
      await this.refreshAll()
      this.enterOrderMenu()
    } catch (error) {
      this.setData({ openSubmitting: false })
      showError(error)
    }
  },

  async joinAndEnter(sessionId: number) {
    try {
      await api.diningJoin(
        sessionId,
        (wx.getStorageSync(STORAGE_KEYS.localNickname) as string) || '我',
      )
      await this.refreshAll()
      this.enterOrderMenu()
    } catch (error) {
      showError(error)
    }
  },

  enterOrderMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  navigateSession(sessionId: number) {
    wx.navigateTo({ url: `/pages/dining-session/dining-session?id=${sessionId}` })
  },

  onSessionTap() {
    if (this.data.activeSession) {
      this.navigateSession(this.data.activeSession.id)
    }
  },

  onContinueOrder() {
    if (this.data.activeSession && this.data.activeSession.status !== 1) {
      toast('本桌已申请结账，暂不能继续加菜')
      return
    }
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  onReserve() {
    wx.navigateTo({ url: '/pages/reservation/reservation' })
  },

  async onCancelReservation(event: TapEvent) {
    const id = numberData(event, 'id')
    const ok = await confirm('取消这条桌台预约？', '取消预约', '取消预约')
    if (!ok) {
      return
    }
    try {
      await api.cancelReservation(id)
      toast('预约已取消')
      await this.refreshAll()
    } catch (error) {
      showError(error)
    }
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  onRetry() {
    void this.refreshAll()
  },

  noop() {
    // 阻止底部弹层点击冒泡到遮罩
  },
})
