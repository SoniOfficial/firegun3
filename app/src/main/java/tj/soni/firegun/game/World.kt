package tj.soni.firegun.game

import tj.soni.firegun.audio.SoundManager
import tj.soni.firegun.core.MathX
import tj.soni.firegun.data.MissionDef
import tj.soni.firegun.data.MissionType
import tj.soni.firegun.data.SaveManager
import tj.soni.firegun.data.SfxId
import tj.soni.firegun.data.Upgrade
import tj.soni.firegun.data.Weapons
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class WState { PLAYING, WON, LOST }

class World(private val save: SaveManager, private val sound: SoundManager, val mission: MissionDef?) {
    companion object {
        const val W = 2600f
        const val H = 1900f
        const val ZOOM = 1.1f
        fun roadX(y: Float): Float = W / 2f + sin(y / 380f) * 120f
    }

    val survival = mission == null
    private val rnd = java.util.Random(if (mission != null) mission.id * 7919L else System.nanoTime())
    private fun r() = rnd.nextFloat()

    val obstacles = ArrayList<Obstacle>()
    val enemies = ArrayList<Enemy>()
    val bullets = ArrayList<Bullet>()
    val grenades = ArrayList<Grenade>()
    val pickups = ArrayList<Pickup>()
    val particles = ArrayList<Particle>()
    val texts = ArrayList<FloatText>()
    val decals = ArrayList<Decal>()
    val owned: List<Int> = save.ownedWeapons()

    val player = Player(roadX(H - 260f), H - 260f)
    var state = WState.PLAYING
    var time = 0f
    var kills = 0
    var score = 0
    var coinsEarned = 0
    var wave = 0
    var banner = ""
    var bannerT = 0f
    var shake = 0f
    var camX = 0f
    var camY = 0f
    var viewW = 1280f / ZOOM
    var viewH = 720f / ZOOM
    var reviveUsed = false
    var stars = 0
    var rewardCoins = 0
    var rewardScore = 0
    var chestGot = false
    var bossesKilled = 0

    // input
    var moveX = 0f
    var moveY = 0f
    var firing = false

    private var spawnT = 0f
    private var spawned = 0
    private var bossesSpawned = false
    private var waveActive = false
    private var breakT = 2f
    private var toSpawn = 0
    private var banked = false
    private val diff: Float get() = mission?.diff ?: (1f + 0.08f * wave)
    private val hpMult: Float get() = diff

    init {
        generateMap()
        val p = player
        p.maxHp = 100f + 20f * save.upgradeLevel(Upgrade.HEALTH)
        p.hp = p.maxHp
        p.maxArmor = 50f + 20f * save.upgradeLevel(Upgrade.ARMOR)
        p.armor = p.maxArmor
        p.dmgMult = 1f + 0.1f * save.upgradeLevel(Upgrade.DAMAGE)
        p.grenades = max(save.grenades, 2)
        for (id in owned) {
            val w = Weapons.get(id)
            p.mag[id] = w.magSize
            p.reserve[id] = if (id == 0) 999 else w.magSize * 3
        }
        p.weaponId = if (save.owns(save.selectedWeapon)) save.selectedWeapon else 0
        p.angle = (-PI / 2).toFloat()
        camX = p.x - viewW / 2; camY = p.y - viewH / 2
        clampCam()
        if (mission != null) {
            banner = "MISSION 1-${mission.id}"; bannerT = 2.5f
            for (i in 0 until min(3, mission.maxAlive)) if (!(mission.type == MissionType.KILL && spawned >= mission.target)) spawnRegular()
        }
    }

    // ---------------------------------------------------------------- map
    private fun generateMap() {
        obstacles.add(Obstacle(120f, 110f, 230f, 170f, ObType.BUILDING, 0))
        obstacles.add(Obstacle(W - 380f, 100f, 260f, 200f, ObType.HELIPAD, 0))
        obstacles.add(Obstacle(W / 2f + 280f, 150f, 200f, 150f, ObType.BUILDING, 1))
        fun place(w: Float, h: Float, t: ObType, count: Int, variants: Int) {
            var placed = 0
            var tries = 0
            while (placed < count && tries < 400) {
                tries++
                val x = 60f + r() * (W - 120f - w)
                val y = 60f + r() * (H - 120f - h)
                val cx = x + w / 2; val cy = y + h / 2
                if (kotlin.math.abs(cx - roadX(cy)) < 90f + w / 2) continue
                if (MathX.dist(cx, cy, player.x, player.y) < 260f) continue
                var ok = true
                for (o in obstacles) {
                    if (x < o.x + o.w + 30f && x + w > o.x - 30f && y < o.y + o.h + 30f && y + h > o.y - 30f) { ok = false; break }
                }
                if (!ok) continue
                var vw = w; var vh = h
                val v = rnd.nextInt(variants)
                if (t == ObType.BARRIER && v == 1) { vw = h; vh = w }
                obstacles.add(Obstacle(x, y, vw, vh, t, v)); placed++
            }
        }
        place(30f, 30f, ObType.TREE, 34, 3)
        place(44f, 36f, ObType.ROCK, 14, 2)
        place(38f, 38f, ObType.CRATE, 14, 2)
        place(90f, 24f, ObType.BARRIER, 9, 2)
        place(110f, 70f, ObType.TANK, 3, 2)
    }

    fun blockedAt(x: Float, y: Float, rad: Float): Boolean {
        for (o in obstacles) if (o.blocksMove && MathX.circleHitsRect(x, y, rad, o.x, o.y, o.w, o.h)) return true
        return false
    }

    private fun resolve(b: Body) {
        for (o in obstacles) {
            if (!o.blocksMove) continue
            val px = MathX.clamp(b.x, o.x, o.x + o.w)
            val py = MathX.clamp(b.y, o.y, o.y + o.h)
            val dx = b.x - px
            val dy = b.y - py
            val d2 = dx * dx + dy * dy
            if (d2 < b.r * b.r) {
                if (d2 > 0.0001f) {
                    val d = sqrt(d2)
                    val k = (b.r - d) / d
                    b.x += dx * k; b.y += dy * k
                } else {
                    val l = b.x - o.x; val rr = o.x + o.w - b.x; val t = b.y - o.y; val bt = o.y + o.h - b.y
                    val m = min(min(l, rr), min(t, bt))
                    when (m) {
                        l -> b.x = o.x - b.r
                        rr -> b.x = o.x + o.w + b.r
                        t -> b.y = o.y - b.r
                        else -> b.y = o.y + o.h + b.r
                    }
                }
            }
        }
        b.x = MathX.clamp(b.x, b.r, W - b.r)
        b.y = MathX.clamp(b.y, b.r, H - b.r)
    }

    private fun move(b: Body, dx: Float, dy: Float) { b.x += dx; b.y += dy; resolve(b) }

    fun hasLOS(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val d = MathX.dist(x1, y1, x2, y2)
        val steps = (d / 20f).toInt()
        for (i in 1 until steps) {
            val t = i / steps.toFloat()
            val px = x1 + (x2 - x1) * t
            val py = y1 + (y2 - y1) * t
            for (o in obstacles) {
                if (o.blocksBullets && px >= o.x && px <= o.x + o.w && py >= o.y && py <= o.y + o.h) return false
            }
        }
        return true
    }

    // ---------------------------------------------------------------- update
    fun update(dt: Float) {
        updateFx(dt)
        if (state == WState.PLAYING) {
            time += dt
            updatePlayer(dt)
            updateSpawner(dt)
            updateEnemies(dt)
            updateBullets(dt)
            updateGrenades(dt)
            updatePickups(dt)
            checkEnd()
        }
        val tx = player.x - viewW / 2f
        val ty = player.y - viewH / 2f
        val k = min(1f, dt * 6f)
        camX += (tx - camX) * k
        camY += (ty - camY) * k
        clampCam()
    }

    private fun clampCam() {
        camX = MathX.clamp(camX, 0f, max(0f, W - viewW))
        camY = MathX.clamp(camY, 0f, max(0f, H - viewH))
    }

    private fun updatePlayer(dt: Float) {
        val p = player
        if (p.dead) {
            p.deadT += dt
            if (p.deadT > 1.3f) {
                state = WState.LOST
                sound.play(SfxId.GAMEOVER)
                bank(false)
            }
            return
        }
        p.cooldown -= dt; p.invuln -= dt; p.hurtT -= dt; p.flashT -= dt
        val len = sqrt(moveX * moveX + moveY * moveY)
        p.moving = len > 0.15f
        if (p.moving) {
            val sp = 195f * min(1f, len)
            move(p, moveX / len * sp * dt, moveY / len * sp * dt)
            p.walk += dt * sp * 0.065f
        }
        if (p.reloadT > 0f) {
            p.reloadT -= dt
            if (p.reloadT <= 0f) finishReload()
        }
        var target: Enemy? = null
        if (firing && save.autoAim) target = nearestEnemy(850f)
        if (target != null) p.angle = MathX.lerpAngle(p.angle, MathX.angleTo(p.x, p.y, target.x, target.y), min(1f, dt * 18f))
        else if (p.moving) p.angle = MathX.lerpAngle(p.angle, atan2(moveY, moveX), min(1f, dt * 12f))
        if (firing) tryFire()
    }

    private fun nearestEnemy(range: Float): Enemy? {
        var best: Enemy? = null
        var bd = range
        for (e in enemies) {
            if (e.dying) continue
            val d = MathX.dist(player.x, player.y, e.x, e.y)
            if (d < bd && hasLOS(player.x, player.y, e.x, e.y)) { bd = d; best = e }
        }
        return best
    }

    fun tryFire() {
        val p = player
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
        sound.play(w.sfx, 0.9f)
        addShake(w.shake)
        spark(mx, my, 4, w.color, 160f, 3f, 0.18f)
        if (p.mag[id] == 0) startReload()
    }

    fun startReload() {
        val p = player
        val w = Weapons.get(p.weaponId)
        if (p.dead || p.reloadT > 0f || p.mag[p.weaponId] >= w.magSize) return
        if (p.reserve[p.weaponId] <= 0) return
        p.reloadT = w.reloadTime
        sound.play(SfxId.RELOAD)
    }

    private fun finishReload() {
        val p = player
        val id = p.weaponId
        val w = Weapons.get(id)
        val take = min(w.magSize - p.mag[id], p.reserve[id])
        p.mag[id] += take
        if (id != 0) p.reserve[id] -= take
    }

    fun switchWeapon() {
        val p = player
        if (owned.size < 2) return
        val idx = owned.indexOf(p.weaponId)
        var next = owned[(idx + 1) % owned.size]
        var guard = 0
        while (next != 0 && p.mag[next] + p.reserve[next] <= 0 && guard++ < owned.size) {
            next = owned[(owned.indexOf(next) + 1) % owned.size]
        }
        p.weaponId = next
        p.reloadT = 0f
        p.cooldown = 0.2f
        sound.play(SfxId.CLICK)
    }

    fun throwGrenade() {
        val p = player
        if (p.dead || p.grenades <= 0 || state != WState.PLAYING) return
        p.grenades--
        var ang = p.angle
        var d = 240f
        val t = if (save.autoAim) nearestEnemy(420f) else null
        if (t != null) {
            ang = MathX.angleTo(p.x, p.y, t.x, t.y)
            d = MathX.clamp(MathX.dist(p.x, p.y, t.x, t.y), 120f, 340f)
        }
        val v = d * 3f
        grenades.add(Grenade(p.x + cos(ang) * 20f, p.y + sin(ang) * 20f, cos(ang) * v, sin(ang) * v, 1.0f))
        sound.play(SfxId.CLICK)
    }

    // ---------------------------------------------------------------- spawning
    private fun pickType(w: List<Int>): EType {
        val total = w.sum()
        if (total <= 0) return EType.BASIC
        var v = rnd.nextInt(total)
        val types = arrayOf(EType.BASIC, EType.FAST, EType.HEAVY, EType.SNIPER)
        for (i in 0 until 4) { v -= w[i]; if (v < 0) return types[i] }
        return EType.BASIC
    }

    private fun freeSpot(cx: Float, cy: Float, minD: Float, maxD: Float, rad: Float): FloatArray? {
        for (i in 0 until 16) {
            val a = r() * 2f * PI.toFloat()
            val d = minD + r() * (maxD - minD)
            val x = cx + cos(a) * d
            val y = cy + sin(a) * d
            if (x < rad + 30f || x > W - rad - 30f || y < rad + 30f || y > H - rad - 30f) continue
            if (blockedAt(x, y, rad + 8f)) continue
            return floatArrayOf(x, y)
        }
        return null
    }

    private fun spawnEnemy(type: EType, x: Float, y: Float, alert: Boolean = false): Enemy {
        val e = Enemy(x, y, type, hpMult)
        e.alert = alert
        enemies.add(e)
        return e
    }

    private fun spawnRegular(weights: List<Int>? = null) {
        val wts = weights ?: mission?.weights ?: listOf(10, 0, 0, 0)
        val t = pickType(wts)
        val s = freeSpot(player.x, player.y, 620f, 900f, t.radius) ?: freeSpot(player.x, player.y, 450f, 700f, t.radius) ?: return
        spawnEnemy(t, s[0], s[1])
        spawned++
    }

    private fun spawnBoss() {
        val s = freeSpot(W / 2f + (r() - 0.5f) * 800f, 380f, 0f, 160f, 40f) ?: freeSpot(player.x, player.y, 600f, 900f, 40f) ?: return
        val b = spawnEnemy(EType.BOSS, s[0], s[1], true)
        b.hp = b.type.hp * hpMult * (if (survival) 1f + wave / 5f * 0.3f else 1f)
        b.maxHp = b.hp
        banner = "BOSS INCOMING!"; bannerT = 2.2f
        addShake(6f)
    }

    private fun aliveRegular() = enemies.count { !it.dying && it.type != EType.BOSS }

    private fun updateSpawner(dt: Float) {
        if (survival) { survivalWaves(dt); return }
        val m = mission!!
        if (m.type == MissionType.BOSS && !bossesSpawned && time > 1f) {
            bossesSpawned = true
            for (i in 0 until m.bosses) spawnBoss()
        }
        spawnT -= dt
        val canMore = if (m.type == MissionType.KILL) spawned < m.target else true
        if (spawnT <= 0f && canMore && aliveRegular() < m.maxAlive) {
            spawnRegular()
            spawnT = m.spawnInterval
        }
    }

    private fun survivalWaves(dt: Float) {
        if (!waveActive) {
            breakT -= dt
            if (breakT <= 0f) {
                wave++
                toSpawn = 5 + 3 * wave
                waveActive = true
                banner = "WAVE $wave"; bannerT = 2.2f
                if (wave % 5 == 0) spawnBoss()
            }
            return
        }
        spawnT -= dt
        val cap = min(7 + wave, 20)
        if (spawnT <= 0f && toSpawn > 0 && aliveRegular() < cap) {
            val w = listOf(10, if (wave >= 2) 5 else 0, if (wave >= 3) 2 + wave / 2 else 0, if (wave >= 4) 2 + wave / 3 else 0)
            spawnRegular(w)
            toSpawn--
            spawnT = max(0.5f, 1.8f - wave * 0.08f)
        }
        if (toSpawn == 0 && enemies.none { !it.dying }) {
            waveActive = false
            breakT = 3f
            score += wave * 200
            banner = "WAVE CLEARED +${wave * 200}"; bannerT = 2f
        }
    }

    // ---------------------------------------------------------------- enemies
    private fun updateEnemies(dt: Float) {
        val p = player
        for (i in enemies.indices.reversed()) {
            val e = enemies[i]
            if (e.dying) {
                e.dyingT += dt
                if (e.dyingT > 0.7f) enemies.removeAt(i)
                continue
            }
            e.age += dt
            e.hitFlash -= dt; e.shotFlash -= dt; e.steerT -= dt; e.strafeT -= dt
            val d = MathX.dist(e.x, e.y, p.x, p.y)
            val detect = if (e.type == EType.SNIPER) 950f else 650f
            if (!e.alert && (d < detect || e.age > 6f)) e.alert = true
            if (!e.alert) {
                e.walk += dt * 1.5f
                continue
            }
            e.losT -= dt
            if (e.losT <= 0f) { e.los = hasLOS(e.x, e.y, p.x, p.y); e.losT = 0.15f }
            val toP = MathX.angleTo(e.x, e.y, p.x, p.y)
            e.angle = MathX.lerpAngle(e.angle, toP, min(1f, dt * 8f))
            val inRange = d < e.type.range && e.los && !p.dead
            val boss = e.type == EType.BOSS
            val speed = e.type.speed * (if (boss && e.hp < e.maxHp * 0.5f) 1.4f else 1f)
            var dx = 0f
            var dy = 0f
            if (!p.dead) {
                if (d > e.type.prefer || !e.los) { dx = cos(toP); dy = sin(toP) }
                else if (d < e.type.prefer * 0.6f) { dx = -cos(toP); dy = -sin(toP) }
                else {
                    if (e.strafeT <= 0f) { e.strafeT = 1f + r() * 1.5f; e.strafeDir = if (r() < 0.5f) 1f else -1f }
                    dx = -sin(toP) * e.strafeDir * 0.6f; dy = cos(toP) * e.strafeDir * 0.6f
                }
            }
            if (e.steerT > 0f) {
                val sx = -dy * e.steerDir; val sy = dx * e.steerDir
                dx = dx * 0.3f + sx; dy = dy * 0.3f + sy
            }
            if (dx != 0f || dy != 0f) {
                val bx = e.x; val by = e.y
                move(e, dx * speed * dt, dy * speed * dt)
                val moved = MathX.dist(bx, by, e.x, e.y)
                if (moved < speed * dt * 0.3f && e.steerT <= 0f) { e.steerT = 0.7f; e.steerDir = if (r() < 0.5f) 1f else -1f }
                e.walk += dt * speed * 0.065f
            }
            for (o in enemies) {
                if (o === e || o.dying) continue
                val sd = MathX.dist(e.x, e.y, o.x, o.y)
                val md = e.r + o.r
                if (sd < md && sd > 0.01f) {
                    val k = (md - sd) * 0.5f / sd
                    e.x += (e.x - o.x) * k; e.y += (e.y - o.y) * k
                }
            }
            e.fireCd -= dt
            e.charging = e.type == EType.SNIPER && inRange && e.fireCd < 0.7f
            if (inRange && e.fireCd <= 0f) enemyShoot(e, toP)
            if (boss) {
                e.minionT -= dt
                if (e.minionT <= 0f) {
                    e.minionT = 10f
                    if (aliveRegular() < 14) for (k in 0 until 2) {
                        val s = freeSpot(e.x, e.y, 60f, 120f, 17f)
                        if (s != null) spawnEnemy(EType.BASIC, s[0], s[1], true)
                    }
                }
            }
        }
    }

    private fun enemyShoot(e: Enemy, ang: Float) {
        val t = e.type
        e.fireCd = t.fireDelay * (0.9f + r() * 0.3f)
        e.shotFlash = 0.07f
        val dmg = t.dmg * (if (survival) 1f + wave * 0.03f else 1f + (mission!!.diff - 1f) * 0.4f)
        val n = when (t) {
            EType.BOSS -> if (e.hp < e.maxHp * 0.5f) 7 else 5
            EType.HEAVY -> 2
            else -> 1
        }
        val fan = if (t == EType.BOSS) 0.9f else 0.14f
        val inacc = if (t == EType.SNIPER) 0.02f else 0.1f
        for (i in 0 until n) {
            val off = if (n == 1) 0f else (i - (n - 1) / 2f) * fan / (n - 1)
            val a = ang + off + (r() - 0.5f) * inacc
            val sx = e.x + cos(a) * (e.r + 14f)
            val sy = e.y + sin(a) * (e.r + 14f)
            bullets.add(Bullet(sx, sy, cos(a) * t.bulletSpeed, sin(a) * t.bulletSpeed, dmg, false, t.range * 1.3f / t.bulletSpeed,
                if (t == EType.SNIPER) 0xFFFF5252.toInt() else 0xFFFF8A50.toInt(), if (t == EType.BOSS) 5f else 4f, 0))
        }
        spark(e.x + cos(ang) * (e.r + 14f), e.y + sin(ang) * (e.r + 14f), 3, 0xFFFFAB40.toInt(), 120f, 3f, 0.15f)
        if (MathX.dist(e.x, e.y, player.x, player.y) < 650f) sound.play(if (t == EType.SNIPER) SfxId.SNIPER else SfxId.PISTOL, 0.35f)
    }

    private fun damageEnemy(e: Enemy, dmg: Float) {
        if (e.dying) return
        e.hp -= dmg
        e.alert = true
        e.hitFlash = 0.1f
        texts.add(FloatText(e.x + (r() - 0.5f) * 14f, e.y - e.r, dmg.toInt().toString(), 0xFFFFFFFF.toInt()))
        sound.play(SfxId.HIT, 0.5f)
        if (e.hp <= 0f) killEnemy(e)
    }

    private fun killEnemy(e: Enemy) {
        e.dying = true
        e.dyingT = 0f
        kills++
        score += e.type.score
        if (e.type == EType.BOSS) {
            bossesKilled++
            explosion(e.x, e.y, 1.6f)
            for (i in 0 until 8) pickups.add(Pickup(e.x + (r() - 0.5f) * 90f, e.y + (r() - 0.5f) * 90f, PickType.COIN, e.type.coin / 8))
            pickups.add(Pickup(e.x + 30f, e.y, PickType.HEALTH, 40))
            pickups.add(Pickup(e.x - 30f, e.y, PickType.ARMOR, 30))
        } else {
            spark(e.x, e.y, 10, 0xFFFFCC80.toInt(), 200f, 4f, 0.5f)
            smoke(e.x, e.y, 3)
            pickups.add(Pickup(e.x, e.y, PickType.COIN, e.type.coin))
            val roll = r()
            if (roll < 0.12f) pickups.add(Pickup(e.x + 18f, e.y, PickType.HEALTH, 30))
            else if (roll < 0.26f) pickups.add(Pickup(e.x + 18f, e.y, PickType.AMMO, 1))
            else if (roll < 0.32f) pickups.add(Pickup(e.x + 18f, e.y, PickType.ARMOR, 25))
        }
    }

    // ---------------------------------------------------------------- bullets, grenades, pickups
    private fun updateBullets(dt: Float) {
        val p = player
        for (i in bullets.indices.reversed()) {
            val b = bullets[i]
            var remove = false
            val sp = sqrt(b.vx * b.vx + b.vy * b.vy) * dt
            val steps = max(1, ceil(sp / 14f).toInt())
            val sdt = dt / steps
            for (s in 0 until steps) {
                b.x += b.vx * sdt
                b.y += b.vy * sdt
                var hitWall = false
                for (o in obstacles) {
                    if (o.blocksBullets && b.x >= o.x && b.x <= o.x + o.w && b.y >= o.y && b.y <= o.y + o.h) { hitWall = true; break }
                }
                if (hitWall) { spark(b.x, b.y, 4, 0xFFFFE0B2.toInt(), 120f, 2.5f, 0.25f); remove = true; break }
                if (b.fromPlayer) {
                    for (e in enemies) {
                        if (e.dying || e === b.lastHit) continue
                        if (MathX.dist(b.x, b.y, e.x, e.y) < e.r + b.size) {
                            damageEnemy(e, b.damage)
                            b.lastHit = e
                            b.pierce--
                            if (b.pierce < 0) remove = true
                            break
                        }
                    }
                } else if (!p.dead && MathX.dist(b.x, b.y, p.x, p.y) < p.r + b.size) {
                    hurtPlayer(b.damage)
                    remove = true
                }
                if (remove) break
            }
            b.life -= dt
            if (remove || b.life <= 0f || b.x < 0f || b.y < 0f || b.x > W || b.y > H) bullets.removeAt(i)
        }
    }

    fun hurtPlayer(d: Float) {
        val p = player
        if (p.invuln > 0f || p.dead) return
        val absorbed = min(p.armor, d * 0.7f)
        p.armor -= absorbed
        p.hp -= d - absorbed
        p.hurtT = 0.2f
        addShake(3f)
        sound.play(SfxId.HIT, 0.7f)
        spark(p.x, p.y, 5, 0xFFFF5252.toInt(), 150f, 3f, 0.3f)
        if (p.hp <= 0f) {
            p.hp = 0f
            p.dead = true
            p.deadT = 0f
            explosion(p.x, p.y, 0.7f)
        }
    }

    private fun updateGrenades(dt: Float) {
        for (i in grenades.indices.reversed()) {
            val g = grenades[i]
            val drag = kotlin.math.exp(-3f * dt)
            g.vx *= drag; g.vy *= drag
            val nx = g.x + g.vx * dt
            val ny = g.y + g.vy * dt
            if (blockedAt(nx, ny, 6f)) { g.vx = -g.vx * 0.4f; g.vy = -g.vy * 0.4f } else { g.x = nx; g.y = ny }
            g.spin += dt * 10f
            g.fuse -= dt
            if (g.fuse <= 0f) {
                grenades.removeAt(i)
                explodeGrenade(g.x, g.y)
            }
        }
    }

    private fun explodeGrenade(x: Float, y: Float) {
        val rad = 150f
        explosion(x, y, 1f)
        for (e in enemies.toList()) {
            if (e.dying) continue
            val d = MathX.dist(x, y, e.x, e.y)
            if (d < rad + e.r) damageEnemy(e, 160f * (1f - min(1f, d / rad) * 0.6f) * player.dmgMult)
        }
        val pd = MathX.dist(x, y, player.x, player.y)
        if (pd < rad) hurtPlayer(45f * (1f - pd / rad))
    }

    private fun updatePickups(dt: Float) {
        val p = player
        for (i in pickups.indices.reversed()) {
            val k = pickups[i]
            k.t += dt
            k.life -= dt
            val d = MathX.dist(k.x, k.y, p.x, p.y)
            if (!p.dead && d < 130f) {
                val a = MathX.angleTo(k.x, k.y, p.x, p.y)
                val sp = (130f - d) * 3f + 60f
                k.x += cos(a) * sp * dt; k.y += sin(a) * sp * dt
            }
            if (!p.dead && d < p.r + 14f) {
                collect(k)
                pickups.removeAt(i)
            } else if (k.life <= 0f) pickups.removeAt(i)
        }
    }

    private fun collect(k: Pickup) {
        val p = player
        sound.play(SfxId.PICKUP, 0.6f)
        when (k.type) {
            PickType.COIN -> { coinsEarned += k.value; texts.add(FloatText(k.x, k.y, "+${k.value}", 0xFFFFD54F.toInt())) }
            PickType.HEALTH -> { p.hp = min(p.maxHp, p.hp + k.value); texts.add(FloatText(k.x, k.y, "+HP", 0xFFFF6B6B.toInt())) }
            PickType.ARMOR -> { p.armor = min(p.maxArmor, p.armor + k.value); texts.add(FloatText(k.x, k.y, "+ARMOR", 0xFF64B5F6.toInt())) }
            PickType.AMMO -> {
                for (id in owned) if (id != 0) {
                    val w = Weapons.get(id)
                    p.reserve[id] = min(w.magSize * 6, p.reserve[id] + w.magSize)
                }
                texts.add(FloatText(k.x, k.y, "+AMMO", 0xFFA5D6A7.toInt()))
            }
        }
    }

    // ---------------------------------------------------------------- end of mission
    private fun checkEnd() {
        val m = mission ?: return
        val won = when (m.type) {
            MissionType.KILL -> kills >= m.target
            MissionType.SURVIVE -> time >= m.target
            MissionType.BOSS -> bossesKilled >= m.bosses
        }
        if (won && !player.dead) {
            state = WState.WON
            val ratio = player.hp / player.maxHp
            stars = if (ratio > 0.66f) 3 else if (ratio > 0.33f) 2 else 1
            rewardCoins = m.rewardCoins
            rewardScore = m.rewardScore
            score += rewardScore
            chestGot = true
            sound.play(SfxId.VICTORY)
            bank(true)
        }
    }

    fun progressText(): String {
        val m = mission ?: return "WAVE $wave   KILLS $kills"
        return when (m.type) {
            MissionType.KILL -> "${min(kills, m.target)} / ${m.target}"
            MissionType.SURVIVE -> "${max(0, m.target - time.toInt())}s"
            MissionType.BOSS -> "BOSS $bossesKilled / ${m.bosses}"
        }
    }

    /** Writes the results of this run to persistent storage (once). */
    fun bank(won: Boolean) {
        if (banked) return
        banked = true
        save.coins = save.coins + coinsEarned + (if (won) rewardCoins else 0)
        save.totalScore = save.totalScore + score
        save.grenades = player.grenades
        if (survival) {
            if (wave > save.bestSurvivalWave) save.bestSurvivalWave = wave
            if (score > save.bestSurvivalScore) save.bestSurvivalScore = score
        } else if (won && mission != null) {
            save.completeMission(mission.id, stars)
            save.chests = save.chests + 1
        }
    }

    /** Called when the player leaves a run early (menu / restart). */
    fun leave() {
        if (state == WState.PLAYING) bank(false)
    }

    fun canRevive() = state == WState.LOST && !reviveUsed && save.coins >= 200

    fun revive(): Boolean {
        if (!canRevive()) return false
        save.coins = save.coins - 200
        banked = false
        reviveUsed = true
        val p = player
        p.dead = false
        p.hp = p.maxHp
        p.armor = p.maxArmor
        p.invuln = 3f
        bullets.clear()
        state = WState.PLAYING
        banner = "REVIVED"; bannerT = 1.5f
        return true
    }

    // ---------------------------------------------------------------- effects
    fun addShake(v: Float) { if (save.shakeOn) shake = min(18f, shake + v) }

    private fun updateFx(dt: Float) {
        shake = max(0f, shake - dt * 30f)
        bannerT -= dt
        for (i in particles.indices.reversed()) {
            val q = particles[i]
            q.life -= dt
            if (q.life <= 0f) { particles.removeAt(i); continue }
            q.x += q.vx * dt; q.y += q.vy * dt
            val drag = kotlin.math.exp(-2.5f * dt)
            q.vx *= drag; q.vy *= drag
            if (q.kind == 1) q.size += dt * 22f
            if (q.kind == 2) q.size += dt * 520f
        }
        for (i in texts.indices.reversed()) {
            val t = texts[i]
            t.t -= dt; t.y -= 34f * dt
            if (t.t <= 0f) texts.removeAt(i)
        }
    }

    fun spark(x: Float, y: Float, n: Int, color: Int, speed: Float, size: Float, life: Float) {
        if (particles.size > 450) return
        for (i in 0 until n) {
            val a = r() * 2f * PI.toFloat()
            val v = speed * (0.3f + r() * 0.7f)
            particles.add(Particle(x, y, cos(a) * v, sin(a) * v, life * (0.6f + r() * 0.4f), life, size * (0.6f + r() * 0.6f), color, 0))
        }
    }

    fun smoke(x: Float, y: Float, n: Int) {
        if (particles.size > 450) return
        for (i in 0 until n) {
            val a = r() * 2f * PI.toFloat()
            particles.add(Particle(x, y, cos(a) * 25f, sin(a) * 25f - 10f, 0.9f + r() * 0.5f, 1.4f, 10f, 0xFF555555.toInt(), 1))
        }
    }

    fun explosion(x: Float, y: Float, scale: Float) {
        sound.play(SfxId.EXPLOSION, min(1f, 0.6f + scale * 0.4f))
        addShake(10f * scale)
        particles.add(Particle(x, y, 0f, 0f, 0.3f, 0.3f, 10f, 0xFFFFE082.toInt(), 2))
        spark(x, y, (22 * scale).toInt(), 0xFFFF9800.toInt(), 300f * scale, 9f * scale, 0.7f)
        spark(x, y, (12 * scale).toInt(), 0xFFFFEB3B.toInt(), 220f * scale, 6f * scale, 0.5f)
        smoke(x, y, (8 * scale).toInt())
        if (decals.size > 40) decals.removeAt(0)
        decals.add(Decal(x, y, 34f * scale))
    }
}
