package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import tj.soni.firegun.core.Game
import tj.soni.firegun.data.MissionDef
import tj.soni.firegun.data.Missions
import tj.soni.firegun.data.SfxId
import tj.soni.firegun.data.Weapons
import tj.soni.firegun.game.EType
import tj.soni.firegun.game.WState
import tj.soni.firegun.game.World
import tj.soni.firegun.render.WorldRenderer
import kotlin.math.min
import kotlin.math.sqrt

class GameScreen(g: Game, private val mission: MissionDef?) : Screen(g) {
    private val world = World(g.save, g.sound, mission)
    private val renderer = WorldRenderer(world)
    private var paused = false
    private var ovKey = ""
    private var ovT = 0f
    private val ov = ArrayList<Btn>()
    private var ovDown: Btn? = null

    // input state
    private var joyId = -1
    private var joyBX = 0f
    private var joyBY = 0f
    private var joyKX = 0f
    private var joyKY = 0f
    private var fireId = -1
    private val path = Path()

    private fun overlayActive() = paused || world.state != WState.PLAYING

    private fun resetInput() {
        joyId = -1; fireId = -1
        world.moveX = 0f; world.moveY = 0f; world.firing = false
    }

    // ------------------------------------------------------------ lifecycle
    override fun onPause() { if (world.state == WState.PLAYING && !paused) { paused = true; resetInput() } }

    override fun onLeave() {
        world.leave()
        renderer.release()
    }

    override fun onBack(): Boolean {
        when {
            world.state != WState.PLAYING -> { g.go(MenuScreen(g)) }
            paused -> paused = false
            else -> { paused = true; resetInput() }
        }
        return true
    }

    override fun update(dt: Float) {
        ovT += dt
        if (!paused) world.update(dt)
        val key = if (paused) "P" else when (world.state) {
            WState.PLAYING -> ""
            WState.WON -> "W"
            WState.LOST -> "L" + world.canRevive()
        }
        if (key != ovKey) {
            if (ovKey == "" || key == "") ovT = 0f
            ovKey = key
            buildOverlay()
            if (key != "") resetInput()
        }
    }

    // ------------------------------------------------------------ overlay buttons
    private fun buildOverlay() {
        ov.clear()
        ovDown = null
        val w = g.vw
        val h = g.vh
        when {
            paused -> {
                val bx = w / 2f - 200f
                var y = h / 2f - 120f
                ov.add(Btn(bx, y, 400f, 62f, "RESUME", BtnStyle.GREEN, 1, 26f) { paused = false })
                y += 76f
                ov.add(Btn(bx, y, 400f, 62f, "RESTART", BtnStyle.BLUE, 0, 26f) { g.go(GameScreen(g, mission)) })
                y += 76f
                ov.add(Btn(bx, y, 400f, 62f, "SOUND: " + (if (g.save.soundOn) "ON" else "OFF"), BtnStyle.DARK, 0, 24f) {
                    g.save.soundOn = !g.save.soundOn; g.applySettings(); buildOverlay()
                })
                y += 76f
                ov.add(Btn(bx, y, 400f, 62f, "MAIN MENU", BtnStyle.RED, 7, 26f) { g.go(MenuScreen(g)) })
            }
            world.state == WState.LOST -> {
                val bx = w / 2f - 200f
                var y = h / 2f + 20f
                if (world.canRevive()) {
                    ov.add(Btn(bx, y, 400f, 62f, "REVIVE  (200 coins)", BtnStyle.GOLD, 0, 24f) { world.revive() })
                    y += 76f
                }
                ov.add(Btn(bx, y, 400f, 62f, "RETRY", BtnStyle.GREEN, 1, 26f) { g.go(GameScreen(g, mission)) })
                y += 76f
                ov.add(Btn(bx, y, 400f, 62f, "MAIN MENU", BtnStyle.RED, 7, 26f) { g.go(MenuScreen(g)) })
            }
            world.state == WState.WON -> {
                val by = h / 2f + 150f
                val m = mission
                if (m != null && m.id < Missions.all.size) {
                    ov.add(Btn(w / 2f - 330f, by, 310f, 66f, "NEXT MISSION", BtnStyle.BLUE, 8, 24f) { g.go(GameScreen(g, Missions.get(m.id + 1))) })
                    ov.add(Btn(w / 2f + 20f, by, 310f, 66f, "MENU", BtnStyle.GREEN, 7, 26f) { g.go(MenuScreen(g)) })
                } else {
                    ov.add(Btn(w / 2f - 155f, by, 310f, 66f, "MENU", BtnStyle.GREEN, 7, 26f) { g.go(MenuScreen(g)) })
                }
            }
        }
    }

    // ------------------------------------------------------------ touch
    private fun hitC(x: Float, y: Float, cx: Float, cy: Float, r: Float): Boolean {
        val dx = x - cx; val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }

    override fun touch(action: Int, id: Int, x: Float, y: Float) {
        if (overlayActive()) {
            when (action) {
                0 -> { ovDown = ov.lastOrNull { it.enabled && it.hit(x, y) }; ovDown?.pressed = true }
                1 -> ovDown?.let { it.pressed = it.hit(x, y) }
                2 -> {
                    val d = ovDown
                    ovDown = null
                    if (d != null) { d.pressed = false; if (d.hit(x, y)) { g.sound.play(SfxId.CLICK); d.onClick() } }
                }
            }
            return
        }
        val w = g.vw
        val h = g.vh
        when (action) {
            0 -> {
                if (hitC(x, y, w - 125f, h - 135f, 88f)) { fireId = id; world.firing = true; return }
                if (hitC(x, y, w - 265f, h - 85f, 52f)) { world.throwGrenade(); return }
                if (hitC(x, y, w - 235f, h - 200f, 46f)) { world.startReload(); return }
                if (hitC(x, y, w - 110f, h - 285f, 46f)) { world.switchWeapon(); return }
                if (x > w - 82f && y < 82f) { paused = true; resetInput(); return }
                if (x < w * 0.5f && joyId == -1) { joyId = id; joyBX = x; joyBY = y; joyKX = x; joyKY = y }
            }
            1 -> if (id == joyId) {
                var dx = x - joyBX
                var dy = y - joyBY
                val len = sqrt(dx * dx + dy * dy)
                val max = 70f
                if (len > max) { dx = dx / len * max; dy = dy / len * max }
                joyKX = joyBX + dx; joyKY = joyBY + dy
                world.moveX = dx / max; world.moveY = dy / max
            }
            2 -> {
                if (id == joyId) { joyId = -1; world.moveX = 0f; world.moveY = 0f }
                if (id == fireId) { fireId = -1; world.firing = false }
            }
        }
    }

    // ------------------------------------------------------------ drawing
    override fun draw(c: Canvas, w: Float, h: Float) {
        renderer.draw(c, w, h)
        drawHud(c, w, h)
        drawControls(c, w, h)
        if (overlayActive()) drawOverlay(c, w, h)
    }

    private fun drawHud(c: Canvas, w: Float, h: Float) {
        val p = world.player
        Ui.panel(c, 8f, 8f, 270f, 118f, 0x88101822.toInt(), 0x00000000, 14f)
        Ui.heart(c, 30f, 30f, 14f)
        Ui.bar(c, 52f, 17f, 214f, 26f, p.hp / p.maxHp, 0xFFFF5A52.toInt(), 0xFFC62828.toInt(), "${p.hp.toInt()}/${p.maxHp.toInt()}")
        tj.soni.firegun.render.Sprites.shield(c, 30f, 70f, 13f, 0xFF42A5F5.toInt(), 0xFF0D47A1.toInt())
        Ui.bar(c, 52f, 57f, 214f, 26f, p.armor / p.maxArmor, 0xFF5AB0FF.toInt(), 0xFF1565C0.toInt(), "${p.armor.toInt()}/${p.maxArmor.toInt()}")
        Ui.coin(c, 30f, 105f, 12f)
        Ui.textC(c, (g.save.coins + world.coinsEarned).toString(), 52f, 105f, 26f, Ui.GOLD, Paint.Align.LEFT, 4f)
        if (g.save.showFps) Ui.text(c, "FPS ${g.fps}", 290f, 30f, 18f, 0xFFB7F7B7.toInt())

        val m = mission
        Ui.panel(c, w / 2 - 110f, 8f, 220f, 62f, 0xCC101822.toInt(), Ui.BORDER, 12f)
        Ui.text(c, if (m != null) m.name.uppercase() else "SURVIVAL", w / 2, 32f, 17f, 0xFFD5DEEA.toInt(), Paint.Align.CENTER)
        if (m != null) Ui.skull(c, w / 2 - 70f, 51f, 9f, 0xFFE8E8E8.toInt())
        Ui.textC(c, world.progressText(), if (m != null) w / 2 + 8f else w / 2, 52f, 24f, Ui.WHITE, Paint.Align.CENTER, 3f)
        Ui.text(c, "SCORE " + world.score, w - 100f, 38f, 24f, Ui.WHITE, Paint.Align.RIGHT, 4f)

        val boss = world.enemies.firstOrNull { it.type == EType.BOSS && !it.dying }
        if (boss != null) Ui.bar(c, w / 2 - 190f, 80f, 380f, 20f, boss.hp / boss.maxHp, 0xFFFF7043.toInt(), 0xFFBF360C.toInt(), "COMMANDER")

        if (world.bannerT > 0f) {
            val a = min(1f, world.bannerT / 0.5f)
            val col = (0x00FFFFFF and Ui.GOLD) or ((255 * a).toInt() shl 24)
            Ui.text(c, world.banner, w / 2, h * 0.3f, 54f, col, Paint.Align.CENTER, 8f, true)
        }

        // ammo panel
        val wd = Weapons.get(p.weaponId)
        Ui.panel(c, w / 2 - 125f, h - 70f, 250f, 58f, 0xCC101822.toInt(), Ui.BORDER, 14f)
        Ui.weaponIcon(c, wd.id, w / 2 - 82f, h - 41f, 0.9f, 0xFFC9D1DB.toInt())
        val reserve = if (wd.id == 0) "∞" else p.reserve[wd.id].toString()
        if (p.reloadT > 0f) Ui.textC(c, "RELOADING", w / 2 + 34f, h - 41f, 22f, 0xFFFFD54F.toInt(), Paint.Align.CENTER)
        else Ui.textC(c, "${p.mag[wd.id]} / $reserve", w / 2 + 34f, h - 41f, 28f, if (p.mag[wd.id] <= 2) 0xFFFF6B6B.toInt() else Ui.WHITE, Paint.Align.CENTER, 3f)
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        // joystick
        val bx = if (joyId != -1) joyBX else 150f
        val by = if (joyId != -1) joyBY else h - 150f
        val kx = if (joyId != -1) joyKX else bx
        val ky = if (joyId != -1) joyKY else by
        c.drawCircle(bx, by, 84f, Ui.fillP(0x44101822))
        c.drawCircle(bx, by, 84f, Ui.lineP(0x88FFFFFF.toInt(), 3f))
        c.drawCircle(kx, ky, 38f, Ui.fillP(0xAA9AA5B4.toInt()))
        c.drawCircle(kx, ky, 38f, Ui.lineP(0xFFFFFFFF.toInt(), 3f))
        // fire
        val firing = fireId != -1
        val fx = w - 125f; val fy = h - 135f
        c.drawCircle(fx, fy, 78f, Ui.fillP(if (firing) 0xDDFF8A00.toInt() else 0xAA1A222E.toInt()))
        c.drawCircle(fx, fy, 78f, Ui.lineP(0xFFFFD54F.toInt(), 5f))
        path.reset()
        for (i in 0 until 8) {
            val a = (i * Math.PI / 4).toFloat()
            val r = if (i % 2 == 0) 40f else 20f
            val px = fx + kotlin.math.cos(a) * r
            val py = fy + kotlin.math.sin(a) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        c.drawPath(path, Ui.fillP(0xFFFFE082.toInt()))
        c.drawCircle(fx, fy, 12f, Ui.fillP(0xFFFFFFFF.toInt()))
        // grenade
        c.drawCircle(w - 265f, h - 85f, 46f, Ui.fillP(0xAA1A222E.toInt()))
        c.drawCircle(w - 265f, h - 85f, 46f, Ui.lineP(0xFF8FA0B8.toInt(), 4f))
        Ui.grenadeIcon(c, w - 265f, h - 85f, 24f)
        c.drawCircle(w - 233f, h - 55f, 15f, Ui.fillP(0xFF0D121A.toInt()))
        Ui.textC(c, world.player.grenades.toString(), w - 233f, h - 55f, 20f, Ui.WHITE)
        // reload + swap
        smallBtn(c, w - 235f, h - 200f, "R")
        smallBtn(c, w - 110f, h - 285f, "")
        Ui.icon(c, 8, w - 110f, h - 285f, 15f, Ui.WHITE)
        // pause
        c.drawRoundRect(w - 70f, 10f, w - 12f, 68f, 12f, 12f, Ui.fillP(0xCC101822.toInt()))
        c.drawRoundRect(w - 70f, 10f, w - 12f, 68f, 12f, 12f, Ui.lineP(Ui.BORDER, 2.5f))
        Ui.icon(c, 10, w - 41f, 39f, 14f, Ui.WHITE)
    }

    private fun smallBtn(c: Canvas, x: Float, y: Float, label: String) {
        c.drawCircle(x, y, 38f, Ui.fillP(0xAA1A222E.toInt()))
        c.drawCircle(x, y, 38f, Ui.lineP(0xFF8FA0B8.toInt(), 3.5f))
        Ui.textC(c, label, x, y, 30f, Ui.WHITE)
    }

    private fun drawOverlay(c: Canvas, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, Ui.fillP(0xAA050A12.toInt()))
        val cx = w / 2f
        when {
            paused -> {
                Ui.panel(c, cx - 250f, h / 2f - 215f, 500f, 440f, 0xEE121B2A.toInt(), Ui.GOLD)
                Ui.text(c, "PAUSED", cx, h / 2f - 150f, 52f, Ui.GOLD, Paint.Align.CENTER, 8f, true)
            }
            world.state == WState.LOST -> {
                Ui.panel(c, cx - 270f, h / 2f - 250f, 540f, 520f, 0xEE121B2A.toInt(), 0xFFE53935.toInt())
                Ui.text(c, "GAME OVER", cx, h / 2f - 170f, 56f, 0xFFFF5A52.toInt(), Paint.Align.CENTER, 8f, true)
                Ui.text(c, "Score: ${world.score}", cx, h / 2f - 110f, 28f, Ui.WHITE, Paint.Align.CENTER)
                Ui.text(c, if (mission == null) "Wave ${world.wave}   Kills ${world.kills}" else "Kills ${world.kills}   Mission ${mission.id}", cx, h / 2f - 72f, 24f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
                Ui.text(c, "Coins collected: ${world.coinsEarned}", cx, h / 2f - 38f, 24f, Ui.GOLD, Paint.Align.CENTER)
            }
            world.state == WState.WON -> drawWon(c, w, h)
        }
        for (b in ov) Ui.button(c, b)
    }

    private fun drawWon(c: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val top = h / 2f - 250f
        Ui.panel(c, cx - 360f, top, 720f, 520f, 0xEE121B2A.toInt(), Ui.GOLD)
        // ribbon
        path.reset()
        path.moveTo(cx - 400f, top + 30f); path.lineTo(cx + 400f, top + 30f); path.lineTo(cx + 360f, top + 80f)
        path.lineTo(cx + 400f, top + 130f); path.lineTo(cx - 400f, top + 130f); path.lineTo(cx - 360f, top + 80f); path.close()
        c.drawPath(path, Ui.fillP(0xFFF2A900.toInt()))
        c.drawPath(path, Ui.lineP(0xFF7A4A00.toInt(), 4f))
        Ui.text(c, "MISSION COMPLETED", cx, top + 96f, 50f, 0xFFFFF3C4.toInt(), Paint.Align.CENTER, 9f, true)
        for (k in 0 until 3) {
            val reveal = ovT > 0.5f + k * 0.35f
            val sc = if (k == 1) 40f else 32f
            Ui.star(c, cx + (k - 1) * 90f, top + (if (k == 1) 40f else 62f), sc, reveal && k < world.stars)
        }
        val ry = top + 190f
        Ui.coin(c, cx - 110f, ry, 20f)
        Ui.text(c, "+${world.coinsEarned + world.rewardCoins}", cx - 70f, ry + 12f, 36f, Ui.WHITE, Paint.Align.LEFT, 5f)
        Ui.star(c, cx - 110f, ry + 64f, 20f, true)
        Ui.text(c, "+${world.score}", cx - 70f, ry + 76f, 36f, Ui.GOLD, Paint.Align.LEFT, 5f)
        Ui.icon(c, 12, cx - 110f, ry + 128f, 20f, Ui.GOLD)
        Ui.text(c, "+1 chest", cx - 70f, ry + 140f, 36f, Ui.WHITE, Paint.Align.LEFT, 5f)
    }
}
