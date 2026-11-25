package com.mzlhsc.orpl.admin.data.model

import com.google.gson.annotations.SerializedName

/** 后端 PageDTO<T>：注意字段是 total / pages / list，不是 MyBatis-Plus 的 records */
data class PageResult<T>(
    val total: Long? = 0,
    val pages: Long? = 0,
    val list: List<T>? = null,
) {
    val items: List<T> get() = list ?: emptyList()
    val pageCount: Long get() = pages ?: 0
}

data class LoginVO(
    val id: Long? = null,
    val userName: String? = null,
    val name: String? = null,
    val token: String? = null,
)

data class Employee(
    val id: Long? = null,
    val name: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val sex: String? = null,
    val idNumber: String? = null,
    val status: Int? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

data class Category(
    val id: Long? = null,
    val type: Int? = null,
    val name: String? = null,
    val sort: Int? = null,
    val status: Int? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

data class Dish(
    val id: Long? = null,
    val name: String? = null,
    val categoryId: Long? = null,
    val price: Double? = null,
    val image: String? = null,
    val description: String? = null,
    val status: Int? = null,
    val stock: Int? = null,
    val updateTime: String? = null,
    val categoryName: String? = null,
    val flavors: List<DishFlavor>? = null,
)

/** 菜品口味，编辑时随 DishDTO 一起提交 */
data class DishFlavor(
    val id: Long? = null,
    val dishId: Long? = null,
    val name: String = "",
    val value: String = "",
)

data class Setmeal(
    val id: Long? = null,
    val categoryId: Long? = null,
    val name: String? = null,
    val price: Double? = null,
    val status: Int? = null,
    val description: String? = null,
    val image: String? = null,
    val updateTime: String? = null,
    val categoryName: String? = null,
    val setmealDishes: List<SetmealDish>? = null,
)

/** 套餐里的菜品，copies 是份数 */
data class SetmealDish(
    val id: Long? = null,
    val setmealId: Long? = null,
    val dishId: Long? = null,
    val name: String? = null,
    val price: Double? = null,
    var copies: Int? = 1,
)

data class OrderDetail(
    val id: Long? = null,
    val name: String? = null,
    val orderId: Long? = null,
    val dishId: Long? = null,
    val setmealId: Long? = null,
    val dishFlavor: String? = null,
    val number: Int? = null,
    val amount: Double? = null,
    val image: String? = null,
)

data class Order(
    val id: Long? = null,
    val number: String? = null,
    val status: Int? = null,
    val userId: Long? = null,
    val orderTime: String? = null,
    val checkoutTime: String? = null,
    val payMethod: Int? = null,
    val payStatus: Int? = null,
    val amount: Double? = null,
    val remark: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val userName: String? = null,
    val consignee: String? = null,
    val cancelReason: String? = null,
    val rejectionReason: String? = null,
    val cancelTime: String? = null,
    val estimatedDeliveryTime: String? = null,
    val deliveryStatus: Int? = null,
    val deliveryTime: String? = null,
    val packAmount: Int? = null,
    val tablewareNumber: Int? = null,
    val orderType: Int? = null,
    val orderDishes: String? = null,
    val orderDetailList: List<OrderDetail>? = null,
)

/** 订单状态统计：派送中已删除，只剩待接单与已接单（待处理） */
data class OrderStatistics(
    val toBeConfirmed: Int? = null,
    val confirmed: Int? = null,
)

/** 店铺自动接单开关，对应 GET/PUT /admin/shop/autoAccept */
data class ShopAutoAccept(
    val takeoutAutoAccept: Boolean? = null,
    val diningAutoAccept: Boolean? = null,
)

data class OrderOverview(
    val allOrders: Int? = null,
    val completedOrders: Int? = null,
    val cancelledOrders: Int? = null,
    val waitingOrders: Int? = null,
    val deliveredOrders: Int? = null,
)

data class GoodsOverview(
    val sold: Int? = null,
    val discontinued: Int? = null,
)

data class BusinessData(
    val turnover: Double? = null,
    val validOrderCount: Int? = null,
    val orderCompletionRate: Double? = null,
    val unitPrice: Double? = null,
    val newUsers: Int? = null,
)

/** 报表类接口统一返回逗号分隔的字符串，用 splitCsv 拆开 */
data class TurnoverReport(
    val dateList: String? = null,
    val turnoverList: String? = null,
)

data class OrderReport(
    val dateList: String? = null,
    val orderCountList: String? = null,
    val validOrderCountList: String? = null,
    val totalOrderCount: Int? = null,
    val validOrderCount: Int? = null,
    val orderCompletionRate: Double? = null,
)

data class UserReport(
    val dateList: String? = null,
    val totalUserList: String? = null,
    val newUserList: String? = null,
)

data class SalesTop10Report(
    val nameList: String? = null,
    val numberList: String? = null,
)

/* ---------------- 堂食 ---------------- */

/** 桌台：status 0空闲 1就餐中 2待结账 */
data class DiningTable(
    val id: Long? = null,
    val name: String? = null,
    val seats: Int? = null,
    val area: String? = null,
    val status: Int? = null,
    val qrToken: String? = null,
    val qrContent: String? = null,
    val reservationEnabled: Int? = null,
    val currentSessionId: Long? = null,
    val currentSessionNo: String? = null,
)

/** 会话成员（AA 结账用） */
data class DiningMember(
    val id: Long? = null,
    val sessionId: Long? = null,
    val userId: Long? = null,
    val nickname: String? = null,
    val isOwner: Int? = null,
    val paidAmount: Double? = null,
    val payStatus: Int? = null,
    val joinTime: String? = null,
)

/** 堂食共享购物车条目 */
data class CartItem(
    val id: Long? = null,
    val name: String? = null,
    val image: String? = null,
    val number: Int? = null,
    val amount: Double? = null,
    val dishFlavor: String? = null,
)

/** 就餐会话：status 1就餐中 2待结账 3已结账 4已关闭 */
data class DiningSession(
    val id: Long? = null,
    val sessionNo: String? = null,
    val tableId: Long? = null,
    val tableName: String? = null,
    val status: Int? = null,
    val guestCount: Int? = null,
    val ownerUserId: Long? = null,
    val aaEnabled: Int? = null,
    val remark: String? = null,
    val totalAmount: Double? = null,
    val paidAmount: Double? = null,
    val unpaidAmount: Double? = null,
    val perPersonAmount: Double? = null,
    val cartAmount: Double? = null,
    val memberCount: Int? = null,
    val openTime: String? = null,
    val checkoutTime: String? = null,
    val tableNames: List<String>? = null,
    val members: List<DiningMember>? = null,
    val cartItems: List<CartItem>? = null,
    val orders: List<Order>? = null,
)

/** 预约：status 1待确认 2已确认 3已到店 4已取消 5已过期 */
data class DiningReservation(
    val id: Long? = null,
    val tableId: Long? = null,
    val userId: Long? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val reserveTime: String? = null,
    val guestCount: Int? = null,
    val status: Int? = null,
    val remark: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

data class DiningReport(
    val sessionCount: Long? = null,
    val totalRevenue: Double? = null,
    val totalGuests: Long? = null,
    val avgAmount: Double? = null,
    val avgGuests: Double? = null,
    val turnoverRate: Double? = null,
    val tableCount: Int? = null,
    val daily: List<DiningDaily>? = null,
)

/** 报表每日明细，后端字段是 snake_case，用 @SerializedName 对齐 */
data class DiningDaily(
    @SerializedName("day") val day: String? = null,
    @SerializedName("revenue") val revenue: Double? = null,
    @SerializedName("session_count") val sessionCount: Long? = null,
    @SerializedName("guests") val guests: Long? = null,
)
