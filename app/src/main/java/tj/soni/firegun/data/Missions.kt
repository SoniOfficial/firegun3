package tj.soni.firegun.data

enum class MissionType { KILL, SURVIVE, BOSS }

/** weights = relative spawn weights for [basic, fast, heavy, sniper] */
data class MissionDef(
    val id: Int,
    val name: String,
    val brief: String,
    val type: MissionType,
    val target: Int,
    val theme: Int,
    val weights: List<Int>,
    val spawnInterval: Float,
    val maxAlive: Int,
    val diff: Float,
    val bosses: Int,
    val rewardCoins: Int,
    val rewardScore: Int
)

object Missions {
    val all: List<MissionDef> = listOf(
        MissionDef(1, "First Contact", "Eliminate 12 enemy soldiers near the road.", MissionType.KILL, 12, 0, listOf(10, 0, 0, 0), 2.6f, 5, 1.0f, 0, 300, 800),
        MissionDef(2, "Road Patrol", "Clear 20 hostiles. Fast scouts are in the area.", MissionType.KILL, 20, 0, listOf(8, 4, 0, 0), 2.2f, 6, 1.0f, 0, 400, 1000),
        MissionDef(3, "Heavy Armor", "Heavy troopers approach. Take down 24 enemies.", MissionType.KILL, 24, 0, listOf(6, 3, 3, 0), 2.1f, 7, 1.1f, 0, 500, 1200),
        MissionDef(4, "Sniper Alley", "Snipers cover the field. Eliminate 25.", MissionType.KILL, 25, 0, listOf(5, 3, 2, 3), 2.0f, 7, 1.1f, 0, 600, 1400),
        MissionDef(5, "Hold the Line", "Survive for 90 seconds against constant attack.", MissionType.SURVIVE, 90, 0, listOf(6, 4, 2, 1), 1.6f, 9, 1.15f, 0, 700, 1600),
        MissionDef(6, "Desert Storm", "Push through the desert. Eliminate 35 enemies.", MissionType.KILL, 35, 1, listOf(5, 4, 3, 2), 1.7f, 9, 1.25f, 0, 800, 1800),
        MissionDef(7, "Blitz", "Fast assault! Eliminate 40 enemies.", MissionType.KILL, 40, 1, listOf(3, 10, 2, 1), 1.3f, 10, 1.3f, 0, 900, 2000),
        MissionDef(8, "The Commander", "Defeat the enemy commander and his guards.", MissionType.BOSS, 1, 1, listOf(6, 3, 2, 1), 3.0f, 6, 1.35f, 1, 1200, 3000),
        MissionDef(9, "Night Siege", "Survive 150 seconds of heavy assault.", MissionType.SURVIVE, 150, 2, listOf(4, 4, 4, 3), 1.3f, 11, 1.5f, 0, 1400, 3500),
        MissionDef(10, "Final Assault", "Destroy both enemy warlords. No retreat.", MissionType.BOSS, 2, 2, listOf(4, 4, 4, 3), 2.4f, 8, 1.7f, 2, 2000, 5000)
    )

    fun get(id: Int): MissionDef = all[(id - 1).coerceIn(0, all.size - 1)]
}
