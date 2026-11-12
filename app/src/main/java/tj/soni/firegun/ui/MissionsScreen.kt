package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.data.MissionType
import tj.soni.firegun.data.Missions

class MissionsScreen(g: Game) : ButtonScreen(g) {
    private var sel = g.save.unlockedMission.coerceAtMost(Missions.all.size)
    private val gap = 14f
    private val cw = (g.vw - 60f - 4 * gap) / 5f
    private val ch = 170f
    private val y0 = 96f
    private val start: Btn

    init {
        addBack()
        start = Btn(g.vw - 310f, 0f, 270f, 72f, "START", BtnStyle.GREEN, 1, 30f) {
            if (sel <= g.save.unlockedMission) g.go(GameScreen(g, Missions.get(sel)))
        }
        start.y = y0 + 2 * ch + gap + 16f + 244f - 100f
        buttons.add(start)
    }

    private fun cardX(i: Int) = 30f + (i % 5) * (cw + gap)
    private fun cardY(i: Int) = y0 + (i / 5) * (ch + gap)

    override fun onTap(x: Float, y: Float) {
        for (i in Missions.all.indices) {
            if (x >= cardX(i) && x <= cardX(i) + cw && y >= cardY(i) && y <= cardY(i) + ch) {
                sel = i + 1
                g.sound.play(tj.soni.firegun.data.SfxId.CLICK)
            }
        }
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        header(c, w, "MISSIONS")
        val s = g.save
        for (i in Missions.all.indices) {
            val m = Missions.all[i]
            val unlocked = m.id <= s.unlockedMission
            val x = cardX(i); val y = cardY(i)
            val selected = sel == m.id
            Ui.panel(c, x, y, cw, ch, if (unlocked) 0xEE1A2536.toInt() else 0xEE10161F.toInt(), if (selected) Ui.GOLD else Ui.BORDER, 14f)
            if (selected) c.drawRoundRect(x + 3, y + 3, x + cw - 3, y + ch - 3, 12f, 12f, Ui.lineP(Ui.GOLD, 3f))
            Ui.text(c, m.id.toString(), x + cw / 2, y + 66f, 58f, if (unlocked) Ui.GOLD else 0xFF4A5566.toInt(), Paint.Align.CENTER, 6f)
            Ui.text(c, m.name, x + cw / 2, y + 96f, 18f, if (unlocked) Ui.WHITE else 0xFF6B7788.toInt(), Paint.Align.CENTER)
            val tag = when (m.type) { MissionType.KILL -> "ELIMINATE"; MissionType.SURVIVE -> "SURVIVE"; MissionType.BOSS -> "BOSS" }
            Ui.text(c, tag, x + cw / 2, y + 118f, 14f, 0xFF8FA0B8.toInt(), Paint.Align.CENTER)
            if (unlocked) {
                val st = s.stars(m.id)
                for (k in 0 until 3) Ui.star(c, x + cw / 2 + (k - 1) * 34f, y + 146f, 13f, k < st)
            } else Ui.icon(c, 9, x + cw / 2, y + 146f, 14f, 0xFF6B7788.toInt())
        }
        val m = Missions.get(sel)
        val py = y0 + 2 * ch + gap + 16f
        val ph = h - py - 14f
        Ui.panel(c, 30f, py, w - 60f, ph)
        Ui.text(c, "MISSION ${m.id}: ${m.name.uppercase()}", 56f, py + 46f, 32f, Ui.GOLD, Paint.Align.LEFT, 5f)
        Ui.text(c, m.brief, 56f, py + 90f, 22f, Ui.WHITE)
        Ui.text(c, "Difficulty x${"%.1f".format(m.diff)}   Reward: ${m.rewardCoins} coins, ${m.rewardScore} score, 1 chest", 56f, py + 128f, 20f, 0xFFB7C3D6.toInt())
        val unlocked = m.id <= s.unlockedMission
        start.enabled = unlocked
        start.label = if (unlocked) "START" else "LOCKED"
        start.icon = if (unlocked) 1 else 9
        drawButtons(c)
    }
}
