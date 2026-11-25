package com.mzlhsc.orpl.admin.util

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.mzlhsc.orpl.admin.R

/** 状态标签配色，对应 web 端 .tag-success / .tag-danger 等 */
enum class Tone(@DrawableRes val background: Int, @ColorRes val textColor: Int) {
    NEUTRAL(R.drawable.bg_tag_neutral, R.color.muted),
    SUCCESS(R.drawable.bg_tag_success, R.color.success),
    WARN(R.drawable.bg_tag_warn, R.color.warn),
    DANGER(R.drawable.bg_tag_danger, R.color.danger),
    INFO(R.drawable.bg_tag_info, R.color.info),
    BRAND(R.drawable.bg_tag_brand, R.color.brand_strong),
}

object Tags {

    fun apply(view: TextView, text: CharSequence, tone: Tone) {
        view.text = text
        view.setBackgroundResource(tone.background)
        view.setTextColor(ContextCompat.getColor(view.context, tone.textColor))
    }

    /** 订单状态：1待付款 2待接单 3已接单 5已完成 6已取消（4派送中已删除） */
    fun order(status: Int?): Pair<String, Tone> = when (status) {
        1 -> "待付款" to Tone.WARN
        2 -> "待接单" to Tone.BRAND
        3 -> "已接单" to Tone.INFO
        4 -> "已接单" to Tone.INFO
        5 -> "已完成" to Tone.SUCCESS
        6 -> "已取消" to Tone.DANGER
        7 -> "退款" to Tone.DANGER
        else -> "状态 ${status ?: "-"}" to Tone.NEUTRAL
    }

    fun pay(status: Int?): Pair<String, Tone> = when (status) {
        1 -> "已支付" to Tone.SUCCESS
        2 -> "已退款" to Tone.DANGER
        else -> "未支付" to Tone.NEUTRAL
    }

    fun enable(status: Int?): Pair<String, Tone> =
        if (status == 1) "启用" to Tone.SUCCESS else "禁用" to Tone.NEUTRAL

    /** 桌台：0空闲 1就餐中 2待结账 */
    fun table(status: Int?): Pair<String, Tone> = when (status) {
        0 -> "空闲" to Tone.SUCCESS
        1 -> "就餐中" to Tone.BRAND
        2 -> "待结账" to Tone.WARN
        else -> "状态 ${status ?: "-"}" to Tone.NEUTRAL
    }

    /** 会话：1就餐中 2待结账 3已结账 4已关闭 */
    fun session(status: Int?): Pair<String, Tone> = when (status) {
        1 -> "就餐中" to Tone.BRAND
        2 -> "待结账" to Tone.WARN
        3 -> "已结账" to Tone.SUCCESS
        4 -> "已关闭" to Tone.DANGER
        else -> "状态 ${status ?: "-"}" to Tone.NEUTRAL
    }

    /** 预约：1待确认 2已确认 3已到店 4已取消 5已过期 */
    fun reservation(status: Int?): Pair<String, Tone> = when (status) {
        1 -> "待确认" to Tone.WARN
        2 -> "已确认" to Tone.INFO
        3 -> "已到店" to Tone.SUCCESS
        4 -> "已取消" to Tone.DANGER
        5 -> "已过期" to Tone.NEUTRAL
        else -> "状态 ${status ?: "-"}" to Tone.NEUTRAL
    }
}
