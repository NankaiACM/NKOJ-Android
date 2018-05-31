package cn.edu.nankai.onlinejudge.main

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ProblemSubmitFragment : Fragment(), Callback {
    override fun onFailure(call: Call?, e: IOException?) {
        val tag = call?.request()?.tag() as String
        mActivity.runOnUiThread {
            Toast.makeText(mActivity, "在 $tag 请求时发生网络错误, 原因: ${e?.message}($tag)", Toast.LENGTH_LONG).show()
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

        if (jsonSuccess) {
            mActivity.runOnUiThread {
                Toast.makeText(mActivity, "提交成功！ Solution ID 是: ${jsonBody?.optInt("solution_id")}", Toast.LENGTH_LONG).show()
            }
        } else {
            mActivity.runOnUiThread {
                Toast.makeText(mActivity, "提交失败, 原因: $jsonMessage($jsonCode)", Toast.LENGTH_LONG).show()
            }
        }
    }

    var mPID = 0
    lateinit var mActivity: Activity
    lateinit var mCode: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_problem_submit, container, false)
        val argv = savedInstanceState ?: arguments
        mActivity = activity as Activity
        mActivity.findViewById<FloatingActionButton>(R.id.fab).hide()
        if (argv != null) {
            mCode = v.findViewById(R.id.text_code)
            mPID = argv.getInt("pid")
            v.findViewById<TextView>(R.id.text_pid).text = mPID.toString()
            v.findViewById<Button>(R.id.btn_submit).setOnClickListener {
                Network.getInstance(activity?.applicationContext).newCall(
                        Request.Builder().url(Static.getAPIUrl(Static.URL_SUBMIT_CODE)).post(
                                FormBody.Builder().add("pid", mPID.toString()).add("lang", "1").add("code", mCode.text.toString()).build()
                        ).tag(HTTPREQ_SUBMIT_CODE).build()
                ).enqueue(this)
            }
        }
        return v
    }

    companion object {
        const val HTTPREQ_SUBMIT_CODE = "SubmitCode"
    }
}
