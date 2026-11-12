package tj.soni.firegun.kingdom

import tj.soni.firegun.game.Body
import kotlin.math.abs
import kotlin.math.max

enum class BType(val label: String, val cost: Int, val hp: Float, val w: Float, val h: Float, val buildTime: Float) {
    CASTLE("Castle", 0, 1500f, 150f, 150f, 0f),
    HOUSE("House", 100, 300f, 56f, 56f, 2f),
    MINE("Gold Mine", 150, 400f, 70f, 60f, 3f),
    BARRACKS("Barracks", 200, 600f, 90f, 70f, 4f),
    STABLE("Stable", 400, 700f, 100f, 76f, 5f),
    TOWER("Tower", 250, 500f, 48f, 48f, 4f),
    WALL("Wall", 30, 400f, 48f, 48f, 1f)
}

enum class UType(
    val label: String, val hp: Float, val dmg: Float, val speed: Float, val reach: Float,
    val cd: Float, val cost: Int, val radius: Float, val reward: Int, val ranged: Boolean
) {
    SOLDIER("Soldier", 60f, 9f, 115f, 12f, 0.7f, 50, 14f, 8, false),
    ARCHER("Archer", 35f, 8f, 105f, 280f, 1.1f, 70, 13f, 10, true),
    KNIGHT("Knight", 170f, 17f, 100f, 14f, 0.9f, 150, 17f, 20, false),
    BANDIT("Bandit", 50f, 8f, 110f, 12f, 0.8f, 0, 14f, 8, false)
}

enum class KOrder(val label: String) { FOLLOW("FOLLOW"), HOLD("HOLD"), ATTACK("ATTACK"), DEFEND("DEFEND") }

interface Tgt {
    val team: Int
    val px: Float
    val py: Float
    val pr: Float
    val alive: Boolean
}

class Building(val type: BType, override val team: Int, val x: Float, val y: Float) : Tgt {
    var level = 1
    var hp = type.hp
    var maxHp = type.hp
    var buildT = type.buildTime
    var cd = 0f
    var flash = 0f
    var dead = false
    val w: Float get() = if (type == BType.CASTLE) 150f + level * 14f else type.w
    val h: Float get() = if (type == BType.CASTLE) 150f + level * 14f else type.h
    val done: Boolean get() = buildT <= 0f
    override val px: Float get() = x
    override val py: Float get() = y
    override val pr: Float get() = max(w, h) / 2f * 0.85f
    override val alive: Boolean get() = !dead
    fun contains(ax: Float, ay: Float, pad: Float = 0f): Boolean = abs(ax - x) < w / 2f + pad && abs(ay - y) < h / 2f + pad
}

class KUnit(val type: UType, override val team: Int, x: Float, y: Float) : Body(x, y, type.radius), Tgt {
    var hp = type.hp
    var maxHp = type.hp
    var angle = 0f
    var walk = 0f
    var cd = 0.3f
    var retarget = 0f
    var target: Tgt? = null
    var homeX = x
    var homeY = y
    var slot = 0f
    var ring = 100f
    var camp = -1
    var dead = false
    var dyingT = 0f
    var steerT = 0f
    var steerDir = 1f
    var hitFlash = 0f
    var atkFlash = 0f
    override val px: Float get() = this.x
    override val py: Float get() = this.y
    override val pr: Float get() = type.radius
    override val alive: Boolean get() = !dead
}

class KArrow(var x: Float, var y: Float, val vx: Float, val vy: Float, val dmg: Float, val team: Int) {
    var life = 1.1f
}

class Kingdom(val id: Int) {
    var gold = 0f
    var alive = true
    var castle: Building? = null
    var raidT = 90f
    var raiding = false
    var raidTarget = 0
    var raidClock = 0f
    var raidStart = 1
    val queue = ArrayList<UType>()
    var trainT = 0f
    var buildT = 20f
    var aiTrainT = 3f
    var slotIdx = 0
}

class Slot(val t: BType, val dx: Float, val dy: Float)
