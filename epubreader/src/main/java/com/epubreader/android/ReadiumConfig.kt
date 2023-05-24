package com.epubreader.android

import android.app.Application
import com.google.android.material.color.DynamicColors
import timber.log.Timber

object ReadiumConfig {
    fun init(application: Application) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        Timber.plant(Timber.DebugTree())
    }
}
