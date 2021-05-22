package org.slavicin.jidelna.activities.settings


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.activities.logout
import org.slavicin.jidelna.consts.*
import org.slavicin.jidelna.data.UserSetting
import org.slavicin.jidelna.data.p
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ServiceBuilder
import org.slavicin.jidelna.utlis.setAppTheme
import org.slavicin.jidelna.utlis.setSystemNavBarColor


private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    lateinit var service: RestApi
    lateinit var cookiePreferences: SharedPreferences
    lateinit var rootLayout: FrameLayout
    lateinit var loadingBar : ProgressBar
    lateinit var settingsElement : FrameLayout
    lateinit var requestProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setSystemNavBarColor(this, window)

        loadingBar = findViewById(R.id.settingsLoading)
        settingsElement = findViewById(R.id.settings)
        rootLayout = findViewById(R.id.rootLayout)
        requestProgressBar = findViewById(R.id.requestProgressBar)

        cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
        val appPreferences = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        service = ServiceBuilder().build(
            appPreferences.getString(
                APP_BASE_URL_KEY,
                APP_BASE_URL_DEFAULT
            )!!, cookiePreferences
        )

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragmentz
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
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
        title = pref.title
        return true
    }
    fun act_logout() {
        logout(cookiePreferences, this)
    }
    class AutoorderFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        var lastUserSetting: p<UserSetting?> = p<UserSetting?>(null)
        var changedByHuman: p<Boolean> = p<Boolean>(true)
        var rootKey: String? = null

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            val act = activity as SettingsActivity
            val autoorderSettingPage = AutoorderSettingPage(
                act.loadingBar, act.settingsElement,
                act.requestProgressBar, act.rootLayout,
                act.service, act::act_logout, resources, ::setPreferencesFromResource
            )
            autoorderSettingPage.onChanged(
                changedByHuman,
                lastUserSetting,
                sharedPreferences,
                rootKey
            )
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
            val act = activity as SettingsActivity
            val autoorderSettingPage = AutoorderSettingPage(
                act.loadingBar, act.settingsElement,
                act.requestProgressBar, act.rootLayout,
                act.service, act::act_logout, resources, ::setPreferencesFromResource
            )
            autoorderSettingPage.onCreatePreferences(
                lastUserSetting,
                rootKey,
                ::getPreferenceScreen
            )

        }

        override fun onStop() {
            super.onStop()
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
        }
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)

        }
    }

    class GeneralFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey)


            val screen: PreferenceScreen = preferenceScreen
            val listPreference: ListPreference? = findPreference<Preference>("theme") as ListPreference?
            listPreference!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val value = newValue.toString()
                    listPreference.value = value
                    if (value == "light") {
                        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                    }
                    if (value == "dark") {
                        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
                    }
                    if (value == "default") {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    val act = activity as SettingsActivity

                    val intent: Intent = act.intent
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    act.finish()
                    startActivity(intent)
                    true
                }

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
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
        }
    }
    class ProfileFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.profile_preferences, rootKey)
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
            ProfileSettingsPage(preferenceScreen, act::act_logout, act.rootLayout, requireContext()).onCreatePreferences(
                act.service
            )

        }
    }
}



