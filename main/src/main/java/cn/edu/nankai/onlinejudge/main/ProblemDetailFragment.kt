package cn.edu.nankai.onlinejudge.main


import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.json.JSONObject

class ProblemDetailFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_problem_detail, container, false)

        val bundle = savedInstanceState ?: arguments
        if (bundle?.getString("problem") == null) return v

        val problemJson = JSONObject(bundle.getString("problem"))
        val pid = problemJson.optInt("problem_id")
        val title = problemJson.optString("title")
        val ac = problemJson.optInt("ac")
        val all = problemJson.optInt("all")
        val special_judge = problemJson.optBoolean("special_judge")
        val detail_judge = problemJson.optBoolean("detail_judge")
        val cases = problemJson.optInt("cases")
        val time_limit = problemJson.optInt("time_limit")
        val memory_limit = problemJson.optInt("memory_limit")
        val level = problemJson.optInt("level")
        val tags = problemJson.optJSONArray("tags")
        val content = problemJson.optJSONObject("content")

        val description = content.optString("description")
        val input = content.optString("input")
        val output = content.optString("output")
        val sample_input = content.optString("sample_input")
        val sample_output = content.optString("sample_output")
        val hint = content.optString("hint")

        v.findViewById<TextView>(R.id.pid).text = pid.toString()
        v.findViewById<TextView>(R.id.desc).text = description
        v.findViewById<TextView>(R.id.desc_input).text = input
        v.findViewById<TextView>(R.id.desc_output).text = output
        v.findViewById<TextView>(R.id.samp_input).text = sample_input
        v.findViewById<TextView>(R.id.samp_output).text = sample_output
        v.findViewById<TextView>(R.id.hint).text = hint

        activity?.findViewById<FloatingActionButton>(R.id.fab)?.show()

        return v
    }
}
