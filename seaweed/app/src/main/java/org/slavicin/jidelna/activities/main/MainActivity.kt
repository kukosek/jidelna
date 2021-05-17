package org.slavicin.jidelna.activities.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.activities.logout
import org.slavicin.jidelna.activities.settings.SettingsActivity
import org.slavicin.jidelna.consts.*
import org.slavicin.jidelna.data.CantryMenu
import org.slavicin.jidelna.data.WeekMenu
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ServiceBuilder
import org.slavicin.jidelna.utlis.setAppTheme
import org.slavicin.jidelna.utlis.setSystemNavBarColor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil


class MainActivity : AppCompatActivity() {
    private lateinit var appPreferences: SharedPreferences
    lateinit var cookiePreferences: SharedPreferences
    lateinit var loginIntent: Intent
    lateinit var service : RestApi
    lateinit var rootLayout : SwipeRefreshLayout
    var menus = mutableListOf<MenuRecyclerviewItem>()
    lateinit var menusRecyclerView: RecyclerView
    private lateinit var menuItemAdapter : MenuItemAdapter
    private val nativeAds =  arrayOfNulls<UnifiedNativeAd>(NUMBER_OF_ADS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppTheme(this)
        appPreferences = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
        loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        val cookies =
            cookiePreferences.getStringSet(
                COOKIE_SET_KEY,
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

            setSystemNavBarColor(this, window)

            service = ServiceBuilder().build(
                appPreferences.getString(
                    APP_BASE_URL_KEY,
                    APP_BASE_URL_DEFAULT
                )!!, cookiePreferences
            )

            rootLayout = findViewById<SwipeRefreshLayout>(R.id.main_layout)
            rootLayout.setOnRefreshListener(OnRefreshListener {
                reloadMenu()
            })

            menusRecyclerView = findViewById(R.id.menus)
            menuItemAdapter = MenuItemAdapter(menus, service, rootLayout, loginIntent,
                this
            )
            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
                this@MainActivity
            )

            menusRecyclerView.layoutManager = layoutManager;
            menusRecyclerView.adapter = menuItemAdapter;
            setAppTheme(this)
            val adLoader = AdLoader.Builder(this, ADMOB_NATIVE_AD_ID)
                .forUnifiedNativeAd { ad : UnifiedNativeAd ->
                    nativeAds[nativeAds.indexOf(null)] = ad
                    if (menus.isNotEmpty()) {
                        addAdsToMenu()
                        menuItemAdapter.notifyDataSetChanged();
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                    // Methods in the NativeAdOptions.Builder class can be
                    // used here to specify individual options settings.
                    .build())
                .build()

            if (NUMBER_OF_ADS > 0) {
                adLoader.loadAds(AdRequest.Builder().build(), NUMBER_OF_ADS)
            }
            reloadMenu()
        }else{
            startActivity(loginIntent)
        }
    }

    private fun addAdsToMenu() {
        val itemsWithoutAds = mutableListOf<MenuRecyclerviewItem>()
        itemsWithoutAds.addAll(menus)
        val itemsWithAds = mutableListOf<MenuRecyclerviewItem>()
        val numberOfItemsWithAds = if (menus.size <=3) {
            ceil(NUMBER_OF_ADS.toFloat()).toInt()
        }else{
            NUMBER_OF_ADS
        }
        for (i in 1..numberOfItemsWithAds) {
            val elemnt = itemsWithoutAds.random()
            itemsWithAds.add(elemnt)
            itemsWithoutAds.add(elemnt)
        }

        val availableAds = mutableListOf<UnifiedNativeAd?>()
        availableAds.addAll(nativeAds)

        if (menus.isNotEmpty()) {
            for (item in menus) {
                item.ad = null
                if (item in itemsWithAds && availableAds.size > 0) {
                    item.ad = availableAds[0]
                    availableAds.removeAt(0)
                }
            }
        }
    }

    private fun reloadMenu(){
        rootLayout.isRefreshing = true;
        val callAsync  = service.getMenus()
        callAsync.enqueue(object : Callback<WeekMenu?> {
            override fun onResponse(
                call: Call<WeekMenu?>,
                response: Response<WeekMenu?>
            ) {
                if (response.isSuccessful) {
                    val message = resources.getString(R.string.credit) + response.body()?.creditLeft.toString()
                    Snackbar.make(
                        rootLayout,
                        message,
                        Snackbar.LENGTH_SHORT
                    ).show()

                    menus.clear()
                    val responseMenus : List<CantryMenu> = response.body()!!.daymenus
                    for (menu in responseMenus) {
                        menus.add(MenuRecyclerviewItem(menu, null))
                    }
                    addAdsToMenu()

                    menuItemAdapter.notifyDataSetChanged();
                } else {
                    if (response.code() == 401) {
                        logout(cookiePreferences, this@MainActivity)
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
                call: Call<WeekMenu?>,
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
                logout(cookiePreferences, this)
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