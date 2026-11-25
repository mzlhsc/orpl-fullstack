import { request, toList } from './request'
import type {
  AddressBook,
  Category,
  DiningAaPayResult,
  DiningReservation,
  DiningScanResult,
  DiningSession,
  DiningTable,
  Dish,
  DishFlavor,
  Order,
  OrderSubmitPayload,
  OrderSubmitResult,
  PageResult,
  Setmeal,
  SetmealItem,
  ShoppingCartItem,
  UserLogin,
} from '../types/api'

export interface CartPayload {
  dishId?: number
  setmealId?: number
  dishFlavor?: string
}

export interface DiningOpenPayload {
  tableId: number
  guestCount: number
  nickname?: string
  remark?: string
  reservationId?: number
}

export interface DiningCartPayload {
  sessionId: number
  dishId: number
  dishFlavor?: string
  number: number
}

export interface ReservationPayload {
  tableId: number
  reserveTime: string
  guestCount: number
  contactName: string
  contactPhone: string
  remark?: string
}

function query(params: Record<string, string | number | undefined>): string {
  return Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`)
    .join('&')
}

export const api = {
  login(code: string): Promise<UserLogin> {
    return request<UserLogin>('/user/user/login', {
      method: 'POST',
      data: { code },
      auth: false,
    })
  },

  shopStatus(): Promise<number> {
    return request<number>('/user/shop/status', { auth: false })
  },

  categories(type: number): Promise<Category[]> {
    return request<Category[] | Category>(`/user/category/list?type=${type}`, { auth: false })
      .then(toList)
  },

  dishes(categoryId: number): Promise<Dish[]> {
    return request<Dish[] | Dish>(`/user/dish/list?categoryId=${categoryId}`, { auth: false })
      .then(toList)
  },

  setmeals(categoryId: number): Promise<Setmeal[]> {
    return request<Setmeal[] | Setmeal>(`/user/setmeal/list?categoryId=${categoryId}`)
      .then(toList)
  },

  setmealItems(id: number): Promise<SetmealItem[]> {
    return request<SetmealItem[] | SetmealItem>(`/user/setmeal/dish/${id}`)
      .then(toList)
  },

  cart(): Promise<ShoppingCartItem[]> {
    return request<ShoppingCartItem[] | ShoppingCartItem>('/shopping-cart/list').then(toList)
  },

  addCart(payload: CartPayload): Promise<void> {
    return request<void>('/shopping-cart/add', { method: 'POST', data: payload })
  },

  subCart(payload: CartPayload): Promise<void> {
    return request<void>('/shopping-cart/sub', { method: 'POST', data: payload })
  },

  cleanCart(): Promise<void> {
    return request<void>('/shopping-cart/clean', { method: 'DELETE' })
  },

  addresses(): Promise<AddressBook[]> {
    return request<AddressBook[] | AddressBook>('/addressBook/list').then(toList)
  },

  address(id: number): Promise<AddressBook> {
    return request<AddressBook>(`/addressBook/${id}`)
  },

  createAddress(address: AddressBook): Promise<void> {
    return request<void>('/addressBook', { method: 'POST', data: address })
  },

  updateAddress(address: AddressBook): Promise<void> {
    return request<void>('/addressBook', { method: 'PUT', data: address })
  },

  setDefaultAddress(id: number): Promise<void> {
    return request<void>('/addressBook/default', { method: 'PUT', data: { id } })
  },

  deleteAddress(id: number): Promise<void> {
    return request<void>(`/addressBook?id=${encodeURIComponent(String(id))}`, { method: 'DELETE' })
  },

  orders(page: number, pageSize: number, status?: number): Promise<PageResult<Order>> {
    const suffix = query({ page, pageSize, status })
    return request<PageResult<Order>>(`/user/order/historyOrders?${suffix}`)
  },

  orderDetail(id: number): Promise<Order> {
    return request<Order>(`/user/order/details/${id}`)
  },

  submitOrder(payload: OrderSubmitPayload): Promise<OrderSubmitResult> {
    return request<OrderSubmitResult>('/user/order/submit', { method: 'POST', data: payload })
  },

  cancelOrder(id: number, cancelReason: string): Promise<void> {
    return request<void>('/user/order/cancel', {
      method: 'PUT',
      data: { id, cancelReason },
    })
  },

  repetition(id: number): Promise<void> {
    return request<void>(`/user/order/repetition/${id}`, { method: 'POST' })
  },

  reminder(id: number): Promise<void> {
    return request<void>(`/user/order/reminder/${id}`)
  },

  diningTables(): Promise<DiningTable[]> {
    return request<DiningTable[] | DiningTable>('/user/dining/table/list', { auth: false })
      .then(toList)
  },

  diningScan(qrToken: string): Promise<DiningScanResult> {
    return request<DiningScanResult>(`/user/dining/scan/${encodeURIComponent(qrToken)}`, { auth: false })
  },

  diningOpen(payload: DiningOpenPayload): Promise<DiningSession> {
    return request<DiningSession>('/user/dining/open', { method: 'POST', data: payload })
  },

  diningJoin(sessionId: number, nickname?: string): Promise<DiningSession> {
    return request<DiningSession>('/user/dining/join', {
      method: 'POST',
      data: { sessionId, nickname },
    })
  },

  diningMine(): Promise<DiningSession | null> {
    return request<DiningSession | null>('/user/dining/session/mine')
  },

  diningDetail(sessionId: number): Promise<DiningSession> {
    return request<DiningSession>(`/user/dining/session/${sessionId}`)
  },

  diningCart(sessionId: number): Promise<ShoppingCartItem[]> {
    return request<ShoppingCartItem[] | ShoppingCartItem>(`/user/dining/cart/${sessionId}`)
      .then(toList)
  },

  diningCartAdd(payload: DiningCartPayload): Promise<void> {
    return request<void>('/user/dining/cart', { method: 'POST', data: payload })
  },

  diningCartRemove(sessionId: number, cartId: number): Promise<void> {
    return request<void>(`/user/dining/cart/${sessionId}/${cartId}`, { method: 'DELETE' })
  },

  diningCartClear(sessionId: number): Promise<void> {
    return request<void>(`/user/dining/cart/${sessionId}/clear`, { method: 'POST' })
  },

  diningSubmit(sessionId: number): Promise<OrderSubmitResult> {
    return request<OrderSubmitResult>(`/user/dining/session/${sessionId}/submit`, { method: 'POST' })
  },

  diningRequestCheckout(sessionId: number): Promise<DiningSession> {
    return request<DiningSession>(`/user/dining/session/${sessionId}/checkout-request`, { method: 'POST' })
  },

  diningEnableAa(sessionId: number): Promise<void> {
    return request<void>(`/user/dining/session/${sessionId}/aa-enable`, { method: 'POST' })
  },

  diningAaPay(sessionId: number): Promise<DiningAaPayResult> {
    return request<DiningAaPayResult>('/user/dining/aa-pay', {
      method: 'POST',
      data: { sessionId, payMethod: 1 },
    })
  },

  reservations(): Promise<DiningReservation[]> {
    return request<DiningReservation[] | DiningReservation>('/user/dining/reservation/mine')
      .then(toList)
  },

  createReservation(payload: ReservationPayload): Promise<DiningReservation> {
    return request<DiningReservation>('/user/dining/reservation', {
      method: 'POST',
      data: payload,
    })
  },

  cancelReservation(id: number): Promise<void> {
    return request<void>(`/user/dining/reservation/${id}/cancel`, { method: 'POST' })
  },
}

export type { DishFlavor }
