package tj.soni.firegun.game

import tj.soni.firegun.data.Weapons

open class Body(var x: Float, var y: Float, var r: Float)

class Player(x: Float, y: Float) : Body(x, y, 18f) {
    var hp = 100f
    var maxHp = 100f
    var armor = 50f
    var maxArmor = 50f
    var angle = 0f
    var walk = 0f
    var moving = false
    var weaponId = 0
    val mag = IntArray(Weapons.all.size)
    val reserve = IntArray(Weapons.all.size)
    var reloadT = 0f
    var cooldown = 0f
    var grenades = 3
    var dead = false
    var deadT = 0f
    var invuln = 0f
    var hurtT = 0f
    var flashT = 0f
    var dmgMult = 1f
}

enum class EType(
    val label: String, val hp: Float, val speed: Float, val dmg: Float, val fireDelay: Float,
    val range: Float, val prefer: Float, val bulletSpeed: Float, val radius: Float,
    val score: Int, val coin: Int
) {
    BASIC("Soldier", 40f, 95f, 8f, 1.3f, 420f, 260f, 330f, 17f, 100, 10),
    FAST("Scout", 26f, 175f, 6f, 1.0f, 300f, 150f, 360f, 15f, 150, 15),
    HEAVY("Heavy", 150f, 62f, 12f, 0.9f, 380f, 220f, 320f, 23f, 300, 30),
    SNIPER("Sniper", 35f, 55f, 24f, 2.4f, 760f, 560f, 950f, 16f, 250, 25),
    BOSS("Commander", 1000f, 72f, 11f, 0.8f, 520f, 300f, 360f, 38f, 2000, 300)
}

class Enemy(x: Float, y: Float, val type: EType, hpMult: Float) : Body(x, y, type.radius) {
    var hp = type.hp * hpMult
    var maxHp = hp
    var alert = false
    var angle = 0f
    var walk = 0f
    var fireCd = 0.6f + Math.random().toFloat() * 1.2f
    var steerT = 0f
    var steerDir = 1f
    var strafeT = 0f
    var strafeDir = 1f
    var dying = false
    var dyingT = 0f
    var hitFlash = 0f
    var shotFlash = 0f
    var minionT = 8f
    var charging = false
    var los = false
    var losT = 0f
    var age = 0f
}

class Bullet(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val damage: Float, val fromPlayer: Boolean, var life: Float, val color: Int,
    val size: Float, var pierce: Int
) {
    var lastHit: Any? = null
}

class Grenade(var x: Float, var y: Float, var vx: Float, var vy: Float, var fuse: Float) {
    var spin = 0f
}

enum class PickType { COIN, HEALTH, AMMO, ARMOR }

class Pickup(var x: Float, var y: Float, val type: PickType, val value: Int) {
    var t = 0f
    var life = 22f
}

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val maxLife: Float, var size: Float, val color: Int, val kind: Int
)

class FloatText(var x: Float, var y: Float, val text: String, val color: Int) {
    var t = 0.8f
}

enum class ObType { TREE, ROCK, CRATE, BARRIER, BUILDING, HELIPAD, TANK }

class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float, val type: ObType, val variant: Int = 0) {
    val blocksBullets get() = type != ObType.TREE && type != ObType.HELIPAD
    val blocksMove get() = type != ObType.HELIPAD
}

class Decal(val x: Float, val y: Float, val r: Float)
