package com.mzlhsc.orpl.admin.data

import android.content.Context
import android.content.SharedPreferences
import com.mzlhsc.orpl.admin.BuildConfig

/**
 * 登录态与服务器地址的本地存储。
 * 后端令牌请求头是 zmlhsc，和 nginx/html/admin/js/api.js 保持一致。
 */
object Prefs {

    private const val FILE = "orpl_admin"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMP_ID = "emp_id"
    private const val KEY_NAME = "emp_name"
    private const val KEY_USERNAME = "emp_username"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = normalize(sp.getString(KEY_BASE_URL, null) ?: BuildConfig.DEFAULT_BASE_URL)
        set(value) {
            sp.edit().putString(KEY_BASE_URL, normalize(value)).apply()
        }

    var token: String?
        get() = sp.getString(KEY_TOKEN, null)
        set(value) {
            sp.edit().putString(KEY_TOKEN, value).apply()
        }

    var empId: Long
        get() = sp.getLong(KEY_EMP_ID, 0L)
        set(value) {
            sp.edit().putLong(KEY_EMP_ID, value).apply()
        }

    var empName: String?
        get() = sp.getString(KEY_NAME, null)
        set(value) {
            sp.edit().putString(KEY_NAME, value).apply()
        }

    var empUserName: String?
        get() = sp.getString(KEY_USERNAME, null)
        set(value) {
            sp.edit().putString(KEY_USERNAME, value).apply()
        }

    val loggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun saveSession(id: Long, userName: String?, name: String?, token: String) {
        this.token = token
        this.empId = id
        this.empUserName = userName
        this.empName = name
    }

    fun clearSession() {
        sp.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMP_ID)
            .remove(KEY_NAME)
            .remove(KEY_USERNAME)
            .apply()
    }

    /** 补全协议头、管理端前缀与结尾斜杠，避免用户少打一个字符就请求失败 */
    fun normalize(raw: String?): String {
        var url = (raw ?: "").trim()
        if (url.isEmpty()) url = BuildConfig.DEFAULT_BASE_URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        // 只填了主机端口时补默认前缀：直连后端 8080 用 /admin/，经 nginx 80/443 用 /api/
        val afterScheme = url.substringAfter("://")
        if (!afterScheme.contains("/")) {
            val isDirectBackend = afterScheme.substringAfter(":", "").substringBefore("/") == "8080"
            url = "$url" + if (isDirectBackend) "/admin/" else "/api/"
        }
        if (!url.endsWith("/")) url = "$url/"
        return url
    }
}
