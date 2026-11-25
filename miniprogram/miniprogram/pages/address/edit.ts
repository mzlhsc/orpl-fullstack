import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { addressRegion } from '../../utils/format'
import type { AddressBook } from '../../types/api'
import { hideLoading, showError, showLoading, toast } from '../../utils/ui'

interface InputEvent {
  detail: { value: string }
}

interface RegionEvent {
  detail: {
    value?: string[]
    code?: string[]
  }
}

interface TapEvent {
  currentTarget: {
    dataset: Record<string, string | number | undefined>
  }
}

Page({
  data: {
    id: 0,
    editing: false,
    loading: false,
    saving: false,
    consignee: '',
    phone: '',
    region: ['', '', ''] as string[],
    regionText: '',
    provinceCode: '',
    provinceName: '',
    cityCode: '',
    cityName: '',
    districtCode: '',
    districtName: '',
    detail: '',
    label: '家',
    labels: ['家', '公司', '学校', '其他'],
    phoneError: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    const id = Number(options.id || 0)
    if (!id) {
      wx.setNavigationBarTitle({ title: '新增收货地址' })
      return
    }
    wx.setNavigationBarTitle({ title: '编辑收货地址' })
    this.setData({ id, editing: true })
    void this.loadAddress(id)
  },

  async loadAddress(id: number) {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const address = await api.address(id)
      const region = [
        address.provinceName || '',
        address.cityName || '',
        address.districtName || '',
      ]
      this.setData({
        loading: false,
        consignee: address.consignee || '',
        phone: address.phone || '',
        region,
        regionText: addressRegion(address),
        provinceCode: address.provinceCode || '',
        provinceName: address.provinceName || '',
        cityCode: address.cityCode || '',
        cityName: address.cityName || '',
        districtCode: address.districtCode || '',
        districtName: address.districtName || '',
        detail: address.detail || '',
        label: address.label || '家',
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  onConsigneeInput(event: InputEvent) {
    this.setData({ consignee: event.detail.value })
  },

  onPhoneInput(event: InputEvent) {
    this.setData({ phone: event.detail.value, phoneError: '' })
  },

  onDetailInput(event: InputEvent) {
    this.setData({ detail: event.detail.value })
  },

  onRegionChange(event: RegionEvent) {
    const names = event.detail.value || []
    const codes = event.detail.code || []
    this.setData({
      region: names,
      regionText: names.filter((name) => !!name).join(' '),
      provinceName: names[0] || '',
      cityName: names[1] || '',
      districtName: names[2] || '',
      provinceCode: codes[0] || '',
      cityCode: codes[1] || '',
      districtCode: codes[2] || '',
    })
  },

  onLabelTap(event: TapEvent) {
    this.setData({ label: String(event.currentTarget.dataset.label || '家') })
  },

  async onSave() {
    if (this.data.saving) {
      return
    }
    if (!this.data.consignee.trim()) {
      toast('请填写收货人')
      return
    }
    if (!/^1\d{10}$/.test(this.data.phone)) {
      this.setData({ phoneError: '请输入 11 位手机号' })
      return
    }
    if (!this.data.detail.trim()) {
      toast('请填写详细地址')
      return
    }
    const payload: AddressBook = {
      id: this.data.editing ? this.data.id : null,
      consignee: this.data.consignee.trim(),
      phone: this.data.phone,
      sex: '',
      provinceCode: this.data.provinceCode,
      provinceName: this.data.provinceName,
      cityCode: this.data.cityCode,
      cityName: this.data.cityName,
      districtCode: this.data.districtCode,
      districtName: this.data.districtName,
      detail: this.data.detail.trim(),
      label: this.data.label,
    }
    this.setData({ saving: true })
    showLoading('保存地址中')
    try {
      if (this.data.editing) {
        await api.updateAddress(payload)
      } else {
        await api.createAddress(payload)
      }
      hideLoading()
      toast('地址已保存')
      setTimeout(() => {
        wx.navigateBack({
          fail: () => wx.switchTab({ url: '/pages/profile/profile' }),
        })
      }, 600)
    } catch (error) {
      hideLoading()
      this.setData({ saving: false })
      showError(error)
    }
  },
})
