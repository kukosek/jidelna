package org.slavicin.jidelna.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.main.MainActivity
import org.slavicin.jidelna.activities.main.Status
import org.slavicin.jidelna.activities.settings.dinner_notification_channel_id
import org.slavicin.jidelna.consts.*
import org.slavicin.jidelna.data.CantryMenu
import org.slavicin.jidelna.data.Dinner
import org.slavicin.jidelna.network.ServiceBuilder
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class EnabledNotifications(
    val today_dinner_not_ordered: Boolean,
    val today_dinner_ordered: Boolean,
    val today_dinner_autoordered: Boolean,
    val tomorrow_dinner_not_ordered: Boolean,
    val tomorrow_dinner_ordered: Boolean,
    val tomorrow_dinner_autoordered: Boolean
)

fun buildEnabledNotifications(prefs: SharedPreferences) : EnabledNotifications {
    return EnabledNotifications(
        prefs.getBoolean("notification_today_dinner_not_ordered", false),
        prefs.getBoolean("notification_today_dinner_ordered", false) ,
        prefs.getBoolean("notification_today_dinner_autoordered", false),
        prefs.getBoolean("notification_tomorrow_dinner_not_ordered", false),
        prefs.getBoolean("notification_tomorrow_dinner_ordered", false),
        prefs.getBoolean("notification_tomorrow_dinner_autoordered", false)
    )
}

class NotificationWorker(val context: Context, params: WorkerParameters) : Worker(context, params)
{
    override fun doWork(): Result
    {
        var notificationTitle : String? = null
        var notificationMessage : String? = null
        var notificationWouldBeSame: Boolean = false

        val appPreferences = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        val cookiePreferences = context.getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
        val notifPreferences = context.getSharedPreferences(NOTIFICATION_VAR_PREFS_NAME, Context.MODE_PRIVATE)

        val gson = Gson()

        val lastEnabledNotifications: EnabledNotifications? = try {
            gson.fromJson(
                notifPreferences.getString(
                    NOTIFICATION_VAR_LAST_ENABLED_NOTIFS, ""
                ), EnabledNotifications::class.java
            )
        } catch(e: JsonParseException){
            null
        }
        val lastMenu: CantryMenu? = gson.fromJson(notifPreferences.getString(NOTIFICATION_VAR_LAST_RESPONSE_KEY, ""), CantryMenu::class.java)


        val service = ServiceBuilder().build(
            appPreferences.getString(
                APP_BASE_URL_KEY,
                APP_BASE_URL_DEFAULT
            )!!, cookiePreferences
        )

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val enabledNotifs = buildEnabledNotifications(prefs)

        val enabledNotifsSameAsBefore = lastEnabledNotifications.hashCode() == enabledNotifs.hashCode()

        val targetDateTime = Calendar.getInstance()
        val hour = targetDateTime.get(Calendar.HOUR_OF_DAY)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var responseMenu: CantryMenu? = null
        if (hour in 6..13) {

            val call: Call<CantryMenu> = service.getMenu(dateFormat.format(targetDateTime.time))
            try {
                val response: Response<CantryMenu> = call.execute()
                if (response.isSuccessful) {
                    responseMenu = response.body()!!
                    val todayMenu = responseMenu
                    if (enabledNotifsSameAsBefore && todayMenu.hashCode() == lastMenu.hashCode()){
                        notificationWouldBeSame = true
                    }else if (todayMenu.menus.isNotEmpty()) {
                        var orderedDinner: Dinner? = null
                        var autoorderedDinner: Dinner? = null
                        val availableDinners = mutableListOf<String>()
                        for (dinner in todayMenu.menus) {
                            if (dinner.status == Status.ORDERED || dinner.status == Status.ORDERING || dinner.status == Status.ORDERED_CLOSED) {
                                orderedDinner = dinner
                                break
                            } else if (dinner.status == Status.AUTOORDER) {
                                autoorderedDinner = dinner
                                break
                            } else if (dinner.status == Status.AVAILABLE) {
                                availableDinners.add(dinner.name)
                            }
                        }

                        if (orderedDinner != null) {
                            if (enabledNotifs.today_dinner_ordered){
                                notificationTitle =
                                    context.resources.getString(R.string.notification_today_dinner_ordered)
                                notificationMessage =
                                    context.resources.getString(R.string.you_have_ordered) + ' ' + orderedDinner.name
                            }
                        } else if (autoorderedDinner != null) {
                            if (enabledNotifs.today_dinner_autoordered) {
                                notificationTitle =
                                    context.resources.getString(R.string.notification_today_dinner_autoordered)
                                notificationMessage =
                                    context.resources.getString(R.string.we_will_order) + ' ' + autoorderedDinner.name
                            }
                        } else if (availableDinners.size > 0) {
                            if (enabledNotifs.today_dinner_not_ordered) {
                                notificationTitle =
                                    context.resources.getString(R.string.notification_today_dinner_not_ordered)
                                notificationMessage =
                                    context.resources.getString(R.string.still_available_dinners) + "\n" + availableDinners.joinToString()
                            }
                        } else if (enabledNotifs.today_dinner_not_ordered) {
                            notificationTitle =
                                context.resources.getString(R.string.notification_today_dinner_not_ordered)
                            notificationMessage =
                                context.resources.getString(R.string.no_available_dinner)
                        }
                    }
                }else{
                    return Result.failure()
                }
            } catch (e: IOException){
                return Result.failure()
            }

        }else if (hour in 18..23){
           targetDateTime.add(Calendar.DATE, 1)
            val call: Call<CantryMenu> = service.getMenu(dateFormat.format(targetDateTime.time))
            try {
                val response: Response<CantryMenu> = call.execute()
                if (response.isSuccessful) {
                    responseMenu = response.body()!!
                    val tomorrowMenu = responseMenu
                    if (enabledNotifsSameAsBefore && tomorrowMenu.hashCode() == lastMenu.hashCode()){
                        notificationWouldBeSame = true
                    }
                    else if (tomorrowMenu.menus.isNotEmpty()) {
                        var orderedDinner: Dinner? = null
                        var autoorderedDinner: Dinner? = null
                        val availableDinners = mutableListOf<String>()
                        for (dinner in tomorrowMenu.menus) {
                            if (dinner.status == Status.ORDERED || dinner.status == Status.ORDERING || dinner.status == Status.ORDERED_CLOSED) {
                                orderedDinner = dinner
                                break
                            } else if (dinner.status == Status.AUTOORDER) {
                                autoorderedDinner = dinner
                                break
                            } else if (dinner.status == Status.AVAILABLE) {
                                availableDinners.add(dinner.name)
                            }
                        }

                        if (orderedDinner != null) {
                            if (enabledNotifs.tomorrow_dinner_ordered) {
                                notificationTitle =
                                    context.resources.getString(R.string.notification_tomorrow_dinner_ordered)
                                notificationMessage =
                                    context.resources.getString(R.string.you_have_ordered) + ' ' + orderedDinner.name
                            }
                        } else if (autoorderedDinner != null) {
                            if (enabledNotifs.tomorrow_dinner_autoordered) {
                                notificationTitle =
                                    context.resources.getString(R.string.notification_tomorrow_dinner_autoordered)
                                notificationMessage =
                                    context.resources.getString(R.string.we_will_order) + ' ' + autoorderedDinner.name
                            }
                        } else if (availableDinners.size > 0) {
                            if (enabledNotifs.tomorrow_dinner_not_ordered) {
                                notificationTitle =
                                    context.resources.getString(R.string.notification_tomorrow_dinner_not_ordered)
                                notificationMessage =
                                    context.resources.getString(R.string.still_available_dinners) + "\n" + availableDinners.joinToString()
                            }
                        } else if (enabledNotifs.tomorrow_dinner_not_ordered) {
                            notificationTitle =
                                context.resources.getString(R.string.notification_tomorrow_dinner_not_ordered)
                            notificationMessage =
                                context.resources.getString(R.string.no_available_dinner)
                        }
                    }
                }else{
                    return Result.failure()
                }
            } catch (e: IOException){
                return Result.failure()
            }
        }else {
            with(NotificationManagerCompat.from(context)) {
                cancel(0)
            }
        }
        if (notificationTitle != null) {
            // Create an explicit intent for an Activity in your app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            val notification =
                NotificationCompat.Builder(context, dinner_notification_channel_id)
                    .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationMessage)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(notificationMessage)
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            with(NotificationManagerCompat.from(context)) {
                // notificationId is a unique int for each notification that you must define
                notify(0, notification)
            }

        }else if (!notificationWouldBeSame){
            with(NotificationManagerCompat.from(context)) {
                cancel(0)
            }
        }
        if (responseMenu != null) {
            notifPreferences.edit {
                putString(NOTIFICATION_VAR_LAST_ENABLED_NOTIFS, gson.toJson(enabledNotifs))
                putString(NOTIFICATION_VAR_LAST_RESPONSE_KEY, gson.toJson(responseMenu))
            }
        }
        return Result.success()
    }
}
