package org.slavicin.jidelna.utlis

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.Window
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.slavicin.jidelna.R

fun setAppTheme(context: Context) {
    AppCompatDelegate.setDefaultNightMode(when(PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "default")) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "default" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_YES
    })
}

fun setSystemNavBarColor(context: Context, window: Window) {
    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentNightMode == Configuration.UI_MODE_NIGHT_YES || currentNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
        window.navigationBarColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
    }
}