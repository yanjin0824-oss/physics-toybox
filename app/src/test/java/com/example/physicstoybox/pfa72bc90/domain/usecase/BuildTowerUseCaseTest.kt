package com.example.physicstoybox.pfa72bc90.domain.usecase
import com.example.physicstoybox.pfa72bc90.data.repository.DefaultTowerRepository
import com.example.physicstoybox.pfa72bc90.data.repository.TowerRepository
import org.junit.Test
import org.junit.Assert.*
class BuildTowerUseCaseTest {
    @Test fun exactly50DistinctBlocks() {
        val blocks = BuildTowerUseCase(DefaultTowerRepository())()
        assertEquals(50, blocks.size)
        assertEquals(50, blocks.map { Triple(it.x, it.y, it.z) }.toSet().size)
        assertEquals((0..49).toList(), blocks.map { it.id })
    }
    @Test fun blocksHaveStableSupportsAndUseAllSixColors() {
        val blocks = BuildTowerUseCase(DefaultTowerRepository())()
        assertEquals(6, blocks.map { it.colorIndex }.toSet().size)
        blocks.filter { it.y > -.33f }.forEach { block ->
            assertTrue(blocks.any { support ->
                kotlin.math.abs(support.y + .12f - block.y) < .001f &&
                kotlin.math.abs(support.x - block.x) < .12f &&
                kotlin.math.abs(support.z - block.z) < .12f
            })
        }
    }
    @Test fun emptyLayoutProducesNoBodies() {
        assertTrue(BuildTowerUseCase(object : TowerRepository { override fun layers() = emptyList<Pair<Int,Int>>() })().isEmpty())
    }
}
