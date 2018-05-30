package cn.edu.nankai.onlinejudge.main


import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import cn.edu.nankai.onlinejudge.main.Static.Companion.URL_LIST_PROBLEM
import cn.edu.nankai.onlinejudge.main.Static.Companion.getUrl
import kotlinx.android.synthetic.main.fragment_problem.*
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
            when (tag) {
                HTTPREQ_LIST_PROBLEM -> {
                    mActivity.runOnUiThread {
                        mDataset = jsonBodyArray!!
                        mAdapter = ProblemAdapter()
                        view.findViewById<RecyclerView>(R.id.problem_rec).adapter = mAdapter
                        shouldLoadMore = jsonBodyObject?.optBoolean("is_end") != true
                    }
                }
                HTTPREQ_LOAD_MORE -> {
                    shouldLoadMore = jsonBodyObject?.optBoolean("is_end") != true
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
        else
            mActivity.runOnUiThread {
                Toast.makeText(mActivity, "服务器返回: $jsonMessage(${jsonCode})", Toast.LENGTH_LONG).show()
            }
    }

    private lateinit var mActivity: Activity
    private lateinit var mNetwork: OkHttpClient
    private lateinit var mAdapter: ProblemAdapter
    private lateinit var mDataset: JSONArray
    private var shouldLoadMore: Boolean = true

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

    inner class ProblemAdapter() :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val mOnClickListener = ItemListener()
        var totalItemCount: Int = 0
        var lastVisibleItem: Int = 0
        var isLoading: Boolean = false
        val visibleThreshold = 15

        init {
            val linearLayoutManager = problem_rec.layoutManager as LinearLayoutManager
            problem_rec.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    totalItemCount = linearLayoutManager.getItemCount()
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                    if (!isLoading && shouldLoadMore && totalItemCount <= lastVisibleItem + visibleThreshold) {
                        loadMore()
                        isLoading = true
                    }
                }
            })
        }

        inner class ProblemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val problemID = view.findViewById<TextView>(R.id.problem_id)
            val title = view.findViewById<TextView>(R.id.problem_name)
            val timeLimit = view.findViewById<TextView>(R.id.time_limit)
            val memoryLimit = view.findViewById<TextView>(R.id.memory_limit)
            val extraInfo = view.findViewById<TextView>(R.id.extra_info)
        }

        inner class LoadingViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val loader = view.findViewById<ProgressBar>(R.id.loading_progress)
        }

        override fun getItemViewType(position: Int): Int {
            return if (mDataset.opt(position) == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_ITEM) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_problem, parent, false)
                ProblemViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val which = mDataset.getJSONObject(position)
            if (holder is ProblemViewHolder) {
                holder.problemID.text = which.optInt("problem_id").toString()
                holder.title.text = which.optString("title")
                holder.memoryLimit.text = which.optInt("memory_limit").toString() + "千字节"
                holder.timeLimit.text = which.optInt("time_limit").toString() + "秒"
                holder.extraInfo.text = "${which.optInt("ac")}/${which.optInt("all")}"
                holder.view.setOnClickListener(mOnClickListener)
            } else if (holder is LoadingViewHolder) {
                holder.loader.isIndeterminate = true;
            }
        }

        override fun getItemCount() = mDataset.length()

        inner class ItemListener : View.OnClickListener {
            override fun onClick(v: View?) {

            }
        }

        fun loadMore() {

            Handler().post {
                mDataset.put(null)
                mAdapter.notifyItemInserted(mDataset.length() - 1)
                mNetwork.newCall(
                        Request.Builder().url(getUrl(URL_LIST_PROBLEM, arrayOf(mDataset.length().toString()))).tag(HTTPREQ_LOAD_MORE).build()
                ).enqueue(this@ProblemFragment)
            }

        }
    }

    companion object {
        const val HTTPREQ_LIST_PROBLEM = "ListProblem"
        const val HTTPREQ_LOAD_MORE = "LoadMore"

        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
