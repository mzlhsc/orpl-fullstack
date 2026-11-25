package com.mzlhsc.orpl.admin.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 轻量 HTTP 客户端。
 *
 * 后端接口存在两种响应形态，这里按前端 api.js 的做法统一兼容：
 * 1. 统一结构 R<T>：{ code, msg, data } —— code != 200 视为业务失败
 * 2. 裸对象：员工/分类/菜品的分页、登录返回都是直接给数据，没有 code 包裹
 */
object ApiClient {

    private val gson = GsonBuilder().create()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json;charset=UTF-8".toMediaType()

    suspend fun request(
        method: String,
        path: String,
        query: Map<String, Any?> = emptyMap(),
        body: Any? = null,
        type: Type? = null,
    ): Any? = withContext<Any?>(Dispatchers.IO) {
        val url = buildUrl(path, query)
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        Prefs.token?.takeIf { it.isNotBlank() }?.let { builder.header("zmlhsc", it) }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> {
                val payload = if (body == null) "" else gson.toJson(body)
                builder.method(method.uppercase(), payload.toRequestBody(jsonMediaType))
            }
        }

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ApiException("网络异常，请确认后端服务已启动、地址可访问")
        } catch (e: IllegalArgumentException) {
            throw ApiException("接口地址不合法：$url")
        }

        response.use { resp -> readResponse(resp, type) }
    }

    /** 上传文件（图片）：multipart/form-data，字段名必须叫 file */
    suspend fun upload(path: String, fileName: String, mime: String, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", fileName, bytes.toRequestBody(mime.toMediaType()))
                .build()
            val builder = Request.Builder().url(buildUrl(path, emptyMap())).post(body)
            Prefs.token?.takeIf { it.isNotBlank() }?.let { builder.header("zmlhsc", it) }
            val response = try {
                client.newCall(builder.build()).execute()
            } catch (e: IOException) {
                throw ApiException("图片上传失败，请检查网络与后端服务")
            }
            response.use { resp -> readResponse(resp, String::class.java) as? String }
        }

    private fun readResponse(resp: okhttp3.Response, type: Type?): Any? {
        val text = resp.body?.string().orEmpty()

        if (resp.code == 401) {
            Prefs.clearSession()
            throw ApiException("登录状态已失效，请重新登录", 401)
        }
        if (text.isBlank()) {
            if (!resp.isSuccessful) throw ApiException(httpMessage(resp.code), resp.code)
            return null
        }

        val element = try {
            JsonParser.parseString(text)
        } catch (e: Exception) {
            throw ApiException("返回数据解析失败（HTTP ${resp.code}）", resp.code)
        }

        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val wrapped = obj.has("code") && (obj.has("msg") || obj.has("data"))
            if (wrapped) {
                val code = obj.get("code")?.takeIf { !it.isJsonNull }?.asInt ?: 0
                if (code != 200) {
                    if (code == 401) Prefs.clearSession()
                    val msg = obj.get("msg")?.takeIf { !it.isJsonNull }?.asString
                    throw ApiException(msg?.takeIf { it.isNotBlank() } ?: "业务处理失败", code)
                }
                val data = obj.get("data")
                if (data == null || data.isJsonNull || type == null) return null
                return gson.fromJson(data, type)
            }
        }

        if (!resp.isSuccessful) throw ApiException(httpMessage(resp.code), resp.code)
        if (type == null) return null
        return gson.fromJson(element, type)
    }

    private fun buildUrl(path: String, query: Map<String, Any?>): String {
        val base = Prefs.baseUrl
        val sb = StringBuilder(base)
        sb.append(path.trimStart('/'))
        val params = query.entries.filter { (_, v) -> v != null && v.toString().isNotBlank() }
        if (params.isNotEmpty()) {
            sb.append('?')
            sb.append(params.joinToString("&") { (k, v) ->
                "${encode(k)}=${encode(v.toString())}"
            })
        }
        return sb.toString()
    }

    private fun encode(value: String): String = try {
        URLEncoder.encode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }

    private fun httpMessage(status: Int): String = when (status) {
        400 -> "请求参数有误"
        401 -> "登录状态已失效，请重新登录"
        403 -> "没有权限访问"
        404 -> "请求的资源不存在"
        405 -> "请求方式不支持"
        500 -> "服务器内部异常"
        502 -> "网关错误，请确认后端服务是否启动"
        503 -> "服务暂不可用"
        504 -> "网关超时"
        else -> "请求失败（HTTP $status）"
    }
}
