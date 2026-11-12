package tj.soni.firegun.kingdom

import tj.soni.firegun.audio.SoundManager
import tj.soni.firegun.core.MathX
import tj.soni.firegun.data.SaveManager
import tj.soni.firegun.data.SfxId
import tj.soni.firegun.data.Upgrade
import tj.soni.firegun.data.Weapons
import tj.soni.firegun.game.Body
import tj.soni.firegun.game.Bullet
import tj.soni.firegun.game.Decal
import tj.soni.firegun.game.FloatText
import tj.soni.firegun.game.Particle
import tj.soni.firegun.game.PickType
import tj.soni.firegun.game.Pickup
import tj.soni.firegun.game.Player
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class KState { PLAYING, WON, LOST }

class KingdomWorld(private val save: SaveManager, private val sound: SoundManager) {
    companion object {
        const val W = 5200f
        const val H = 3800f
        const val ZOOM = 0.95f
        const val DAY_LEN = 320f
        val CASTLE_POS = arrayOf(floatArrayOf(800f, 3000f), floatArrayOf(4400f, 800f), floatArrayOf(800f, 800f), floatArrayOf(4400f, 3000f))
        val CAMPS = arrayOf(floatArrayOf(2600f, 1900f), floatArrayOf(2600f, 520f), floatArrayOf(2600f, 3280f), floatArrayOf(1500f, 1950f), floatArrayOf(3700f, 1850f))
        val TEAM_COLOR = intArrayOf(0xFF2F6FE0.toInt(), 0xFFD13B3B.toInt(), 0xFF8E44AD.toInt(), 0xFFE67E22.toInt(), 0xFF6B6B6B.toInt())
        val TEAM_NAME = arrayOf("BLUE", "RED", "PURPLE", "ORANGE", "BANDITS")
        val RANKS = arrayOf("LORD", "BARON", "DUKE", "KING")
        val SLOTS = arrayOf(
            Slot(BType.HOUSE, -170f, 60f), Slot(BType.MINE, -180f, -120f), Slot(BType.BARRACKS, 0f, 190f),
            Slot(BType.TOWER, 190f, -110f), Slot(BType.HOUSE, 170f, 60f), Slot(BType.STABLE, -110f, -210f),
            Slot(BType.TOWER, 240f, 170f), Slot(BType.TOWER, -250f, 170f), Slot(BType.HOUSE, -100f, 250f),
            Slot(BType.HOUSE, 110f, -220f), Slot(BType.MINE, 260f, -10f)
        )
    }

    private val rnd = java.util.Random(2026L)
    private fun r() = rnd.nextFloat()

    val kingdoms = Array(4) { Kingdom(it) }
    val buildings = ArrayList<Building>()
    val units = ArrayList<KUnit>()
    val arrows = ArrayList<KArrow>()
    val bullets = ArrayList<Bullet>()
    val particles = ArrayList<Particle>()
    val texts = ArrayList<FloatText>()
    val pickups = ArrayList<Pickup>()
    val decals = ArrayList<Decal>()
    val lakes = ArrayList<FloatArray>()
    val rocks = ArrayList<FloatArray>()
    val trees = ArrayList<FloatArray>()
    private val campCleared = BooleanArray(CAMPS.size)
    val owned: List<Int> = save.ownedWeapons()

    val hero = Player(CASTLE_POS[0][0], CASTLE_POS[0][1] + 150f)
    private val heroTgt = object : Tgt {
        override val team: Int get() = 0
        override val px: Float get() = hero.x
        override val py: Float get() = hero.y
        override val pr: Float get() = hero.r
        override val alive: Boolean get() = !hero.dead
    }

    var state = KState.PLAYING
    var time = 0f
    var kills = 0
    var score = 0
    var shake = 0f
    var camX = 0f
    var camY = 0f
    var viewW = 1280f / ZOOM
    var viewH = 720f / ZOOM
    var banner = ""
    var bannerT = 0f
    var order = KOrder.FOLLOW
    var attackTarget = 1
    var moveX = 0f
    var moveY = 0f
    var firing = false
    var heroDeadT = 0f
    var coinsReward = 0
    private var banked = false
    private var regenT = 0f

    init {
        genWorld()
        val p = hero
        p.maxHp = 100f + 20f * save.upgradeLevel(Upgrade.HEALTH)
        p.hp = p.maxHp
        p.maxArmor = 50f + 20f * save.upgradeLevel(Upgrade.ARMOR)
        p.armor = p.maxArmor
        p.dmgMult = 1f + 0.1f * save.upgradeLevel(Upgrade.DAMAGE)
        for (id in owned) {
            val w = Weapons.get(id)
            p.mag[id] = w.magSize
            p.reserve[id] = if (id == 0) 999 else w.magSize * 3
        }
        p.weaponId = if (save.owns(save.selectedWeapon)) save.selectedWeapon else 0
        p.angle = (-PI / 2).toFloat()
        camX = p.x - viewW / 2; camY = p.y - viewH / 2
        clampCam()
        banner = "BUILD YOUR KINGDOM"; bannerT = 3.5f
    }

    // ------------------------------------------------------------ world generation
    private fun genWorld() {
        for (k in 0 until 4) {
            val cx = CASTLE_POS[k][0]; val cy = CASTLE_POS[k][1]
            val castle = Building(BType.CASTLE, k, cx, cy)
            castle.buildT = 0f
            buildings.add(castle)
            kingdoms[k].castle = castle
            kingdoms[k].gold = if (k == 0) 350f else 200f
            kingdoms[k].raidT = 110f + k * 35f
            val startSlots = if (k == 0) 3 else 4
            for (i in 0 until startSlots) {
                val s = SLOTS[i]
                val b = Building(s.t, k, cx + s.dx, cy + s.dy)
                b.buildT = 0f
                buildings.add(b)
            }
            kingdoms[k].slotIdx = startSlots
            val n = if (k == 0) 5 else 5
            for (i in 0 until n) spawnUnit(if (i % 3 == 2) UType.ARCHER else UType.SOLDIER, k, cx + (r() - 0.5f) * 160f, cy + 130f + r() * 60f)
        }
        var tries = 0
        while (lakes.size < 9 && tries < 300) {
            tries++
            val x = 500f + r() * (W - 1000f)
            val y = 400f + r() * (H - 800f)
            val rad = 110f + r() * 120f
            if (farFromPoints(x, y, rad + 420f) && lakes.none { MathX.dist(x, y, it[0], it[1]) < it[2] + rad + 120f }) lakes.add(floatArrayOf(x, y, rad))
        }
        tries = 0
        while (rocks.size < 70 && tries < 600) {
            tries++
            val x = 150f + r() * (W - 300f)
            val y = 150f + r() * (H - 300f)
            val rad = 20f + r() * 16f
            if (farFromPoints(x, y, 300f) && !inLake(x, y, rad + 20f)) rocks.add(floatArrayOf(x, y, rad, r()))
        }
        for (c in 0 until 46) {
            val fx = 200f + r() * (W - 400f)
            val fy = 200f + r() * (H - 400f)
            val fr = 120f + r() * 230f
            val count = 14 + rnd.nextInt(26)
            for (i in 0 until count) {
                val a = r() * 2f * PI.toFloat()
                val d = sqrt(r()) * fr
                val x = fx + cos(a) * d
                val y = fy + sin(a) * d
                if (x < 40f || y < 40f || x > W - 40f || y > H - 40f) continue
                if (!farFromCastles(x, y, 340f) || !farFromCamps(x, y, 130f) || inLake(x, y, 50f)) continue
                trees.add(floatArrayOf(x, y, rnd.nextInt(3).toFloat()))
            }
        }
        for (i in CAMPS.indices) {
            val n = 4 + (i % 2) * 2
            for (k in 0 until n) {
                val a = r() * 2f * PI.toFloat()
                val u = spawnUnit(UType.BANDIT, 4, CAMPS[i][0] + cos(a) * (40f + r() * 60f), CAMPS[i][1] + sin(a) * (40f + r() * 60f))
                u.homeX = CAMPS[i][0]; u.homeY = CAMPS[i][1]; u.camp = i
            }
        }
    }

    private fun farFromCastles(x: Float, y: Float, d: Float): Boolean = CASTLE_POS.none { MathX.dist(x, y, it[0], it[1]) < d }
    private fun farFromCamps(x: Float, y: Float, d: Float): Boolean = CAMPS.none { MathX.dist(x, y, it[0], it[1]) < d }
    private fun farFromPoints(x: Float, y: Float, d: Float): Boolean = farFromCastles(x, y, d) && farFromCamps(x, y, d * 0.5f) && abs(x - W / 2) + abs(y - H / 2) > 120f
    fun inLake(x: Float, y: Float, pad: Float): Boolean = lakes.any { MathX.dist(x, y, it[0], it[1]) < it[2] + pad }

    // ------------------------------------------------------------ helpers
    val playerCastle: Building? get() = kingdoms[0].castle
    fun castleLevel(): Int = kingdoms[0].castle?.level ?: 1
    fun rank(): String = RANKS[(castleLevel() - 1).coerceIn(0, 3)]
    fun gold(): Int = kingdoms[0].gold.toInt()
    fun armyCount(team: Int): Int = units.count { it.team == team && !it.dead }
    fun popOf(team: Int): Int = armyCount(team) + (if (team < 4) kingdoms[team].queue.size else 0)
    fun popCap(team: Int): Int = 6 + 5 * buildings.count { it.team == team && it.type == BType.HOUSE && it.done && !it.dead } + 4 * (kingdoms[team].castle?.level ?: 1)
    private fun count(team: Int, t: BType): Int = buildings.count { it.team == team && it.type == t && it.done && !it.dead }
    fun enemiesAlive(): Int = (1..3).count { kingdoms[it].alive }
    fun dayPhase(): Float = (0.1f + time / DAY_LEN) % 1f
    fun darkness(): Float = MathX.clamp(0.25f - sin(2f * PI.toFloat() * dayPhase()) * 0.9f, 0f, 0.62f)
    fun addShake(v: Float) { if (save.shakeOn) shake = min(14f, shake + v) }
    private fun say(s: String) { banner = s; bannerT = 2.6f }

    private fun spawnUnit(t: UType, team: Int, x: Float, y: Float): KUnit {
        val u = KUnit(t, team, x, y)
        if (team in 1..3) { val f = 1f + min(0.5f, time / 900f); u.hp *= f; u.maxHp = u.hp }
        u.slot = r() * 2f * PI.toFloat()
        u.ring = 70f + r() * 120f
        val c = if (team < 4) CASTLE_POS[team] else null
        if (c != null) { u.homeX = c[0]; u.homeY = c[1] }
        u.angle = r() * 6.28f
        resolve(u, team)
        units.add(u)
        return u
    }

    // ------------------------------------------------------------ collisions
    private fun resolve(b: Body, team: Int) {
        for (l in lakes) pushCircle(b, l[0], l[1], l[2] + b.r * 0.5f)
        for (k in rocks) pushCircle(b, k[0], k[1], k[2] + b.r)
        for (bd in buildings) {
            if (bd.dead || bd.team == team) continue
            val hw = bd.w / 2f; val hh = bd.h / 2f
            val px = MathX.clamp(b.x, bd.x - hw, bd.x + hw)
            val py = MathX.clamp(b.y, bd.y - hh, bd.y + hh)
            val dx = b.x - px; val dy = b.y - py
            val d2 = dx * dx + dy * dy
            if (d2 < b.r * b.r) {
                if (d2 > 0.0001f) { val d = sqrt(d2); val k = (b.r - d) / d; b.x += dx * k; b.y += dy * k }
                else b.y = bd.y + hh + b.r
            }
        }
        b.x = MathX.clamp(b.x, b.r, W - b.r)
        b.y = MathX.clamp(b.y, b.r, H - b.r)
    }

    private fun pushCircle(b: Body, cx: Float, cy: Float, rad: Float) {
        val dx = b.x - cx; val dy = b.y - cy
        val d2 = dx * dx + dy * dy
        if (d2 < rad * rad) {
            val d = sqrt(d2)
            if (d > 0.01f) { val k = rad / d; b.x = cx + dx * k; b.y = cy + dy * k } else b.x = cx + rad
        }
    }

    // ------------------------------------------------------------ main update
    fun update(dt: Float) {
        updateFx(dt)
        if (state == KState.PLAYING) {
            time += dt
            economy(dt)
            updateHero(dt)
            aiKingdoms(dt)
            updateUnits(dt)
            updateBuildings(dt)
            updateArrows(dt)
            updateBullets(dt)
            updatePickups(dt)
            checkEnd()
        }
        val k = min(1f, dt * 6f)
        camX += (hero.x - viewW / 2f - camX) * k
        camY += (hero.y - viewH / 2f - camY) * k
        clampCam()
    }

    private fun clampCam() {
        camX = MathX.clamp(camX, 0f, max(0f, W - viewW))
        camY = MathX.clamp(camY, 0f, max(0f, H - viewH))
    }

    private fun economy(dt: Float) {
        for (k in kingdoms) {
            if (!k.alive) continue
            val bonus = if (k.id == 0) 1f else 1f + time / 1200f
            k.gold += (2f + 3f * count(k.id, BType.MINE)) * dt * bonus
        }
        val kg = kingdoms[0]
        if (kg.queue.isNotEmpty()) {
            kg.trainT -= dt
            if (kg.trainT <= 0f) { finishTraining(kg); kg.trainT = 2f }
        }
        for (i in 1..3) {
            val k = kingdoms[i]
            if (k.alive && k.queue.isNotEmpty()) { k.trainT -= dt; if (k.trainT <= 0f) { finishTraining(k); k.trainT = 2f } }
        }
    }

    private fun finishTraining(k: Kingdom) {
        if (k.queue.isEmpty()) return
        val t = k.queue.removeAt(0)
        val need = if (t == UType.KNIGHT) BType.STABLE else BType.BARRACKS
        val src = buildings.firstOrNull { it.team == k.id && it.type == need && it.done && !it.dead } ?: k.castle ?: return
        val u = spawnUnit(t, k.id, src.x + (r() - 0.5f) * 50f, src.y + src.h / 2f + 24f)
        if (k.id == 0) { texts.add(FloatText(u.x, u.y - 20f, t.label, 0xFF9CC9FF.toInt())); sound.play(SfxId.PICKUP, 0.4f) }
    }

    // ------------------------------------------------------------ player commands
    fun train(t: UType): String {
        val k = kingdoms[0]
        val need = if (t == UType.KNIGHT) BType.STABLE else BType.BARRACKS
        if (count(0, need) == 0) return "Build a ${need.label} first"
        if (popOf(0) >= popCap(0)) return "Army full - build Houses"
        if (k.gold < t.cost) return "Not enough gold"
        if (k.queue.size >= 6) return "Queue full"
        k.gold -= t.cost
        k.queue.add(t)
        return "${t.label} training"
    }

    fun canPlace(t: BType, x: Float, y: Float): Boolean {
        val c = playerCastle ?: return false
        if (t == BType.CASTLE) return false
        if (MathX.dist(x, y, c.x, c.y) > 420f + 60f * c.level) return false
        val hw = t.w / 2f + 6f; val hh = t.h / 2f + 6f
        if (x < hw || y < hh || x > W - hw || y > H - hh) return false
        for (b in buildings) {
            if (b.dead) continue
            if (abs(x - b.x) < hw + b.w / 2f && abs(y - b.y) < hh + b.h / 2f) return false
        }
        if (lakes.any { MathX.dist(x, y, it[0], it[1]) < it[2] + t.w / 2f }) return false
        if (rocks.any { MathX.dist(x, y, it[0], it[1]) < it[2] + t.w / 2f }) return false
        return true
    }

    fun build(t: BType, x: Float, y: Float): String {
        if (kingdoms[0].gold < t.cost) return "Not enough gold"
        if (!canPlace(t, x, y)) return "Cannot build here"
        kingdoms[0].gold -= t.cost
        buildings.add(Building(t, 0, x, y))
        sound.play(SfxId.CLICK)
        return "${t.label} under construction"
    }

    fun upgradeCastle(): String {
        val c = playerCastle ?: return ""
        if (c.level >= 4) return "Castle is at max level"
        val cost = 500 * c.level * c.level
        if (kingdoms[0].gold < cost) return "Need $cost gold"
        kingdoms[0].gold -= cost
        c.level++
        c.maxHp = 1500f + 1200f * (c.level - 1)
        c.hp = c.maxHp
        say("YOU ARE NOW ${rank()}!")
        sound.play(SfxId.VICTORY, 0.6f)
        return "Castle upgraded"
    }

    fun upgradeCost(): Int = 500 * castleLevel() * castleLevel()

    fun cycleTarget() {
        for (i in 1..3) {
            attackTarget = attackTarget % 3 + 1
            if (kingdoms[attackTarget].alive) return
        }
    }

    // ------------------------------------------------------------ hero
    private fun updateHero(dt: Float) {
        val p = hero
        if (p.dead) {
            heroDeadT += dt
            if (heroDeadT >= 6f) {
                val c = playerCastle
                if (c != null) { p.x = c.x; p.y = c.y + c.h / 2f + 40f }
                p.dead = false; p.hp = p.maxHp; p.armor = p.maxArmor; p.invuln = 2.5f; heroDeadT = 0f
            }
            return
        }
        p.cooldown -= dt; p.invuln -= dt; p.hurtT -= dt; p.flashT -= dt
        val len = sqrt(moveX * moveX + moveY * moveY)
        p.moving = len > 0.15f
        if (p.moving) {
            val sp = 215f * min(1f, len)
            p.x += moveX / len * sp * dt; p.y += moveY / len * sp * dt
            resolve(p, 0)
            p.walk += dt * sp * 0.065f
        }
        if (p.reloadT > 0f) { p.reloadT -= dt; if (p.reloadT <= 0f) finishReload() }
        var target: Tgt? = null
        if (firing && save.autoAim) target = nearestEnemy(760f)
        if (target != null) p.angle = MathX.lerpAngle(p.angle, MathX.angleTo(p.x, p.y, target.px, target.py), min(1f, dt * 18f))
        else if (p.moving) p.angle = MathX.lerpAngle(p.angle, atan2(moveY, moveX), min(1f, dt * 12f))
        if (firing) tryFire()
        val c = playerCastle
        if (c != null && MathX.dist(p.x, p.y, c.x, c.y) < 260f) {
            regenT += dt
            if (regenT > 1f) {
                regenT = 0f
                p.hp = min(p.maxHp, p.hp + 6f)
                for (id in owned) if (id != 0) { val w = Weapons.get(id); p.reserve[id] = min(w.magSize * 6, p.reserve[id] + w.magSize / 3 + 1) }
            }
        }
    }

    private fun nearestEnemy(range: Float): Tgt? {
        var best: Tgt? = null
        var bd = range
        for (u in units) {
            if (u.dead || u.team == 0) continue
            val d = MathX.dist(hero.x, hero.y, u.x, u.y)
            if (d < bd) { bd = d; best = u }
        }
        return best
    }

    fun tryFire() {
        val p = hero
        val id = p.weaponId
        val w = Weapons.get(id)
        if (p.dead || p.reloadT > 0f || p.cooldown > 0f) return
        if (p.mag[id] <= 0) {
            if (p.reserve[id] > 0) startReload() else { sound.play(SfxId.EMPTY); switchWeapon() }
            p.cooldown = 0.3f
            return
        }
        p.mag[id]--
        p.cooldown = w.fireDelay
        p.flashT = 0.06f
        val mx = p.x + cos(p.angle) * (p.r + w.gunLen)
        val my = p.y + sin(p.angle) * (p.r + w.gunLen)
        for (i in 0 until w.pellets) {
            val a = p.angle + (r() - 0.5f) * 2f * w.spread
            bullets.add(Bullet(mx, my, cos(a) * w.bulletSpeed, sin(a) * w.bulletSpeed, w.damage * p.dmgMult, true, w.range / w.bulletSpeed, w.color, if (w.pellets > 1) 3f else 4f, w.pierce))
        }
        sound.play(w.sfx, 0.8f)
        spark(mx, my, 4, w.color, 160f, 3f, 0.18f)
        if (p.mag[id] == 0) startReload()
    }

    fun startReload() {
        val p = hero
        val w = Weapons.get(p.weaponId)
        if (p.dead || p.reloadT > 0f || p.mag[p.weaponId] >= w.magSize || p.reserve[p.weaponId] <= 0) return
        p.reloadT = w.reloadTime
        sound.play(SfxId.RELOAD)
    }

    private fun finishReload() {
        val p = hero
        val id = p.weaponId
        val w = Weapons.get(id)
        val take = min(w.magSize - p.mag[id], p.reserve[id])
        p.mag[id] += take
        if (id != 0) p.reserve[id] -= take
    }

    fun switchWeapon() {
        val p = hero
        if (owned.size < 2) return
        var next = owned[(owned.indexOf(p.weaponId) + 1) % owned.size]
        var guard = 0
        while (next != 0 && p.mag[next] + p.reserve[next] <= 0 && guard++ < owned.size) next = owned[(owned.indexOf(next) + 1) % owned.size]
        p.weaponId = next
        p.reloadT = 0f
        p.cooldown = 0.2f
        sound.play(SfxId.CLICK)
    }

    private fun hurtHero(d: Float) {
        val p = hero
        if (p.invuln > 0f || p.dead) return
        val absorbed = min(p.armor, d * 0.7f)
        p.armor -= absorbed
        p.hp -= d - absorbed
        p.hurtT = 0.2f
        spark(p.x, p.y, 4, 0xFFFF5252.toInt(), 150f, 3f, 0.3f)
        if (p.hp <= 0f) {
            p.hp = 0f; p.dead = true; heroDeadT = 0f
            explosion(p.x, p.y, 0.6f)
            say("You fell! Respawning...")
        }
    }

    // ------------------------------------------------------------ damage
    private fun damage(t: Tgt, amount: Float, srcTeam: Int) {
        if (t is KUnit) {
            if (t.dead) return
            t.hp -= amount; t.hitFlash = 0.1f
            if (srcTeam == 0) texts.add(FloatText(t.x, t.y - t.r, amount.toInt().toString(), 0xFFFFFFFF.toInt()))
            if (t.hp <= 0f) killUnit(t, srcTeam)
        } else if (t is Building) {
            if (t.dead) return
            t.hp -= amount; t.flash = 0.1f
            if (t.hp <= 0f) destroyBuilding(t, srcTeam)
        } else hurtHero(amount)
    }

    private fun killUnit(u: KUnit, srcTeam: Int) {
        u.dead = true
        u.dyingT = 0f
        spark(u.x, u.y, 8, 0xFFFFCC80.toInt(), 180f, 3.5f, 0.45f)
        if (srcTeam == 0 && u.team != 0) {
            kingdoms[0].gold += u.type.reward
            kills++; score += u.type.reward * 10
            texts.add(FloatText(u.x, u.y - 24f, "+${u.type.reward}g", 0xFFFFD54F.toInt()))
            if (r() < 0.08f) pickups.add(Pickup(u.x, u.y, PickType.HEALTH, 30))
        }
        if (u.team == 4 && u.camp >= 0 && !campCleared[u.camp] && units.none { it.team == 4 && it.camp == u.camp && !it.dead }) {
            campCleared[u.camp] = true
            for (i in 0 until 6) pickups.add(Pickup(CAMPS[u.camp][0] + (r() - 0.5f) * 80f, CAMPS[u.camp][1] + (r() - 0.5f) * 80f, PickType.COIN, 30))
            say("BANDIT CAMP CLEARED!")
        }
    }

    private fun destroyBuilding(b: Building, srcTeam: Int) {
        b.dead = true
        explosion(b.x, b.y, if (b.type == BType.CASTLE) 1.8f else 0.9f)
        if (b.type != BType.CASTLE) return
        val k = kingdoms[b.team]
        k.alive = false
        k.raiding = false
        k.queue.clear()
        for (o in buildings) if (o.team == b.team && !o.dead) { o.dead = true; explosion(o.x, o.y, 0.7f) }
        for (u in units) if (u.team == b.team && !u.dead) { u.dead = true; u.dyingT = 0f }
        if (b.team == 0) say("YOUR KINGDOM HAS FALLEN!")
        else {
            say("${TEAM_NAME[b.team]} KINGDOM CONQUERED!")
            if (srcTeam == 0) { kingdoms[0].gold += 500; coinsReward += 300; score += 3000 }
            else for (i in 1..3) if (kingdoms[i].alive && i != b.team) kingdoms[i].gold += 150
        }
    }

    // ------------------------------------------------------------ units
    private fun updateUnits(dt: Float) {
        for (i in units.indices.reversed()) {
            val u = units[i]
            if (u.dead) { u.dyingT += dt; if (u.dyingT > 0.6f) units.removeAt(i); continue }
            aiUnit(u, dt)
        }
        val n = units.size
        for (i in 0 until n) {
            val a = units[i]
            if (a.dead) continue
            for (j in i + 1 until n) {
                val b = units[j]
                if (b.dead) continue
                val dx = a.x - b.x
                if (dx > 30f || dx < -30f) continue
                val dy = a.y - b.y
                val md = a.r + b.r - 2f
                val d2 = dx * dx + dy * dy
                if (d2 < md * md && d2 > 0.01f) {
                    val d = sqrt(d2)
                    val k = (md - d) * 0.5f / d
                    a.x += dx * k; a.y += dy * k; b.x -= dx * k; b.y -= dy * k
                }
            }
        }
    }

    private var destX = 0f
    private var destY = 0f
    private var stopD = 40f

    private fun orderDest(u: KUnit) {
        stopD = 40f
        if (u.team == 4) { destX = u.homeX + cos(u.slot) * u.ring * 0.5f; destY = u.homeY + sin(u.slot) * u.ring * 0.5f; return }
        if (u.team == 0) {
            when (order) {
                KOrder.FOLLOW -> { destX = hero.x + cos(u.slot) * u.ring; destY = hero.y + sin(u.slot) * u.ring }
                KOrder.HOLD -> { destX = u.x; destY = u.y; stopD = 99999f }
                KOrder.ATTACK -> {
                    val c = kingdoms[attackTarget].castle
                    if (kingdoms[attackTarget].alive && c != null) { destX = c.x + cos(u.slot) * 110f; destY = c.y + sin(u.slot) * 110f; stopD = 50f }
                    else { destX = hero.x + cos(u.slot) * u.ring; destY = hero.y + sin(u.slot) * u.ring }
                }
                KOrder.DEFEND -> { val c = playerCastle; destX = (c?.x ?: u.x) + cos(u.slot) * (u.ring + 140f); destY = (c?.y ?: u.y) + sin(u.slot) * (u.ring + 140f) }
            }
            return
        }
        val k = kingdoms[u.team]
        val tc = if (k.raiding) kingdoms[k.raidTarget].castle else null
        if (tc != null && kingdoms[k.raidTarget].alive) { destX = tc.x + cos(u.slot) * 120f; destY = tc.y + sin(u.slot) * 120f; stopD = 50f }
        else { destX = u.homeX + cos(u.slot) * (u.ring + 150f); destY = u.homeY + sin(u.slot) * (u.ring + 150f) }
    }

    private fun aggroOf(u: KUnit): Float {
        val base = if (u.type.ranged) 340f else 300f
        if (u.team == 0 && order == KOrder.HOLD) return u.type.reach + 70f
        if ((u.team == 0 && order == KOrder.ATTACK) || (u.team in 1..3 && kingdoms[u.team].raiding)) return base + 60f
        return base
    }

    private fun leashOk(u: KUnit, t: Tgt): Boolean {
        if (u.team == 4) return MathX.dist(t.px, t.py, u.homeX, u.homeY) < 520f
        if (u.team == 0) return when (order) {
            KOrder.FOLLOW -> MathX.dist(t.px, t.py, hero.x, hero.y) < 620f
            KOrder.DEFEND -> MathX.dist(t.px, t.py, playerCastle?.x ?: u.x, playerCastle?.y ?: u.y) < 650f
            else -> true
        }
        if (kingdoms[u.team].raiding) return true
        return MathX.dist(t.px, t.py, u.homeX, u.homeY) < 700f
    }

    private fun findTarget(u: KUnit): Tgt? {
        var best: Tgt? = null
        val aggro = aggroOf(u)
        var bd = aggro
        for (o in units) {
            if (o.dead || o.team == u.team) continue
            val d = MathX.dist(u.x, u.y, o.x, o.y) - o.r
            if (d < bd && leashOk(u, o)) { bd = d; best = o }
        }
        if (u.team != 4) {
            for (b in buildings) {
                if (b.dead || b.team == u.team) continue
                val d = MathX.dist(u.x, u.y, b.x, b.y) - b.pr + (if (b.type == BType.WALL) 140f else 30f)
                if (d < bd && leashOk(u, b)) { bd = d; best = b }
            }
        }
        if (u.team != 0 && !hero.dead) {
            val d = MathX.dist(u.x, u.y, hero.x, hero.y) - hero.r
            if (d < bd && leashOk(u, heroTgt)) { best = heroTgt }
        }
        return best
    }

    private fun aiUnit(u: KUnit, dt: Float) {
        u.cd -= dt; u.hitFlash -= dt; u.atkFlash -= dt; u.steerT -= dt; u.retarget -= dt
        var tgt = u.target
        if (tgt != null && !tgt.alive) tgt = null
        if (u.retarget <= 0f || tgt == null) { u.retarget = 0.3f + r() * 0.25f; tgt = findTarget(u) }
        u.target = tgt
        var mvx = 0f
        var mvy = 0f
        var speed = u.type.speed
        if (tgt != null) {
            val ang = MathX.angleTo(u.x, u.y, tgt.px, tgt.py)
            val d = MathX.dist(u.x, u.y, tgt.px, tgt.py) - tgt.pr - u.r
            u.angle = MathX.lerpAngle(u.angle, ang, min(1f, dt * 12f))
            if (d <= u.type.reach) {
                if (u.cd <= 0f) attack(u, tgt, ang)
                if (u.type.ranged && d < u.type.reach * 0.4f) { mvx = -cos(ang); mvy = -sin(ang) }
            } else { mvx = cos(ang); mvy = sin(ang) }
        } else {
            orderDest(u)
            val d = MathX.dist(u.x, u.y, destX, destY)
            if (d > stopD) {
                val a = MathX.angleTo(u.x, u.y, destX, destY)
                mvx = cos(a); mvy = sin(a)
                if (d > 350f) speed *= 1.3f
                u.angle = MathX.lerpAngle(u.angle, a, min(1f, dt * 10f))
            }
        }
        if (mvx != 0f || mvy != 0f) {
            if (u.steerT > 0f) { val sx = -mvy * u.steerDir; val sy = mvx * u.steerDir; mvx = mvx * 0.3f + sx; mvy = mvy * 0.3f + sy }
            val bx = u.x; val by = u.y
            u.x += mvx * speed * dt; u.y += mvy * speed * dt
            resolve(u, u.team)
            if (MathX.dist(bx, by, u.x, u.y) < speed * dt * 0.25f && u.steerT <= 0f) { u.steerT = 0.8f; u.steerDir = if (rnd.nextBoolean()) 1f else -1f }
            u.walk += dt * speed * 0.06f
        }
    }

    private fun attack(u: KUnit, t: Tgt, ang: Float) {
        u.cd = u.type.cd * (0.9f + r() * 0.2f)
        u.atkFlash = 0.08f
        val dmgMult = if (u.team == 0) 1f + 0.1f * castleLevel() else 1f
        if (u.type.ranged) {
            val a = ang + (r() - 0.5f) * 0.08f
            arrows.add(KArrow(u.x + cos(a) * 16f, u.y + sin(a) * 16f, cos(a) * 560f, sin(a) * 560f, u.type.dmg * dmgMult, u.team))
        } else {
            val k = if (t is Building) 0.6f else 1f
            damage(t, u.type.dmg * dmgMult * k, u.team)
            spark((u.x + t.px) / 2f, (u.y + t.py) / 2f, 2, 0xFFFFE0B2.toInt(), 90f, 2.5f, 0.2f)
            if (MathX.dist(u.x, u.y, hero.x, hero.y) < 600f) sound.play(SfxId.HIT, 0.25f)
        }
    }

    // ------------------------------------------------------------ buildings
    private fun updateBuildings(dt: Float) {
        for (i in buildings.indices.reversed()) {
            val b = buildings[i]
            if (b.dead) { if (b.type != BType.CASTLE) buildings.removeAt(i); continue }
            b.flash -= dt
            if (b.buildT > 0f) { b.buildT -= dt; continue }
            if (b.type == BType.TOWER || b.type == BType.CASTLE) {
                b.cd -= dt
                if (b.cd <= 0f) {
                    val range = if (b.type == BType.TOWER) 400f else 360f
                    var best: Tgt? = null
                    var bd = range
                    for (u in units) {
                        if (u.dead || u.team == b.team) continue
                        val d = MathX.dist(b.x, b.y, u.x, u.y)
                        if (d < bd) { bd = d; best = u }
                    }
                    if (b.team != 0 && !hero.dead) { val d = MathX.dist(b.x, b.y, hero.x, hero.y); if (d < bd) { bd = d; best = heroTgt } }
                    if (best != null) {
                        val a = MathX.angleTo(b.x, b.y, best.px, best.py)
                        val dm = if (b.type == BType.TOWER) 14f else 10f + 3f * b.level
                        arrows.add(KArrow(b.x, b.y, cos(a) * 600f, sin(a) * 600f, dm, b.team))
                        b.cd = if (b.type == BType.TOWER) 1.0f else 1.3f - 0.1f * b.level
                    } else b.cd = 0.3f
                }
            }
        }
    }

    private fun updateArrows(dt: Float) {
        for (i in arrows.indices.reversed()) {
            val a = arrows[i]
            var hit = false
            for (s in 0 until 2) {
                a.x += a.vx * dt / 2f; a.y += a.vy * dt / 2f
                for (u in units) {
                    if (u.dead || u.team == a.team) continue
                    if (abs(u.x - a.x) < u.r + 4f && abs(u.y - a.y) < u.r + 4f && MathX.dist(u.x, u.y, a.x, a.y) < u.r + 4f) { damage(u, a.dmg, a.team); hit = true; break }
                }
                if (hit) break
                if (a.team != 0 && !hero.dead && MathX.dist(hero.x, hero.y, a.x, a.y) < hero.r + 4f) { hurtHero(a.dmg); hit = true; break }
                for (b in buildings) {
                    if (b.dead || b.team == a.team) continue
                    if (b.contains(a.x, a.y)) { damage(b, a.dmg * 0.5f, a.team); hit = true; break }
                }
                if (hit) break
            }
            a.life -= dt
            if (hit) spark(a.x, a.y, 2, 0xFFFFE0B2.toInt(), 80f, 2f, 0.2f)
            if (hit || a.life <= 0f || a.x < 0f || a.y < 0f || a.x > W || a.y > H) arrows.removeAt(i)
        }
    }

    private fun updateBullets(dt: Float) {
        for (i in bullets.indices.reversed()) {
            val b = bullets[i]
            var remove = false
            val steps = max(1, kotlin.math.ceil(sqrt(b.vx * b.vx + b.vy * b.vy) * dt / 14f).toInt())
            val sdt = dt / steps
            for (s in 0 until steps) {
                b.x += b.vx * sdt; b.y += b.vy * sdt
                for (u in units) {
                    if (u.dead || u.team == 0 || u === b.lastHit) continue
                    if (MathX.dist(b.x, b.y, u.x, u.y) < u.r + b.size) {
                        damage(u, b.damage, 0); b.lastHit = u; b.pierce--
                        if (b.pierce < 0) remove = true
                        break
                    }
                }
                if (!remove) for (bd in buildings) {
                    if (bd.dead || bd.team == 0) continue
                    if (bd.contains(b.x, b.y)) { damage(bd, b.damage * 0.35f, 0); remove = true; spark(b.x, b.y, 3, 0xFFFFE0B2.toInt(), 110f, 2.5f, 0.25f); break }
                }
                if (remove) break
            }
            b.life -= dt
            if (remove || b.life <= 0f || b.x < 0f || b.y < 0f || b.x > W || b.y > H) bullets.removeAt(i)
        }
    }

    private fun updatePickups(dt: Float) {
        val p = hero
        for (i in pickups.indices.reversed()) {
            val k = pickups[i]
            k.t += dt; k.life -= dt
            val d = MathX.dist(k.x, k.y, p.x, p.y)
            if (!p.dead && d < 110f) { val a = MathX.angleTo(k.x, k.y, p.x, p.y); val sp = (110f - d) * 3f + 60f; k.x += cos(a) * sp * dt; k.y += sin(a) * sp * dt }
            if (!p.dead && d < p.r + 14f) {
                when (k.type) {
                    PickType.COIN -> { kingdoms[0].gold += k.value; texts.add(FloatText(k.x, k.y, "+${k.value}g", 0xFFFFD54F.toInt())) }
                    PickType.HEALTH -> { p.hp = min(p.maxHp, p.hp + k.value); texts.add(FloatText(k.x, k.y, "+HP", 0xFFFF6B6B.toInt())) }
                    else -> {}
                }
                sound.play(SfxId.PICKUP, 0.5f)
                pickups.removeAt(i)
            } else if (k.life <= 0f) pickups.removeAt(i)
        }
    }

    // ------------------------------------------------------------ rival kingdoms (AI)
    private fun aiKingdoms(dt: Float) {
        for (id in 1..3) {
            val k = kingdoms[id]
            if (!k.alive) continue
            k.buildT -= dt
            if (k.buildT <= 0f) { k.buildT = 20f; aiBuild(k) }
            k.aiTrainT -= dt
            if (k.aiTrainT <= 0f) { k.aiTrainT = 2.5f; aiTrain(k) }
            val c = k.castle
            if (c != null && c.level < 4 && k.gold > 700f * c.level) { k.gold -= 500f * c.level * c.level * 0.6f; c.level++; c.maxHp = 1500f + 1200f * (c.level - 1); c.hp = c.maxHp }
            if (!k.raiding) {
                k.raidT -= dt
                if (k.raidT <= 0f) { if (armyCount(id) >= 6) startRaid(k) else k.raidT = 12f }
            } else {
                k.raidClock += dt
                if (!kingdoms[k.raidTarget].alive || k.raidClock > 85f || armyCount(id) < k.raidStart * 0.35f) {
                    k.raiding = false
                    k.raidT = max(40f, 95f - time / 20f) + r() * 20f
                }
            }
        }
    }

    private fun aiBuild(k: Kingdom) {
        if (k.slotIdx >= SLOTS.size) return
        val s = SLOTS[k.slotIdx]
        val c = k.castle ?: return
        if (k.gold < s.t.cost + 60) return
        k.gold -= s.t.cost
        buildings.add(Building(s.t, k.id, c.x + s.dx, c.y + s.dy))
        k.slotIdx++
    }

    private fun aiTrain(k: Kingdom) {
        if (popOf(k.id) >= popCap(k.id)) return
        val t = when {
            count(k.id, BType.STABLE) > 0 && k.gold >= 150f && r() < 0.3f -> UType.KNIGHT
            r() < 0.45f -> UType.ARCHER
            else -> UType.SOLDIER
        }
        if (count(k.id, BType.BARRACKS) == 0 && t != UType.KNIGHT) return
        if (k.gold < t.cost || k.queue.size >= 3) return
        k.gold -= t.cost
        k.queue.add(t)
    }

    private fun startRaid(k: Kingdom) {
        var best = -1
        var bw = -1f
        for (i in 0..3) {
            if (i == k.id || !kingdoms[i].alive) continue
            val c = kingdoms[i].castle ?: continue
            val kc = k.castle ?: continue
            val w = (if (i == 0) 2f else 1f) * 3000f / (MathX.dist(c.x, c.y, kc.x, kc.y) + 500f) + r()
            if (w > bw) { bw = w; best = i }
        }
        if (best < 0) { k.raidT = 30f; return }
        k.raiding = true; k.raidTarget = best; k.raidClock = 0f; k.raidStart = max(1, armyCount(k.id))
        if (best == 0) say("${TEAM_NAME[k.id]} ARMY ATTACKS YOUR KINGDOM!")
    }

    // ------------------------------------------------------------ end of game
    private fun checkEnd() {
        if (!kingdoms[0].alive) { state = KState.LOST; sound.play(SfxId.GAMEOVER); bank(false) }
        else if (enemiesAlive() == 0) { state = KState.WON; coinsReward += 1500; sound.play(SfxId.VICTORY); save.crowns = save.crowns + 1; bank(true) }
    }

    fun bank(won: Boolean) {
        if (banked) return
        banked = true
        save.coins = save.coins + coinsReward
        save.totalScore = save.totalScore + score
        save.conquered = save.conquered + (1..3).count { !kingdoms[it].alive }
    }

    fun leave() { if (state == KState.PLAYING) bank(false) }

    // ------------------------------------------------------------ effects
    private fun updateFx(dt: Float) {
        shake = max(0f, shake - dt * 28f)
        bannerT -= dt
        for (i in particles.indices.reversed()) {
            val q = particles[i]
            q.life -= dt
            if (q.life <= 0f) { particles.removeAt(i); continue }
            q.x += q.vx * dt; q.y += q.vy * dt
            val drag = exp(-2.5f * dt)
            q.vx *= drag; q.vy *= drag
            if (q.kind == 1) q.size += dt * 24f
            if (q.kind == 2) q.size += dt * 520f
        }
        for (i in texts.indices.reversed()) { val t = texts[i]; t.t -= dt; t.y -= 34f * dt; if (t.t <= 0f) texts.removeAt(i) }
    }

    fun spark(x: Float, y: Float, n: Int, color: Int, speed: Float, size: Float, life: Float) {
        if (particles.size > 500) return
        for (i in 0 until n) {
            val a = r() * 2f * PI.toFloat()
            val v = speed * (0.3f + r() * 0.7f)
            particles.add(Particle(x, y, cos(a) * v, sin(a) * v, life * (0.6f + r() * 0.4f), life, size * (0.6f + r() * 0.6f), color, 0))
        }
    }

    fun explosion(x: Float, y: Float, scale: Float) {
        if (MathX.dist(x, y, hero.x, hero.y) < 900f) sound.play(SfxId.EXPLOSION, min(1f, 0.3f + scale * 0.4f))
        addShake(6f * scale)
        particles.add(Particle(x, y, 0f, 0f, 0.3f, 0.3f, 10f, 0xFFFFE082.toInt(), 2))
        spark(x, y, (20 * scale).toInt(), 0xFFFF9800.toInt(), 300f * scale, 9f * scale, 0.7f)
        spark(x, y, (10 * scale).toInt(), 0xFFFFEB3B.toInt(), 220f * scale, 6f * scale, 0.5f)
        for (i in 0 until (7 * scale).toInt()) {
            val a = r() * 2f * PI.toFloat()
            particles.add(Particle(x, y, cos(a) * 25f, sin(a) * 25f - 10f, 1.2f + r() * 0.6f, 1.8f, 12f, 0xFF4A4A4A.toInt(), 1))
        }
        if (decals.size > 60) decals.removeAt(0)
        decals.add(Decal(x, y, 36f * scale))
    }
}
