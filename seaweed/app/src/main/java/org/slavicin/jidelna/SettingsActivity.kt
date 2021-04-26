package org.slavicin.jidelna


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.data.*
import org.slavicin.jidelna.ui.login.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        loadingBar = findViewById(R.id.settingsLoading)
        settingsElement = findViewById(R.id.settings)
        rootLayout = findViewById(R.id.rootLayout)
        requestProgressBar = findViewById(R.id.requestProgressBar)

        cookiePreferences = getSharedPreferences("PREF_COOKIES", Context.MODE_PRIVATE)
        service = ServiceBuilder().build("http://10.143.201.147:8080/", cookiePreferences)

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
        // Instantiate the new Fragment
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

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
        }
    }

    class GeneralFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey)
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
        }
    }
    fun logout(){
        val cookies =
            cookiePreferences.getStringSet(
                AddCookiesInterceptor.PREF_COOKIES,
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
            this.remove("PREF_COOKIES")
            this.apply()
            this.putStringSet("PREF_COOKIES", cookies)
        }
        val loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent)
    }



    class AutoorderFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        var lastUserSetting: UserSetting? = null
        var changedByHuman: Boolean = true
        var rootKey : String? = null
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (changedByHuman) {
                val act = activity as SettingsActivity
                act.requestProgressBar.visibility = View.VISIBLE

                val userSetting = UserSettingBuilder().fromPreferences(sharedPreferences)


                val callAsync: Call<Void> = act.service.updateSettings(userSetting)
                callAsync.enqueue(object : Callback<Void> {
                    override fun onResponse(
                        call: Call<Void>,
                        response: Response<Void>
                    ) {
                        act.requestProgressBar.visibility = View.GONE
                        if (!response.isSuccessful) {
                            if (response.code() == 401) {
                                act.logout()
                            } else {
                                if (lastUserSetting != null) {
                                    changedByHuman = false
                                    PreferenceHelper(sharedPreferences).saveSetting(lastUserSetting)
                                    setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                                }
                                val errorMessage = when (response.code()) {
                                    500 -> resources.getString(R.string.server_error);
                                    502 -> resources.getString(R.string.gateway_timeout);
                                    else -> resources.getString(R.string.request_error) + response.errorBody()
                                }
                                Snackbar.make(
                                    act.rootLayout,
                                    errorMessage,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            lastUserSetting = userSetting
                        }
                    }

                    override fun onFailure(
                        call: Call<Void>,
                        t: Throwable
                    ) {
                        if (lastUserSetting != null) {
                            changedByHuman = false
                            PreferenceHelper(sharedPreferences).saveSetting(lastUserSetting)
                            setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                        }
                        act.requestProgressBar.visibility = View.GONE
                        val errorMessage =
                            resources.getString(R.string.network_error) + t.localizedMessage
                        Snackbar.make(
                            act.rootLayout,
                            errorMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                })
            }else{
                changedByHuman = true
            }

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
            act.loadingBar.visibility = View.VISIBLE
            act.settingsElement.visibility = View.GONE
            val callAsync : Call<UserSetting> = act.service.getSettings()
            callAsync.enqueue(object : Callback<UserSetting?> {
                override fun onResponse(
                    call: Call<UserSetting?>,
                    response: Response<UserSetting?>
                ) {

                    if (response.isSuccessful) {
                        act.loadingBar.visibility = View.GONE
                        act.settingsElement.visibility = View.VISIBLE
                        setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                        val userSetting = response.body()
                        lastUserSetting = userSetting


                        val myPrefScreen: PreferenceScreen = preferenceScreen as PreferenceScreen
                        val prefs = myPrefScreen.sharedPreferences
                        PreferenceHelper(prefs).saveSetting(userSetting)

                        setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                    } else {
                        if (response.code() == 401) {
                            act.logout()
                        } else {
                            val errorMessage = when (response.code()) {
                                500 -> resources.getString(R.string.server_error);
                                502 -> resources.getString(R.string.gateway_timeout);
                                else -> resources.getString(R.string.request_error) + response.errorBody()
                            }
                            Snackbar.make(
                                act.rootLayout,
                                errorMessage,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                    }
                }

                override fun onFailure(
                    call: Call<UserSetting?>,
                    t: Throwable
                ) {
                    act.loadingBar.visibility = View.GONE
                    act.settingsElement.visibility = View.VISIBLE
                    val errorMessage =
                        resources.getString(R.string.network_error) + t.localizedMessage
                    Snackbar.make(
                        act.rootLayout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            })
        }

        override fun onStop() {
            super.onStop()
            val act = activity as SettingsActivity
            act.loadingBar.visibility = View.GONE
            act.settingsElement.visibility = View.VISIBLE
        }
    }
    class NotificationsFragment : PreferenceFragmentCompat() {
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
        }
    }
}