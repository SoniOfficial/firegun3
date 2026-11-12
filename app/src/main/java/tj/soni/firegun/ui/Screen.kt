package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.render.MenuArt

abstract class Screen(val g: Game) {
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas, w: Float, h: Float)
    open fun touch(action: Int, id: Int, x: Float, y: Float) {}
    /** Returns true when the back press was handled inside the game. */
    open fun onBack(): Boolean { g.go(MenuScreen(g)); return true }
    open fun onPause() {}
    open fun onLeave() {}
}

abstract class ButtonScreen(g: Game) : Screen(g) {
    val buttons = ArrayList<Btn>()
    private var down: Btn? = null

    open fun onTap(x: Float, y: Float) {}

    override fun touch(action: Int, id: Int, x: Float, y: Float) {
        when (action) {
            0 -> { down = buttons.lastOrNull { it.enabled && it.hit(x, y) }; down?.pressed = true }
            1 -> { down?.let { it.pressed = it.hit(x, y) } }
            2 -> {
                val d = down
                down = null
                if (d != null) {
                    d.pressed = false
                    if (d.hit(x, y)) { g.sound.play(tj.soni.firegun.data.SfxId.CLICK); d.onClick() }
                } else onTap(x, y)
            }
        }
    }

    protected fun addBack() {
        buttons.add(Btn(16f, 12f, 76f, 52f, "", BtnStyle.GOLD, 6) { onBack() })
    }

    protected fun drawButtons(c: Canvas) { for (b in buttons) Ui.button(c, b) }

    protected fun header(c: Canvas, w: Float, title: String, showCoins: Boolean = true) {
        MenuArt.backdrop(c, w, g.vh)
        c.drawRect(0f, 0f, w, 76f, Ui.fillP(0xEE0A101A.toInt()))
        c.drawRect(0f, 74f, w, 78f, Ui.fillP(Ui.GOLD))
        Ui.text(c, title, w / 2f, 52f, 36f, Ui.GOLD, Paint.Align.CENTER, 6f, true)
        if (showCoins) coinBox(c, w - 200f, 16f)
    }

    protected fun coinBox(c: Canvas, x: Float, y: Float) {
        Ui.panel(c, x, y, 184f, 44f, 0xFF141C28.toInt(), 0xFF3D4A60.toInt(), 22f)
        Ui.coin(c, x + 26f, y + 22f, 13f)
        Ui.textC(c, g.save.coins.toString(), x + 108f, y + 22f, 26f, Ui.GOLD)
    }
}
