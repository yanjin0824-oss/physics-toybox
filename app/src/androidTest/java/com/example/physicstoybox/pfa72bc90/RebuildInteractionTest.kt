package com.example.physicstoybox.pfa72bc90

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RebuildInteractionTest {
    @Test
    fun fireThenRebuildRepeatedlyRestoresCountersAndKeepsSceneOpen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fireDescription = context.getString(R.string.toybox_fire_description)
        val rebuildDescription = context.getString(R.string.toybox_rebuild_description)
        val readyHint = context.getString(R.string.toybox_hint_ready)
        val emptyStats = context.getString(R.string.toybox_stats, 0, 0)
        fun shotLabel(shots: Int) = context.getString(R.string.toybox_stats, shots, 0).substringBefore(" · ")
        val automation = instrumentation.uiAutomation
        val serviceInfo = automation.serviceInfo
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = serviceInfo
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.packageName?.toString() == context.packageName && predicate(node)) return node
            for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
            return null
        }

        fun awaitNode(timeout: Long = 30_000, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
            val deadline = SystemClock.uptimeMillis() + timeout
            while (SystemClock.uptimeMillis() < deadline) {
                find(automation.rootInActiveWindow, predicate)?.let { return it }
                automation.windows.forEach { window -> find(window.root, predicate)?.let { return it } }
                SystemClock.sleep(100)
            }
            throw AssertionError("Expected Toybox control or counters were absent")
        }

        fun awaitBallBlockCollision() {
            val deadline = SystemClock.uptimeMillis() + 30_000
            var roundLog = ""
            while (SystemClock.uptimeMillis() < deadline) {
                val log = ParcelFileDescriptor.AutoCloseInputStream(
                    automation.executeShellCommand("logcat -d -s PhysicsToybox:I *:S"))
                    .bufferedReader().use { it.readText() }
                roundLog = log.substringAfterLast("REBUILD blocks=50 balls=0")
                if (roundLog.contains("HIT shot=1 block=")) return
                SystemClock.sleep(250)
            }
            throw AssertionError("Cannon ball did not collide with any native block after Fire: $roundLog")
        }

        fun click(description: String) {
            var node = awaitNode { it.contentDescription?.toString() == description && it.isEnabled }
            while (!node.isClickable) node = node.parent ?: throw AssertionError("Control has no click action")
            assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }

        // Mixed Stage waits for the first live head pose before enabling Fire.
        // Read the visible readiness state instead of clicking a disabled startup button.
        awaitNode(timeout = 60_000) { it.text?.toString() == readyHint }
        fun sceneLog(): String = ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("logcat -d -s PhysicsToybox:I *:S"))
            .bufferedReader().use { it.readText() }.substringAfterLast("SCENE_READY")
        val previousRebuilds = Regex("REBUILD blocks=50 balls=0").findAll(sceneLog()).count()
        // Verify consecutive Rebuild commands execute even when counters are already zero.
        // Seeing the initial zero-shot label alone would let a dead Rebuild button pass.
        repeat(3) { index ->
            click(rebuildDescription)
            val deadline = SystemClock.uptimeMillis() + 30_000
            var count = 0
            while (SystemClock.uptimeMillis() < deadline) {
                count = Regex("REBUILD blocks=50 balls=0").findAll(sceneLog()).count()
                if (count >= previousRebuilds + index + 1) break
                SystemClock.sleep(100)
            }
            assertTrue("Consecutive Rebuild command was not executed", count >= previousRebuilds + index + 1)
            assertTrue("Rebuild unexpectedly fired a ball", !sceneLog().contains("FIRE shots="))
        }
        var burstButton = awaitNode { it.contentDescription?.toString() == fireDescription }
        while (!burstButton.isClickable) burstButton = burstButton.parent!!
        repeat(12) { assertTrue("Fire action rejected during burst", burstButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) }
        awaitNode { it.text?.toString()?.startsWith(shotLabel(12)) == true }
        click(rebuildDescription)
        awaitNode { it.text?.toString() == emptyStats }
        // First verify consecutive shots in the same round. Resetting after every
        // shot previously hid failures that only happened on the second Fire.
        repeat(6) { index ->
            click(fireDescription)
            awaitNode { it.text?.toString()?.startsWith(shotLabel(index + 1)) == true }
        }
        click(rebuildDescription)
        awaitNode { it.text?.toString() == emptyStats }
        repeat(4) {
            click(fireDescription)
            awaitNode { it.text?.toString()?.startsWith(shotLabel(1)) == true }
            // A counter/toppled count alone does not prove a real projectile hit.
            awaitBallBlockCollision()
            click(rebuildDescription)
            awaitNode { it.text?.toString() == emptyStats }
        }
        awaitNode { it.contentDescription?.toString() == fireDescription && it.isEnabled }
    }
}
