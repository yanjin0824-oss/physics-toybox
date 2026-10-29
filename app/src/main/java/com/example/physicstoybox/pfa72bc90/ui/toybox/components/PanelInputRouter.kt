package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxEvent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.math.Vector3

/** Maps native panel hits to the current Compose button bounds, after parent transforms. */
class PanelInputRouter(private val panel: Entity, private val metersPerPixel: Float,
    private val activate: (ToyboxEvent) -> Unit) {
    private var size = IntSize.Zero
    private val buttons = mutableMapOf<ToyboxEvent, Rect>()
    private var activePointer: Long? = null
    private var pressedButton: ToyboxEvent? = null

    fun updateSize(value: IntSize) { size = value }
    fun updateButton(event: ToyboxEvent, bounds: Rect) { buttons[event] = bounds }

    fun dispatch(pointerId: Long, down: Boolean, up: Boolean, pressed: Boolean,
        targetsPanel: Boolean, stagePosition: Vector3): Boolean {
        if (activePointer != null && activePointer != pointerId) return targetsPanel
        if (activePointer == null && (!down || !targetsPanel)) return false
        if (size.width <= 0 || size.height <= 0) return targetsPanel
        val local = panel.convertPositionFrom(stagePosition, null)
        val x = size.width * .5f + local.x / metersPerPixel
        val y = size.height * .5f - local.y / metersPerPixel
        val hit = if (targetsPanel && x.isFinite() && y.isFinite()) buttons.entries.firstOrNull {
            x >= it.value.left && x < it.value.right && y >= it.value.top && y < it.value.bottom
        }?.key else null
        if (down) { activePointer = pointerId; pressedButton = hit }
        else if (!targetsPanel || hit != pressedButton) pressedButton = null
        if (up) {
            val action = pressedButton
            cancel()
            action?.let(activate)
        } else if (!pressed) cancel()
        // Panel presses must never fall through to cannon firing behind the UI.
        return true
    }

    fun cancel() { activePointer = null; pressedButton = null }
}
