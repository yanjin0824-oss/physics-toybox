package com.example.physicstoybox.pfa72bc90.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.physicstoybox.pfa72bc90.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
