package com.example.physicstoybox.pfa72bc90.domain.model

data class BlockSpec(val id: Int, val x: Float, val y: Float, val z: Float, val colorIndex: Int)
data class ToyboxSnapshot(val shots: Int = 0, val toppled: Int = 0)
