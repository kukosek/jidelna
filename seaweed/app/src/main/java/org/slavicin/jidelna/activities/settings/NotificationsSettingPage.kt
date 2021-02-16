package org.slavicin.jidelna.activities.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.*
import org.slavicin.jidelna.workers.NotificationWorker
import java.util.concurrent.TimeUnit
import org.slavicin.jidelna.R

const val dinner_notification_channel_id = "dinner"
class NotificationsSettingPage(val context: Context) {
    fun onChanged(prefs: SharedPreferences) {
        val values = setOf(
            prefs.getBoolean("notification_today_dinner_not_ordered", false),
            prefs.getBoolean("notification_today_dinner_ordered", false),
            prefs.getBoolean("notification_today_dinner_autoordered", false),
            prefs.getBoolean("notification_tomorrow_dinner_not_ordered", false),
            prefs.getBoolean("notification_tomorrow_dinner_ordered", false),
            prefs.getBoolean("notification_tomorrow_dinner_autoordered", false)
        )
        var notificationsEnabled = false
        for(item in values){
            if (item) {
                notificationsEnabled = true
                break
            }
        }
        val workManager = WorkManager.getInstance(context)
        val uniqueWorkName = "notificationWork"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.resources.getString(R.string.notifications_channel_name)
            val descriptionText = context.resources.getString(R.string.notifications_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(dinner_notification_channel_id, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        if (notificationsEnabled) {
            val workConstraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequest
                    .Builder(NotificationWorker::class.java, 15L, TimeUnit.MINUTES)
                    .setConstraints(workConstraints)
                    .build()
            )
        }else{
            workManager.cancelUniqueWork(uniqueWorkName)
        }
    }

}