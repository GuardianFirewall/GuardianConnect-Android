package com.guardianconnect.demo

import android.os.StrictMode
import com.guardianconnect.managers.GRDConnectManager

class DemoApplication : android.app.Application() {

    override fun onCreate() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                // or .detectAll() for all detectable problems
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        super.onCreate()
        GRDConnectManager().init(this.applicationContext)
    }
}