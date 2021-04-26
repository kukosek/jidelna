package org.slavicin.jidelna

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.data.CantryMenu
import org.slavicin.jidelna.ui.login.LoginActivity
import org.slavicin.jidelna.ui.main.MenuItemAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    lateinit var cookiePreferences: SharedPreferences
    lateinit var loginIntent: Intent
    lateinit var service : RestApi
    lateinit var rootLayout : SwipeRefreshLayout
    var menus = mutableListOf<CantryMenu>()
    lateinit var menusRecyclerView: RecyclerView
    private lateinit var menuItemAdapter : MenuItemAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cookiePreferences = getSharedPreferences("PREF_COOKIES", Context.MODE_PRIVATE)
        loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        val cookies =
            cookiePreferences.getStringSet(
                AddCookiesInterceptor.PREF_COOKIES,
                HashSet()
            ) as HashSet<String>?
        var authidFound = false
        for (cookie in cookies!!) {
            val cookieSp = cookie.split("=")
            if (cookieSp[0] == "authid"){
                authidFound = true
            }
        }

        if (authidFound) {
            setContentView(R.layout.activity_main)

            service = ServiceBuilder().build("http://10.143.201.147:8080/", cookiePreferences)

            rootLayout = findViewById<SwipeRefreshLayout>(R.id.main_layout)
            rootLayout.setOnRefreshListener(OnRefreshListener {
                reloadMenu()
            })

            menusRecyclerView = findViewById(R.id.menus)
            menuItemAdapter = MenuItemAdapter(menus, service, rootLayout, loginIntent)
            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
                this@MainActivity
            )
            menusRecyclerView.layoutManager = layoutManager;
            menusRecyclerView.adapter = menuItemAdapter;

            reloadMenu()
        }else{
            startActivity(loginIntent)
        }
    }

    private fun reloadMenu(){
        rootLayout.isRefreshing = true;
        val callAsync : Call<List<CantryMenu>> = service.getMenus()
        callAsync.enqueue(object : Callback<List<CantryMenu>?> {
            override fun onResponse(
                call: Call<List<CantryMenu>?>,
                response: Response<List<CantryMenu>?>
            ) {
                if (response.isSuccessful) {
                    menus.clear()
                    menus.addAll(response.body()!!)
                    menuItemAdapter.notifyDataSetChanged();
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
                rootLayout.isRefreshing = false
            }

            override fun onFailure(
                call: Call<List<CantryMenu>?>,
                t: Throwable
            ) {
                rootLayout.isRefreshing = false
                val errorMessage = resources.getString(R.string.network_error) + t.localizedMessage
                Snackbar.make(
                    rootLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

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
        startActivity(loginIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }
    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.logout -> {
                logout()
                true
            }
            R.id.menu_refresh -> {
                reloadMenu()
                true
            }
            R.id.settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}