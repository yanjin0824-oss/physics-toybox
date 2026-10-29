package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import android.util.Log
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hmd.HMDPose
import com.pico.spatial.tracking.hmd.HMDTrackingData
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import java.util.concurrent.atomic.AtomicReference

/** Copies live Stage-coordinate poses; the tracking callback never mutates ECS or Compose. */
internal class HeadPoseTracker {
    private val provider = HMDTrackingProvider()
    private val latest = AtomicReference<HMDPose?>(null)
    private var started = false
    private val listener = DataProvider.DataListener<HMDTrackingData> { latest.set(it.hmdPose) }

    fun start() {
        if (started) return
        started = true
        provider.addListener(listener)
        val result = provider.start()
        Log.i("PhysicsToybox", "HMD_START result=$result support=${provider.supportState} state=${provider.state}")
    }

    fun currentPose(): HMDPose? {
        if (!started || provider.supportState != DataProvider.SupportState.SUPPORTED ||
            provider.state != DataProvider.State.STARTED) return null
        val pose = latest.get() ?: return null
        val p = pose.position
        return pose.takeIf { p.x.isFinite() && p.y.isFinite() && p.z.isFinite() }
    }

    fun stop() {
        if (!started) return
        started = false
        provider.removeListener(listener)
        provider.stop()
        latest.set(null)
    }
}
