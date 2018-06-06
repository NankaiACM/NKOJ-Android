package cn.edu.nankai.onlinejudge.main


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.TextView
import org.json.JSONObject
import thereisnospon.codeview.CodeView

class StatusDetailFragment : Fragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_status_detail, container, false)

        val bundle = savedInstanceState ?: arguments
        if (bundle?.getString("status") == null) return v

        val statusJson = JSONObject(bundle.getString("status"))
        val sid = statusJson.optInt("solution_id")
        val pid = statusJson.optString("problem_id")
        val code_size = statusJson.optInt("code_size")
        val time = statusJson.optInt("time")
        val memory = statusJson.optInt("memory")
        val msg_en = statusJson.optString("msg_en")
        val msg_cn = statusJson.optString("msg_cn")
        val nickname = statusJson.optString("nickname")
        val compile_info = statusJson.optString("compile_info")
        val code = statusJson.optString("code")

        v.findViewById<TextView>(R.id.sta_id).text = "$sid --> $pid"
        v.findViewById<TextView>(R.id.sta_result).text = "$msg_cn ($msg_en)"
        v.findViewById<TextView>(R.id.sta_time_memory_codesize).text = "Time: $time ms, Memory: $memory kb, Code: $code_size bytes"
        Log.e("debug", code)
        v.findViewById<TextView>(R.id.sta_compile_info).text = compile_info
        v.findViewById<TextView>(R.id.sta_compile_info).isSelected = true

        v.findViewById<CodeView>(R.id.sta_code).showCode(code)
       // v.findViewById<CodeView>(R.id.sta_code).settings.textSize = WebSettings.TextSize.SMALLEST
        //v.findViewById<TextView>(R.id.sta_code).isSelected = true
        return v
    }
}
