package com.guardianconnect.demo

import com.guardianconnect.GRDConnectManager

class DemoApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        GRDConnectManager().init(this.applicationContext)
    }
}