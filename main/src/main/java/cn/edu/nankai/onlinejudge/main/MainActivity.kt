package cn.edu.nankai.onlinejudge.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    override fun onClick(v: View?) {
        if (v?.id == R.id.nav_header) {
            when (isLoggedIn) {
                true -> startActivity(Intent(this, UserInfoActivity::class.java))
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

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if(shouldFinishActivity)
                super.onBackPressed()
            else {
                shouldFinishActivity = true
                Toast.makeText(this,R.string.click_again_to_finish, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ shouldFinishActivity = false }, 2000)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_LOGIN -> {
                when (resultCode) {
                    RESULT_LOGIN_SUCCESS -> {
                        Toast.makeText(this@MainActivity, "登录成功", Toast.LENGTH_SHORT).show()


                    }
                    RESULT_REGISTER_SUCCESS -> {
                        Toast.makeText(this@MainActivity, "注册成功", Toast.LENGTH_SHORT).show()
                        val nickname = data?.extras?.getString("nickname")
                        val email = data?.extras?.getString("email")

                        val header = nav_view.getHeaderView(0)

                        header.findViewById<TextView>(R.id.nav_nickname).text = nickname
                        header.findViewById<TextView>(R.id.nav_email).text = email
                    }
                    else -> Toast.makeText(this@MainActivity, "未知问题或用户取消($resultCode)", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun switchLoginState(isLogin: Boolean = true) {
        isLoggedIn = isLogin
        val header = nav_view.getHeaderView(0)
        if (isLoggedIn) header.setOnClickListener { } else header.setOnClickListener { }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {

            }
            R.id.nav_problem -> {

            }
            R.id.nav_contest -> {

            }
            R.id.nav_discuss -> {

            }
            R.id.nav_open_source -> {

            }
            R.id.nav_about -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        val REQUEST_LOGIN = 0x20
        val RESULT_REGISTER_SUCCESS = 0x22
        val RESULT_LOGIN_SUCCESS = 0x23
    }
}
