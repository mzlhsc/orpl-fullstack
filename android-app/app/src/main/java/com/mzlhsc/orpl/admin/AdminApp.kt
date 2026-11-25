package com.mzlhsc.orpl.admin

import android.app.Application
import com.mzlhsc.orpl.admin.data.Prefs

class AdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
