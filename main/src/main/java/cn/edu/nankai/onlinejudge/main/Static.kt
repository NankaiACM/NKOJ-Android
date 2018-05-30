package cn.edu.nankai.onlinejudge.main

import android.text.TextUtils

class Static {
    companion object {

        private const val BASE_METHOD = "http:"
        private const val BASE_HOST = "acm.nankai.edu.cn"
        const val URL_CAPTCHA = "/api/captcha"
        const val URL_LOGIN = "/api/user/login"
        const val URL_REGISTER = "/api/user/register"
        const val URL_VERIFY_EMAIL = "/api/user/verify"
        const val URL_GET_KEY = "/api/user/key"
        const val URL_APPLY_KEY = "/api/user/key/apply/Android+Api+Key"
        const val URL_DELETE_KEY = "/api/user/key/remove"
        const val URL_USER_INFO = "/api/user/"
        const val URL_USER_AVATAR = "/api/avatar"
        const val URL_LIST_PROBLEM = "/api/problems/list"

        fun getUrl(target: String, array: Array<String>? = null): String {
            return "$BASE_METHOD//$BASE_HOST$target${if (array == null) "" else "/${TextUtils.join("/", array)}"}"
        }

        var API_KEY: String? = null
        var API_SECRET: String? = null

        var user_nickname: String? = null
        var user_email: String? = null
        var user_id: Int? = null

        fun getAPIUrl(target: String, array: Array<String>? = null): String {
            return "$BASE_METHOD//$BASE_HOST$target${if (array == null) "" else "/${TextUtils.join("/", array)}"}?akey=$API_KEY&asecret=$API_SECRET"
        }

    }
}