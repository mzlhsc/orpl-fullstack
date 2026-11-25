export function toast(title: string): void {
  wx.showToast({
    title: title.length > 24 ? `${title.slice(0, 24)}…` : title,
    icon: 'none',
    duration: 2200,
  })
}

export function showError(error: unknown): void {
  const message = error instanceof Error ? error.message : '操作失败，请稍后重试'
  toast(message)
}

export function confirm(content: string, title = '提示', confirmText = '确定'): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    wx.showModal({
      title,
      content,
      confirmText,
      confirmColor: '#e8552d',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    })
  })
}

export function showLoading(title: string): void {
  wx.showLoading({ title, mask: true })
}

export function hideLoading(): void {
  wx.hideLoading()
}

export function copyText(text: string, tip = '已复制'): void {
  wx.setClipboardData({
    data: text,
    success: () => toast(tip),
    fail: () => toast('复制失败'),
  })
}
