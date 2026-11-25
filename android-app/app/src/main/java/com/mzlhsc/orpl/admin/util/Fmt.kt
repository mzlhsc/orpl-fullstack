package com.mzlhsc.orpl.admin.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 与前端 util.js 保持一致的展示格式 */
object Fmt {

    private val moneyFormat = DecimalFormat("#,##0.00")
    private val countFormat = DecimalFormat("#,##0")

    private fun parse(raw: String?): Date? {
        val text = raw?.trim()?.replace('T', ' ') ?: return null
        val pattern = if (text.length >= 19) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd HH:mm"
        return try {
            SimpleDateFormat(pattern, Locale.CHINA).parse(text.substring(0, minOf(text.length, pattern.length)))
        } catch (e: Exception) {
            null
        }
    }

    fun money(value: Double?): String = "¥" + moneyFormat.format(value ?: 0.0)

    fun moneyPlain(value: Double?): String = moneyFormat.format(value ?: 0.0)

    fun count(value: Int?): String = countFormat.format(value ?: 0)

    fun count(value: Long?): String = countFormat.format(value ?: 0L)

    fun percent(rate: Double?): String {
        val v = (rate ?: 0.0) * 100
        return if (v == 0.0) "0%" else String.format(Locale.CHINA, "%.1f%%", v)
    }

    fun dateTime(raw: String?): String {
        val date = parse(raw) ?: return raw?.take(16)?.replace('T', ' ') ?: "—"
        return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(date)
    }

    fun date(raw: String?): String {
        val date = parse(raw) ?: return raw?.take(10) ?: "—"
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date)
    }

    fun time(raw: String?): String {
        val date = parse(raw) ?: return "—"
        return SimpleDateFormat("HH:mm", Locale.CHINA).format(date)
    }

    fun splitCsv(raw: String?): List<String> =
        raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun isoDate(offsetDays: Int = 0): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(calendar.time)
    }

    fun mdLabel(iso: String): String = if (iso.length >= 10) iso.substring(5) else iso

    fun weekdayToday(): String {
        val names = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val calendar = Calendar.getInstance()
        return names[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun dash(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "—"

    fun orderTypeLabel(type: Int?): String = if (type == 2) "堂食" else "外卖"

    fun payMethodLabel(method: Int?): String = when (method) {
        1 -> "微信"
        2 -> "支付宝"
        else -> "—"
    }

    fun payStatusLabel(status: Int?): String = when (status) {
        0 -> "未支付"
        1 -> "已支付"
        2 -> "已退款"
        else -> "—"
    }
}
