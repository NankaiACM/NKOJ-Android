package cn.edu.nankai.onlinejudge.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.MainActivity.Companion.HTTPREQ_STATUS_DETAIL
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_LIST_STATUS
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_STATUS_DETAIL
import cn.edu.nankai.onlinejudge.main.Static.Companion.getUrl
import kotlinx.android.synthetic.main.fragment_status.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StatusFragment : Fragment(), Callback {

    private lateinit var mActivity: MainActivity
    private lateinit var mNetwork: OkHttpClient
    private lateinit var mAdapter: StatusAdapter
    private lateinit var mDataset: JSONArray
    private var shouldLoadMore: Boolean = true


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
        val jsonBodyArray = if (jsonSuccess) jsonResponse.optJSONArray("data") else null
        val jsonMessage = if (isJson) jsonResponse.optString("message") else null

        if (jsonSuccess)
            when (tag) {
                HTTPREQ_LIST_STATUS -> {
                    mActivity.runOnUiThread {
                        mDataset = jsonBodyArray!!
                        mAdapter = StatusAdapter()
                        view?.findViewById<RecyclerView>(R.id.status_rec)?.adapter = mAdapter
                    }
                }
                HTTPREQ_LOAD_MORE -> {
                    mDataset.remove(mDataset.length() - 1)
                    mActivity.runOnUiThread {
                        mAdapter.notifyItemRemoved(mDataset.length())
                    }

                    for (i in 0 until jsonBodyArray!!.length()) {
                        mDataset.put(jsonBodyArray[i])
                    }

                    mActivity.runOnUiThread {
                        mAdapter.notifyDataSetChanged()
                    }
                    mAdapter.isLoading = false
                }
            }
        else if (isJson)
            mActivity.runOnUiThread {
                Toast.makeText(mActivity, "服务器返回: $jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
            }
        else
            shouldLoadMore = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_status, container, false)
        mActivity = activity as MainActivity
        val mDividerItemDecoration = DividerItemDecoration(mActivity, View.LAYOUT_DIRECTION_LTR)
        v.findViewById<RecyclerView>(R.id.status_rec).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(mActivity)
            addItemDecoration(mDividerItemDecoration)
        }
        mNetwork = Network.getInstance(mActivity)
        mNetwork.newCall(Request.Builder().url(getUrl(URL_LIST_STATUS)).tag(HTTPREQ_LIST_STATUS).build()).enqueue(this)

        return v
    }

    inner class StatusAdapter :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val mOnClickListener = ItemListener()
        var totalItemCount: Int = 0
        var lastVisibleItem: Int = 0
        var isLoading: Boolean = false
        val visibleThreshold = 15

        init {
            if (status_rec == null) {
                throw Error()
            } else {
                val linearLayoutManager = status_rec.layoutManager as LinearLayoutManager
                status_rec?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        totalItemCount = linearLayoutManager.itemCount
                        lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                        if (!isLoading && shouldLoadMore && totalItemCount <= lastVisibleItem + visibleThreshold) {
                            loadMore()
                            isLoading = true
                        }
                    }
                })
            }
        }

        inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val statusID = view.findViewById<TextView>(R.id.status_id)
            val judgeResult = view.findViewById<TextView>(R.id.judge_result)
            val statusTime = view.findViewById<TextView>(R.id.status_time)
            val statusMemory = view.findViewById<TextView>(R.id.status_memory)
            val nickname = view.findViewById<TextView>(R.id.status_nickname)
        }

        inner class LoadingViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val loader = view.findViewById<ProgressBar>(R.id.loading_progress)
        }

        override fun getItemViewType(position: Int): Int {
            return if (mDataset.opt(position) == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_ITEM) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status, parent, false)
                StatusViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is StatusViewHolder) {
                val which = mDataset.getJSONObject(position)
                holder.statusID.text = "${which.optInt("solution_id")} -> ${which.optInt("problem_id")}"
                holder.judgeResult.text = which.optString("msg_cn")
                holder.statusTime.text = "${which.optInt("time")} 毫秒"
                holder.statusMemory.text = "${which.optInt("memory")} 字节"
                holder.nickname.text = which.optString("nickname")
                holder.view.tag = which.optInt("solution_id").toString()
                holder.view.setOnClickListener(mOnClickListener)
            } else if (holder is LoadingViewHolder) {
                holder.loader.isIndeterminate = true;
            }
        }

        override fun getItemCount() = mDataset.length()

        inner class ItemListener : View.OnClickListener {
            override fun onClick(v: View?) {
                val pid = v?.tag as String
                mNetwork.newCall(
                        Request.Builder().url(getUrl(URL_STATUS_DETAIL, arrayOf(pid))).tag(HTTPREQ_STATUS_DETAIL).build()
                ).enqueue(mActivity)
            }
        }

        fun loadMore() {
            Handler().post {
                mDataset.put(null)
                mAdapter.notifyItemInserted(mDataset.length() - 1)
                mNetwork.newCall(
                        Request.Builder().url(getUrl(URL_LIST_STATUS, arrayOf((mDataset.length() - 2).toString(), "20"))).tag(HTTPREQ_LOAD_MORE).build()
                ).enqueue(this@StatusFragment)
            }
        }
    }

    companion object {
        const val HTTPREQ_LIST_STATUS = "ListStatus"
        const val HTTPREQ_LOAD_MORE = "LoadMore"

        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
