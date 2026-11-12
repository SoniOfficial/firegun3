package tj.soni.firegun.core

import android.content.Context
import android.graphics.Canvas
import tj.soni.firegun.audio.SoundManager
import tj.soni.firegun.data.PrefsStore
import tj.soni.firegun.data.SaveManager
import tj.soni.firegun.ui.GameScreen
import tj.soni.firegun.ui.MenuScreen
import tj.soni.firegun.ui.Screen

class Game(context: Context) {
    val save = SaveManager(PrefsStore(context))
    val sound = SoundManager(context)
    var vw = 1280f
    var vh = 720f
    var fps = 0
    var time = 0f
    var screen: Screen? = null

    init { applySettings() }

    fun applySettings() { sound.enabled = save.soundOn }

    fun go(s: Screen) {
        screen?.onLeave()
        screen = s
    }

    fun resize(w: Float, h: Float) {
        val changed = kotlin.math.abs(w - vw) > 1f || kotlin.math.abs(h - vh) > 1f
        vw = w; vh = h
        if (screen == null || (changed && screen !is GameScreen)) go(MenuScreen(this))
    }

    fun update(dt: Float) { time += dt; screen?.update(dt) }
    fun draw(c: Canvas) { screen?.draw(c, vw, vh) }
    fun touch(action: Int, id: Int, x: Float, y: Float) { screen?.touch(action, id, x, y) }
    fun onPause() { screen?.onPause() }
    fun onBack(): Boolean = screen?.onBack() ?: false
    fun release() { screen?.onLeave(); sound.release() }
}
