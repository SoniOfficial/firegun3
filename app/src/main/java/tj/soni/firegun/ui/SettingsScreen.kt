package tj.soni.firegun.ui

import android.graphics.Canvas
import tj.soni.firegun.core.Game

class SettingsScreen(g: Game) : ButtonScreen(g) {
    private val sound: Btn
    private val aim: Btn
    private val shake: Btn
    private val fps: Btn
    private val reset: Btn
    private var confirm = 0f

    init {
        addBack()
        val bw = 560f
        val x = (g.vw - bw) / 2f
        val s = g.save
        sound = Btn(x, 110f, bw, 66f, "", BtnStyle.GREEN, 0, 26f) { s.soundOn = !s.soundOn; g.applySettings(); refresh() }
        aim = Btn(x, 192f, bw, 66f, "", BtnStyle.GREEN, 0, 26f) { s.autoAim = !s.autoAim; refresh() }
        shake = Btn(x, 274f, bw, 66f, "", BtnStyle.GREEN, 0, 26f) { s.shakeOn = !s.shakeOn; refresh() }
        fps = Btn(x, 356f, bw, 66f, "", BtnStyle.GREEN, 0, 26f) { s.showFps = !s.showFps; refresh() }
        reset = Btn(x, 460f, bw, 66f, "RESET PROGRESS", BtnStyle.RED, 0, 24f) {
            if (confirm > 0f) { s.resetAll(); g.applySettings(); g.go(MenuScreen(g)) } else { confirm = 3f }
        }
        buttons.add(sound); buttons.add(aim); buttons.add(shake); buttons.add(fps); buttons.add(reset)
        refresh()
    }

    private fun refresh() {
        val s = g.save
        fun set(b: Btn, name: String, on: Boolean) { b.label = "$name: ${if (on) "ON" else "OFF"}"; b.style = if (on) BtnStyle.GREEN else BtnStyle.DARK }
        set(sound, "SOUND", s.soundOn)
        set(aim, "AUTO-AIM", s.autoAim)
        set(shake, "SCREEN SHAKE", s.shakeOn)
        set(fps, "SHOW FPS", s.showFps)
    }

    override fun update(dt: Float) {
        if (confirm > 0f) confirm -= dt
        reset.label = if (confirm > 0f) "TAP AGAIN TO ERASE EVERYTHING" else "RESET PROGRESS"
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        header(c, w, "SETTINGS", false)
        drawButtons(c)
    }
}
