import { STORAGE_KEYS } from '../../config'
import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { addressRegion } from '../../utils/format'
import type { AddressBook } from '../../types/api'
import { confirm, showError, toast } from '../../utils/ui'

interface AddressView extends AddressBook {
  regionText: string
  fullAddress: string
  labelText: string
  defaultText: string
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

function numberData(event: TapEvent, key: string): number {
  return Number(event.currentTarget.dataset[key] || 0)
}

function toView(address: AddressBook): AddressView {
  const regionText = addressRegion(address)
  return {
    ...address,
    regionText,
    fullAddress: [regionText, address.detail].filter((item) => !!item).join(' '),
    labelText: address.label || '地址',
    defaultText: address.isDefault ? '默认' : '',
  }
}

Page({
  data: {
    initialized: false,
    loading: true,
    error: '',
    selectMode: false,
    addresses: [] as AddressView[],
  },

  onLoad(options: Record<string, string | undefined>) {
    this.setData({ selectMode: options.select === '1' })
    void this.loadAddresses()
  },

  onShow() {
    if (this.data.initialized) {
      void this.loadAddresses()
    }
  },

  async loadAddresses() {
    this.setData({ loading: true, error: '' })
    try {
      await ensureLogin()
      const list = await api.addresses()
      this.setData({
        initialized: true,
        loading: false,
        addresses: list.map(toView),
      })
    } catch (error) {
      this.setData({
        initialized: true,
        loading: false,
        error: error instanceof Error ? error.message : '地址加载失败',
      })
    }
  },

  onAddressTap(event: TapEvent) {
    const id = numberData(event, 'id')
    const address = this.data.addresses.find((item) => item.id === id)
    if (!address) {
      return
    }
    if (this.data.selectMode) {
      wx.setStorageSync(STORAGE_KEYS.selectedAddress, address)
      toast('已选择收货地址')
      setTimeout(() => wx.navigateBack(), 400)
      return
    }
    wx.navigateTo({ url: `/pages/address/edit?id=${id}` })
  },

  onSetDefault(event: TapEvent) {
    const id = numberData(event, 'id')
    if (!id) {
      return
    }
    void this.setDefault(id)
  },

  async setDefault(id: number) {
    try {
      await api.setDefaultAddress(id)
      toast('已设为默认地址')
      await this.loadAddresses()
    } catch (error) {
      showError(error)
    }
  },

  onEdit(event: TapEvent) {
    const id = numberData(event, 'id')
    if (id) {
      wx.navigateTo({ url: `/pages/address/edit?id=${id}` })
    }
  },

  async onDelete(event: TapEvent) {
    const id = numberData(event, 'id')
    if (!id) {
      return
    }
    const ok = await confirm('删除这个收货地址？', '删除地址', '删除')
    if (!ok) {
      return
    }
    try {
      await api.deleteAddress(id)
      const selected = wx.getStorageSync(STORAGE_KEYS.selectedAddress) as AddressBook | undefined
      if (selected && selected.id === id) {
        wx.removeStorageSync(STORAGE_KEYS.selectedAddress)
      }
      toast('地址已删除')
      await this.loadAddresses()
    } catch (error) {
      showError(error)
    }
  },

  onAdd() {
    wx.navigateTo({ url: '/pages/address/edit' })
  },

  onRetry() {
    void this.loadAddresses()
  },
})
