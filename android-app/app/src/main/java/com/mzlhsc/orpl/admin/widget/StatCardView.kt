package com.mzlhsc.orpl.admin.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.databinding.ViewStatCardBinding
import com.mzlhsc.orpl.admin.util.dp

/**
 * 工作台/报表用的统计卡：左侧浅色图标 + 标题，数值大字 + 辅助说明。
 * 视觉与 web 端 .stat-card 一致（白底、1dp 描边、14dp 圆角）。
 */
class StatCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    enum class IconTone(
        @DrawableRes val background: Int,
        @ColorRes val color: Int,
    ) {
        BRAND(R.drawable.bg_icon_brand, R.color.brand_strong),
        GREEN(R.drawable.bg_icon_green, R.color.success),
        BLUE(R.drawable.bg_icon_blue, R.color.info),
        AMBER(R.drawable.bg_icon_amber, R.color.warn),
        SLATE(R.drawable.bg_icon_slate, R.color.slate),
    }

    private val binding = ViewStatCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        cardElevation = 0f
        radius = context.dp(14).toFloat()
        strokeWidth = context.dp(1)
        strokeColor = ContextCompat.getColor(context, R.color.line)
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
        setContentPadding(0, 0, 0, 0)
    }

    fun bind(
        icon: Int,
        tone: IconTone,
        label: String,
        value: String,
        unit: String = "",
        sub: String = "",
    ) {
        binding.scIcon.setImageResource(icon)
        binding.scIcon.setBackgroundResource(tone.background)
        binding.scIcon.imageTintList =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, tone.color))
        binding.scLabel.text = label
        binding.scValue.text = value
        if (unit.isBlank()) {
            binding.scUnit.visibility = GONE
        } else {
            binding.scUnit.visibility = VISIBLE
            binding.scUnit.text = unit
        }
        binding.scSub.text = sub
    }
}
