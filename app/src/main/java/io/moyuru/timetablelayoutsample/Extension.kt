package io.moyuru.timetablelayoutsample

import android.content.res.Resources

val Int.dp get() = (Resources.getSystem().displayMetrics.density * this).toInt()