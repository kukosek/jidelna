package org.slavicin.jidelna.ui.login

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.slavicin.jidelna.RestApi
import org.slavicin.jidelna.data.LoginDataSource
import org.slavicin.jidelna.data.LoginRepository
import retrofit2.Retrofit

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class LoginViewModelFactory(
    val service: RestApi, val progressBar: ProgressBar,
    val constraintLayout: ConstraintLayout, val successIntent: Intent,
    val button : Button,
    val context: Context

    ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource(service, progressBar, constraintLayout, successIntent, button, context)
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}