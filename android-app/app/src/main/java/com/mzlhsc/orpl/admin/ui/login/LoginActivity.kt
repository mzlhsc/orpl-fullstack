package com.mzlhsc.orpl.admin.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.ApiException
import com.mzlhsc.orpl.admin.data.Prefs
import com.mzlhsc.orpl.admin.databinding.ActivityLoginBinding
import com.mzlhsc.orpl.admin.ui.main.MainActivity
import com.mzlhsc.orpl.admin.util.hide
import com.mzlhsc.orpl.admin.util.show
import com.mzlhsc.orpl.admin.util.toast
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Prefs.loggedIn) {
            goMain()
            return
        }

        binding.etServer.setText(Prefs.baseUrl)
        binding.tvToggleServer.setOnClickListener {
            if (binding.serverBox.visibility == View.VISIBLE) binding.serverBox.hide() else binding.serverBox.show()
        }
        binding.btnLogin.setOnClickListener { doLogin() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doLogin()
                true
            } else {
                false
            }
        }
    }

    private fun doLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (username.isEmpty() || password.isEmpty()) {
            showError("请输入用户名和密码")
            return
        }

        // 服务器地址即使折叠也保存，方便下次登录直接复用
        Prefs.baseUrl = binding.etServer.text.toString()
        binding.tvError.hide()
        setLoading(true)

        lifecycleScope.launch {
            try {
                val vo = AdminApi.login(username, password)
                val token = vo?.token
                if (vo == null || token.isNullOrBlank()) {
                    throw ApiException("登录返回异常：未获取到令牌")
                }
                Prefs.saveSession(vo.id ?: 0L, vo.userName, vo.name, token)
                toast("欢迎回来，${vo.name ?: vo.userName ?: "管理员"}")
                goMain()
            } catch (e: Exception) {
                showError(e.message ?: "登录失败")
                binding.etPassword.setText("")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "登录中…" else getString(R.string.login_action)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.show()
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
