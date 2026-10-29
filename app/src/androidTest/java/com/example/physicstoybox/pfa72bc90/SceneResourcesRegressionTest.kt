package com.example.physicstoybox.pfa72bc90

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.physicstoybox.pfa72bc90.ui.toybox.components.SceneResources
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.core.math.Vector3
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneResourcesRegressionTest {
    @Test
    fun sharedNativeResourcesSurviveRepeatedLastOwnerDestruction() {
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resources = SceneResources()
            try {
                val mesh = resources.retain(MeshResource.createBox(Vector3(.48f)))
                val material = resources.retain(PhysicallyBasedMaterial.create())
                val shape = resources.retain(ShapeResource.createBox(Vector3(.48f)))
                repeat(4) {
                    val entities = List(50) {
                        ModelEntity(mesh, material).apply {
                            components.set(CollisionComponent(listOf(shape), PhysicsMaterialResource(), CollisionResponseMode.COLLIDER_FULL))
                        }
                    }
                    entities.forEach { it.destroy(true) }
                }
            } catch (error: Throwable) {
                failure = error
            } finally {
                resources.close()
            }
        }
        failure?.let { throw AssertionError("Shared resources could not be reused after Rebuild", it) }
    }
}
