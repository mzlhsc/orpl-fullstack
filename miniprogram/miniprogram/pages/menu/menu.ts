import { api } from '../../utils/api'
import { ensureLogin } from '../../utils/auth'
import { diningStatus, foodFallback, foodImage, money } from '../../utils/format'
import type {
  Category,
  DiningSession,
  Dish,
  DishFlavor,
  Setmeal,
  SetmealItem,
  ShoppingCartItem,
} from '../../types/api'
import { confirm, showError, toast } from '../../utils/ui'

interface DishView extends Dish {
  priceText: string
  imageSrc: string
  soldOut: boolean
  flavorText: string
  actionText: string
}

interface SetmealView extends Setmeal {
  priceText: string
  imageSrc: string
  itemSummary: string
}

interface FlavorGroup {
  name: string
  options: string[]
  selected: string
}

interface SetmealItemView extends SetmealItem {
  imageSrc: string
}

interface CartItemView extends ShoppingCartItem {
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

function numberData(event: TapEvent, key: string): number {
  return Number(event.currentTarget.dataset[key] || 0)
}

function stringData(event: TapEvent, key: string): string {
  return String(event.currentTarget.dataset[key] || '')
}

function flavorGroupsOf(dish: Dish): FlavorGroup[] {
  return (dish.flavors || []).map((flavor: DishFlavor) => {
    const options = (flavor.value || '')
      .split(',')
      .map((value) => value.trim())
      .filter((value) => value.length > 0)
    return {
      name: flavor.name,
      options,
      selected: options[0] || '',
    }
  }).filter((group) => group.options.length > 0)
}

function buildCartViews(items: ShoppingCartItem[]): CartItemView[] {
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
    loadError: '',
    shopOpen: true,
    activeType: 1,
    categories: [] as Category[],
    activeCategoryId: 0,
    allDishes: [] as DishView[],
    allSetmeals: [] as SetmealView[],
    dishes: [] as DishView[],
    setmeals: [] as SetmealView[],
    searchKeyword: '',
    activeSession: null as DiningSession | null,
    canOrder: true,
    hasSubmittedOrder: false,
    cartCount: 0,
    cartTotal: '0.00',
    cartItems: [] as CartItemView[],
    cartButtonText: '去结算',
    sessionStatusText: '',
    sessionTableName: '',
    sessionTotalText: '0.00',
    sessionMemberCount: 0,
    showCartSheet: false,
    sheetBusyId: 0,
    showFlavor: false,
    flavorDish: null as DishView | null,
    flavorImage: '',
    flavorName: '',
    flavorPrice: '0.00',
    flavorGroups: [] as FlavorGroup[],
    showSetmeal: false,
    setmealDetail: null as SetmealView | null,
    setmealItems: [] as SetmealItemView[],
  },

  onLoad() {
    void this.bootstrap()
  },

  onShow() {
    if (this.data.initialized) {
      void this.refreshContext()
    }
  },

  onPullDownRefresh() {
    void this.refreshPage()
  },

  async bootstrap() {
    this.setData({ loading: true, loadError: '' })
    try {
      await ensureLogin()
      await this.refreshContext()
      await this.loadMenu(this.data.activeType)
      this.setData({ initialized: true })
    } catch (error) {
      this.setData({ loadError: error instanceof Error ? error.message : '菜单加载失败' })
    } finally {
      this.setData({ loading: false, initialized: true })
    }
  },

  async refreshPage() {
    try {
      await ensureLogin()
      await this.refreshContext()
      await this.loadMenu(this.data.activeType)
    } catch (error) {
      showError(error)
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  async refreshContext() {
    try {
      const [status, session] = await Promise.all([
        api.shopStatus().catch(() => 1),
        api.diningMine().catch(() => null),
      ])
      const open = status === 1
      const sessionMeta = session ? diningStatus(session.status) : null
      const ordering = open && (!session || session.status === 1)
      const hasSubmittedOrder = !!session && (
        (session.orderRounds || []).length > 0
        || (session.orders || []).some((order) => order.status !== 6)
      )
      let cartButtonText = '去结算'
      if (session) {
        if (!ordering) {
          cartButtonText = '已申请结账'
        } else {
          cartButtonText = hasSubmittedOrder ? '加餐' : '下单'
        }
      }
      this.setData({
        shopOpen: open,
        activeSession: session,
        canOrder: ordering,
        hasSubmittedOrder,
        sessionStatusText: sessionMeta ? sessionMeta.label : '',
        sessionTableName: session ? session.tableName : '',
        sessionTotalText: money(session ? session.totalAmount : 0),
        sessionMemberCount: session ? (session.memberCount || 0) : 0,
        cartButtonText,
      })
      await this.refreshCart()
    } catch (error) {
      showError(error)
    }
  },

  async refreshCart() {
    try {
      const session = this.data.activeSession
      const items = session
        ? await api.diningCart(session.id)
        : await api.cart()
      const cartItems = buildCartViews(items)
      const count = cartItems.reduce((sum, item) => sum + (item.number || 0), 0)
      const total = cartItems.reduce(
        (sum, item) => sum + Number(item.amount || 0) * Number(item.number || 0),
        0,
      )
      this.setData({
        cartItems,
        cartCount: count,
        cartTotal: money(total),
        showCartSheet: this.data.showCartSheet && count > 0,
      })
    } catch (error) {
      this.setData({
        cartItems: [],
        cartCount: 0,
        cartTotal: '0.00',
        showCartSheet: false,
      })
    }
  },

  async loadMenu(type: number) {
    this.setData({ loading: true, loadError: '', activeType: type })
    try {
      const categories = await api.categories(type)
      if (type === 1) {
        const lists = await Promise.all(
          categories.map((category) => api.dishes(category.id).catch(() => [] as Dish[])),
        )
        const dishes = lists
          .reduce((all, list) => all.concat(list), [] as Dish[])
          .map((dish): DishView => ({
            ...dish,
            priceText: money(dish.price),
            imageSrc: foodImage(dish.name, dish.image),
            soldOut: dish.status !== 1 || Number(dish.stock || 0) <= 0,
            flavorText: dish.flavors && dish.flavors.length > 0
              ? `可选口味 · ${dish.flavors.length} 组`
              : '',
            actionText: flavorGroupsOf(dish).length > 0 ? '选规格' : '加入',
          }))
        this.setData({
          categories,
          activeCategoryId: 0,
          allDishes: dishes,
          allSetmeals: [],
          dishes: [],
          setmeals: [],
        })
      } else {
        const lists = await Promise.all(
          categories.map((category) => api.setmeals(category.id).catch(() => [] as Setmeal[])),
        )
        const setmeals = lists
          .reduce((all, list) => all.concat(list), [] as Setmeal[])
          .map((setmeal): SetmealView => ({
            ...setmeal,
            priceText: money(setmeal.price),
            imageSrc: foodImage(setmeal.name, setmeal.image),
            itemSummary: setmeal.description || '套餐组合，搭配好味',
          }))
        this.setData({
          categories,
          activeCategoryId: 0,
          allSetmeals: setmeals,
          allDishes: [],
          dishes: [],
          setmeals: [],
        })
      }
      this.applyFilter()
    } catch (error) {
      this.setData({
        loadError: error instanceof Error ? error.message : '菜单加载失败',
        dishes: [],
        setmeals: [],
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  applyFilter() {
    const keyword = this.data.searchKeyword.trim().toLowerCase()
    const categoryId = this.data.activeCategoryId
    if (this.data.activeType === 1) {
      const dishes = this.data.allDishes.filter((dish) => {
        const inCategory = categoryId === 0 || dish.categoryId === categoryId
        const matchKeyword = !keyword
          || dish.name.toLowerCase().indexOf(keyword) >= 0
          || (dish.description || '').toLowerCase().indexOf(keyword) >= 0
        return inCategory && matchKeyword
      })
      this.setData({ dishes })
    } else {
      const setmeals = this.data.allSetmeals.filter((setmeal) => {
        const inCategory = categoryId === 0 || setmeal.categoryId === categoryId
        const matchKeyword = !keyword
          || setmeal.name.toLowerCase().indexOf(keyword) >= 0
          || (setmeal.description || '').toLowerCase().indexOf(keyword) >= 0
        return inCategory && matchKeyword
      })
      this.setData({ setmeals })
    }
  },

  onTypeChange(event: TapEvent) {
    const type = numberData(event, 'type')
    if (type === this.data.activeType || this.data.loading) {
      return
    }
    void this.loadMenu(type)
  },

  onCategoryTap(event: TapEvent) {
    this.setData({ activeCategoryId: numberData(event, 'id') })
    this.applyFilter()
  },

  onSearchInput(event: InputEvent) {
    this.setData({ searchKeyword: event.detail.value })
    this.applyFilter()
  },

  onClearSearch() {
    this.setData({ searchKeyword: '' })
    this.applyFilter()
  },

  onDishImageError(event: TapEvent) {
    const id = numberData(event, 'id')
    const target = this.data.allDishes.find((dish) => dish.id === id)
    if (!target) {
      return
    }
    const fallback = foodFallback(target.name)
    const allDishes = this.data.allDishes.map((dish) => (
      dish.id === id ? { ...dish, imageSrc: fallback } : dish
    ))
    this.setData({ allDishes })
    this.applyFilter()
  },

  onSetmealImageError(event: TapEvent) {
    const id = numberData(event, 'id')
    const target = this.data.allSetmeals.find((setmeal) => setmeal.id === id)
    if (!target) {
      return
    }
    const fallback = foodFallback(target.name)
    const allSetmeals = this.data.allSetmeals.map((setmeal) => (
      setmeal.id === id ? { ...setmeal, imageSrc: fallback } : setmeal
    ))
    this.setData({ allSetmeals })
    this.applyFilter()
  },

  onAddDish(event: TapEvent) {
    const id = numberData(event, 'id')
    const dish = this.data.allDishes.find((item) => item.id === id)
    if (!this.guardOrdering() || !dish) {
      return
    }
    if (dish.soldOut) {
      toast('该菜品已售罄')
      return
    }
    const groups = flavorGroupsOf(dish)
    if (groups.length > 0) {
      this.setData({
        showFlavor: true,
        flavorDish: dish,
        flavorImage: dish.imageSrc,
        flavorName: dish.name,
        flavorPrice: dish.priceText,
        flavorGroups: groups,
      })
      return
    }
    void this.addDish(dish, '')
  },

  onFlavorSelect(event: TapEvent) {
    const groupIndex = numberData(event, 'gi')
    const option = stringData(event, 'option')
    const groups = this.data.flavorGroups.map((group, index) => (
      index === groupIndex ? { ...group, selected: option } : group
    ))
    this.setData({ flavorGroups: groups })
  },

  onCloseFlavor() {
    this.setData({ showFlavor: false })
  },

  noop() {
    // 用于 catchtap 阻止底部弹层点击冒泡到遮罩。
  },

  onFlavorConfirm() {
    const dish = this.data.flavorDish
    if (!dish) {
      return
    }
    const selected = this.data.flavorGroups
      .map((group) => group.selected)
      .filter((value) => value.length > 0)
      .join(',')
    this.setData({ showFlavor: false })
    void this.addDish(dish, selected)
  },

  async addDish(dish: DishView, dishFlavor: string) {
    try {
      const session = this.data.activeSession
      if (session) {
        await api.diningCartAdd({
          sessionId: session.id,
          dishId: dish.id,
          dishFlavor: dishFlavor || undefined,
          number: 1,
        })
      } else {
        await api.addCart({
          dishId: dish.id,
          dishFlavor: dishFlavor || undefined,
        })
      }
      await this.refreshCart()
      if (wx.vibrateShort) {
        wx.vibrateShort({ type: 'light' })
      }
      toast(`已加入${session ? '本桌购物车' : '购物车'}`)
    } catch (error) {
      showError(error)
    }
  },

  async onSetmealTap(event: TapEvent) {
    const id = numberData(event, 'id')
    const setmeal = this.data.allSetmeals.find((item) => item.id === id)
    if (!setmeal) {
      return
    }
    try {
      const items = await api.setmealItems(setmeal.id)
      const setmealItems = items.map((item) => ({
        ...item,
        imageSrc: foodImage(item.name, item.image),
      }))
      this.setData({
        showSetmeal: true,
        setmealDetail: setmeal,
        setmealItems,
      })
    } catch (error) {
      showError(error)
    }
  },

  onCloseSetmeal() {
    this.setData({ showSetmeal: false })
  },

  async onAddSetmeal() {
    const setmeal = this.data.setmealDetail
    if (!setmeal || !this.guardOrdering()) {
      return
    }
    if (this.data.activeSession) {
      toast('堂食共享购物车暂不支持套餐，请单点菜品')
      return
    }
    try {
      await api.addCart({ setmealId: setmeal.id })
      await this.refreshCart()
      this.setData({ showSetmeal: false })
      toast('已加入购物车')
    } catch (error) {
      showError(error)
    }
  },

  guardOrdering(): boolean {
    if (!this.data.shopOpen) {
      toast('门店打烊中，暂不接单')
      return false
    }
    if (this.data.activeSession && this.data.activeSession.status !== 1) {
      toast('本桌已申请结账，暂不能继续加菜')
      return false
    }
    return true
  },

  onSheetImageError(event: TapEvent) {
    const id = numberData(event, 'id')
    const target = this.data.cartItems.find((item) => item.id === id)
    if (!target) {
      return
    }
    const imageSrc = foodFallback(target.name)
    const cartItems = this.data.cartItems.map((item) => (
      item.id === id ? { ...item, imageSrc } : item
    ))
    this.setData({ cartItems })
  },

  async onSheetPlus(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.cartItems.find((entry) => entry.id === id)
    if (!item || this.data.sheetBusyId || !this.guardOrdering()) {
      return
    }
    this.setData({ sheetBusyId: id })
    try {
      const session = this.data.activeSession
      if (session) {
        if (!item.dishId) {
          toast('该商品暂不支持堂食加菜')
          return
        }
        await api.diningCartAdd({
          sessionId: session.id,
          dishId: item.dishId,
          dishFlavor: item.dishFlavor || undefined,
          number: 1,
        })
      } else if (item.dishId) {
        await api.addCart({ dishId: item.dishId, dishFlavor: item.dishFlavor || undefined })
      } else if (item.setmealId) {
        await api.addCart({ setmealId: item.setmealId })
      }
      await this.refreshCart()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ sheetBusyId: 0 })
    }
  },

  async onSheetMinus(event: TapEvent) {
    const id = numberData(event, 'id')
    const item = this.data.cartItems.find((entry) => entry.id === id)
    if (!item || this.data.sheetBusyId) {
      return
    }
    this.setData({ sheetBusyId: id })
    try {
      const session = this.data.activeSession
      if (session) {
        if (!item.dishId) {
          toast('该商品暂不支持堂食减菜')
          return
        }
        await api.diningCartAdd({
          sessionId: session.id,
          dishId: item.dishId,
          dishFlavor: item.dishFlavor || undefined,
          number: -1,
        })
      } else if (item.dishId) {
        await api.subCart({ dishId: item.dishId, dishFlavor: item.dishFlavor || undefined })
      } else if (item.setmealId) {
        await api.subCart({ setmealId: item.setmealId })
      }
      await this.refreshCart()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ sheetBusyId: 0 })
    }
  },

  async onSheetClear() {
    if (this.data.cartItems.length === 0) {
      return
    }
    const ok = await confirm('清空当前购物车？', '清空购物车')
    if (!ok) {
      return
    }
    try {
      const session = this.data.activeSession
      if (session) {
        await api.diningCartClear(session.id)
      } else {
        await api.cleanCart()
      }
      await this.refreshCart()
      toast('购物车已清空')
    } catch (error) {
      showError(error)
    }
  },

  openCartSheet() {
    if (this.data.cartCount <= 0) {
      toast(this.data.activeSession ? '还没选菜，先挑几道吧' : '购物车还是空的')
      return
    }
    this.setData({ showCartSheet: true })
  },

  closeCartSheet() {
    this.setData({ showCartSheet: false })
  },

  goCheckout() {
    const session = this.data.activeSession
    if (session && session.status !== 1) {
      toast('本桌已申请结账，暂不能继续下单')
      return
    }
    if (this.data.cartCount <= 0) {
      toast(session
        ? (this.data.hasSubmittedOrder ? '先选几道菜，再加餐' : '先选几道菜，再下单')
        : '购物车还是空的')
      return
    }
    this.setData({ showCartSheet: false })
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  goSession() {
    const session = this.data.activeSession
    if (!session) {
      return
    }
    wx.navigateTo({ url: `/pages/dining-session/dining-session?id=${session.id}` })
  },

  goDining() {
    wx.switchTab({ url: '/pages/dining/dining' })
  },

  onRetry() {
    void this.bootstrap()
  },
})
