package com.mzlhsc.orpl.admin.util

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.ApiException
import com.mzlhsc.orpl.admin.data.Prefs
import com.mzlhsc.orpl.admin.ui.login.LoginActivity
import kotlinx.coroutines.launch

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.toast(message: CharSequence?) {
    if (message.isNullOrBlank()) return
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.show() {
    if (visibility != View.VISIBLE) visibility = View.VISIBLE
}

fun View.hide() {
    if (visibility != View.GONE) visibility = View.GONE
}

fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun Fragment.toast(message: CharSequence?) {
    context?.toast(message)
}

/** 统一的 401 处理：清理本地登录态并回到登录页 */
fun Fragment.handleError(error: Throwable) {
    val message = error.message ?: "操作失败"
    if (error is ApiException && error.unauthorized) {
        Prefs.clearSession()
        toast(message)
        val ctx = context ?: return
        val intent = Intent(ctx, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    } else {
        toast(message)
    }
}

/** 页面内发起异步请求：自动捕获异常并提示 */
fun Fragment.safeLaunch(block: suspend () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            block()
        } catch (e: Exception) {
            if (isAdded) handleError(e)
        }
    }
}

fun androidx.appcompat.app.AppCompatActivity.safeLaunch(block: suspend () -> Unit) {
    lifecycleScope.launch {
        try {
            block()
        } catch (e: Exception) {
            toast(e.message ?: "操作失败")
        }
    }
}

fun Fragment.requireDp(value: Int): Int = requireContext().dp(value)
