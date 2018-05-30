package cn.edu.nankai.onlinejudge.main


import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_LIST_PROBLEM
import cn.edu.nankai.onlinejudge.main.Static.Companion.getUrl
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProblemFragment : Fragment(), Callback {
    override fun onFailure(call: Call?, e: IOException?) {
        val tag = call?.request()?.tag()
        mActivity.runOnUiThread {
            Toast.makeText(mActivity, "网络请求失败$tag(${e?.message})", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResponse(call: Call?, response: Response?) {
        val tag = call?.request()?.tag() as String
        val isJson = response?.header("Content-Type")?.contains("json", true) == true
        val jsonResponse = if (isJson) JSONObject(response?.body()?.string()) else JSONObject()
        val jsonCode = if (isJson) jsonResponse.optInt("code") else -1
        val jsonSuccess = if (isJson) jsonCode == 0 else false
        val jsonBodyObject = if (jsonSuccess) jsonResponse.optJSONObject("data") else null
        val jsonBodyArray = if (jsonSuccess) jsonBodyObject?.optJSONArray("list") else null
        val jsonMessage = if (isJson) jsonResponse.optString("message") else null

        if (jsonSuccess)
            mActivity.runOnUiThread {
                view.findViewById<RecyclerView>(R.id.problem_rec).adapter = ProblemAdapter(jsonBodyArray!!)
            }
        else
            mActivity.runOnUiThread {
                Toast.makeText(mActivity, "服务器返回: $jsonMessage(${jsonCode})", Toast.LENGTH_LONG).show()
            }
    }

    private lateinit var mActivity: Activity
    private lateinit var mNetwork: OkHttpClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_problem, container, false)
        mActivity = activity
        val mDividerItemDecoration = DividerItemDecoration(mActivity, View.LAYOUT_DIRECTION_LTR)
        v.findViewById<RecyclerView>(R.id.problem_rec).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(mActivity)
            addItemDecoration(mDividerItemDecoration)
        }
        mNetwork = Network.getInstance(mActivity)
        mNetwork.newCall(Request.Builder().url(getUrl(URL_LIST_PROBLEM)).tag(HTTPREQ_LIST_PROBLEM).build()).enqueue(this)
        return v
    }

    inner class ProblemAdapter(private val myDataset: JSONArray) :
            RecyclerView.Adapter<ProblemAdapter.ViewHolder>() {

        val mOnClickListener = ItemListener()

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemAdapter.ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_problem, parent, false) as View
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val which = myDataset.getJSONObject(position)
            holder.view.findViewById<TextView>(R.id.problem_id).text = which.optInt("problem_id").toString()
            holder.view.findViewById<TextView>(R.id.problem_name).text = which.optString("title")
            holder.view.findViewById<TextView>(R.id.time_limit).text = which.optInt("time_limit").toString() + "秒"
            holder.view.findViewById<TextView>(R.id.memory_limit).text = which.optInt("memory_limit").toString() + "千字节"
            holder.view.findViewById<TextView>(R.id.extra_info).text = "${which.optInt("ac")}/${which.optInt("all")}"
            holder.view.setOnClickListener(mOnClickListener)
        }

        override fun getItemCount() = myDataset.length()

        inner class ItemListener : View.OnClickListener {
            override fun onClick(v: View?) {

            }
        }
    }

    companion object {
        const val HTTPREQ_LIST_PROBLEM = "ListProblem"
    }
}
