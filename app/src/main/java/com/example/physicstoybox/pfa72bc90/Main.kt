package com.example.physicstoybox.pfa72bc90

import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxScreen
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultStage { PicoTheme { ToyboxScreen() } }
}
