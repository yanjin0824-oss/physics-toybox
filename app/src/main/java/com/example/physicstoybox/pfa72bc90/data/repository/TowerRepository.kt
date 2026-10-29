package com.example.physicstoybox.pfa72bc90.data.repository

interface TowerRepository { fun layers(): List<Pair<Int, Int>> }
class DefaultTowerRepository : TowerRepository {
    override fun layers() = listOf(5 to 4, 4 to 3, 3 to 3, 2 to 3, 1 to 2, 1 to 1)
}
