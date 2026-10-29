package com.example.physicstoybox.pfa72bc90.ui.toybox
import com.example.physicstoybox.pfa72bc90.data.repository.DefaultTowerRepository
import com.example.physicstoybox.pfa72bc90.domain.usecase.BuildTowerUseCase
import com.example.physicstoybox.pfa72bc90.domain.model.ToyboxSnapshot
import org.junit.Test
import org.junit.Assert.*
class ToyboxViewModelTest {
    private fun vm() = ToyboxViewModel(BuildTowerUseCase(DefaultTowerRepository()))
    @Test fun initialSceneHas50BlocksAndWaitsForReadiness() {
        val vm = vm(); assertEquals(50,vm.blocks.size); assertFalse(vm.state.value.ready)
    }
    @Test fun fireIgnoredBeforeSceneReady() {
        val vm = vm(); vm.onEvent(ToyboxEvent.Fire); assertEquals(0L,vm.state.value.command)
    }
    @Test fun fireAfterReadyIssuesExactlyOneCommand() {
        val vm = vm(); vm.onEvent(ToyboxEvent.Ready); vm.onEvent(ToyboxEvent.Fire)
        assertEquals(listOf(ToyboxCommand(1, false)),vm.state.value.pendingCommands)
    }
    @Test fun rebuildClearsErrorAndSelectsResetCommand() {
        val vm = vm(); vm.onEvent(ToyboxEvent.Failed); vm.onEvent(ToyboxEvent.Rebuild)
        assertFalse(vm.state.value.error); assertTrue(vm.state.value.pendingCommands.single().rebuild)
    }
    @Test fun observedPhysicsOnlyUpdatesCounters() {
        val vm = vm(); vm.onEvent(ToyboxEvent.Observed(ToyboxSnapshot(3,29)))
        assertEquals(29,vm.state.value.snapshot.toppled); assertEquals(0L,vm.state.value.command)
    }
    @Test fun burstIsRetainedUntilSceneAcknowledgesAndKeepsRebuildOrder() {
        val vm = vm(); vm.onEvent(ToyboxEvent.Ready)
        repeat(12) { vm.onEvent(ToyboxEvent.Fire) }
        vm.onEvent(ToyboxEvent.Rebuild); vm.onEvent(ToyboxEvent.Fire)
        assertEquals((1L..14L).toList(), vm.state.value.pendingCommands.map { it.id })
        assertEquals(listOf(false, true, false), vm.state.value.pendingCommands.takeLast(3).map { it.rebuild })
        vm.onEvent(ToyboxEvent.CommandsHandled(12))
        assertEquals(listOf(ToyboxCommand(13, true), ToyboxCommand(14, false)), vm.state.value.pendingCommands)
        vm.onEvent(ToyboxEvent.CommandsHandled(12))
        assertEquals(2, vm.state.value.pendingCommands.size)
        vm.onEvent(ToyboxEvent.CommandsHandled(14))
        assertTrue(vm.state.value.pendingCommands.isEmpty())
    }
}
