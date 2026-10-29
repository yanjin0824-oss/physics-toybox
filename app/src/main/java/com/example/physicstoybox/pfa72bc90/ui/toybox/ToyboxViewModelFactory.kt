package com.example.physicstoybox.pfa72bc90.ui.toybox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.physicstoybox.pfa72bc90.data.repository.DefaultTowerRepository
import com.example.physicstoybox.pfa72bc90.domain.usecase.BuildTowerUseCase

class ToyboxViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ToyboxViewModel(BuildTowerUseCase(DefaultTowerRepository())) as T
}
