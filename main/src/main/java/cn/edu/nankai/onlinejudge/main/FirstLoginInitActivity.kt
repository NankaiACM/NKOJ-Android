package cn.edu.nankai.onlinejudge.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_APPLY_KEY
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_DELETE_KEY
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_GET_KEY
import kotlinx.android.synthetic.main.activity_first_login_init.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UNCHECKED_CAST")
class FirstLoginInitActivity : AppCompatActivity(), Callback {

    private val keyArray = ArrayList<String>()

    override fun onFailure(call: Call?, e: IOException?) {
        val tag = call?.request()?.tag() as String
        Toast.makeText(applicationContext, "网络连接失败，请重新尝试 ${e?.message}($tag)", Toast.LENGTH_LONG).show()
        setResult(-1)
        finish()
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
            HTTPREQ_APPLYKEY -> {
                when (jsonCode) {
                    0 -> {
                        val key = jsonBodyObject?.optString("key")
                        val secret = jsonBodyObject?.optString("secret")
                        runOnUiThread {
                            val intent = Intent()
                            intent.putExtra("key", key)
                            intent.putExtra("secret", secret)
                            setResult(0x22, intent)
                            finish()
                        }
                    }
                    1 -> {
                        mNetwork.newCall(Request.Builder().tag(HTTPREQ_GETKEY).url(Static.getUrl(URL_GET_KEY)).build()).enqueue(this)
                    }
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this, "未知服务器返回$jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
                            getkey_progress.visibility = View.INVISIBLE
                        }
                    }
                }
            }
            HTTPREQ_GETKEY -> {
                when (jsonCode) {
                    0 -> {
                        runOnUiThread {
                            key_recycler.adapter = ApiKeyAdapter(jsonBodyArray!!)
                            key_recycler.visibility = View.VISIBLE
                            hint_message.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this, "未知服务器返回$jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
                            getkey_progress.visibility = View.INVISIBLE
                        }
                    }
                }
            }
            HTTPREQ_DELETEKEY -> {
                runOnUiThread {
                    hint_message.visibility = View.INVISIBLE
                    key_recycler.visibility = View.INVISIBLE
                }
                applyNewKey()
            }
        }
    }

    private fun applyNewKey() {
        mNetwork.newCall(Request.Builder().tag(HTTPREQ_APPLYKEY).url(Static.getUrl(URL_APPLY_KEY)).build()).enqueue(this)
    }

    private val mHideHandler = Handler()
    private lateinit var mNetwork: OkHttpClient
    private val mHidePart2Runnable = Runnable {
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }

    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_login_init)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mVisible = true
        dummy_button.setOnTouchListener(mDelayHideTouchListener)
        mNetwork = Network.getInstance(applicationContext)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide(100)
        applyNewKey()
        val mDividerItemDecoration = DividerItemDecoration(key_recycler.context, View.LAYOUT_DIRECTION_LTR)
        key_recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@FirstLoginInitActivity)
            addItemDecoration(mDividerItemDecoration)
        }
    }

    private fun hide() {
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }


    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        private const val AUTO_HIDE = true
        private const val AUTO_HIDE_DELAY_MILLIS = 0
        private const val UI_ANIMATION_DELAY = 300

        private const val HTTPREQ_GETKEY = "GetKey"
        private const val HTTPREQ_APPLYKEY = "ApplyKey"
        private const val HTTPREQ_DELETEKEY = "DeleteKey"

        const val TYPE_DELETE = "delete"
        const val TYPE_ENTER = "enter"
    }

    inner class ApiKeyAdapter(private val myDataset: JSONArray) :
            RecyclerView.Adapter<ApiKeyAdapter.ViewHolder>() {

        val mOnClickListener = ItemListener()

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiKeyAdapter.ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_api_key, parent, false) as View
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val which = myDataset.getJSONObject(position)
            val key = which.optString("api_key")
            val name = which.optString("api_name") ?: "未命名"
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(which.optString("since"))
            val since = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA).format(date);

            holder.view.findViewById<TextView>(R.id.api_key).text = key
            holder.view.findViewById<TextView>(R.id.api_name).text = name
            holder.view.findViewById<TextView>(R.id.api_since).text = since
            holder.view.findViewById<TextView>(R.id.api_delete).tag = arrayOf(TYPE_DELETE, key)
            holder.view.findViewById<TextView>(R.id.api_delete).setOnClickListener(mOnClickListener)
            holder.view.findViewById<TextView>(R.id.api_enter).tag = arrayOf(TYPE_ENTER, key)
            holder.view.findViewById<TextView>(R.id.api_enter).setOnClickListener(mOnClickListener)
        }

        override fun getItemCount() = myDataset.length()

        inner class ItemListener() : View.OnClickListener {
            override fun onClick(v: View?) {
                val array = v?.tag as Array<String>
                when (array[0]) {
                    TYPE_DELETE -> {
                        mNetwork.newCall(
                                Request.Builder().url(Static.getUrl(URL_DELETE_KEY, arrayOf(array[1]))).tag(HTTPREQ_DELETEKEY).build()
                        ).enqueue(this@FirstLoginInitActivity)
                    }
                    TYPE_ENTER -> {
                        // TODO...
                    }
                }

            }
        }
    }
}
