package com.example.physicstoybox.pfa72bc90.ui.toybox

import com.example.physicstoybox.pfa72bc90.domain.model.ToyboxSnapshot

data class ToyboxCommand(val id: Long, val rebuild: Boolean)
data class ToyboxUiState(val snapshot: ToyboxSnapshot = ToyboxSnapshot(), val ready: Boolean = false,
    val command: Long = 0, val pendingCommands: List<ToyboxCommand> = emptyList(), val error: Boolean = false)
sealed interface ToyboxEvent {
    data object Ready : ToyboxEvent
    data object Fire : ToyboxEvent
    data object Rebuild : ToyboxEvent
    data object Failed : ToyboxEvent
    data class CommandsHandled(val throughId: Long) : ToyboxEvent
    data class Observed(val snapshot: ToyboxSnapshot) : ToyboxEvent
}
