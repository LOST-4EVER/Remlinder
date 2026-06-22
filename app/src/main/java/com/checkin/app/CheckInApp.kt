package com.checkin.app

import android.app.Application
import com.checkin.app.data.repository.CheckInRepository
import com.checkin.app.domain.SchedulingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CheckInApp : Application() {

    @Inject
    lateinit var repository: CheckInRepository

    @Inject
    lateinit var schedulingManager: SchedulingManager

    companion object {
        lateinit var instance: CheckInApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
