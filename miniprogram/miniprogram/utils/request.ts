import { API_BASE, STORAGE_KEYS } from '../config'
import type { ApiEnvelope } from '../types/api'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  method?: HttpMethod
  data?: unknown
  auth?: boolean
}

export class ApiError extends Error {
  code: number

  constructor(message: string, code = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

function isEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  return !!value
    && typeof value === 'object'
    && 'code' in value
    && 'msg' in value
}

function clearLoginState(): void {
  wx.removeStorageSync(STORAGE_KEYS.token)
  wx.removeStorageSync(STORAGE_KEYS.user)
}

function stringifyBody(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(value)
  } catch (error) {
    return String(value)
  }
}

export function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method || 'GET'
  const auth = options.auth !== false
  const token = wx.getStorageSync(STORAGE_KEYS.token) as string | undefined

  return new Promise<T>((resolve, reject) => {
    wx.request({
      url: `${API_BASE}${url}`,
      method,
      data: options.data as WechatMiniprogram.IAnyObject | undefined,
      timeout: 20000,
      header: {
        'content-type': 'application/json',
        ...(auth && token ? { authorization: token } : {}),
      },
      success: (res) => {
        const statusCode = res.statusCode
        const body = res.data as unknown

        if (isEnvelope(body)) {
          if (body.code === 200) {
            resolve(body.data as T)
            return
          }
          if (body.code === 401) {
            clearLoginState()
          }
          reject(new ApiError(body.msg || '请求失败', body.code))
          return
        }

        if (statusCode >= 200 && statusCode < 300) {
          resolve(body as T)
          return
        }

        if (statusCode === 401) {
          clearLoginState()
          reject(new ApiError('登录已失效，请重新进入小程序', 401))
          return
        }

        reject(new ApiError(`网络请求失败（${statusCode}）`, statusCode))
      },
      fail: (err) => {
        const raw = err.errMsg || 'request:fail'
        console.warn('[request]', url, raw)
        const message = raw.indexOf('timeout') >= 0
          ? '请求超时，请确认后端服务已启动'
          : raw.indexOf('domain') >= 0
            ? '请求域名未配置，请在开发者工具关闭合法域名校验'
            : `无法连接后端服务（${raw}）`
        reject(new ApiError(message, -1))
      },
    })
  })
}

export function toList<T>(value: T[] | T | null | undefined): T[] {
  if (Array.isArray(value)) {
    return value
  }
  if (value === null || value === undefined) {
    return []
  }
  return [value]
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return stringifyBody(error)
}
