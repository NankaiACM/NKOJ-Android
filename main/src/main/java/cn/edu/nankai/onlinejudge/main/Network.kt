package cn.edu.nankai.onlinejudge.main

import android.content.Context
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.net.CookiePolicy

class Network(nothing: Nothing) {
    companion object {
        private lateinit var mLogger: HttpLoggingInterceptor
        private var mInstance: OkHttpClient? = null
        fun getInstance(context: Context): OkHttpClient {
            if (mInstance == null) {
                val cookieManager = CookieManager()
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
                mLogger = HttpLoggingInterceptor()
                mLogger.level = HttpLoggingInterceptor.Level.BODY
                mInstance = OkHttpClient.Builder().cookieJar(JavaNetCookieJar(cookieManager)).followRedirects(false).addInterceptor(mLogger).addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                            .addHeader("Accept", "application/json;q=0.9,image/webp,image/apng,*/*;q=0.8")
                            .addHeader("Accept-Encoding", "gzip, deflate")
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 AndroidApp")
                            .addHeader("X-Requested-With", "Android App")
                            .addHeader("Cache-Control", "max-age=0")
                            .build()
                    chain.proceed(request)
                }.build()
            }
            return mInstance!!
        }
    }
}