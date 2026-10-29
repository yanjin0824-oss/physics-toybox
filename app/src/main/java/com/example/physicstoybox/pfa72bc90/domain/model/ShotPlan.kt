package com.example.physicstoybox.pfa72bc90.domain.model

data class ShotPoint(val x: Float, val y: Float, val z: Float)
data class ShotBounds(val min: ShotPoint, val max: ShotPoint)
data class ShotPlan(val origin: ShotPoint, val velocity: ShotPoint, val flightTime: Float)
