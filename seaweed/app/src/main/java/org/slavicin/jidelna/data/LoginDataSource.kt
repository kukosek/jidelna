package org.slavicin.jidelna.data



import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.UserCredentials
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


fun getString(context: Context, name : String) : String{
    return context.resources.getString(context.resources.getIdentifier(name, "string", context.packageName))
}
/**
 * Class that handles authentication w/ login_menu credentials and retrieves user information.
 */
class LoginDataSource(
    var service: RestApi, val progressBar: ProgressBar,
    val constraintLayout: ConstraintLayout, val successIntent: Intent, val button: Button, val context: Context
) {

    fun login(username: String, password: String) {
        progressBar.visibility = View.VISIBLE;
        button.isEnabled = false
        val callAsync : Call<Void> = service.login(UserCredentials(username, password))
        callAsync.enqueue(object : Callback<Void?> {
            override fun onResponse(
                call: Call<Void?>,
                response: Response<Void?>
            ) {
                progressBar.visibility = View.GONE;
                button.isEnabled = true
                if (response.isSuccessful) {
                    startActivity(context, successIntent, Bundle())
                } else {
                    val errorMessage : String = when {
                        response.code() == 401 -> {
                            getString(context,"incorrect_creds")
                        }
                        response.code() == 502 -> {
                            getString(context,"gateway_timeout")
                        }
                        else -> {
                            getString(context,"request_error") + response.errorBody()
                        }
                    }
                    Snackbar.make(constraintLayout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                call: Call<Void?>,
                t: Throwable
            ) {
                button.isEnabled = true
                progressBar.visibility = View.GONE;
                val errorMessage = getString(context, "network_error") + t.localizedMessage
                Snackbar.make(constraintLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun logout() {
        // TODO: revoke authentication
    }
}