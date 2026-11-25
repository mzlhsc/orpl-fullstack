import { STORAGE_KEYS } from '../../config'
import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { addMinutes, addressRegion, dateTime, foodFallback, foodImage, money } from '../../utils/format'
import type { AddressBook, ShoppingCartItem } from '../../types/api'
import { showError, showLoading, toast } from '../../utils/ui'

interface ConfirmItemView extends ShoppingCartItem {
  imageSrc: string
  unitText: string
  lineText: string
  flavorText: string
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

interface InputEvent {
  detail: { value: string }
}

function buildViews(items: ShoppingCartItem[]): ConfirmItemView[] {
  return items.map((item) => ({
    ...item,
    imageSrc: foodImage(item.name, item.image),
    unitText: money(item.amount),
    lineText: money(Number(item.amount || 0) * Number(item.number || 0)),
    flavorText: item.dishFlavor ? item.dishFlavor.replace(/,/g, ' · ') : '',
  }))
}

Page({
  data: {
    initialized: false,
    loading: true,
    submitting: false,
    shopOpen: true,
    address: null as AddressBook | null,
    addressRegionText: '',
    addressLine: '',
    items: [] as ConfirmItemView[],
    count: 0,
    subtotalText: '0.00',
    totalText: '0.00',
    remark: '',
    tablewareMode: 'auto' as 'auto' | 'none',
    expectedTime: '',
  },

  onLoad() {
    void this.loadData()
  },

  onShow() {
    if (this.data.initialized) {
      this.syncSelectedAddress()
    }
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [addresses, items, status] = await Promise.all([
        api.addresses(),
        api.cart(),
        api.shopStatus().catch(() => 1),
      ])
      if (items.length === 0) {
        this.setData({ loading: false, initialized: true })
        toast('购物车为空，先去点餐吧')
        setTimeout(() => wx.navigateBack(), 800)
        return
      }
      const views = buildViews(items)
      const count = views.reduce((sum, item) => sum + Number(item.number || 0), 0)
      const subtotal = views.reduce(
        (sum, item) => sum + Number(item.amount || 0) * Number(item.number || 0),
        0,
      )
      const selected = this.resolveAddress(addresses)
      this.setData({
        initialized: true,
        loading: false,
        shopOpen: status === 1,
        address: selected,
        addressRegionText: selected ? addressRegion(selected) : '',
        addressLine: selected ? selected.detail : '',
        items: views,
        count,
        subtotalText: money(subtotal),
        totalText: money(subtotal),
        expectedTime: dateTime(addMinutes(30)),
      })
    } catch (error) {
      this.setData({ loading: false, initialized: true })
      showError(error)
    }
  },

  resolveAddress(addresses: AddressBook[]): AddressBook | null {
    const stored = wx.getStorageSync(STORAGE_KEYS.selectedAddress) as unknown
    const storedAddress = stored && typeof stored === 'object'
      ? stored as AddressBook
      : null
    if (storedAddress && storedAddress.id) {
      const matched = addresses.find((item) => item.id === storedAddress.id)
      if (matched) {
        return matched
      }
    }
    const defaultAddress = addresses.find((item) => item.isDefault)
    return defaultAddress || null
  },

  syncSelectedAddress() {
    const stored = wx.getStorageSync(STORAGE_KEYS.selectedAddress) as unknown
    if (!stored || typeof stored !== 'object') {
      return
    }
    const address = stored as AddressBook
    if (!address.id) {
      return
    }
    this.setData({
      address,
      addressRegionText: addressRegion(address),
      addressLine: address.detail,
    })
  },

  onAddressTap() {
    if (!this.data.address) {
      wx.navigateTo({ url: '/pages/address/edit' })
      return
    }
    wx.navigateTo({ url: '/pages/address/list?select=1' })
  },

  onAddAddress() {
    wx.navigateTo({ url: '/pages/address/edit' })
  },

  onRemarkInput(event: InputEvent) {
    this.setData({ remark: event.detail.value })
  },

  onTablewareTap(event: TapEvent) {
    const mode = String(event.currentTarget.dataset.mode || 'auto')
    this.setData({ tablewareMode: mode === 'none' ? 'none' : 'auto' })
  },

  onItemImageError(event: TapEvent) {
    const id = Number(event.currentTarget.dataset.id || 0)
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

  async onSubmit() {
    if (this.data.submitting) {
      return
    }
    if (!this.data.shopOpen) {
      toast('门店已打烊，暂不可下单')
      return
    }
    const address = this.data.address
    if (!address || !address.id) {
      toast('请先选择收货地址')
      return
    }
    this.setData({ submitting: true })
    showLoading('提交订单中')
    try {
      const result = await api.submitOrder({
        addressBookId: address.id,
        payMethod: 1,
        remark: this.data.remark,
        estimatedDeliveryTime: addMinutes(30),
        deliveryStatus: 1,
        tablewareNumber: 0,
        tablewareStatus: this.data.tablewareMode === 'none' ? 0 : 1,
        packAmount: 0,
        amount: Number(this.data.totalText),
      })
      wx.hideLoading()
      toast('下单成功，等待门店接单')
      wx.removeStorageSync(STORAGE_KEYS.selectedAddress)
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/order-detail/order-detail?id=${result.id}&new=1`,
        })
      }, 900)
    } catch (error) {
      wx.hideLoading()
      this.setData({ submitting: false })
      showError(error)
    }
  },
})
