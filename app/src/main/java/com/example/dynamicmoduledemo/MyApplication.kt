package com.example.dynamicmoduledemo

import android.app.Application
import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }
}