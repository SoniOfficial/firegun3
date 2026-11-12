package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game

class AboutScreen(g: Game) : ButtonScreen(g) {
    init { addBack() }

    override fun draw(c: Canvas, w: Float, h: Float) {
        header(c, w, "ABOUT", false)
        val pw = 760f
        val px = (w - pw) / 2f
        Ui.panel(c, px, 110f, pw, 500f)
        Ui.text(c, "FIRE GUN", w / 2, 190f, 64f, Ui.GOLD, Paint.Align.CENTER, 10f, true)
        val lines = listOf(
            "Version 1.1   -   Package tj.soni.firegun",
            "2D top-down military shooter, 100% offline",
            "",
            "10 missions, survival and KINGDOM open world",
            "5 weapons, upgrades, coins and chests",
            "All graphics and sounds are generated in the app",
            "Progress is saved on your device",
            "",
            "Move: left stick     Shoot: FIRE button",
            "Grenade, reload and weapon switch buttons on the right"
        )
        var y = 250f
        for (l in lines) { Ui.text(c, l, w / 2, y, 22f, 0xFFD5DEEA.toInt(), Paint.Align.CENTER); y += 34f }
        drawButtons(c)
    }
}
