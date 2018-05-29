package cn.edu.nankai.onlinejudge.main

import android.text.TextUtils

class Static {
    companion object {
        val BASE_METHOD = "http:"
        val BASE_HOST = "acm.nankai.edu.cn"
        val URL_CAPTCHA = "/api/captcha"
        val URL_LOGIN = "/api/user/login"
        val URL_REGISTER = "/api/user/register"
        val URL_VERIFY_EMAIL = "/api/user/verify"

        fun getURL(target: String, array: Array<String>? = null): String {
            return "$BASE_METHOD//$BASE_HOST$target${if (array == null) "" else "/${TextUtils.join("/", array)}"}"
        }
    }
}