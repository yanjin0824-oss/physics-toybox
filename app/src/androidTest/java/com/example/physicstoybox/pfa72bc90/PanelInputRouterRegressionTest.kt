package com.example.physicstoybox.pfa72bc90

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxEvent
import com.example.physicstoybox.pfa72bc90.ui.toybox.components.PanelInputRouter
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PanelInputRouterRegressionTest {
    @Test
    fun repeatedPanelTapsRespectNativeTransformsAndRecoverAfterCancellation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val owner = Entity()
            val panel = Entity()
            owner.addChild(panel)
            try {
                owner.components[TransformComponent::class.java]!!.position = Vector3(0f, 1.1f, -2.2f)
                panel.components[TransformComponent::class.java]!!.apply {
                    position = Vector3(0f, -.3f, .7f)
                    setScaleVector(Vector3(1.6927083f))
                }
                val actions = mutableListOf<ToyboxEvent>()
                val router = PanelInputRouter(panel, .0004f, actions::add)
                router.updateSize(IntSize(1920, 296))
                // Actual bounds observed in the native attachment's Compose root.
                router.updateButton(ToyboxEvent.Fire, Rect(912f, 144f, 1074f, 240f))
                router.updateButton(ToyboxEvent.Rebuild, Rect(1098f, 144f, 1292f, 240f))
                fun stage(x: Float, y: Float) = panel.convertPositionTo(
                    Vector3((x - 960f) * .0004f, (148f - y) * .0004f, 0f), null)
                fun tap(x: Float) {
                    val position = stage(x, 193f)
                    router.dispatch(1, true, false, true, true, position)
                    router.dispatch(1, false, true, false, true, position)
                }
                repeat(3) { tap(1193f) }
                tap(993f)
                assertEquals(listOf(ToyboxEvent.Rebuild, ToyboxEvent.Rebuild,
                    ToyboxEvent.Rebuild, ToyboxEvent.Fire), actions)
                actions.clear()

                // A press that leaves the button/panel must neither activate nor lock input.
                router.dispatch(1, true, false, true, true, stage(1193f, 193f))
                router.dispatch(1, false, true, false, false, stage(1193f, 193f))
                router.dispatch(1, true, false, true, true, stage(1193f, 193f))
                router.dispatch(1, false, false, true, true, stage(993f, 193f))
                router.dispatch(1, false, true, false, true, stage(993f, 193f))
                assertEquals(emptyList<ToyboxEvent>(), actions)
                tap(993f)
                assertEquals(listOf(ToyboxEvent.Fire), actions)

                // Native parent movement/scale must not invalidate button pixel mapping.
                owner.components[TransformComponent::class.java]!!.apply {
                    position = Vector3(.5f, 1.3f, -1.9f)
                    setScaleVector(Vector3(.8f))
                }
                tap(1193f)
                assertEquals(listOf(ToyboxEvent.Fire, ToyboxEvent.Rebuild), actions)
            } finally { owner.destroy(true) }
        }
    }
}
