package cn.edu.nankai.onlinejudge.main


import android.graphics.Color
import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yydcdut.sdlv.Menu
import com.yydcdut.sdlv.MenuItem
import com.yydcdut.sdlv.SlideAndDragListView
import kotlinx.android.synthetic.main.fragment_home.*



class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        return view
    }
}
