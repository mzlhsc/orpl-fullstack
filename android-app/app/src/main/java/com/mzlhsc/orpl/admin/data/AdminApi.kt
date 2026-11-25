package com.mzlhsc.orpl.admin.data

import com.google.gson.reflect.TypeToken
import com.mzlhsc.orpl.admin.data.model.BusinessData
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.data.model.DiningReport
import com.mzlhsc.orpl.admin.data.model.DiningReservation
import com.mzlhsc.orpl.admin.data.model.DiningSession
import com.mzlhsc.orpl.admin.data.model.DiningTable
import com.mzlhsc.orpl.admin.data.model.Dish
import com.mzlhsc.orpl.admin.data.model.Employee
import com.mzlhsc.orpl.admin.data.model.GoodsOverview
import com.mzlhsc.orpl.admin.data.model.LoginVO
import com.mzlhsc.orpl.admin.data.model.Order
import com.mzlhsc.orpl.admin.data.model.OrderOverview
import com.mzlhsc.orpl.admin.data.model.OrderReport
import com.mzlhsc.orpl.admin.data.model.OrderStatistics
import com.mzlhsc.orpl.admin.data.model.PageResult
import com.mzlhsc.orpl.admin.data.model.SalesTop10Report
import com.mzlhsc.orpl.admin.data.model.Setmeal
import com.mzlhsc.orpl.admin.data.model.ShopAutoAccept
import com.mzlhsc.orpl.admin.data.model.TurnoverReport
import com.mzlhsc.orpl.admin.data.model.UserReport

/**
 * 管理端接口封装，路径与 nginx/html/admin/js/api.js 一一对应。
 * 服务端前缀 /admin/ 已包含在 baseUrl 里，这里只写业务路径。
 */
@Suppress("UNCHECKED_CAST")
object AdminApi {

    private val EMPLOYEE_PAGE = object : TypeToken<PageResult<Employee>>() {}.type
    private val CATEGORY_PAGE = object : TypeToken<PageResult<Category>>() {}.type
    private val CATEGORY_LIST = object : TypeToken<List<Category>>() {}.type
    private val DISH_PAGE = object : TypeToken<PageResult<Dish>>() {}.type
    private val SETMEAL_PAGE = object : TypeToken<PageResult<Setmeal>>() {}.type
    private val ORDER_PAGE = object : TypeToken<PageResult<Order>>() {}.type
    private val EMPLOYEE_ONE = Employee::class.java
    private val DISH_ONE = Dish::class.java
    private val SETMEAL_ONE = Setmeal::class.java
    private val DISH_LIST = object : TypeToken<List<Dish>>() {}.type
    private val DINING_TABLES = object : TypeToken<List<DiningTable>>() {}.type
    private val DINING_SESSIONS = object : TypeToken<List<DiningSession>>() {}.type
    private val DINING_RESERVATIONS = object : TypeToken<List<DiningReservation>>() {}.type

    /* ---------------- 登录 / 账号 ---------------- */

    suspend fun login(username: String, password: String): LoginVO? =
        ApiClient.request(
            "POST",
            "employee/login",
            body = mapOf("username" to username, "password" to password),
            type = LoginVO::class.java,
        ) as? LoginVO

    suspend fun logout() {
        ApiClient.request("POST", "employee/logout")
    }

    suspend fun editPassword(empId: Long, oldPassword: String, newPassword: String) {
        ApiClient.request(
            "PUT",
            "employee/editPassword",
            body = mapOf(
                "empId" to empId,
                "oldPassword" to oldPassword,
                "newPassword" to newPassword,
            ),
        )
    }

    /* ---------------- 员工 ---------------- */

    suspend fun employeePage(page: Int, pageSize: Int, name: String? = null): PageResult<Employee> =
        ApiClient.request(
            "GET",
            "employee/page",
            query = mapOf("page" to page, "pageSize" to pageSize, "name" to name),
            type = EMPLOYEE_PAGE,
        ) as? PageResult<Employee> ?: PageResult()

    suspend fun employeeStatus(status: Int, id: Long) {
        ApiClient.request("POST", "employee/status/$status", query = mapOf("id" to id))
    }

    suspend fun employeeGet(id: Long): Employee? =
        ApiClient.request("GET", "employee/$id", type = EMPLOYEE_ONE) as? Employee

    /** 新增员工：后端默认密码 123456，账号重复会返回业务错误 */
    suspend fun employeeSave(body: Map<String, Any?>) {
        ApiClient.request("POST", "employee", body = body)
    }

    /** 编辑员工：接口不修改密码与状态 */
    suspend fun employeeUpdate(body: Map<String, Any?>) {
        ApiClient.request("PUT", "employee", body = body)
    }

    /* ---------------- 分类 ---------------- */

    suspend fun categoryPage(page: Int, pageSize: Int, type: Int? = null, name: String? = null): PageResult<Category> =
        ApiClient.request(
            "GET",
            "category/page",
            query = mapOf("page" to page, "pageSize" to pageSize, "type" to type, "name" to name),
            type = CATEGORY_PAGE,
        ) as? PageResult<Category> ?: PageResult()

    suspend fun categoryList(type: Int? = null): List<Category> =
        ApiClient.request(
            "GET",
            "category/list",
            query = mapOf("type" to type),
            type = CATEGORY_LIST,
        ) as? List<Category> ?: emptyList()

    suspend fun categorySave(body: Map<String, Any?>) {
        ApiClient.request("POST", "category", body = body)
    }

    suspend fun categoryUpdate(body: Map<String, Any?>) {
        ApiClient.request("PUT", "category", body = body)
    }

    suspend fun categoryDelete(id: Long) {
        ApiClient.request("DELETE", "category", query = mapOf("id" to id))
    }

    suspend fun categoryStatus(status: Int, id: Long) {
        ApiClient.request("POST", "category/status/$status", query = mapOf("id" to id))
    }

    /* ---------------- 菜品 ---------------- */

    suspend fun dishPage(
        page: Int,
        pageSize: Int,
        name: String? = null,
        categoryId: Long? = null,
        status: Int? = null,
    ): PageResult<Dish> =
        ApiClient.request(
            "GET",
            "dish/page",
            query = mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "name" to name,
                "categoryId" to categoryId,
                "status" to status,
            ),
            type = DISH_PAGE,
        ) as? PageResult<Dish> ?: PageResult()

    suspend fun dishStatus(status: Int, id: Long) {
        ApiClient.request("POST", "dish/status/$status", query = mapOf("id" to id))
    }

    suspend fun dishStock(dishId: Long): Int? {
        val result = ApiClient.request("GET", "dish/stock/$dishId", type = Int::class.javaObjectType)
        return (result as? Number)?.toInt()
    }

    suspend fun dishGet(id: Long): Dish? =
        ApiClient.request("GET", "dish/$id", type = DISH_ONE) as? Dish

    suspend fun dishSave(body: Map<String, Any?>) {
        ApiClient.request("POST", "dish", body = body)
    }

    suspend fun dishUpdate(body: Map<String, Any?>) {
        ApiClient.request("PUT", "dish", body = body)
    }

    suspend fun dishDelete(ids: List<Long>) {
        ApiClient.request("DELETE", "dish", query = mapOf("ids" to ids.joinToString(",")))
    }

    suspend fun dishSetStock(dishId: Long, stock: Int) {
        ApiClient.request(
            "PUT",
            "dish/stock",
            body = mapOf("dishId" to dishId, "stock" to stock),
        )
    }

    /* ---------------- 套餐 ---------------- */

    suspend fun setmealPage(
        page: Int,
        pageSize: Int,
        name: String? = null,
        categoryId: Long? = null,
        status: Int? = null,
    ): PageResult<Setmeal> =
        ApiClient.request(
            "GET",
            "setmeal/page",
            query = mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "name" to name,
                "categoryId" to categoryId,
                "status" to status,
            ),
            type = SETMEAL_PAGE,
        ) as? PageResult<Setmeal> ?: PageResult()

    suspend fun setmealStatus(status: Int, id: Long) {
        ApiClient.request("POST", "setmeal/status/$status", query = mapOf("id" to id))
    }

    suspend fun setmealGet(id: Long): Setmeal? =
        ApiClient.request("GET", "setmeal/$id", type = SETMEAL_ONE) as? Setmeal

    suspend fun setmealSave(body: Map<String, Any?>) {
        ApiClient.request("POST", "setmeal", body = body)
    }

    suspend fun setmealUpdate(body: Map<String, Any?>) {
        ApiClient.request("PUT", "setmeal", body = body)
    }

    suspend fun setmealDelete(ids: List<Long>) {
        ApiClient.request("DELETE", "setmeal", query = mapOf("ids" to ids.joinToString(",")))
    }

    /** 按分类查菜品（后端的 dish/list 必须带 categoryId，且返回裸数组） */
    suspend fun dishList(categoryId: Long): List<Dish> =
        ApiClient.request(
            "GET",
            "dish/list",
            query = mapOf("categoryId" to categoryId),
            type = DISH_LIST,
        ) as? List<Dish> ?: emptyList()

    /** 套餐编辑用：把菜品分类下的菜品全部拉平 */
    suspend fun dishAll(): List<Dish> {
        val categories = categoryList(1)
        val all = mutableListOf<Dish>()
        categories.forEach { c ->
            val id = c.id ?: return@forEach
            all += runCatching { dishList(id) }.getOrDefault(emptyList())
        }
        return all
    }

    /** 图片上传，需要后端配置阿里云 OSS */
    suspend fun uploadImage(fileName: String, mime: String, bytes: ByteArray): String? =
        ApiClient.upload("common/upload", fileName, mime, bytes)

    /* ---------------- 订单 ---------------- */

    suspend fun orderPage(
        page: Int,
        pageSize: Int,
        status: Int? = null,
        number: String? = null,
        phone: String? = null,
    ): PageResult<Order> =
        ApiClient.request(
            "GET",
            "order/conditionSearch",
            query = mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "status" to status,
                "number" to number,
                "phone" to phone,
            ),
            type = ORDER_PAGE,
        ) as? PageResult<Order> ?: PageResult()

    suspend fun orderDetail(id: Long): Order? =
        ApiClient.request("GET", "order/details/$id", type = Order::class.java) as? Order

    suspend fun orderConfirm(id: Long) {
        ApiClient.request("PUT", "order/confirm", body = mapOf("id" to id))
    }

    /** 派送中状态已删除，已接单可直接完成 */
    suspend fun orderComplete(id: Long) {
        ApiClient.request("PUT", "order/complete/$id")
    }

    /** 管理端手动标记支付状态：0 未支付 / 1 已支付 */
    suspend fun orderPayStatus(id: Long, payStatus: Int) {
        ApiClient.request(
            "PUT",
            "order/payStatus",
            body = mapOf("id" to id, "payStatus" to payStatus),
        )
    }

    suspend fun orderReject(id: Long, reason: String) {
        ApiClient.request(
            "PUT",
            "order/rejection",
            body = mapOf("id" to id, "rejectionReason" to reason),
        )
    }

    suspend fun orderCancel(id: Long, reason: String) {
        ApiClient.request(
            "PUT",
            "order/cancel",
            body = mapOf("id" to id, "cancelReason" to reason),
        )
    }

    suspend fun orderStatistics(): OrderStatistics =
        ApiClient.request("GET", "order/statistics", type = OrderStatistics::class.java)
            as? OrderStatistics ?: OrderStatistics()

    /* ---------------- 店铺 ---------------- */

    suspend fun shopStatus(): Int? {
        val result = ApiClient.request("GET", "shop/status", type = Int::class.javaObjectType)
        return (result as? Number)?.toInt()
    }

    suspend fun shopSet(status: Int) {
        ApiClient.request("PUT", "shop/$status")
    }

    suspend fun shopAutoAccept(): ShopAutoAccept? =
        ApiClient.request("GET", "shop/autoAccept", type = ShopAutoAccept::class.java) as? ShopAutoAccept

    /** 只传要改的字段，未传的字段后端保持原值 */
    suspend fun shopAutoAcceptSet(body: Map<String, Any?>): ShopAutoAccept? =
        ApiClient.request("PUT", "shop/autoAccept", body = body, type = ShopAutoAccept::class.java)
            as? ShopAutoAccept

    /* ---------------- 堂食 ---------------- */

    suspend fun diningTables(status: Int? = null, area: String? = null): List<DiningTable> =
        ApiClient.request(
            "GET",
            "dining/table/list",
            query = mapOf("status" to status, "area" to area),
            type = DINING_TABLES,
        ) as? List<DiningTable> ?: emptyList()

    suspend fun diningTableSave(id: Long?, body: Map<String, Any?>) {
        if (id == null) {
            ApiClient.request("POST", "dining/table", body = body)
        } else {
            ApiClient.request("PUT", "dining/table/$id", body = body)
        }
    }

    suspend fun diningTableDelete(id: Long) {
        ApiClient.request("DELETE", "dining/table/$id")
    }

    suspend fun diningTableRefreshQr(id: Long): DiningTable? =
        ApiClient.request("POST", "dining/table/$id/refresh-qr", type = DiningTable::class.java)
            as? DiningTable

    suspend fun diningReservations(status: Int? = null, date: String? = null): List<DiningReservation> =
        ApiClient.request(
            "GET",
            "dining/reservation/list",
            query = mapOf("status" to status, "date" to date),
            type = DINING_RESERVATIONS,
        ) as? List<DiningReservation> ?: emptyList()

    suspend fun diningReservationConfirm(id: Long) {
        ApiClient.request("POST", "dining/reservation/$id/confirm")
    }

    suspend fun diningReservationCancel(id: Long) {
        ApiClient.request("POST", "dining/reservation/$id/cancel")
    }

    suspend fun diningReservationArrive(id: Long) {
        ApiClient.request("POST", "dining/reservation/$id/arrive")
    }

    suspend fun diningSessions(status: Int? = null, tableId: Long? = null): List<DiningSession> =
        ApiClient.request(
            "GET",
            "dining/session/list",
            query = mapOf("status" to status, "tableId" to tableId),
            type = DINING_SESSIONS,
        ) as? List<DiningSession> ?: emptyList()

    suspend fun diningSession(id: Long): DiningSession? =
        ApiClient.request("GET", "dining/session/$id", type = DiningSession::class.java) as? DiningSession

    /** 员工代客开台 */
    suspend fun diningOpen(tableId: Long, guestCount: Int, remark: String? = null): DiningSession? =
        ApiClient.request(
            "POST",
            "dining/session/open",
            body = mapOf("tableId" to tableId, "guestCount" to guestCount, "remark" to remark),
            type = DiningSession::class.java,
        ) as? DiningSession

    suspend fun diningTransfer(id: Long, targetTableId: Long, remark: String? = null) {
        ApiClient.request(
            "POST",
            "dining/session/$id/transfer",
            body = mapOf("targetTableId" to targetTableId, "remark" to remark),
        )
    }

    suspend fun diningMerge(targetId: Long, sourceSessionIds: List<Long>, remark: String? = null) {
        ApiClient.request(
            "POST",
            "dining/session/$targetId/merge",
            body = mapOf("sourceSessionIds" to sourceSessionIds, "remark" to remark),
        )
    }

    /** 关闭会话：取消未完成订单、清空共享购物车、释放桌台 */
    /** 关闭会话：reason 可不填，后端默认记为「管理员关闭堂食会话」 */
    suspend fun diningClose(id: Long, reason: String = "") {
        val query: Map<String, Any?> = if (reason.isBlank()) emptyMap() else mapOf("reason" to reason)
        ApiClient.request("POST", "dining/session/$id/close", query = query)
    }

    suspend fun diningCheckout(
        sessionId: Long,
        payMethod: Int,
        settleUnpaid: Boolean,
        remark: String? = null,
    ) {
        ApiClient.request(
            "POST",
            "dining/session/checkout",
            body = mapOf(
                "sessionId" to sessionId,
                "payMethod" to payMethod,
                "settleUnpaid" to settleUnpaid,
                "remark" to remark,
            ),
        )
    }

    suspend fun diningReport(begin: String?, end: String?): DiningReport? =
        ApiClient.request(
            "GET",
            "dining/report",
            query = mapOf("begin" to begin, "end" to end),
            type = DiningReport::class.java,
        ) as? DiningReport

    /* ---------------- 工作台 ---------------- */

    suspend fun businessData(): BusinessData? =
        ApiClient.request("GET", "workspace/businessData", type = BusinessData::class.java) as? BusinessData

    suspend fun overviewOrders(): OrderOverview? =
        ApiClient.request("GET", "workspace/overviewOrders", type = OrderOverview::class.java) as? OrderOverview

    suspend fun overviewDishes(): GoodsOverview? =
        ApiClient.request("GET", "workspace/overviewDishes", type = GoodsOverview::class.java) as? GoodsOverview

    suspend fun overviewSetmeals(): GoodsOverview? =
        ApiClient.request("GET", "workspace/overviewSetmeals", type = GoodsOverview::class.java) as? GoodsOverview

    /* ---------------- 数据统计 ---------------- */

    suspend fun reportTurnover(begin: String, end: String): TurnoverReport? =
        ApiClient.request(
            "GET",
            "report/turnoverStatistics",
            query = mapOf("begin" to begin, "end" to end),
            type = TurnoverReport::class.java,
        ) as? TurnoverReport

    suspend fun reportOrders(begin: String, end: String): OrderReport? =
        ApiClient.request(
            "GET",
            "report/ordersStatistics",
            query = mapOf("begin" to begin, "end" to end),
            type = OrderReport::class.java,
        ) as? OrderReport

    suspend fun reportUsers(begin: String, end: String): UserReport? =
        ApiClient.request(
            "GET",
            "report/userStatistics",
            query = mapOf("begin" to begin, "end" to end),
            type = UserReport::class.java,
        ) as? UserReport

    suspend fun reportTop10(begin: String, end: String): SalesTop10Report? =
        ApiClient.request(
            "GET",
            "report/top10",
            query = mapOf("begin" to begin, "end" to end),
            type = SalesTop10Report::class.java,
        ) as? SalesTop10Report
}
