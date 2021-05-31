package org.slavicin.jidelna.utlis

import android.content.Context
import android.icu.util.TimeUnit
import org.slavicin.jidelna.R
import java.text.SimpleDateFormat
import java.util.*

fun agoString(context: Context, past: Date): CharSequence? {
    val now = Date()
    val seconds: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
    val minutes: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
    val hours: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(now.time - past.time)
    val days: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now.time - past.time)
//
//          System.out.println(TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime()) + " milliseconds ago");
//          System.out.println(TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime()) + " minutes ago");
//          System.out.println(TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime()) + " hours ago");
//          System.out.println(TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime()) + " days ago");

    //
//          System.out.println(TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime()) + " milliseconds ago");
//          System.out.println(TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime()) + " minutes ago");
//          System.out.println(TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime()) + " hours ago");
//          System.out.println(TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime()) + " days ago");
    return when {
        seconds < 60 -> {
            context.getString(R.string.seconds_ago, seconds)
        }
        minutes < 60 -> {
            context.getString(R.string.minutes_ago, minutes)
        }
        hours < 24 -> {
            context.getString(R.string.hours_ago, hours)
        }
        else -> {
            context.getString(R.string.days_ago, days)
        }
    }


}