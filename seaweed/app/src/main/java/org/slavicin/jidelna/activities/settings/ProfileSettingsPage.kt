package org.slavicin.jidelna.activities.settings

import android.content.Context
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.R
import org.slavicin.jidelna.network.RestApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


fun getDialogProgressBar(context: Context): AlertDialog.Builder {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(R.string.logging_out)
    val progressBar = ProgressBar(context)
    val lp = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    progressBar.layoutParams = lp
    builder.setView(progressBar)
    return builder
}

class ProfileSettingsPage(
    private val preferenceScreen: PreferenceScreen,
    val logout: () -> Unit,
    val rootLayout: FrameLayout,
    private val context: Context
){
    fun onCreatePreferences(service: RestApi){
        val deleteAccountPref : Preference = preferenceScreen.findPreference<Preference>("delete_account") as Preference
        deleteAccountPref.onPreferenceClickListener = Preference.OnPreferenceClickListener { // dialog code here
            AlertDialog.Builder(context)
                .setTitle(R.string.delete_account_really)
                .setMessage(R.string.delete_account_really_description)
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    dialog.dismiss()
                    val progressDialog = getDialogProgressBar(context).show()
                    val callAsync: Call<Void> = service.logout(true)
                    callAsync.enqueue(object : Callback<Void> {
                        override fun onResponse(
                            call: Call<Void>,
                            response: Response<Void>
                        ) {
                            progressDialog.dismiss()
                            if (!response.isSuccessful) {
                                if (response.code() == 401) {
                                    logout()
                                } else {
                                    val errorMessage = when (response.code()) {
                                        500 -> context.resources.getString(R.string.server_error);
                                        502 -> context.resources.getString(R.string.gateway_timeout);
                                        else -> context.resources.getString(R.string.request_error) + response.errorBody()
                                    }
                                    Snackbar.make(
                                        rootLayout,
                                        errorMessage,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }else{
                                logout()
                            }
                        }

                        override fun onFailure(
                            call: Call<Void>,
                            t: Throwable
                        ) {
                            progressDialog.dismiss()
                            val errorMessage =
                                context.resources.getString(R.string.network_error) + t.localizedMessage
                            Snackbar.make(
                                rootLayout,
                                errorMessage,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
                .setNegativeButton(android.R.string.no) { dialog, which ->

                }

            .show()
            true
        }

        val logoutPref : Preference = preferenceScreen.findPreference<Preference>("logout") as Preference
        logoutPref.onPreferenceClickListener = Preference.OnPreferenceClickListener { // dialog code here
            AlertDialog.Builder(context)
                .setTitle(R.string.logout_really)
                .setMessage(R.string.logout_really_description)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    logout()
                }
                .setNegativeButton(android.R.string.no) { _, _ ->

                }

                .show()
            true
        }
    }
}