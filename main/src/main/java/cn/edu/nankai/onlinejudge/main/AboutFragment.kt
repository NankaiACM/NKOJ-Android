package cn.edu.nankai.onlinejudge.main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        val mImageViewPager = view.findViewById<ViewPager>(R.id.pager) as ViewPager
        mImageViewPager.adapter = ScreenSlidePagerAdapter((activity as AppCompatActivity).supportFragmentManager)

        val tabLayout = view.findViewById<ViewPager>(R.id.tabDots) as TabLayout
        tabLayout.setupWithViewPager(mImageViewPager, true)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val vg = tabLayout.getChildAt(0) as ViewGroup
                vg.alpha = 0.8f
                vg.animate()
                        .alpha(0.4f)
                        .setStartDelay(600)
                        .setDuration(400)
                        .setInterpolator(LinearOutSlowInInterpolator())
                        .start()

                val vgTab = vg.getChildAt(tab.position) as ViewGroup
                vgTab.scaleX = 0.8f
                vgTab.scaleY = 0.8f
                vgTab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .setDuration(450)
                        .start()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }

    private inner class ScreenSlidePagerAdapter(supportFragmentManager: FragmentManager) : FragmentStatePagerAdapter(supportFragmentManager) {
        override fun getItem(position: Int): android.support.v4.app.Fragment {
            return if (position == 1) PageAboutFoxFragment() else PageAboutSaurusFragment()
        }

        override fun getCount(): Int {
            return 2
        }
    }
}