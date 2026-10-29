package com.example.physicstoybox.pfa72bc90.ui.toybox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.physicstoybox.pfa72bc90.ui.toybox.components.ToyboxStage

@Composable
fun ToyboxScreen() {
    val viewModel: ToyboxViewModel = viewModel(factory = ToyboxViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    ToyboxContent(viewModel, state)
}

@Composable
private fun ToyboxContent(viewModel: ToyboxViewModel, state: ToyboxUiState) {
    ToyboxStage(viewModel.blocks, state,
        onEvent = viewModel::onEvent)
}
