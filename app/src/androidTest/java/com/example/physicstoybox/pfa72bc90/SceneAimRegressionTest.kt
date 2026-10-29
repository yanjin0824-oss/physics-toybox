package com.example.physicstoybox.pfa72bc90

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.physicstoybox.pfa72bc90.ui.toybox.components.SceneResources
import com.example.physicstoybox.pfa72bc90.ui.toybox.components.ToyboxScene
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneAimRegressionTest {
    @Test
    fun clickedBlockUsesItsNativeBoundsInTranslatedArena() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resources = SceneResources()
            val scene = ToyboxScene(emptyList(), { null }, {}, {})
            try {
                scene.root.components[TransformComponent::class.java]!!.position = Vector3(0f, 1.1f, -2.2f)
                val block = ModelEntity(resources.retain(MeshResource.createBox(Vector3(.12f))),
                    resources.retain(PhysicallyBasedMaterial.create()))
                scene.root.addChild(block)
                for (position in listOf(Vector3(.12f, -.10f, -.10f), Vector3(-.2f, -.30f, .15f))) {
                    block.components[TransformComponent::class.java]!!.position = position
                    // Target the native Entity in the translated arena, including moved blocks.
                    val aim = scene.aimPoint(block)
                    val bounds = block.getVisualBounds(scene.root)
                    assertEquals((bounds.min.x + bounds.max.x) / 2f, aim.x, .001f)
                    assertEquals((bounds.min.y + bounds.max.y) / 2f, aim.y, .001f)
                    assertEquals((bounds.min.z + bounds.max.z) / 2f, aim.z, .001f)
                }
            } finally {
                scene.root.destroy(true)
                resources.close()
            }
        }
    }
}
