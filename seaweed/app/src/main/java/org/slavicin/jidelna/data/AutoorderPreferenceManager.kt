package org.slavicin.jidelna.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class PreferenceHelper(private val prefs: SharedPreferences) {
    fun saveSetting(userSetting: UserSetting?){
        prefs.edit {
            if (userSetting!!.autoorder?.enable != null) {
                this.putBoolean("autoorder_enable", userSetting.autoorder?.enable!!)
            } else {
                this.putBoolean("autoorder_enable", false)
            }
            if (userSetting.autoorder?.config?.orderUncomplying != null) {
                this.putBoolean(
                    "autoorder_order_uncomplying",
                    userSetting.autoorder.config.orderUncomplying
                )
            } else {
                this.putBoolean("autoorder_order_uncomplying", false)
            }
            if (userSetting.autoorder?.config?.prefferedMenuNumber != null) {
                this.putString(
                    "preffered_menu_number",
                    userSetting.autoorder.config.prefferedMenuNumber.toString()
                )
            } else {
                this.putString("preffered_menu_number", "None")
            }
            val allergenLovelist = mutableSetOf<String>()
            if (userSetting.autoorder?.config?.allergens?.loveList != null) {
                for (allergen in userSetting.autoorder.config.allergens.loveList) {
                    allergenLovelist.add(allergen.toString())
                }
            }
            this.putStringSet("allergens_lovelist", allergenLovelist)

            val allergenBlacklist = mutableSetOf<String>()
            if (userSetting.autoorder?.config?.allergens?.blackList != null) {
                for (allergen in userSetting.autoorder.config.allergens.blackList) {
                    allergenBlacklist.add(allergen.toString())
                }
            }
            this.putStringSet("allergens_blacklist", allergenBlacklist)

            if (userSetting.autoorder?.requestConfig?.orderAll != null) {
                this.putBoolean(
                    "autoorder_request_order_one_by_one",
                    !userSetting.autoorder.requestConfig.orderAll
                )
            } else {
                this.putBoolean("autoorder_request_order_one_by_one", false)
            }

            if (userSetting.autoorder?.requestConfig?.orderDaysInAdvance != null) {
                this.putString(
                    "autoorder_request_days_in_advance",
                    userSetting.autoorder.requestConfig.orderDaysInAdvance.toString()
                )
            } else {
                this.putString("autoorder_request_days_in_advance", "0")
            }
        }
    }
}

class UserSettingBuilder() {
    fun fromPreferences(sharedPreferences: SharedPreferences) : UserSetting{
        val pmns = sharedPreferences.getString("preffered_menu_number", "None")
        val prefferedMenuNumber : Int? = if (pmns == "None") {
            null
        }else{
            pmns?.toInt()
        }

        var allergensListStrings = sharedPreferences.getStringSet("allergens_lovelist", setOf<String>())
        val allergenLoveList = mutableListOf<Int>()
        if (allergensListStrings != null) {
            for (allergen in allergensListStrings) {
                allergenLoveList.add(allergen.toInt())
            }
        }

        allergensListStrings = sharedPreferences.getStringSet("allergens_blacklist", setOf<String>())
        val allergenBlackList = mutableListOf<Int>()
        if (allergensListStrings != null) {
            for (allergen in allergensListStrings) {
                allergenBlackList.add(allergen.toInt())
            }
        }

        return UserSetting(
            AutoorderSetting(
                sharedPreferences.getBoolean("autoorder_enable", false),
                AutoorderConfig(
                    false, prefferedMenuNumber,
                    AllergensConfig(
                        allergenLoveList.toList(), allergenBlackList.toList()
                    ),
                    sharedPreferences.getBoolean("autoorder_order_uncomplying", false)
                ),
                AutoorderRequestConfig(
                    !sharedPreferences.getBoolean("autoorder_request_order_one_by_one", true),
                    sharedPreferences.getString("autoorder_request_days_in_advance", "0")!!.toInt())
            )
        )
    }
}