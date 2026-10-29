package com.example.physicstoybox.pfa72bc90.ui.toybox

import androidx.lifecycle.ViewModel
import com.example.physicstoybox.pfa72bc90.domain.model.BlockSpec
import com.example.physicstoybox.pfa72bc90.domain.model.ToyboxSnapshot
import com.example.physicstoybox.pfa72bc90.domain.usecase.BuildTowerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ToyboxViewModel(buildTower: BuildTowerUseCase) : ViewModel() {
    val blocks: List<BlockSpec> = buildTower()
    private val mutableState = MutableStateFlow(ToyboxUiState())
    val state = mutableState.asStateFlow()
    fun onEvent(event: ToyboxEvent) {
        mutableState.update { current -> when (event) {
            ToyboxEvent.Ready -> current.copy(ready = true, error = false)
            ToyboxEvent.Fire -> if (current.ready) current.copy(command = current.command + 1,
                pendingCommands = current.pendingCommands + ToyboxCommand(current.command + 1, false)) else current
            ToyboxEvent.Rebuild -> current.copy(command = current.command + 1,
                pendingCommands = current.pendingCommands + ToyboxCommand(current.command + 1, true), error = false)
            ToyboxEvent.Failed -> current.copy(ready = false, error = true)
            is ToyboxEvent.CommandsHandled -> current.copy(
                pendingCommands = current.pendingCommands.filter { it.id > event.throughId })
            is ToyboxEvent.Observed -> current.copy(snapshot = event.snapshot)
        } }
    }
}
