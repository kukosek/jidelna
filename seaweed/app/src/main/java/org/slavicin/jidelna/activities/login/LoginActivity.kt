package org.slavicin.jidelna.activities.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_login.*
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.onboarding.OnboardingActivity
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ServiceBuilder
import org.slavicin.jidelna.utlis.setAppTheme
import org.slavicin.jidelna.utlis.setSystemNavBarColor
import android.widget.CompoundButton
import org.slavicin.jidelna.consts.*


class LoginActivity : AppCompatActivity() {
    private lateinit var appPreferences: SharedPreferences
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var loginPersistence: SharedPreferences
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var service : RestApi
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme(this)
        super.onCreate(savedInstanceState)
        setSystemNavBarColor(this, window)

        appPreferences = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
        loginPersistence = getSharedPreferences(LOGIN_PERSISTENCE_PREFS_NAME, Context.MODE_PRIVATE)

        val nextActivityIntent = Intent(this, OnboardingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        setContentView(R.layout.activity_login)
        service = ServiceBuilder().build(
            appPreferences.getString(
                APP_BASE_URL_KEY,
                APP_BASE_URL_DEFAULT
            )!!, cookiePreferences
        )
        val username = findViewById<EditText>(R.id.username)
        username.setText(loginPersistence.getString("username", ""))
        val password = findViewById<EditText>(R.id.password)
        password.setText(loginPersistence.getString("password", ""))
        val textView = findViewById<View>(R.id.textViewAcceptTerms) as TextView
        textView.text = Html.fromHtml(
            "&nbsp;<a href='org.slavicin.jidelna.activities.main.webview://'>"+getString(
                R.string.terms_and_conditions
            ) + "</a>"
        )

        textView.isClickable = true;
        textView.movementMethod = LinkMovementMethod.getInstance();

        val checkbox = findViewById<View>(R.id.checkBoxAcceptTerms) as CheckBox

        val textViewAcceptText = findViewById<TextView>(R.id.textView)
        textViewAcceptText.setOnClickListener(View.OnClickListener {
            checkbox.isChecked = !checkbox.isChecked
        })

        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)
        val rootLayout = findViewById<ConstraintLayout>(R.id.container)

        loginViewModel = ViewModelProviders.of(
            this,
            LoginViewModelFactory(service, loading, rootLayout, nextActivityIntent, login, this)
        )
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login_menu button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login_menu activity once successful
            finish()
        })

        fun dataChanged() {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString(),
                checkbox.isChecked
            )
            loginPersistence.edit {
                putString("username", username.text.toString())
                putString("password", password.text.toString())
                apply()
            }
        }

        username.afterTextChanged {
            dataChanged()
        }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            dataChanged()
        }

        password.apply {
            afterTextChanged {
                dataChanged()
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.login_menu, menu)
        return true
    }
    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.change_url -> {
                val taskEditText = EditText(this)
                taskEditText.setText(
                    appPreferences.getString(
                        APP_BASE_URL_KEY,
                        APP_BASE_URL_DEFAULT
                    )!!
                )
                val dialog: AlertDialog = AlertDialog.Builder(this, R.style.dialogTheme)
                    .setTitle(R.string.change_url)
                    .setMessage(R.string.change_url_description)
                    .setView(taskEditText)
                    .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                        val baseUrl = taskEditText.text.toString()
                        appPreferences.edit {
                            this.putString(APP_BASE_URL_KEY, baseUrl)
                        }
                        service = ServiceBuilder().build(baseUrl, cookiePreferences)
                        loginViewModel.loginRepository.dataSource.service = service

                    })
                    .setNeutralButton(
                        R.string.default_url,
                        DialogInterface.OnClickListener { _, _ ->
                            val baseUrl = APP_BASE_URL_DEFAULT
                            taskEditText.setText(baseUrl)
                            appPreferences.edit {
                                this.putString(APP_BASE_URL_KEY, baseUrl)
                            }
                            service = ServiceBuilder().build(baseUrl, cookiePreferences)
                            loginViewModel.loginRepository.dataSource.service = service

                        })
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                dialog.show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}