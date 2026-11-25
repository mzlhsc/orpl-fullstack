package com.mzlhsc.orpl.admin.data

/** 统一业务异常：code 401 表示登录态失效 */
class ApiException(message: String, val code: Int = 0) : Exception(message) {

    val unauthorized: Boolean
        get() = code == 401 || code == 403
}
