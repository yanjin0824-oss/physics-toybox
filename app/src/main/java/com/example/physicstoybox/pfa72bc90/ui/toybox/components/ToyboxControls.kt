package com.example.physicstoybox.pfa72bc90.ui.toybox.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import com.example.physicstoybox.pfa72bc90.R
import com.example.physicstoybox.pfa72bc90.ui.toybox.ToyboxEvent
import com.example.physicstoybox.pfa72bc90.domain.model.ToyboxSnapshot
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

// design-node: controls — auxiliary / always-visible / supports / inline.
// Native attachment on the arena front rail inside the Mixed Stage.
@Composable
fun ToyboxControls(snapshot: ToyboxSnapshot, ready: Boolean, error: Boolean,
    fire: () -> Unit, rebuild: () -> Unit,
    onPanelSize: (IntSize) -> Unit = {},
    onButtonBounds: (ToyboxEvent, Rect) -> Unit = { _, _ -> }) {
    val fireDescription = stringResource(R.string.toybox_fire_description)
    val rebuildDescription = stringResource(R.string.toybox_rebuild_description)
    Column(Modifier.width(960.dp).onGloballyPositioned {
        onPanelSize(it.size)
    }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // design-node: title
        Text(stringResource(R.string.app_name), style = PicoTheme.typography.headlineSmall,
            color = PicoTheme.colorScheme.labelPrimaryLight)
        // design-node: hint
        Text(stringResource(if (error) R.string.toybox_hint_error else if (ready)
            R.string.toybox_hint_ready else R.string.toybox_hint_preparing),
            style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimaryLight)
        // design-node: action-row
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // design-node: stats
            Text(stringResource(R.string.toybox_stats, snapshot.shots, snapshot.toppled),
                style = PicoTheme.typography.labelMedium, color = PicoTheme.colorScheme.labelPrimaryLight)
            // design-node: fire
            Button(onClick = fire, enabled = ready,
                modifier = Modifier.onGloballyPositioned { onButtonBounds(ToyboxEvent.Fire, it.boundsInRoot()) }
                    .semantics { contentDescription = fireDescription }) { Text(stringResource(R.string.toybox_fire)) }
            // design-node: rebuild
            Button(onClick = rebuild,
                modifier = Modifier.onGloballyPositioned { onButtonBounds(ToyboxEvent.Rebuild, it.boundsInRoot()) }
                    .semantics { contentDescription = rebuildDescription }) { Text(stringResource(R.string.toybox_rebuild)) }
        }
    }
}
