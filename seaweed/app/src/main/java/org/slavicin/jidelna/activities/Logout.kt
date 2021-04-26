package org.slavicin.jidelna.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.consts.COOKIE_SET_KEY
import org.slavicin.jidelna.consts.LOGIN_PERSISTENCE_PREFS_NAME

fun logout(cookiePreferences: SharedPreferences, context: Context){
    val cookies =
        cookiePreferences.getStringSet(
            COOKIE_SET_KEY,
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
        this.remove(COOKIE_SET_KEY)
        this.apply()
        this.putStringSet(COOKIE_SET_KEY, cookies)
    }
    val loginIntent = Intent(context, LoginActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    val loginPersistence = context.getSharedPreferences(LOGIN_PERSISTENCE_PREFS_NAME, Context.MODE_PRIVATE)

    loginPersistence.edit {
        remove("username")
        remove("password")
    }

    context.startActivity(loginIntent)
}