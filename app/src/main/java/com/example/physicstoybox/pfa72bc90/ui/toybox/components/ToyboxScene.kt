package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.physicstoybox.pfa72bc90.domain.model.BlockSpec
import com.example.physicstoybox.pfa72bc90.domain.model.ToyboxSnapshot
import com.example.physicstoybox.pfa72bc90.domain.model.ShotPoint
import com.example.physicstoybox.pfa72bc90.domain.usecase.PlanShotUseCase
import com.example.physicstoybox.pfa72bc90.ui.theme.ToyboxColors
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.PhysicsWorldComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.RigidBodyComponent
import com.pico.spatial.core.ecs.PhysicsVelocityComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.PointLightComponent
import com.pico.spatial.core.ecs.event.SceneEvents
import com.pico.spatial.core.ecs.event.CollisionEvents
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.simulation.SolverIterations
import com.pico.spatial.core.ecs.simulation.SimulationClock
import com.pico.spatial.core.ecs.simulation.MassProperties
import com.pico.spatial.core.ecs.simulation.RigidBodyMode
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.core.ecs.simulation.CollisionDetectionMode
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.toColor4
import com.pico.spatial.tracking.hmd.HMDPose

/** Owns resources and rigid bodies for one Mixed Stage lifetime. All methods run on the main thread. */
class ToyboxScene(private val specs: List<BlockSpec>,
    private val viewerPose: () -> HMDPose?, private val onReady: () -> Unit,
    private val observed: (ToyboxSnapshot) -> Unit) {
    val root = Entity()
    private val placementOwner = Entity()
    private var readyReported = false
    private val resources = SceneResources()
    private val blocks = mutableListOf<Pair<Entity, BlockSpec>>()
    private val balls = ArrayDeque<Pair<Entity, Float>>()
    private var cubeMesh: MeshResource? = null
    private var cubeShape: ShapeResource? = null
    private var ballMesh: MeshResource? = null
    private var ballShape: ShapeResource? = null
    private var blockMaterials = emptyList<Material>()
    private var ballMaterial: Material? = null
    private var unsubscribe: (() -> Unit)? = null
    private var unsubscribeCollisions: (() -> Unit)? = null
    private val shotNumbers = mutableMapOf<Entity, Int>()
    private var elapsed = 0f
    private var reportTime = 0f
    private var shots = 0
    private var lastToppled = -1
    private var attached = false
    private val planShot = PlanShotUseCase()
    val readyForShots: Boolean get() = attached && viewerPose() != null

    private fun mesh(value: MeshResource) = resources.retain(value)
    private fun shape(value: ShapeResource) = resources.retain(value)
    private fun pbr(color: Color): Material = PhysicallyBasedMaterial.create().apply {
        setBaseColor(color.toColor4()); setRoughness(.55f); setMetallic(.05f)
    }.let { resources.retain(it) }
    private fun unlit(color: Color): UnlitMaterial = UnlitMaterial.create().apply {
        setBaseColor(color.toColor4())
    }.let { resources.retain(it) }

    // design-node: scene — content / always-visible / peer / inline; native 3D physics arena.
    fun attach(content: SpatialViewContent) {
        check(!attached)
        attached = true
        root.components.set(PhysicsWorldComponent(
            gravity = Vector3(0f, -9.81f, 0f),
            solverIterations = SolverIterations(positionIterations = 12, velocityIterations = 4),
            simulationClock = SimulationClock(fixedTimeStep = 1f / 90f, maxTimeStep = .10f)))
        content.addEntity(placementOwner)
        placementOwner.addChild(root)
        root.components[TransformComponent::class.java]!!.position = Vector3(0f, 1.1f, -2.2f)
        cubeMesh = mesh(MeshResource.createBox(Vector3(.12f), .002f))
        cubeShape = shape(ShapeResource.createBox(Vector3(.12f)))
        ballMesh = mesh(MeshResource.createSphere(.05f))
        ballShape = shape(ShapeResource.createSphere(.05f))
        blockMaterials = ToyboxColors.blocks.map(::pbr)
        ballMaterial = pbr(ToyboxColors.ball)
        val groundSize = Vector3(1.28f, .05f, 1.28f)
        val ground = body(mesh(MeshResource.createBox(groundSize)), pbr(ToyboxColors.arena),
            shape(ShapeResource.createBox(groundSize)), Vector3(0f, -.425f, -.10f), null, "Ground")
        root.addChild(ground)
        groundEntity = ground
        // Thin 3D grid marks sit above the plane; no extra collision surfaces.
        val gridMat = unlit(ToyboxColors.grid)
        val xLine = mesh(MeshResource.createBox(Vector3(1.28f, .001f, .002f)))
        val zLine = mesh(MeshResource.createBox(Vector3(.002f, .001f, 1.28f)))
        for (i in -5..5) {
            root.addChild(ModelEntity(xLine, gridMat).apply {
                components[TransformComponent::class.java]!!.position = Vector3(0f, -.399f, i * .128f - .10f)
            })
            root.addChild(ModelEntity(zLine, gridMat).apply {
                components[TransformComponent::class.java]!!.position = Vector3(i * .128f, -.399f, -.10f)
            })
        }
        // Low rails bound scatter while retaining a clear view of the tower.
        val railMaterial = pbr(ToyboxColors.arena)
        for (sign in listOf(-1f, 1f)) {
            val xSize = Vector3(.02f, .12f, 1.28f)
            val zSize = Vector3(1.28f, .12f, .02f)
            root.addChild(body(mesh(MeshResource.createBox(xSize)), railMaterial,
                shape(ShapeResource.createBox(xSize)), Vector3(sign * .63f, -.34f, -.10f), null, "SideRail"))
            root.addChild(body(mesh(MeshResource.createBox(zSize)), railMaterial,
                shape(ShapeResource.createBox(zSize)), Vector3(0f, -.34f, sign * .63f - .10f), null, "EndRail"))
        }
        val backdrop = Entity().apply {
            components[TransformComponent::class.java]!!.position = Vector3(0f, .4f, -4f)
            components.set(CollisionComponent(listOf(shape(ShapeResource.createBox(Vector3(8f, 4f, .005f)))),
                PhysicsMaterialResource(), CollisionResponseMode.TRIGGER_LITE))
            components.set(InteractableComponent())
        }
        root.addChild(backdrop)
        backdropEntity = backdrop
        root.addChild(Entity().apply {
                        components.set(DirectionalLightComponent(Color4.WHITE, 2200f))
            components[TransformComponent::class.java]!!.setEulerAngles(EulerAngles(-35f, -30f, 0f))
        })
        root.addChild(Entity().apply {
                        components.set(PointLightComponent(Color4.WHITE, 800f, 3f))
            components[TransformComponent::class.java]!!.position = Vector3(.8f, 1f, .8f)
        })
        rebuild()
        val subscription = content.subscribe(SceneEvents.Update::class.java) { event -> tick(event.deltaTime) }
        unsubscribe = { subscription.cancel() }
        val collisions = content.subscribe(CollisionEvents.Enter::class.java) { event ->
            val ball = when {
                shotNumbers.containsKey(event.entityA) -> event.entityA
                shotNumbers.containsKey(event.entityB) -> event.entityB
                else -> null
            }
            if (ball != null) {
                val other = if (ball == event.entityA) event.entityB else event.entityA
                blocks.firstOrNull { it.first == other }?.let { (block, spec) ->
                    Log.i("PhysicsToybox", "HIT shot=${shotNumbers[ball]} block=${spec.id} ballPosition=${ball.convertPositionTo(Vector3.ZERO, root)} blockPosition=${block.convertPositionTo(Vector3.ZERO, root)}")
                }
            }
        }
        unsubscribeCollisions = { collisions.cancel() }
        Log.i("PhysicsToybox", "SCENE_READY blocks=${blocks.size} stage=Mixed physics=90Hz CCD=true")
    }

    private fun body(mesh: MeshResource, material: Material, collider: ShapeResource,
        position: Vector3, mass: Float?, label: String, projectile: Boolean = false): Entity =
        ModelEntity(mesh, material).apply {
            components[TransformComponent::class.java]!!.position = position
            components.set(CollisionComponent(listOf(collider),
                PhysicsMaterialResource(staticFriction = .7f, dynamicFriction = .6f, restitution = .12f),
                CollisionResponseMode.COLLIDER_FULL))
            if (mass != null) components.set(RigidBodyComponent(
                MassProperties.generateByShapesAndMass(listOf(collider), mass), RigidBodyMode.DYNAMIC).apply {
                isAffectedByGravity = true
                collisionDetectionMode = if (projectile) CollisionDetectionMode.CONTINUOUS_DYNAMIC else CollisionDetectionMode.DISCRETE
            })
            // Projectiles keep their physics collider without intercepting the next tap.
            if (!projectile && (label == "Ground" || label.startsWith("Block_"))) {
                components.set(InteractableComponent())
            }
        }

    fun isTarget(entity: Entity): Boolean = blocks.any { it.first == entity } ||
        entity == groundEntity || entity == backdropEntity

    private var backdropEntity: Entity? = null
    private var groundEntity: Entity? = null
    fun aimPoint(entity: Entity): Vector3 {
        // Targeted gestures identify the actual Entity. Use its current native bounds
        // in arena coordinates, rather than a tap coordinate from the view/Stage.
        if (entity == groundEntity || entity == backdropEntity) return Vector3(0f, -.10f, -.10f)
        val bounds = entity.getVisualBounds(root)
        return (bounds.min + bounds.max) * .5f
    }

    fun fireAt(entity: Entity): Boolean {
        if (!isTarget(entity) || !readyForShots) return false
        val previousShots = shots
        fire(aimPoint(entity))
        return shots > previousShots
    }

    fun rebuild() {
        if (!attached) return
        balls.forEach { it.first.destroy(true) }; balls.clear(); shotNumbers.clear()
        blocks.forEach { it.first.destroy(true) }; blocks.clear()
        for (spec in specs) {
            val block = body(cubeMesh!!, blockMaterials[spec.colorIndex], cubeShape!!,
                Vector3(spec.x, spec.y, spec.z), .04f, "Block_${spec.id.toString().padStart(2, '0')}")
            root.addChild(block); blocks.add(block to spec)
        }
        shots = 0; lastToppled = -1; reportTime = 0f
        observed(ToyboxSnapshot()); Log.i("PhysicsToybox", "REBUILD blocks=${blocks.size} balls=${balls.size}")
    }

    fun fire(target: Vector3 = Vector3(0f, -.10f, -.10f)) {
        if (!attached) return
        val pose = viewerPose() ?: return
        // Tracking data uses the current Stage coordinate system. The arena root
        // is translated only, so root-local direction vectors retain Stage/world axes.
        val camera = root.convertPositionFrom(pose.position, null)
        val shot = planShot.fromHead(ShotPoint(camera.x, camera.y, camera.z),
            ShotPoint(target.x, target.y, target.z)) ?: return
        val origin = Vector3(shot.origin.x, shot.origin.y, shot.origin.z)
        val velocity = Vector3(shot.velocity.x, shot.velocity.y, shot.velocity.z)
        val ball = body(ballMesh!!, ballMaterial!!, ballShape!!, origin, .25f,
            "CannonBall_${shots + 1}", projectile = true)
        if (balls.size >= 16) retireOldestBall()
        root.addChild(ball)
        ball.components.set(PhysicsVelocityComponent(velocity, Vector3.ZERO))
        balls.addLast(ball to elapsed)
        shotNumbers[ball] = shots + 1
        shots++; reportTime = .2f
        Log.i("PhysicsToybox", "FIRE shots=$shots head=${pose.position} origin=$origin target=$target velocity=$velocity")
    }

    private fun retireOldestBall() {
        val ball = balls.removeFirst().first
        shotNumbers.remove(ball)
        ball.destroy(true)
    }

    private fun tick(dt: Float) {
        if (!readyReported && readyForShots) {
            readyReported = true
            Log.i("PhysicsToybox", "HMD_READY head=${viewerPose()?.position}")
            onReady()
        }
        elapsed += dt; reportTime += dt
        while (balls.isNotEmpty() && elapsed - balls.first().second > 8f) retireOldestBall()
        if (reportTime < .15f) return
        reportTime = 0f
        val toppled = blocks.count { (entity, initial) ->
            val p = entity.components[TransformComponent::class.java]!!.position
            val dx = p.x - initial.x; val dy = p.y - initial.y; val dz = p.z - initial.z
            dx * dx + dy * dy + dz * dz > .0022f
        }
        observed(ToyboxSnapshot(shots, toppled))
        if (toppled != lastToppled) {
            lastToppled = toppled
            Log.i("PhysicsToybox", "PHYSICS shots=$shots toppled=$toppled bodies=${blocks.size} balls=${balls.size}")
        }
    }

    fun placeControls(entity: Entity, scale: Float, panelSize: Vector3) {
        root.addChild(entity)
        entity.components[TransformComponent::class.java]!!.apply {
            position = Vector3(0f, -.30f, .70f)
            setScaleVector(Vector3(scale))
        }
        // A trigger catches the UI ray before the arena without affecting ball physics.
        val panelShape = shape(ShapeResource.createBox(panelSize))
        entity.components.set(CollisionComponent(listOf(panelShape), PhysicsMaterialResource(),
            CollisionResponseMode.TRIGGER_LITE))
        entity.components.set(InteractableComponent())
    }

    fun dispose() {
        unsubscribe?.invoke(); unsubscribe = null
        unsubscribeCollisions?.invoke(); unsubscribeCollisions = null
        placementOwner.destroy(true); blocks.clear(); balls.clear(); shotNumbers.clear()
        resources.close()
        attached = false
    }
}
