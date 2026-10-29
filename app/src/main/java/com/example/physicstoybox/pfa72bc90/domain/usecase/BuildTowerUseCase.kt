package com.example.physicstoybox.pfa72bc90.domain.usecase

import com.example.physicstoybox.pfa72bc90.data.repository.TowerRepository
import com.example.physicstoybox.pfa72bc90.domain.model.BlockSpec

class BuildTowerUseCase(private val repository: TowerRepository) {
    operator fun invoke(): List<BlockSpec> = buildList {
        repository.layers().forEachIndexed { level, (nx, nz) ->
            for (x in 0 until nx) for (z in 0 until nz) {
                val index = size
                add(BlockSpec(index, (x - (nx - 1) / 2f) * .1225f,
                    -.34f + level * .12f, -.10f + (z - (nz - 1) / 2f) * .1225f,
                    (index + level) % 6))
            }
        }
    }
}
