package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.render.MenuArt

class MenuScreen(g: Game) : ButtonScreen(g) {
    init {
        val w = g.vw
        val h = g.vh
        val bw = (w * 0.3f).coerceIn(300f, 400f)
        val bx = w - bw - 44f
        var y = h * 0.43f
        buttons.add(Btn(bx, y, bw, 74f, "PLAY", BtnStyle.GREEN, 1, 36f) { g.go(ModeScreen(g)) })
        y += 74f + 14f
        val labels = listOf("MISSIONS", "WEAPONS", "SETTINGS", "ABOUT")
        val icons = listOf(2, 3, 4, 5)
        val actions: List<() -> Unit> = listOf(
            { g.go(MissionsScreen(g)) },
            { g.go(WeaponsScreen(g)) },
            { g.go(SettingsScreen(g)) },
            { g.go(AboutScreen(g)) }
        )
        for (i in labels.indices) {
            buttons.add(Btn(bx, y, bw, 50f, labels[i], BtnStyle.DARK, icons[i], 24f, actions[i]))
            y += 50f + 10f
        }
    }

    override fun onBack(): Boolean = false

    override fun draw(c: Canvas, w: Float, h: Float) {
        MenuArt.draw(c, w, h, g.time)
        val tx = w * 0.60f
        c.save()
        c.translate(tx, h * 0.23f)
        c.skew(-0.16f, 0f)
        Ui.text(c, "FIRE", 0f, 0f, 128f, Ui.GOLD, Paint.Align.CENTER, 18f, true)
        c.save()
        c.clipRect(-320f, -140f, 320f, -62f)
        Ui.text(c, "FIRE", 0f, 0f, 128f, 0xFFFFE27A.toInt(), Paint.Align.CENTER, 0f, true)
        c.restore()
        Ui.text(c, "GUN", -20f, 124f, 128f, 0xFFEAF0F6.toInt(), Paint.Align.CENTER, 18f, true)
        c.drawLine(140f, 60f, 250f, 140f, Ui.lineP(0xFFE53935.toInt(), 10f))
        c.restore()
        drawButtons(c)
        Ui.text(c, "v1.1", 20f, h - 18f, 22f, 0xFFFFFFFF.toInt(), Paint.Align.LEFT, 4f)
        menuCoins(c, w - 204f, 14f)
    }

    private fun menuCoins(c: Canvas, x: Float, y: Float) {
        Ui.panel(c, x, y, 184f, 44f, 0xCC141C28.toInt(), 0xFF3D4A60.toInt(), 22f)
        Ui.coin(c, x + 26f, y + 22f, 13f)
        Ui.textC(c, g.save.coins.toString(), x + 108f, y + 22f, 26f, Ui.GOLD)
    }
}
