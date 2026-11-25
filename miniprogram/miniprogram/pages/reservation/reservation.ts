import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { addDays, localDate, today } from '../../utils/format'
import type { DiningTable } from '../../types/api'
import { hideLoading, showError, showLoading, toast } from '../../utils/ui'

interface ReservationTableView extends DiningTable {
  seatsText: string
  areaText: string
}

interface InputEvent {
  detail: { value: string }
}

interface PickerEvent {
  detail: { value: string }
}

function defaultDateTime(): { date: string; time: string } {
  const now = new Date()
  const date = localDate(now)
  const next = new Date(now.getTime() + 60 * 60 * 1000)
  if (now.getHours() >= 21 || next.getDate() !== now.getDate()) {
    return { date: addDays(1), time: '11:30' }
  }
  let hour = next.getHours()
  let minute = next.getMinutes() < 30 ? 30 : 0
  if (minute === 0) {
    hour += 1
  }
  if (hour >= 22) {
    return { date, time: '21:30' }
  }
  if (hour < 10) {
    return { date, time: '11:30' }
  }
  return {
    date,
    time: `${hour < 10 ? '0' : ''}${hour}:${minute === 0 ? '00' : '30'}`,
  }
}

Page({
  data: {
    loading: true,
    submitting: false,
    tables: [] as ReservationTableView[],
    tableIndex: 0,
    selectedTable: null as ReservationTableView | null,
    date: today(),
    time: '11:30',
    minDate: today(),
    maxDate: addDays(7),
    guestCount: 2,
    contactName: '',
    contactPhone: '',
    remark: '',
    phoneError: '',
  },

  onLoad() {
    void this.loadData()
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [tables, addresses] = await Promise.all([
        api.diningTables(),
        api.addresses().catch(() => []),
      ])
      const available = tables
        .filter((table) => table.status === 0 && table.reservationEnabled === 1)
        .map((table): ReservationTableView => ({
          ...table,
          seatsText: `${table.seats || 0} 座`,
          areaText: table.area || '门店',
        }))
      const defaultAddress = addresses.find((item) => item.isDefault)
      const initial = defaultDateTime()
      this.setData({
        loading: false,
        tables: available,
        tableIndex: 0,
        selectedTable: available[0] || null,
        date: initial.date,
        time: initial.time,
        guestCount: available[0] ? Math.min(2, available[0].seats || 2) : 2,
        contactName: defaultAddress ? defaultAddress.consignee : '',
        contactPhone: defaultAddress ? defaultAddress.phone : '',
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  onTableChange(event: PickerEvent) {
    const index = Number(event.detail.value || 0)
    const table = this.data.tables[index] || null
    this.setData({
      tableIndex: index,
      selectedTable: table,
      guestCount: table ? Math.min(this.data.guestCount, table.seats || 2) : 2,
    })
  },

  onDateChange(event: PickerEvent) {
    this.setData({ date: event.detail.value })
  },

  onTimeChange(event: PickerEvent) {
    this.setData({ time: event.detail.value })
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

  onContactInput(event: InputEvent) {
    this.setData({ contactName: event.detail.value })
  },

  onPhoneInput(event: InputEvent) {
    this.setData({ contactPhone: event.detail.value, phoneError: '' })
  },

  onRemarkInput(event: InputEvent) {
    this.setData({ remark: event.detail.value })
  },

  async onSubmit() {
    if (this.data.submitting) {
      return
    }
    const table = this.data.selectedTable
    if (!table) {
      toast('暂无可预约桌台')
      return
    }
    if (!this.data.contactName.trim()) {
      toast('请填写联系人')
      return
    }
    if (!/^1\d{10}$/.test(this.data.contactPhone)) {
      this.setData({ phoneError: '请输入 11 位手机号' })
      return
    }
    this.setData({ submitting: true })
    showLoading('提交预约中')
    try {
      await api.createReservation({
        tableId: table.id,
        reserveTime: `${this.data.date} ${this.data.time}:00`,
        guestCount: this.data.guestCount,
        contactName: this.data.contactName.trim(),
        contactPhone: this.data.contactPhone,
        remark: this.data.remark.trim(),
      })
      hideLoading()
      toast('预约已提交，等待门店确认')
      setTimeout(() => {
        wx.navigateBack({
          fail: () => wx.switchTab({ url: '/pages/dining/dining' }),
        })
      }, 700)
    } catch (error) {
      hideLoading()
      this.setData({ submitting: false })
      showError(error)
    }
  },

  onCancel() {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: '/pages/dining/dining' }),
    })
  },

  retry() {
    void this.loadData()
  },

  noop() {
    // 阻止点击冒泡
  },
})
