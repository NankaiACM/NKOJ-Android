package cn.edu.nankai.onlinejudge.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_CAPTCHA
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_LOGIN
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_REGISTER
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_VERIFY_EMAIL
import cn.edu.nankai.onlinejudge.main.Static.Companion.getURL
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity(), okhttp3.Callback {

    var isLogin = false
    lateinit var mNickname: String
    lateinit var mEmail: String

    override fun onFailure(call: Call?, e: IOException?) {
        val tag = call?.request()?.tag() as String
        runOnUiThread {
            Toast.makeText(this, "在 $tag 请求时发生网络错误, 原因: ${e?.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResponse(call: Call?, response: Response?) {

        val tag = call?.request()?.tag() as String

        val isJson = response?.header("Content-Type")?.contains("json", true) == true
        val jsonResponse = if (isJson) JSONObject(response?.body()?.string()) else JSONObject()
        val jsonCode = if (isJson) jsonResponse.optInt("code") else -1
        val jsonSuccess = if (isJson) jsonCode == 0 else false
        val jsonBody = if (jsonSuccess) jsonResponse.optJSONObject("data") else null
        val jsonError = if (isJson) jsonResponse.optJSONArray("error") else null
        val jsonMessage = if (isJson) jsonResponse.optString("message") else null

        runOnUiThread {
            when {
                tag == HTTPREQ_CAPTCHA -> {
                    try {
                        val svg = SVG.getFromInputStream(response?.body()?.byteStream())
                        val pd = PictureDrawable(svg.renderToPicture())
                        val bitmap = Bitmap.createBitmap(pd.intrinsicWidth, pd.intrinsicHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawPicture(pd.picture)
                        captchaImage.setImageBitmap(bitmap)
                    } catch (e: SVGParseException) {
                        Toast.makeText(this, "验证码图片加载失败，也许是还没有登录网关？", Toast.LENGTH_LONG).show()
                    }
                }
                jsonSuccess -> {
                    when (tag) {
                        HTTPREQ_GET_ECODE -> Toast.makeText(this, "验证码发送成功，请检查邮箱", Toast.LENGTH_LONG).show()
                        HTTPREQ_REGISTER -> {
                            Toast.makeText(this, "注册成功了喵~", Toast.LENGTH_LONG).show()
                            val intent = Intent()
                            intent.putExtra("nickname", mNickname)
                            intent.putExtra("email", mEmail)
                            setResult(MainActivity.RESULT_REGISTER_SUCCESS, intent)
                            finish()
                        }
                        HTTPREQ_LOGIN -> {
                            Toast.makeText(this, "登录成功了喵~", Toast.LENGTH_LONG).show()
                            setResult(MainActivity.RESULT_LOGIN_SUCCESS)
                            finish()
                        }
                    }
                }
                jsonError != null -> {
                    loadCaptcha()
                    formErrorFromJson(jsonError)
                    Toast.makeText(this, "服务器返回: $jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
                }
                else -> Toast.makeText(this, "未知错误... $jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formErrorFromJson(json: JSONArray) {
        for (i in 0 until json.length()) {
            val id: Int = resources.getIdentifier(json.getJSONObject(i).optString("name"), "id", packageName)
            val editText = findViewById<EditText>(id) ?: continue
            editText.error = json.getJSONObject(i).optString("message")
        }
    }

    private fun switchLoginRegister() {
        if (!isLogin) {
            user.visibility = View.VISIBLE
            nickname.visibility = View.GONE
            email.visibility = View.GONE
            ecode_layout.visibility = View.GONE
            text_switch_login.text = getString(R.string.prompt_register_switch)
            register_button.visibility = View.GONE
            login_button.visibility = View.VISIBLE
            isLogin = true
        } else {
            user.visibility = View.GONE
            nickname.visibility = View.VISIBLE
            email.visibility = View.VISIBLE
            ecode_layout.visibility = View.VISIBLE
            text_switch_login.text = getString(R.string.prompt_login_switch)
            register_button.visibility = View.VISIBLE
            login_button.visibility = View.GONE
            isLogin = false
        }
        loadCaptcha()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ecode.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptRegister()
                return@OnEditorActionListener true
            }
            false
        })

        register_button.setOnClickListener { attemptRegister() }
        login_button.setOnClickListener { attemptLogin() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadCaptcha()
        captchaImage.setOnClickListener { loadCaptcha() }
        sendEmailBtn.setOnClickListener { verifyEmailRequest() }
        text_switch_login.setOnClickListener { switchLoginRegister() }
    }

    private fun loadCaptcha() {
        Network.getInstance(applicationContext).newCall(
                Request.Builder().url(Static.getURL(URL_CAPTCHA)).tag(HTTPREQ_CAPTCHA).build()
        ).enqueue(this)
    }

    private fun attemptLogin() {
        user.error = null
        password.error = null
        captcha.error = null

        val captchaStr = captcha.text.toString()
        val userStr = user.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(captchaStr)) {
            captcha.error = getString(R.string.error_field_required)
            focusView = captcha
            cancel = true
        } else if (captchaStr.length != 6) {
            captcha.error = getString(R.string.error_invalid_captcha)
            focusView = captcha
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            Network.getInstance(applicationContext).newCall(Request.Builder().url(getURL(URL_LOGIN)).post(
                    FormBody.Builder()
                            .add("captcha", captchaStr)
                            .add("user", userStr)
                            .add("password", passwordStr).build()
            ).tag(HTTPREQ_LOGIN).build()).enqueue(this)
        }
    }

    private fun verifyEmailRequest() {
        email.error = null
        captcha.error = null

        val emailStr = email.text.toString()
        val captchaStr = captcha.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (TextUtils.isEmpty(captchaStr)) {
            captcha.error = getString(R.string.error_field_required)
            focusView = captcha
            cancel = true
        } else if (captchaStr.length != 6) {
            captcha.error = getString(R.string.error_invalid_captcha)
            focusView = captcha
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            Network.getInstance(applicationContext).newCall(
                    Request.Builder()
                            .url(getURL(URL_VERIFY_EMAIL, arrayOf("$emailStr?captcha=$captchaStr")))
                            .get().tag(HTTPREQ_GET_ECODE).build()
            ).enqueue(this)
        }

    }

    private fun attemptRegister() {

        nickname.error = null
        email.error = null
        password.error = null
        ecode.error = null

        val nicknameStr = nickname.text.toString()
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()
        val ecodeStr = ecode.text.toString()

        var cancel = false
        var focusView: View? = null


        if (TextUtils.isEmpty(nicknameStr)) {
            nickname.error = getString(R.string.error_field_required)
            focusView = nickname
            cancel = true
        } else if (!isNicknameValid(nicknameStr)) {
            nickname.error = getString(R.string.error_invalid_nickname)
            focusView = nickname
            cancel = true
        }

        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (TextUtils.isEmpty(ecodeStr)) {
            ecode.error = getString(R.string.error_field_required)
            focusView = ecode
            cancel = true
        } else if (!isEmailValid(ecodeStr)) {
            ecode.error = getString(R.string.error_invalid_captcha)
            focusView = ecode
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            mEmail = emailStr
            mNickname = nicknameStr
            Network.getInstance(applicationContext).newCall(Request.Builder().url(getURL(URL_REGISTER)).post(
                    FormBody.Builder()
                            .add("nickname", nicknameStr)
                            .add("email", emailStr)
                            .add("password", passwordStr)
                            .add("ecode", ecodeStr).build()
            ).tag(HTTPREQ_REGISTER).build()).enqueue(this)
        }
    }

    private fun isNicknameValid(nickname: String): Boolean {
        return nickname.length in 3..20
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length in 6..16
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                setResult(0x20)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val HTTPREQ_CAPTCHA = "验证码"
        const val HTTPREQ_LOGIN = "登录"
        const val HTTPREQ_REGISTER = "注册"
        const val HTTPREQ_VERIFY_EMAIL = "验证邮箱"
        const val HTTPREQ_VERIFY_NICKNAME = "验证昵称"
        const val HTTPREQ_GET_ECODE = "获得邮箱验证码"
    }
}
