package org.slavicin.jidelna.activities.settings

import android.content.SharedPreferences
import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.preference.PreferenceScreen
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.R
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.data.PreferenceHelper
import org.slavicin.jidelna.data.UserSetting
import org.slavicin.jidelna.data.UserSettingBuilder
import org.slavicin.jidelna.data.p
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AutoorderSettingPage(val loadingBar: ProgressBar, val settingsElement: FrameLayout, val requestProgressBar: ProgressBar, val rootLayout: FrameLayout, val service: RestApi, val logout: () -> Unit,
                           val resources: Resources, val setPreferencesFromResource: (Int, String?) -> Unit) {
    fun onChanged(changedByHuman: p<Boolean>, lastUserSetting : p<UserSetting?>, sharedPreferences: SharedPreferences, rootKey: String?) {
        if (changedByHuman.v) {
            requestProgressBar.visibility = View.VISIBLE

            val userSetting = UserSettingBuilder().fromPreferences(sharedPreferences)

            val callAsync: Call<Void> = service.updateSettings(userSetting)
            callAsync.enqueue(object : Callback<Void> {
                override fun onResponse(
                    call: Call<Void>,
                    response: Response<Void>
                ) {
                    requestProgressBar.visibility = View.GONE
                    if (!response.isSuccessful) {
                        if (response.code() == 401) {
                            logout()
                        } else {
                            if (lastUserSetting.v != null) {
                                changedByHuman.v = false
                                PreferenceHelper(sharedPreferences).saveSetting(lastUserSetting.v)
                                setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                            }
                            val errorMessage = when (response.code()) {
                                500 -> resources.getString(R.string.server_error);
                                502 -> resources.getString(R.string.gateway_timeout);
                                else -> resources.getString(R.string.request_error) + response.errorBody()
                            }
                            Snackbar.make(
                                rootLayout,
                                errorMessage,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        lastUserSetting.v = userSetting
                    }
                }

                override fun onFailure(
                    call: Call<Void>,
                    t: Throwable
                ) {
                    if (lastUserSetting.v != null) {
                        changedByHuman.v = false
                        PreferenceHelper(sharedPreferences).saveSetting(lastUserSetting.v)
                        setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                    }
                    requestProgressBar.visibility = View.GONE
                    val errorMessage =
                        resources.getString(R.string.network_error) + t.localizedMessage
                    Snackbar.make(
                        rootLayout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            changedByHuman.v = true
        }
    }

    fun onCreatePreferences(lastUserSetting : p<UserSetting?>, rootKey: String?, getPrefsScreen: () -> PreferenceScreen) {
        loadingBar.visibility = View.VISIBLE
        settingsElement.visibility = View.GONE
        val callAsync: Call<UserSetting> = service.getSettings()
        callAsync.enqueue(object : Callback<UserSetting?> {
            override fun onResponse(
                call: Call<UserSetting?>,
                response: Response<UserSetting?>
            ) {

                if (response.isSuccessful) {
                    loadingBar.visibility = View.GONE
                    settingsElement.visibility = View.VISIBLE
                    setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                    val userSetting = response.body()
                    lastUserSetting.v = userSetting

                    val prefs = getPrefsScreen().sharedPreferences
                    PreferenceHelper(prefs).saveSetting(userSetting)

                    setPreferencesFromResource(R.xml.autoorder_preferences, rootKey)
                } else {
                    if (response.code() == 401) {
                        logout()
                    } else {
                        val errorMessage = when (response.code()) {
                            500 -> resources.getString(R.string.server_error);
                            502 -> resources.getString(R.string.gateway_timeout);
                            else -> resources.getString(R.string.request_error) + response.errorBody()
                        }
                        Snackbar.make(
                            rootLayout,
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
                loadingBar.visibility = View.GONE
                settingsElement.visibility = View.VISIBLE
                val errorMessage =
                    resources.getString(R.string.network_error) + t.localizedMessage
                Snackbar.make(
                    rootLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }
}