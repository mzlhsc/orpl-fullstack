import { FOOD_PLACEHOLDER } from '../config'
import type { AddressBook } from '../types/api'

const FOOD_FALLBACKS: Array<{ pattern: RegExp; image: string }> = [
  { pattern: /可乐|王老吉|雪碧|饮品|饮料|茶|汁|酒|咖啡|奶茶/, image: '/assets/food/drinks.jpg' },
  { pattern: /水饺|饺子|馄饨|包子|面条|面/, image: '/assets/food/dumplings.jpg' },
  { pattern: /米饭|盖饭|炒饭|饭/, image: '/assets/food/rice.jpg' },
  { pattern: /套餐|组合|双人|单人/, image: '/assets/food/setmeal.jpg' },
  { pattern: /鸡|牛|羊|猪|肉|鱼|虾|豆腐|菜|汤|锅/, image: '/assets/food/chicken.jpg' },
]

export function foodFallback(name: string): string {
  const match = FOOD_FALLBACKS.find((item) => item.pattern.test(name))
  return match ? match.image : FOOD_PLACEHOLDER
}

export function foodImage(name: string, remote?: string | null): string {
  if (!remote || remote.indexOf('sky-take-out-demo.oss-cn-hangzhou.aliyuncs.com') >= 0) {
    return foodFallback(name)
  }
  return remote
}

export function money(value: number | string | null | undefined): string {
  const n = typeof value === 'number' ? value : Number(value || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

export function dateTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 16)
}

export function dateOnly(value?: string | null): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 10)
}

export function timeOnly(value?: string | null): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(11, 16)
}

function pad2(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

export function localDateTime(date: Date): string {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
}

export function localDate(date: Date): string {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

export function addMinutes(minutes: number): string {
  return localDateTime(new Date(Date.now() + minutes * 60 * 1000))
}

export function addDays(days: number): string {
  return localDate(new Date(Date.now() + days * 24 * 60 * 60 * 1000))
}

export function today(): string {
  return localDate(new Date())
}

export interface StatusMeta {
  label: string
  tone: string
}

export function orderStatus(status: number): StatusMeta {
  switch (status) {
    case 1:
      return { label: '待付款', tone: 'warm' }
    case 2:
      return { label: '待接单', tone: 'warm' }
    case 3:
      return { label: '已接单', tone: 'active' }
    case 4:
      // 兼容迁移前的历史状态，界面统一显示为已接单
      return { label: '已接单', tone: 'active' }
    case 5:
      return { label: '已完成', tone: 'done' }
    case 6:
      return { label: '已取消', tone: 'muted' }
    case 7:
      return { label: '退款', tone: 'muted' }
    default:
      return { label: '处理中', tone: 'muted' }
  }
}

export function orderCanCancel(status: number): boolean {
  return status !== 5 && status !== 6 && status !== 7
}

export function orderCanRemind(status: number): boolean {
  return status === 3
}

export function orderCanRepeat(status: number): boolean {
  return status === 5 || status === 6 || status === 7
}

export function diningStatus(status: number): StatusMeta {
  switch (status) {
    case 1:
      return { label: '就餐中', tone: 'active' }
    case 2:
      return { label: '待结账', tone: 'warm' }
    case 3:
      return { label: '已结账', tone: 'done' }
    case 4:
      return { label: '已取消', tone: 'muted' }
    default:
      return { label: '处理中', tone: 'muted' }
  }
}

export function tableStatus(status: number): StatusMeta {
  switch (status) {
    case 0:
      return { label: '空闲', tone: 'done' }
    case 1:
      return { label: '就餐中', tone: 'active' }
    case 2:
      return { label: '待结账', tone: 'warm' }
    default:
      return { label: '未知', tone: 'muted' }
  }
}

export function reservationStatus(status: number): StatusMeta {
  switch (status) {
    case 1:
      return { label: '待确认', tone: 'warm' }
    case 2:
      return { label: '已确认', tone: 'active' }
    case 3:
      return { label: '已到店', tone: 'done' }
    case 4:
      return { label: '已取消', tone: 'muted' }
    case 5:
      return { label: '已过期', tone: 'muted' }
    default:
      return { label: '预约', tone: 'muted' }
  }
}

export function addressRegion(address: AddressBook): string {
  const names = [address.provinceName, address.cityName, address.districtName]
    .filter((item): item is string => !!item && item.length > 0)
  if (names.length > 0) {
    return names.join(' ')
  }
  return ''
}

export function maskOpenid(value?: string | null): string {
  if (!value) {
    return ''
  }
  if (value.length <= 8) {
    return value
  }
  return `${value.slice(0, 4)}…${value.slice(-4)}`
}
