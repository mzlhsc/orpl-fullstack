package com.mzlhsc.orpl.admin.ui.common

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.util.dp

data class ChipItem(val label: String, val value: Any? = null)

/** 横向筛选胶囊，对应 web 端的分类/状态筛选条 */
object Chips {

    fun render(
        container: LinearLayout,
        items: List<ChipItem>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
    ) {
        container.removeAllViews()
        items.forEachIndexed { index, item ->
            val context = container.context
            val selected = index == selectedIndex
            val chip = TextView(context).apply {
                text = item.label
                textSize = 12.5f
                gravity = Gravity.CENTER
                setPadding(context.dp(14), 0, context.dp(14), 0)
                setBackgroundResource(
                    if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (selected) R.color.brand_strong else R.color.muted
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    context.dp(34)
                ).apply { marginEnd = context.dp(8) }
                setOnClickListener { onSelect(index) }
            }
            container.addView(chip)
        }
        container.setBackgroundColor(Color.TRANSPARENT)
    }
}
