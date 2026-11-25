package com.mzlhsc.orpl.admin.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.util.dp

/**
 * 极简柱状图（营业额趋势）。
 * 不引第三方图表库：手机端只画柱体 + 底部日期 + 淡色网格线，够用且首屏更快。
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.line_soft)
        strokeWidth = context.dp(1).toFloat()
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted_2)
        textSize = context.dp(10).toFloat()
        textAlign = Paint.Align.CENTER
    }
    private val maxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted)
        textSize = context.dp(10).toFloat()
        textAlign = Paint.Align.LEFT
    }

    private var labels: List<String> = emptyList()
    private var values: List<Float> = emptyList()

    private val brand = ContextCompat.getColor(context, R.color.brand)
    private val brandLight = ContextCompat.getColor(context, R.color.brand_light)

    fun setData(labels: List<String>, values: List<Float>) {
        this.labels = labels
        this.values = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty() || width == 0) return

        val labelHeight = context.dp(22).toFloat()
        val topPad = context.dp(10).toFloat()
        val chartBottom = height - labelHeight
        val chartTop = topPad
        val chartHeight = chartBottom - chartTop
        if (chartHeight <= 0) return

        // 网格线
        for (i in 0..2) {
            val y = chartTop + chartHeight * i / 2f
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val slot = width.toFloat() / values.size
        val barWidth = minOf(slot * 0.46f, context.dp(22).toFloat())
        val radius = context.dp(5).toFloat()

        values.forEachIndexed { index, value ->
            val centerX = slot * index + slot / 2f
            val ratio = (value / max).coerceIn(0f, 1f)
            val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0f) context.dp(4).toFloat() else 0f)
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val top = chartBottom - barHeight
            val rect = RectF(left, top, right, chartBottom)

            if (barHeight <= 0f) {
                barPaint.shader = null
                barPaint.color = ContextCompat.getColor(context, R.color.line)
                canvas.drawRoundRect(RectF(left, chartBottom - context.dp(2), right, chartBottom), radius, radius, barPaint)
            } else {
                barPaint.shader = LinearGradient(
                    left, top, left, chartBottom,
                    brandLight, brand,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRoundRect(rect, radius, radius, barPaint)
            }

            labels.getOrNull(index)?.let { label ->
                canvas.drawText(label, centerX, height - context.dp(6).toFloat(), labelPaint)
            }
        }

        barPaint.shader = null
        canvas.drawText("峰值 " + max.toInt(), context.dp(2).toFloat(), chartTop + context.dp(10), maxPaint)
    }
}
