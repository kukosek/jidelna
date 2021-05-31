package org.slavicin.jidelna.activities.onboarding

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import org.slavicin.jidelna.activities.main.MainActivity
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.activities.settings.AutoorderSettingPage
import org.slavicin.jidelna.activities.settings.NotificationsSettingPage
import org.slavicin.jidelna.activities.settings.SettingsActivity
import org.slavicin.jidelna.consts.*
import org.slavicin.jidelna.data.UserSetting
import org.slavicin.jidelna.data.p
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ServiceBuilder


fun tintMyDrawable(drawable: Drawable?, color: Int): Drawable? {
    var drawable = drawable
    drawable = DrawableCompat.wrap(drawable!!)
    DrawableCompat.setTint(drawable, color)
    DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)
    return drawable
}
class OnboardingActivity : AppCompatActivity() {
    /**
     * The [androidx.viewpager.widget.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [androidx.fragment.app.FragmentStatePagerAdapter].
     */
    var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */


    private var mViewPager: ViewPager? = null
    var mNextBtn: ImageButton? = null
    var mSkipBtn: Button? = null
    var mFinishBtn: Button? = null
    var zero: ImageView? = null
    var one: ImageView? = null
    private lateinit var indicators: Array<ImageView?>
    var lastLeftValue = 0
    var mCoordinator: CoordinatorLayout? = null
    var page = 0 //  to track page position
    val numOfPages = 2

    lateinit var cookiePreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
        val actionBar: androidx.appcompat.app.ActionBar? = supportActionBar
        actionBar?.hide()
        setContentView(R.layout.activity_onboarding)

        val nextActivityIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(
            supportFragmentManager
        )


        mNextBtn = findViewById<View>(R.id.intro_btn_next) as ImageButton
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) mNextBtn!!.setImageDrawable(
            tintMyDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_chevron_right_24dp),
                Color.WHITE
            )
        )
        mSkipBtn = findViewById<View>(R.id.intro_btn_skip) as Button
        mFinishBtn = findViewById<View>(R.id.intro_btn_finish) as Button
        zero = findViewById<View>(R.id.intro_indicator_0) as ImageView
        one = findViewById<View>(R.id.intro_indicator_1) as ImageView
        mCoordinator = findViewById<View>(R.id.main_content) as CoordinatorLayout
        indicators = arrayOf(zero, one)

        //


        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.container) as ViewPager
        mViewPager!!.adapter = mSectionsPagerAdapter
        mViewPager!!.currentItem = page
        updateIndicators(page)
        val color1 = ContextCompat.getColor(this, R.color.cyan)
        val color2 = ContextCompat.getColor(this, R.color.orange)
        val colorList = intArrayOf(color1, color2)
        val evaluator = ArgbEvaluator()
        mViewPager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

                /*
                color update
                 */
                val colorUpdate = evaluator.evaluate(
                    positionOffset,
                    colorList[position], colorList[if (position == numOfPages-1) position else position + 1]
                ) as Int
                mViewPager!!.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                page = position
                updateIndicators(page)
                when (position) {
                    0 -> mViewPager!!.setBackgroundColor(color1)
                    1 -> mViewPager!!.setBackgroundColor(color2)
                }
                mNextBtn!!.visibility = if (position == numOfPages-1) View.GONE else View.VISIBLE
                mFinishBtn!!.visibility = if (position == numOfPages-1) View.VISIBLE else View.GONE
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        mNextBtn!!.setOnClickListener {
            page += 1
            mViewPager!!.setCurrentItem(page, true)
        }
        mSkipBtn!!.setOnClickListener {
            startActivity(nextActivityIntent)
        }
        mFinishBtn!!.setOnClickListener {
            startActivity(nextActivityIntent)

        }
    }

    fun updateIndicators(position: Int) {
        for (i in indicators.indices) {
            indicators[i]!!.setBackgroundResource(
                if (i == position) R.drawable.indicator_selected else R.drawable.indicator_unselected
            )
        }
    }

    fun logout(){
        val cookies =
            cookiePreferences.getStringSet(
                COOKIE_SET_KEY,
                HashSet()
            ) as HashSet<String>?
        val cookiesToRemove : MutableCollection<String> = mutableListOf<String>()
        for (cookie in cookies!!) {
            if (cookie.split("=")[0] == "authid") {
                cookiesToRemove.add(cookie)
            }
        }
        cookies.removeAll(cookiesToRemove)
        cookiePreferences.edit {
            this.remove(COOKIE_SET_KEY)
            this.apply()
            this.putStringSet(COOKIE_SET_KEY, cookies)
        }
        val loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent)
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() , PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
        var img: ImageView? = null
        var bgs = intArrayOf(
            R.drawable.sync,
            R.drawable.ic_baseline_notifications_24
        )
        lateinit var infoLayout: FrameLayout
        lateinit var settingsLayout: FrameLayout
        //setting fragments
        lateinit var service: RestApi
        lateinit var cookiePreferences: SharedPreferences
        lateinit var rootLayout: FrameLayout
        lateinit var loadingBar : ProgressBar
        lateinit var settingsElement : FrameLayout
        lateinit var requestProgressBar: ProgressBar
        lateinit var supportFragmentManager: FragmentManager
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val sectionNumber = requireArguments().getInt(ARG_SECTION_NUMBER)
            val rootView: View = inflater.inflate(R.layout.fragment_onboarding_pager, container, false)

            infoLayout = rootView.findViewById(R.id.info_layout)
            settingsLayout = rootView.findViewById(R.id.settings_layout)

            loadingBar = rootView.findViewById(R.id.settingsLoading)
            settingsElement = rootView.findViewById(R.id.settings)
            rootLayout = rootView.findViewById(R.id.rootLayout)
            requestProgressBar = rootView.findViewById(R.id.requestProgressBar)


            val appPreferences = this.requireActivity().getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
            cookiePreferences = this.requireActivity().getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
            service = context?.let {
                ServiceBuilder().build(
                    appPreferences.getString(
                        APP_BASE_URL_KEY,
                        APP_BASE_URL_DEFAULT
                    )!!, cookiePreferences,
                    false, it
                )
            }!!

            val sectionText = rootView.findViewById<View>(R.id.section_label) as TextView
            sectionText.text =
                getString(when (sectionNumber) {
                    1 -> R.string.autoorder_header
                    else -> R.string.notifications_header
                })

            val descriptionText = rootView.findViewById<TextView>(R.id.section_text)
            descriptionText.text =
                getString(when (sectionNumber) {
                    1 -> R.string.autoorder_set_description
                    else -> R.string.notifications_set_description
                })

            img = rootView.findViewById<View>(R.id.section_img) as ImageView
            img!!.setBackgroundResource(bgs[sectionNumber - 1])
            supportFragmentManager = childFragmentManager


            val setupButton: Button = rootView.findViewById<Button>(R.id.button)
            setupButton.setOnClickListener {
                infoLayout.visibility = View.GONE
                settingsLayout.visibility = View.VISIBLE
                when (sectionNumber){
                    1 -> supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.settings, OnboardingActivity.AutoorderFragment())
                        .commit()
                    2 -> supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.settings, OnboardingActivity.NotificationsFragment())
                        .commit()
                }

            }
            return rootView
        }
        override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
        ): Boolean {
            // Instantiate the new Fragment
            val args = pref.extras
            val fragment = supportFragmentManager.fragmentFactory.instantiate(
                ClassLoader.getSystemClassLoader(),
                pref.fragment
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
            return true
        }
        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager?) :
        FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int {
            return numOfPages
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "SECTION 1"
                1 -> return "SECTION 2"
            }
            return null
        }
    }

    class AutoorderFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        var lastUserSetting: p<UserSetting?> = p<UserSetting?>(null)
        var changedByHuman: p<Boolean> = p<Boolean>(true)
        var rootKey: String? = null

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            val frag = parentFragment as PlaceholderFragment
            val act = activity as OnboardingActivity
            val autoorderSettingPage = AutoorderSettingPage(frag.loadingBar, frag.settingsElement,
                frag.requestProgressBar, frag.rootLayout,
                frag.service, act::logout, resources, ::setPreferencesFromResource)
            autoorderSettingPage.onChanged(changedByHuman, lastUserSetting, sharedPreferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            this.rootKey = rootKey
            val frag = parentFragment as PlaceholderFragment
            val act = activity as OnboardingActivity
            val autoorderSettingPage = AutoorderSettingPage(frag.loadingBar, frag.settingsElement,
                frag.requestProgressBar, frag.rootLayout,
                frag.service, act::logout, resources, ::setPreferencesFromResource)
            autoorderSettingPage.onCreatePreferences(lastUserSetting, rootKey, ::getPreferenceScreen)

        }

        override fun onStop() {
            super.onStop()
            val frag = parentFragment as PlaceholderFragment
            frag.loadingBar.visibility = View.GONE
            frag.settingsElement.visibility = View.VISIBLE
        }
    }
    class NotificationsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            NotificationsSettingPage(requireContext()).onChanged(sharedPreferences)
        }
        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.notifications_preferences, rootKey)
            val frag = parentFragment as PlaceholderFragment
            frag.loadingBar.visibility = View.GONE
            frag.settingsElement.visibility = View.VISIBLE
        }
    }

    companion object {
        const val TAG = "PagerActivity"
    }
}