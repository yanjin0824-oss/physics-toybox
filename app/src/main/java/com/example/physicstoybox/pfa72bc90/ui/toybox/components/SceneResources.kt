package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import com.pico.spatial.core.ecs.resource.Resource

/** Resources shared by successive generations of entities in one scene. */
internal class SceneResources {
    private val resources = mutableListOf<Resource>()

    fun <T : Resource> retain(resource: T): T = resource.also {
        // Keeping a Kotlin reference does not retain the SDK's native resource.
        // Rebuild destroys the final entity owner; explicitly retain the resource
        // until the scene closes so the next generation can reuse it.
        it.toGlobal()
        resources.add(it)
    }

    fun close() {
        resources.asReversed().forEach { it.close() }
        resources.clear()
    }
}
