package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.data.SfxId
import tj.soni.firegun.data.Weapons
import tj.soni.firegun.kingdom.BType
import tj.soni.firegun.kingdom.KOrder
import tj.soni.firegun.kingdom.KRenderer
import tj.soni.firegun.kingdom.KState
import tj.soni.firegun.kingdom.KingdomWorld
import tj.soni.firegun.kingdom.UType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class KingdomScreen(g: Game) : Screen(g) {
    private val world = KingdomWorld(g.save, g.sound)
    private val renderer = KRenderer(world)
    private var paused = false
    private var menu = 0
    private var buildSel = BType.HOUSE
    private var msg = ""
    private var msgT = 0f
    private var ovKey = ""
    private var ovT = 0f
    private var gx = 0f
    private var gy = 0f

    private var joyId = -1
    private var joyBX = 0f
    private var joyBY = 0f
    private var joyKX = 0f
    private var joyKY = 0f
    private var fireId = -1
    private var down: Btn? = null
    private var downId = -1

    private val bar = ArrayList<Btn>()
    private val buildBtns = ArrayList<Btn>()
    private val trainBtns = ArrayList<Btn>()
    private val ov = ArrayList<Btn>()
    private lateinit var orderBtn: Btn
    private lateinit var targetBtn: Btn
    private val buildTypes = listOf(BType.HOUSE, BType.MINE, BType.BARRACKS, BType.STABLE, BType.TOWER, BType.WALL)

    init { layout() }

    private fun say(s: String) { msg = s; msgT = 2.4f }

    private fun layout() {
        val w = g.vw
        val h = g.vh
        val x0 = w / 2f - 250f
        bar.add(Btn(x0, h - 66f, 140f, 54f, "BUILD", BtnStyle.GOLD, 0, 22f) { menu = if (menu == 1) 0 else 1 })
        bar.add(Btn(x0 + 150f, h - 66f, 140f, 54f, "ARMY", BtnStyle.BLUE, 0, 22f) { menu = if (menu == 2) 0 else 2 })
        orderBtn = Btn(x0 + 300f, h - 66f, 200f, 54f, "", BtnStyle.DARK, 0, 20f) {
            world.order = KOrder.values()[(world.order.ordinal + 1) % 4]
            if (world.order == KOrder.ATTACK && !world.kingdoms[world.attackTarget].alive) world.cycleTarget()
            say("Army order: " + world.order.label)
        }
        bar.add(orderBtn)
        targetBtn = Btn(w / 2f - 110f, h - 128f, 220f, 48f, "", BtnStyle.RED, 0, 20f) { world.cycleTarget() }
        // build menu
        val bx = w / 2f - 295f
        for ((i, t) in buildTypes.withIndex()) {
            val row = i / 4; val col = i % 4
            buildBtns.add(Btn(bx + col * 150f, h - 244f + row * 60f, 140f, 52f, "", BtnStyle.DARK, 0, 18f) { buildSel = t })
        }
        buildBtns.add(Btn(bx + 2 * 150f, h - 184f, 140f, 52f, "", BtnStyle.BLUE, 0, 18f) { say(world.upgradeCastle()) })
        buildBtns.add(Btn(bx + 3 * 150f, h - 184f, 140f, 52f, "PLACE", BtnStyle.GREEN, 0, 22f) { say(world.build(buildSel, gx, gy)) })
        // train menu
        val tx = w / 2f - 235f
        for ((i, t) in listOf(UType.SOLDIER, UType.ARCHER, UType.KNIGHT).withIndex()) {
            trainBtns.add(Btn(tx + i * 160f, h - 150f, 150f, 52f, "${t.label.uppercase()} ${t.cost}", BtnStyle.GREEN, 0, 18f) { say(world.train(t)) })
        }
    }

    private fun overlayActive() = paused || world.state != KState.PLAYING

    private fun activeButtons(): List<Btn> {
        if (overlayActive()) return ov
        val l = ArrayList<Btn>(bar)
        if (menu == 1) l.addAll(buildBtns) else if (menu == 2) l.addAll(trainBtns) else if (world.order == KOrder.ATTACK) l.add(targetBtn)
        return l
    }

    private fun resetInput() { joyId = -1; fireId = -1; world.moveX = 0f; world.moveY = 0f; world.firing = false }

    override fun onPause() { if (world.state == KState.PLAYING && !paused) { paused = true; resetInput() } }
    override fun onLeave() { world.leave(); renderer.release() }

    override fun onBack(): Boolean {
        when {
            world.state != KState.PLAYING -> g.go(MenuScreen(g))
            menu != 0 -> menu = 0
            paused -> paused = false
            else -> { paused = true; resetInput() }
        }
        return true
    }

    // ------------------------------------------------------------ update
    override fun update(dt: Float) {
        ovT += dt
        if (msgT > 0f) msgT -= dt
        if (!paused) world.update(dt)
        // refresh labels
        orderBtn.label = "ORDER: " + world.order.label
        targetBtn.label = "TARGET: " + KingdomWorld.TEAM_NAME[world.attackTarget]
        for ((i, t) in buildTypes.withIndex()) {
            val b = buildBtns[i]
            b.label = "${t.label.uppercase().replace("GOLD ", "")} ${t.cost}"
            b.style = if (t == buildSel) BtnStyle.GOLD else BtnStyle.DARK
        }
        buildBtns[6].label = if (world.castleLevel() >= 4) "MAX LEVEL" else "UPGRADE ${world.upgradeCost()}"
        // ghost
        if (menu == 1 && !overlayActive()) {
            val h = world.hero
            val d = 60f + buildSel.w / 2f
            gx = Math.round((h.x + cos(h.angle) * d) / 20f) * 20f
            gy = Math.round((h.y + sin(h.angle) * d) / 20f) * 20f
            renderer.ghostType = buildSel
            renderer.ghostX = gx; renderer.ghostY = gy
            renderer.ghostOk = world.canPlace(buildSel, gx, gy) && world.gold() >= buildSel.cost
        } else renderer.ghostType = null
        val key = if (paused) "P" else when (world.state) { KState.PLAYING -> ""; KState.WON -> "W"; KState.LOST -> "L" }
        if (key != ovKey) {
            ovKey = key
            ovT = 0f
            buildOverlay()
            if (key != "") resetInput()
        }
    }

    private fun buildOverlay() {
        ov.clear()
        val w = g.vw
        val h = g.vh
        val bx = w / 2f - 200f
        when {
            paused -> {
                ov.add(Btn(bx, h / 2f - 90f, 400f, 62f, "RESUME", BtnStyle.GREEN, 1, 26f) { paused = false })
                ov.add(Btn(bx, h / 2f - 14f, 400f, 62f, "SOUND: " + (if (g.save.soundOn) "ON" else "OFF"), BtnStyle.DARK, 0, 24f) {
                    g.save.soundOn = !g.save.soundOn; g.applySettings(); buildOverlay()
                })
                ov.add(Btn(bx, h / 2f + 62f, 400f, 62f, "MAIN MENU", BtnStyle.RED, 7, 26f) { g.go(MenuScreen(g)) })
            }
            else -> {
                ov.add(Btn(bx, h / 2f + 70f, 400f, 62f, "PLAY AGAIN", BtnStyle.GREEN, 1, 26f) { g.go(KingdomScreen(g)) })
                ov.add(Btn(bx, h / 2f + 146f, 400f, 62f, "MAIN MENU", BtnStyle.RED, 7, 26f) { g.go(MenuScreen(g)) })
            }
        }
    }

    // ------------------------------------------------------------ touch
    private fun hitC(x: Float, y: Float, cx: Float, cy: Float, r: Float): Boolean { val dx = x - cx; val dy = y - cy; return dx * dx + dy * dy <= r * r }

    override fun touch(action: Int, id: Int, x: Float, y: Float) {
        when (action) {
            0 -> {
                val b = activeButtons().lastOrNull { it.enabled && it.hit(x, y) }
                if (b != null) { down = b; downId = id; b.pressed = true; return }
            }
            1 -> { val d = down; if (d != null && id == downId) { d.pressed = d.hit(x, y); return } }
            2 -> {
                val d = down
                if (d != null && id == downId) {
                    down = null; d.pressed = false
                    if (d.hit(x, y)) { g.sound.play(SfxId.CLICK); d.onClick() }
                    return
                }
            }
        }
        if (overlayActive()) return
        val w = g.vw
        val h = g.vh
        when (action) {
            0 -> {
                if (hitC(x, y, w - 125f, h - 135f, 88f)) { fireId = id; world.firing = true; return }
                if (hitC(x, y, w - 235f, h - 200f, 46f)) { world.startReload(); return }
                if (hitC(x, y, w - 110f, h - 285f, 46f)) { world.switchWeapon(); return }
                if (x > w - 82f && y < 82f) { paused = true; resetInput(); return }
                if (x > w - 206f && x < w - 16f && y > 84f && y < 223f) { minimapTap(x - (w - 206f), y - 84f); return }
                if (x < w * 0.5f && joyId == -1) { joyId = id; joyBX = x; joyBY = y; joyKX = x; joyKY = y }
            }
            1 -> if (id == joyId) {
                var dx = x - joyBX
                var dy = y - joyBY
                val len = sqrt(dx * dx + dy * dy)
                if (len > 70f) { dx = dx / len * 70f; dy = dy / len * 70f }
                joyKX = joyBX + dx; joyKY = joyBY + dy
                world.moveX = dx / 70f; world.moveY = dy / 70f
            }
            2 -> {
                if (id == joyId) { joyId = -1; world.moveX = 0f; world.moveY = 0f }
                if (id == fireId) { fireId = -1; world.firing = false }
            }
        }
    }

    private fun minimapTap(mx: Float, my: Float) {
        var best = -1
        var bd = 40f
        for (i in 1..3) {
            if (!world.kingdoms[i].alive) continue
            val c = KingdomWorld.CASTLE_POS[i]
            val cx = c[0] / KingdomWorld.W * 190f
            val cy = c[1] / KingdomWorld.H * 139f
            val d = sqrt((cx - mx) * (cx - mx) + (cy - my) * (cy - my))
            if (d < bd) { bd = d; best = i }
        }
        if (best > 0) { world.attackTarget = best; world.order = KOrder.ATTACK; say("Army attacks ${KingdomWorld.TEAM_NAME[best]}!") }
    }

    // ------------------------------------------------------------ drawing
    override fun draw(c: Canvas, w: Float, h: Float) {
        renderer.draw(c, w, h)
        drawHud(c, w, h)
        drawControls(c, w, h)
        drawMenus(c, w, h)
        for (b in bar) Ui.button(c, b)
        if (menu == 0 && world.order == KOrder.ATTACK && !overlayActive()) Ui.button(c, targetBtn)
        if (menu == 1) for (b in buildBtns) Ui.button(c, b)
        if (menu == 2) for (b in trainBtns) Ui.button(c, b)
        if (overlayActive()) drawOverlay(c, w, h)
    }

    private fun drawHud(c: Canvas, w: Float, h: Float) {
        val p = world.hero
        Ui.panel(c, 8f, 8f, 270f, 160f, 0x88101822.toInt(), 0x00000000, 14f)
        Ui.heart(c, 30f, 30f, 14f)
        Ui.bar(c, 52f, 17f, 214f, 26f, p.hp / p.maxHp, 0xFFFF5A52.toInt(), 0xFFC62828.toInt(), "${p.hp.toInt()}/${p.maxHp.toInt()}")
        tj.soni.firegun.render.Sprites.shield(c, 30f, 70f, 13f, 0xFF42A5F5.toInt(), 0xFF0D47A1.toInt())
        Ui.bar(c, 52f, 57f, 214f, 26f, p.armor / p.maxArmor, 0xFF5AB0FF.toInt(), 0xFF1565C0.toInt(), "${p.armor.toInt()}/${p.maxArmor.toInt()}")
        Ui.coin(c, 30f, 105f, 12f)
        Ui.textC(c, "${world.gold()}  gold", 52f, 105f, 24f, Ui.GOLD, Paint.Align.LEFT, 4f)
        Ui.textC(c, "ARMY ${world.popOf(0)}/${world.popCap(0)}", 20f, 140f, 22f, Ui.WHITE, Paint.Align.LEFT, 4f)
        if (g.save.showFps) Ui.text(c, "FPS ${g.fps}", 290f, 30f, 18f, 0xFFB7F7B7.toInt())

        Ui.panel(c, w / 2 - 150f, 8f, 300f, 62f, 0xCC101822.toInt(), Ui.BORDER, 12f)
        Ui.text(c, "${world.rank()}  -  ${if (world.darkness() > 0.35f) "NIGHT" else "DAY"}", w / 2, 33f, 20f, Ui.GOLD, Paint.Align.CENTER, 3f)
        Ui.text(c, "Enemy kingdoms left: ${world.enemiesAlive()}", w / 2, 58f, 18f, Ui.WHITE, Paint.Align.CENTER)
        Ui.text(c, "SCORE " + world.score, w - 100f, 38f, 22f, Ui.WHITE, Paint.Align.RIGHT, 4f)
        if (msgT > 0f) {
            Ui.panel(c, w / 2 - 190f, 78f, 380f, 38f, 0xDD0A101A.toInt(), Ui.GOLD, 19f)
            Ui.textC(c, msg, w / 2, 97f, 20f, Ui.WHITE)
        }
        if (world.bannerT > 0f) {
            val a = min(1f, world.bannerT / 0.5f)
            Ui.text(c, world.banner, w / 2, h * 0.3f, 44f, (0x00FFFFFF and Ui.GOLD) or ((255 * a).toInt() shl 24), Paint.Align.CENTER, 8f, true)
        }
        if (world.hero.dead) Ui.text(c, "RESPAWN IN ${(6f - world.heroDeadT).toInt() + 1}", w / 2, h * 0.45f, 36f, 0xFFFF8A80.toInt(), Paint.Align.CENTER, 6f)

        // minimap
        val mx = w - 206f; val my = 84f
        Ui.panel(c, mx - 4f, my - 4f, 198f, 147f, 0xDD0E1520.toInt(), Ui.BORDER, 8f)
        c.drawRect(mx, my, mx + 190f, my + 139f, Ui.fillP(0xFF3D6B2E.toInt()))
        val sx = 190f / KingdomWorld.W; val sy = 139f / KingdomWorld.H
        for (l in world.lakes) c.drawCircle(mx + l[0] * sx, my + l[1] * sy, l[2] * sx, Ui.fillP(0xFF3A83B5.toInt()))
        for (u in world.units) if (!u.dead) c.drawRect(mx + u.x * sx - 1.5f, my + u.y * sy - 1.5f, mx + u.x * sx + 1.5f, my + u.y * sy + 1.5f, Ui.fillP(KingdomWorld.TEAM_COLOR[u.team.coerceIn(0, 4)]))
        for (i in 0..3) {
            val k = world.kingdoms[i]
            val cc = k.castle
            if (!k.alive || cc == null) continue
            c.drawRect(mx + cc.x * sx - 6f, my + cc.y * sy - 6f, mx + cc.x * sx + 6f, my + cc.y * sy + 6f, Ui.fillP(KingdomWorld.TEAM_COLOR[i]))
            c.drawRect(mx + cc.x * sx - 6f, my + cc.y * sy - 6f, mx + cc.x * sx + 6f, my + cc.y * sy + 6f, Ui.lineP(0xFFFFFFFF.toInt(), 1.5f))
            if (world.order == KOrder.ATTACK && i == world.attackTarget) c.drawCircle(mx + cc.x * sx, my + cc.y * sy, 11f, Ui.lineP(0xFFFFEB3B.toInt(), 2f))
        }
        c.drawCircle(mx + p.x * sx, my + p.y * sy, 3.5f, Ui.fillP(0xFFFFFFFF.toInt()))
        c.drawRect(mx + world.camX * sx, my + world.camY * sy, mx + (world.camX + world.viewW) * sx, my + (world.camY + world.viewH) * sy, Ui.lineP(0xAAFFFFFF.toInt(), 1.5f))

        val wd = Weapons.get(p.weaponId)
        Ui.panel(c, w - 246f, h - 52f, 210f, 40f, 0xCC101822.toInt(), Ui.BORDER, 12f)
        Ui.weaponIcon(c, wd.id, w - 214f, h - 32f, 0.6f, 0xFFC9D1DB.toInt())
        val reserve = if (wd.id == 0) "∞" else p.reserve[wd.id].toString()
        if (p.reloadT > 0f) Ui.textC(c, "RELOADING", w - 120f, h - 32f, 18f, 0xFFFFD54F.toInt())
        else Ui.textC(c, "${p.mag[wd.id]} / $reserve", w - 120f, h - 32f, 22f, Ui.WHITE, Paint.Align.CENTER, 3f)
    }

    private fun drawMenus(c: Canvas, w: Float, h: Float) {
        if (menu == 1) {
            Ui.panel(c, w / 2 - 305f, h - 258f, 610f, 184f, 0xEE121B2A.toInt(), Ui.GOLD, 14f)
            val ok = renderer.ghostOk
            Ui.text(c, if (ok) "Walk to a spot, then press PLACE" else "Cannot place here (stay inside your base area)", w / 2, h - 266f, 18f, if (ok) 0xFFA5D6A7.toInt() else 0xFFFF8A80.toInt(), Paint.Align.CENTER, 4f)
        } else if (menu == 2) {
            Ui.panel(c, w / 2 - 245f, h - 190f, 490f, 116f, 0xEE121B2A.toInt(), 0xFF42A5F5.toInt(), 14f)
            Ui.text(c, "Queue: ${world.kingdoms[0].queue.size}   Needs Barracks (Knight: Stable)", w / 2, h - 164f, 17f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
        }
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        val bx = if (joyId != -1) joyBX else 150f
        val by = if (joyId != -1) joyBY else h - 150f
        val kx = if (joyId != -1) joyKX else bx
        val ky = if (joyId != -1) joyKY else by
        c.drawCircle(bx, by, 84f, Ui.fillP(0x44101822))
        c.drawCircle(bx, by, 84f, Ui.lineP(0x88FFFFFF.toInt(), 3f))
        c.drawCircle(kx, ky, 38f, Ui.fillP(0xAA9AA5B4.toInt()))
        c.drawCircle(kx, ky, 38f, Ui.lineP(0xFFFFFFFF.toInt(), 3f))
        val firing = fireId != -1
        val fx = w - 125f; val fy = h - 135f
        c.drawCircle(fx, fy, 78f, Ui.fillP(if (firing) 0xDDFF8A00.toInt() else 0xAA1A222E.toInt()))
        c.drawCircle(fx, fy, 78f, Ui.lineP(0xFFFFD54F.toInt(), 5f))
        Ui.weaponIcon(c, 1, fx, fy, 1.3f, 0xFFFFE082.toInt())
        c.drawCircle(w - 235f, h - 200f, 38f, Ui.fillP(0xAA1A222E.toInt()))
        c.drawCircle(w - 235f, h - 200f, 38f, Ui.lineP(0xFF8FA0B8.toInt(), 3.5f))
        Ui.textC(c, "R", w - 235f, h - 200f, 30f, Ui.WHITE)
        c.drawCircle(w - 110f, h - 285f, 38f, Ui.fillP(0xAA1A222E.toInt()))
        c.drawCircle(w - 110f, h - 285f, 38f, Ui.lineP(0xFF8FA0B8.toInt(), 3.5f))
        Ui.icon(c, 8, w - 110f, h - 285f, 15f, Ui.WHITE)
        c.drawRoundRect(w - 70f, 10f, w - 12f, 68f, 12f, 12f, Ui.fillP(0xCC101822.toInt()))
        c.drawRoundRect(w - 70f, 10f, w - 12f, 68f, 12f, 12f, Ui.lineP(Ui.BORDER, 2.5f))
        Ui.icon(c, 10, w - 41f, 39f, 14f, Ui.WHITE)
    }

    private fun drawOverlay(c: Canvas, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, Ui.fillP(0xAA050A12.toInt()))
        val cx = w / 2f
        when {
            paused -> {
                Ui.panel(c, cx - 250f, h / 2f - 200f, 500f, 410f, 0xEE121B2A.toInt(), Ui.GOLD)
                Ui.text(c, "PAUSED", cx, h / 2f - 130f, 52f, Ui.GOLD, Paint.Align.CENTER, 8f, true)
            }
            world.state == KState.WON -> {
                Ui.panel(c, cx - 300f, h / 2f - 250f, 600f, 500f, 0xEE121B2A.toInt(), Ui.GOLD)
                Ui.text(c, "YOU ARE THE KING!", cx, h / 2f - 170f, 46f, Ui.GOLD, Paint.Align.CENTER, 8f, true)
                Ui.icon(c, 2, cx, h / 2f - 100f, 34f, Ui.GOLD)
                Ui.text(c, "All rival kingdoms conquered", cx, h / 2f - 40f, 24f, Ui.WHITE, Paint.Align.CENTER)
                Ui.text(c, "+${world.coinsReward} coins   Score ${world.score}", cx, h / 2f, 26f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
            }
            else -> {
                Ui.panel(c, cx - 300f, h / 2f - 250f, 600f, 500f, 0xEE121B2A.toInt(), 0xFFE53935.toInt())
                Ui.text(c, "KINGDOM FALLEN", cx, h / 2f - 170f, 50f, 0xFFFF5A52.toInt(), Paint.Align.CENTER, 8f, true)
                Ui.text(c, "Kills ${world.kills}   Score ${world.score}", cx, h / 2f - 100f, 26f, Ui.WHITE, Paint.Align.CENTER)
                Ui.text(c, "+${world.coinsReward} coins earned", cx, h / 2f - 60f, 24f, Ui.GOLD, Paint.Align.CENTER)
            }
        }
        for (b in ov) Ui.button(c, b)
    }
}
