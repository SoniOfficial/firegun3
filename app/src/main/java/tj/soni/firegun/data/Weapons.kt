package tj.soni.firegun.data

enum class SfxId { PISTOL, RIFLE, SHOTGUN, SNIPER, MG, EXPLOSION, RELOAD, HIT, CLICK, EMPTY, PICKUP, VICTORY, GAMEOVER }

data class WeaponDef(
    val id: Int,
    val name: String,
    val price: Int,
    val damage: Float,
    val fireDelay: Float,
    val reloadTime: Float,
    val magSize: Int,
    val pellets: Int,
    val spread: Float,
    val bulletSpeed: Float,
    val range: Float,
    val color: Int,
    val sfx: SfxId,
    val shake: Float,
    val pierce: Int,
    val gunLen: Float
)

object Weapons {
    val all: List<WeaponDef> = listOf(
        WeaponDef(0, "Pistol", 0, 18f, 0.34f, 1.0f, 12, 1, 0.04f, 820f, 700f, 0xFFFFE082.toInt(), SfxId.PISTOL, 0.5f, 0, 13f),
        WeaponDef(1, "Assault Rifle", 1000, 14f, 0.12f, 1.6f, 30, 1, 0.07f, 980f, 800f, 0xFFFFD54F.toInt(), SfxId.RIFLE, 0.8f, 0, 26f),
        WeaponDef(2, "Shotgun", 1500, 11f, 0.85f, 1.9f, 6, 7, 0.32f, 760f, 380f, 0xFFFFAB40.toInt(), SfxId.SHOTGUN, 2.5f, 0, 24f),
        WeaponDef(3, "Sniper Rifle", 2000, 95f, 1.1f, 2.2f, 5, 1, 0.0f, 1500f, 1400f, 0xFF80D8FF.toInt(), SfxId.SNIPER, 3f, 2, 34f),
        WeaponDef(4, "Machine Gun", 2500, 10f, 0.07f, 3.0f, 80, 1, 0.12f, 1000f, 800f, 0xFFFF8A65.toInt(), SfxId.MG, 0.9f, 0, 30f)
    )

    fun get(id: Int): WeaponDef = all[id.coerceIn(0, all.size - 1)]
}
