import { MOCK_LOGIN_CODE, STORAGE_KEYS, USE_MOCK_LOGIN } from '../config'
import { api } from './api'
import type { UserLogin } from '../types/api'

let pendingLogin: Promise<UserLogin> | null = null

function isUserLogin(value: unknown): value is UserLogin {
  if (!value || typeof value !== 'object') {
    return false
  }
  const record = value as Record<string, unknown>
  return typeof record.id === 'number'
    && typeof record.openid === 'string'
    && typeof record.token === 'string'
    && record.token.length > 0
}

export function getStoredUser(): UserLogin | null {
  const value = wx.getStorageSync(STORAGE_KEYS.user) as unknown
  return isUserLogin(value) ? value : null
}

export function hasToken(): boolean {
  const token = wx.getStorageSync(STORAGE_KEYS.token) as string | undefined
  return typeof token === 'string' && token.length > 0
}

function getLoginCode(): Promise<string> {
  if (USE_MOCK_LOGIN) {
    return Promise.resolve(MOCK_LOGIN_CODE)
  }
  return new Promise<string>((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error('微信登录失败，未获取到 code'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '微信登录失败'))
      },
    })
  })
}

async function login(): Promise<UserLogin> {
  const code = await getLoginCode()
  const user = await api.login(code)
  wx.setStorageSync(STORAGE_KEYS.token, user.token)
  wx.setStorageSync(STORAGE_KEYS.user, user)
  return user
}

export function ensureLogin(force = false): Promise<UserLogin> {
  const user = getStoredUser()
  if (!force && user && hasToken()) {
    return Promise.resolve(user)
  }
  if (pendingLogin) {
    return pendingLogin
  }

  pendingLogin = login().finally(() => {
    pendingLogin = null
  })
  return pendingLogin
}

export function logout(): void {
  wx.removeStorageSync(STORAGE_KEYS.token)
  wx.removeStorageSync(STORAGE_KEYS.user)
  pendingLogin = null
}
