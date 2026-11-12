package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.data.Missions

class ModeScreen(g: Game) : ButtonScreen(g) {
    private val cw = minOf(390f, (g.vw - 100f) / 3f)
    private val ch = 400f
    private val lx = g.vw / 2f - cw * 1.5f - 20f
    private val mx = g.vw / 2f - cw / 2f
    private val rx = g.vw / 2f + cw * 0.5f + 20f
    private val cy = 120f

    init {
        addBack()
        val unlocked = g.save.unlockedMission
        buttons.add(Btn(lx + 40f, cy + ch - 90f, cw - 80f, 66f, "START", BtnStyle.GREEN, 1, 30f) { g.go(GameScreen(g, Missions.get(unlocked))) })
        buttons.add(Btn(mx + 40f, cy + ch - 90f, cw - 80f, 66f, "START", BtnStyle.GOLD, 1, 30f) { g.go(GameScreen(g, null)) })
        buttons.add(Btn(rx + 40f, cy + ch - 90f, cw - 80f, 66f, "BUILD", BtnStyle.BLUE, 1, 30f) { g.go(KingdomScreen(g)) })
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        header(c, w, "SELECT MODE")
        val s = g.save
        Ui.panel(c, lx, cy, cw, ch)
        Ui.text(c, "MISSION MODE", lx + cw / 2, cy + 56f, 34f, Ui.GOLD, Paint.Align.CENTER, 5f)
        Ui.icon(c, 2, lx + cw / 2, cy + 120f, 36f, Ui.GOLD)
        Ui.text(c, "Continue: Mission ${s.unlockedMission}", lx + cw / 2, cy + 200f, 26f, Ui.WHITE, Paint.Align.CENTER)
        Ui.text(c, Missions.get(s.unlockedMission).name, lx + cw / 2, cy + 236f, 22f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
        Ui.text(c, "10 missions with bosses", lx + cw / 2, cy + 270f, 20f, 0xFF8FA0B8.toInt(), Paint.Align.CENTER)

        Ui.panel(c, mx, cy, cw, ch)
        Ui.text(c, "SURVIVAL MODE", mx + cw / 2, cy + 56f, 34f, Ui.GOLD, Paint.Align.CENTER, 5f)
        Ui.skull(c, mx + cw / 2, cy + 120f, 34f, 0xFFE8E8E8.toInt())
        Ui.text(c, "Best wave: ${s.bestSurvivalWave}", mx + cw / 2, cy + 200f, 26f, Ui.WHITE, Paint.Align.CENTER)
        Ui.text(c, "Best score: ${s.bestSurvivalScore}", mx + cw / 2, cy + 236f, 22f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
        Ui.text(c, "Endless waves, boss every 5th", mx + cw / 2, cy + 270f, 20f, 0xFF8FA0B8.toInt(), Paint.Align.CENTER)

        Ui.panel(c, rx, cy, cw, ch, 0xDD121B2A.toInt(), Ui.GOLD)
        Ui.text(c, "KINGDOM", rx + cw / 2, cy + 56f, 34f, Ui.GOLD, Paint.Align.CENTER, 5f)
        Ui.text(c, "OPEN WORLD", rx + cw / 2, cy + 86f, 20f, 0xFF9CC9FF.toInt(), Paint.Align.CENTER)
        tj.soni.firegun.render.Sprites.shield(c, rx + cw / 2, cy + 140f, 34f, 0xFF2F6FE0.toInt(), 0xFF0D2F73.toInt())
        Ui.text(c, "Build a castle and army,", rx + cw / 2, cy + 220f, 22f, Ui.WHITE, Paint.Align.CENTER)
        Ui.text(c, "conquer 3 kingdoms", rx + cw / 2, cy + 250f, 22f, Ui.WHITE, Paint.Align.CENTER)
        Ui.text(c, "Crowns: ${s.crowns}   Conquered: ${s.conquered}", rx + cw / 2, cy + 286f, 18f, 0xFF8FA0B8.toInt(), Paint.Align.CENTER)
        drawButtons(c)
    }
}
