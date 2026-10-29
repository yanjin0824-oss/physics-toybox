package com.example.physicstoybox.pfa72bc90.domain.usecase

import com.example.physicstoybox.pfa72bc90.domain.model.ShotBounds
import com.example.physicstoybox.pfa72bc90.domain.model.ShotPlan
import com.example.physicstoybox.pfa72bc90.domain.model.ShotPoint
import com.example.physicstoybox.pfa72bc90.domain.model.ViewingSide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Camera-to-target shooting with a sphere-safe entry point inside a bounded volume. */
class PlanShotUseCase {
    /** Full Space can launch directly from the actual HMD position without window clipping. */
    fun fromHead(camera: ShotPoint, target: ShotPoint, muzzleDistance: Float = .20f,
        speed: Float = 4.5f, gravity: Float = 9.81f): ShotPlan? {
        val values = floatArrayOf(camera.x, camera.y, camera.z, target.x, target.y, target.z,
            muzzleDistance, speed, gravity)
        if (values.any { !it.isFinite() } || muzzleDistance < 0f || speed <= 0f || gravity < 0f) return null
        val dx = target.x - camera.x
        val dy = target.y - camera.y
        val dz = target.z - camera.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        if (distance <= .05f) return null
        val offset = min(muzzleDistance, distance * .5f)
        val origin = ShotPoint(camera.x + dx * offset / distance,
            camera.y + dy * offset / distance, camera.z + dz * offset / distance)
        val time = (distance - offset) / speed
        return ShotPlan(origin, ShotPoint((target.x - origin.x) / time,
            (target.y - origin.y) / time + .5f * gravity * time, (target.z - origin.z) / time), time)
    }

    /** Shared Space provides a viewing side, not a continuous camera position. */
    fun fromViewingSide(side: ViewingSide, target: ShotPoint, bounds: ShotBounds): ShotPlan? {
        val viewOrigin = when (side) {
            ViewingSide.Front -> ShotPoint(target.x, target.y, bounds.max.z + 1f)
            ViewingSide.Right -> ShotPoint(bounds.max.x + 1f, target.y, target.z)
            ViewingSide.Back -> ShotPoint(target.x, target.y, bounds.min.z - 1f)
            ViewingSide.Left -> ShotPoint(bounds.min.x - 1f, target.y, target.z)
        }
        return invoke(viewOrigin, target, bounds)
    }

    operator fun invoke(camera: ShotPoint, target: ShotPoint, bounds: ShotBounds,
        radius: Float = .05f, speed: Float = 4.5f, gravity: Float = 9.81f): ShotPlan? {
        val from = floatArrayOf(camera.x, camera.y, camera.z)
        val to = floatArrayOf(target.x, target.y, target.z)
        val lower = floatArrayOf(bounds.min.x + radius, bounds.min.y + radius, bounds.min.z + radius)
        val upper = floatArrayOf(bounds.max.x - radius, bounds.max.y - radius, bounds.max.z - radius)
        if ((from + to + lower + upper).any { !it.isFinite() } ||
            !radius.isFinite() || radius <= 0 || !speed.isFinite() || speed <= 0 ||
            !gravity.isFinite() || gravity < 0) return null
        val direction = FloatArray(3) { to[it] - from[it] }
        val distance = sqrt(direction.sumOf { (it * it).toDouble() }).toFloat()
        if (distance <= radius) return null
        var entry = 0f
        var exit = 1f
        for (axis in 0..2) {
            if (lower[axis] >= upper[axis]) return null
            if (abs(direction[axis]) < .000001f) {
                if (from[axis] < lower[axis] || from[axis] > upper[axis]) return null
            } else {
                val a = (lower[axis] - from[axis]) / direction[axis]
                val b = (upper[axis] - from[axis]) / direction[axis]
                entry = max(entry, min(a, b))
                exit = min(exit, max(a, b))
                if (entry > exit) return null
            }
        }
        // Enter from the current viewing side; an in-volume camera fires just ahead of itself.
        val offset = if (entry == 0f) .10f else .01f
        val start = entry + min(offset / distance, (exit - entry) * .5f)
        val origin = FloatArray(3) { from[it] + direction[it] * start }
        val remaining = FloatArray(3) { to[it] - origin[it] }
        val travel = sqrt(remaining.sumOf { (it * it).toDouble() }).toFloat()
        if (travel <= radius) return null
        val time = travel / speed
        return ShotPlan(
            ShotPoint(origin[0], origin[1], origin[2]),
            ShotPoint(remaining[0] / time, remaining[1] / time + .5f * gravity * time,
                remaining[2] / time), time)
    }
}
