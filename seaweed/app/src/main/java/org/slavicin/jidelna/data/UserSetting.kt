package org.slavicin.jidelna.data

import com.google.gson.annotations.SerializedName

data class AllergensConfig(
    @SerializedName("lovelist") val loveList: List<Int>?,
    @SerializedName("blacklist") val blackList: List<Int>?
)

data class AutoorderConfig(
    @SerializedName("random") val randomOrders: Boolean?,
    @SerializedName("prefferedMenuNumber") val prefferedMenuNumber: Int?,
    @SerializedName("allergens") val allergens: AllergensConfig?,
    @SerializedName("orderUncomplying") val orderUncomplying: Boolean?
)

data class AutoorderRequestConfig(
    @SerializedName("orderAll") val orderAll: Boolean?,
    @SerializedName("orderDaysInAdvance") val orderDaysInAdvance: Int?
)

data class AutoorderSetting(
    @SerializedName("enable") val enable: Boolean?,
    @SerializedName("settings") val config: AutoorderConfig?,
    @SerializedName("requestSettings") val requestConfig: AutoorderRequestConfig?
)

data class UserSetting(
    @SerializedName("autoorder") val autoorder: AutoorderSetting?
)