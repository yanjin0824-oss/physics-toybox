package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LengthUnit
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.physicstoybox.pfa72bc90.domain.model.BlockSpec
import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxEvent
import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxUiState
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.ViewCoordinateSpace
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialPointerEvent

@Composable
fun ToyboxStage(blocks: List<BlockSpec>, state: ToyboxUiState, onEvent: (ToyboxEvent) -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lengthConverter = LocalPhysicalLengthConverter.current
    val panelScale = 1.3f / lengthConverter.dpToLength(960.dp, LengthUnit.Meters)
    val tracking = remember { HeadPoseTracker() }
    val scene = remember { ToyboxScene(blocks, viewerPose = tracking::currentPose,
        onReady = { onEvent(ToyboxEvent.Ready) }, observed = { onEvent(ToyboxEvent.Observed(it)) }) }
    val contentHolder = remember { arrayOfNulls<SpatialViewContent>(1) }
    val panelHolder = remember { arrayOfNulls<Entity>(1) }
    val panelInput = remember { arrayOfNulls<PanelInputRouter>(1) }
    val panelSize = remember { arrayOf(IntSize.Zero) }
    val buttonBounds = remember { mutableMapOf<ToyboxEvent, androidx.compose.ui.geometry.Rect>() }
    DisposableEffect(scene, tracking) {
        onDispose {
            panelInput[0]?.cancel()
            tracking.stop()
            scene.dispose()
        }
    }
    LaunchedEffect(state.pendingCommands) {
        // StateFlow may combine UI updates; keep every command until the scene consumes it.
        for (command in state.pendingCommands) {
            if (command.rebuild) scene.rebuild() else scene.fire()
        }
        state.pendingCommands.lastOrNull()?.let { onEvent(ToyboxEvent.CommandsHandled(it.id)) }
    }
    // design-node: toybox — Mixed Stage with live HMD-origin firing and attached controls.

    SpatialView(modifier = Modifier.fillMaxSize().pointerInput(scene) {
        var shotPointer: androidx.compose.ui.input.pointer.PointerId? = null
        var shotTarget: Entity? = null
        var pressX = 0f
        var pressY = 0f
        detectSpatialPointerEvent(context = context, targetedToEntity = TargetEntity.any()) { events ->
            var handled = false
            for (event in events) {
                val content = contentHolder[0] ?: continue
                val target = event.targetedEntity
                val stagePosition = content.convertPosition(Vector3(event.x, event.y, event.z),
                    ViewCoordinateSpace.Local, content.localSpatialCoordinateSpace)
                if (panelInput[0]?.dispatch(event.pointerId.value,
                        event.isDownEvent(), event.isUpEvent(), event.pressed,
                        target == panelHolder[0], stagePosition) == true) {
                    handled = true
                    shotPointer = null; shotTarget = null
                    continue
                }
                if (event.isDownEvent() && target != null && scene.isTarget(target)) {
                    shotPointer = event.pointerId; shotTarget = target
                    pressX = event.x; pressY = event.y
                    handled = true
                } else if (event.pointerId == shotPointer) {
                    handled = true
                    val dx = event.x - pressX; val dy = event.y - pressY
                    if (target != shotTarget || dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop) {
                        shotTarget = null
                    }
                    if (event.isUpEvent()) {
                        shotTarget?.let { scene.fireAt(it) }
                        shotPointer = null; shotTarget = null
                    } else if (!event.pressed) { shotPointer = null; shotTarget = null }
                }
            }
            handled
        }
    }, attachments = {
        AttachmentPanel("toybox_controls", size = IntSize(with(density) { 960.dp.roundToPx() }, with(density) { 148.dp.roundToPx() })) {
            ToyboxControls(state.snapshot, state.ready, state.error,
                fire = { onEvent(ToyboxEvent.Fire) }, rebuild = { onEvent(ToyboxEvent.Rebuild) },
                onPanelSize = { panelSize[0] = it; panelInput[0]?.updateSize(it) },
                onButtonBounds = { event, bounds ->
                    buttonBounds[event] = bounds; panelInput[0]?.updateButton(event, bounds)
                })
        }
    }, initial = { content, attachments ->
        contentHolder[0] = content
        try {
            scene.attach(content)
            attachments.entity("toybox_controls")?.let {
                panelHolder[0] = it
                val metersPerPixel = lengthConverter.dpToLength(1.dp, LengthUnit.Meters) / density.density
                scene.placeControls(it, panelScale, Vector3(
                    lengthConverter.dpToLength(960.dp, LengthUnit.Meters),
                    lengthConverter.dpToLength(148.dp, LengthUnit.Meters), .002f))
                panelInput[0] = PanelInputRouter(it, metersPerPixel, onEvent).apply {
                    updateSize(panelSize[0])
                    buttonBounds.forEach { (event, bounds) -> updateButton(event, bounds) }
                }
            }
            tracking.start()
        } catch (error: Exception) {
            Log.e("PhysicsToybox", "Scene initialization failed", error)
            onEvent(ToyboxEvent.Failed)
        }
    })

}
