export interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
}

export interface Category {
  id: number
  type: number
  name: string
  sort?: number | null
  status?: number | null
}

export interface DishFlavor {
  id: number
  dishId: number
  name: string
  value: string
}

export interface Dish {
  id: number
  name: string
  categoryId: number
  price: number
  image?: string | null
  description?: string | null
  status: number
  stock: number
  categoryName?: string | null
  flavors?: DishFlavor[] | null
}

export interface Setmeal {
  id: number
  categoryId: number
  name: string
  price: number
  status: number
  description?: string | null
  image?: string | null
}

export interface SetmealItem {
  name: string
  copies: number
  image?: string | null
  description?: string | null
}

export interface ShoppingCartItem {
  id: number
  name: string
  image?: string | null
  userId?: number | null
  dishId?: number | null
  setmealId?: number | null
  dishFlavor?: string | null
  number: number
  amount: number
  diningSessionId?: number | null
  createTime?: string | null
}

export interface AddressBook {
  id?: number | null
  userId?: number | null
  consignee: string
  sex?: string | null
  phone: string
  provinceCode?: string | null
  provinceName?: string | null
  cityCode?: string | null
  cityName?: string | null
  districtCode?: string | null
  districtName?: string | null
  detail: string
  label?: string | null
  isDefault?: boolean | null
}

export interface OrderDetailItem {
  id: number
  name: string
  orderId?: number | null
  diningRound?: number | null
  dishId?: number | null
  setmealId?: number | null
  dishFlavor?: string | null
  number: number
  amount: number
  image?: string | null
}

export interface OrderRecord {
  id: number
  number: string
  status: number
  userId?: number | null
  addressBookId?: number | null
  orderTime?: string | null
  checkoutTime?: string | null
  payMethod?: number | null
  payStatus?: number | null
  amount: number
  remark?: string | null
  phone?: string | null
  address?: string | null
  userName?: string | null
  consignee?: string | null
  cancelReason?: string | null
  rejectionReason?: string | null
  cancelTime?: string | null
  estimatedDeliveryTime?: string | null
  deliveryStatus?: number | null
  deliveryTime?: string | null
  packAmount?: number | null
  tablewareNumber?: number | null
  tablewareStatus?: number | null
  orderType?: number | null
  diningSessionId?: number | null
}

export interface Order extends OrderRecord {
  orderDishes?: string | null
  orderDetailList?: OrderDetailItem[] | null
}

export interface PageResult<T> {
  total: number
  pages: number
  list: T[]
}

export interface OrderSubmitPayload {
  addressBookId: number
  payMethod: number
  remark: string
  estimatedDeliveryTime: string
  deliveryStatus: number
  tablewareNumber: number
  tablewareStatus: number
  packAmount: number
  amount: number
}

export interface OrderSubmitResult {
  id: number
  orderNumber: string
  orderAmount: number
  orderTime: string
  diningRound?: number | null
}

export interface UserLogin {
  id: number
  openid: string
  token: string
}

export interface DiningTable {
  id: number
  name: string
  seats: number
  area?: string | null
  status: number
  qrToken?: string | null
  qrContent?: string | null
  reservationEnabled?: number | null
  currentSessionId?: number | null
  currentSessionNo?: string | null
}

export interface DiningSessionMember {
  id: number
  sessionId: number
  userId: number
  nickname?: string | null
  isOwner: number
  paidAmount?: number | null
  payStatus: number
  joinTime?: string | null
}

export interface DiningOrderRound {
  round: number
  orderId: number
  orderNumber: string
  orderTime?: string | null
  amount: number
  items: OrderDetailItem[]
}

export interface DiningSession {
  id: number
  sessionNo: string
  tableId: number
  tableName: string
  status: number
  guestCount?: number | null
  ownerUserId?: number | null
  aaEnabled?: number | null
  remark?: string | null
  totalAmount?: number | null
  paidAmount?: number | null
  unpaidAmount?: number | null
  perPersonAmount?: number | null
  memberCount?: number | null
  openTime?: string | null
  checkoutTime?: string | null
  tableNames?: string[] | null
  members?: DiningSessionMember[] | null
  cartItems?: ShoppingCartItem[] | null
  orders?: OrderRecord[] | null
  orderRounds?: DiningOrderRound[] | null
  currentOrderId?: number | null
  currentOrderNo?: string | null
  cartAmount?: number | null
}

export interface DiningReservation {
  id: number
  tableId: number
  userId?: number | null
  contactName: string
  contactPhone: string
  reserveTime: string
  guestCount: number
  status: number
  remark?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface DiningScanResult {
  table: DiningTable
  session?: DiningSession | null
}

export interface DiningAaPayResult {
  share: number
  paidAmount: number
  totalAmount: number
  allPaid: boolean
  sessionStatus: number
}
