package cn.edu.nankai.onlinejudge.main

import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_USER_AVATAR
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_USER_INFO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, Callback {

    override fun onFailure(call: Call?, e: IOException?) {
        val tag = call?.request()?.tag() as String
        runOnUiThread {
            Toast.makeText(this, "网络连接失败 ${e?.message}($tag)", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResponse(call: Call?, response: Response?) {
        val tag = call?.request()?.tag() as String
        val isJson = response?.header("Content-Type")?.contains("json", true) == true
        val jsonResponse = if (isJson) JSONObject(response?.body()?.string()) else JSONObject()
        val jsonCode = if (isJson) jsonResponse.optInt("code") else -1
        val jsonSuccess = if (isJson) jsonCode == 0 else false
        val jsonBodyArray = if (jsonSuccess) jsonResponse.optJSONArray("data") else null
        val jsonBodyObject = if (jsonSuccess) jsonResponse.optJSONObject("data") else null
        val jsonError = if (isJson) jsonResponse.optJSONArray("error") else null
        val jsonMessage = if (isJson) jsonResponse.optString("message") else null

        when (tag) {
            HTTPREQ_USER_INFO -> {
                when {
                    jsonSuccess -> {
                        Static.user_nickname = jsonBodyObject?.getString("nickname")
                        Static.user_email = jsonBodyObject?.getString("email")
                        Static.user_id = jsonBodyObject?.getInt("user_id")

                        runOnUiThread {
                            Toast.makeText(this, "登录成功~", Toast.LENGTH_LONG).show()
                            nav_header.findViewById<TextView>(R.id.nav_email).text = Static.user_email
                            nav_header.findViewById<TextView>(R.id.nav_nickname).text = Static.user_nickname
                            isLoggedIn = true
                        }

                        Network.getInstance(applicationContext).newCall(
                                Request.Builder().url(Static.getUrl(URL_USER_AVATAR, arrayOf(Static.user_id.toString()))).tag(HTTPREQ_USER_AVATAR).build()
                        ).enqueue(this)
                    }
                    jsonCode == 401 -> {
                        runOnUiThread {
                            Toast.makeText(this, "登录信息失效，请重试 ${jsonMessage}($jsonCode)", Toast.LENGTH_LONG).show()
                        }
                        getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().apply()
                        isLoggedIn = false
                    }
                    else -> runOnUiThread {
                        Toast.makeText(this, "未知的服务器返回: ${jsonMessage}($jsonCode)", Toast.LENGTH_LONG).show()
                    }
                }
            }
            HTTPREQ_USER_AVATAR -> {
                val bytes = response?.body()?.bytes()
                runOnUiThread {
                    nav_header.findViewById<ImageView>(R.id.nav_avatar).setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size))
                }
            }
            HTTPREQ_PROBLEM_DETAIL -> {
                val pdf = ProblemDetailFragment()
                val bundle = Bundle()
                bundle.putString("problem", jsonBodyObject?.toString())
                pdf.arguments = bundle
                runOnUiThread {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, pdf).addToBackStack(null).commit()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.nav_header) {
            when (isLoggedIn) {
                true -> startActivityForResult(Intent(this, UserInfoActivity::class.java), REQUEST_INFO)
                else -> startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_LOGIN)
            }
        }
    }

    private var isLoggedIn = false
    private var shouldFinishActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.itemIconTintList = null
        nav_view.setNavigationItemSelectedListener(this)

        nav_view.getHeaderView(0).setOnClickListener(this)

        if (Static.API_KEY != null) {
            Network.getInstance(applicationContext).newCall(
                    Request.Builder().url(Static.getAPIUrl(URL_USER_INFO)).tag(HTTPREQ_USER_INFO).build()).enqueue(this)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if (shouldFinishActivity)
                super.onBackPressed()
            else {
                shouldFinishActivity = true
                Toast.makeText(this, R.string.click_again_to_finish, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ shouldFinishActivity = false }, 2000)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_LOGIN -> {
                when (resultCode) {
                    RESULT_LOGIN_SUCCESS -> {
                        if (data == null || data.extras == null) {
                            Toast.makeText(this@MainActivity, "未收到所需信息，请重试", Toast.LENGTH_LONG).show()
                            return
                        }
                        Static.API_KEY = data.extras.getString("key")
                        Static.API_SECRET = data.extras.getString("secret")
                        getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                                .putString("key", Static.API_KEY).putString("secret", Static.API_SECRET).apply()

                        Network.getInstance(applicationContext).newCall(
                                Request.Builder().url(Static.getUrl(URL_USER_INFO)).tag(HTTPREQ_USER_INFO).build()
                        ).enqueue(this)
                    }
                    else -> Toast.makeText(this@MainActivity, "未知问题或用户取消($resultCode)", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_INFO -> {
                Toast.makeText(this@MainActivity, "Hello Info($resultCode)", Toast.LENGTH_LONG).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        // TODO: popbackstack will cause CRASH on some of the Fragments...
        // So may be we have to find better ways to manage them
        val al = supportFragmentManager.fragments
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        var fm = supportFragmentManager.beginTransaction()
        when (item.itemId) {
            R.id.nav_home -> {
                fm.replace(R.id.fragment_container, HomeFragment())
            }
            R.id.nav_problem -> {
                fm.replace(R.id.fragment_container, ProblemFragment())
            }
            R.id.nav_contest -> {
                fm.replace(R.id.fragment_container, ContestFragment())
            }
            R.id.nav_discuss -> {
                fm.replace(R.id.fragment_container, DiscussFragment())
            }
            R.id.nav_open_source -> {
                fm.replace(R.id.fragment_container, OpenSourceFragment())
            }
            R.id.nav_about -> {
                fm.replace(R.id.fragment_container, AboutFragment())
            }
        }
        fm.commitNowAllowingStateLoss()
        fm = supportFragmentManager.beginTransaction()
        for (frag in al)
            fm.remove(frag)
        fm.commitAllowingStateLoss()
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        const val REQUEST_LOGIN = 0x20
        const val REQUEST_INFO = 0x18

        const val RESULT_LOGIN_SUCCESS = 0x23

        const val HTTPREQ_USER_INFO = "UserInfo"
        const val HTTPREQ_USER_AVATAR = "UserAvatar"

        const val HTTPREQ_PROBLEM_DETAIL = "ProblemDetail"
    }
}
