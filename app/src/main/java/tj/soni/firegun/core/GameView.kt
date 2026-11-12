package tj.soni.firegun.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context, private val game: Game) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var thread: Thread? = null
    @Volatile private var running = false
    private var scale = 1f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this, "GameLoop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        scale = height / 720f
        synchronized(game) { game.resize(width / scale, 720f) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { thread?.join(1000) } catch (_: InterruptedException) { }
        thread = null
    }

    override fun run() {
        var last = System.nanoTime()
        var fpsTimer = 0f
        var frames = 0
        while (running) {
            val start = System.nanoTime()
            var dt = (start - last) / 1_000_000_000f
            last = start
            if (dt > 0.05f) dt = 0.05f
            synchronized(game) { game.update(dt) }
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas()
                val cv: Canvas? = canvas
                if (cv != null) {
                    cv.drawColor(0xFF0B1320.toInt())
                    cv.save()
                    cv.scale(scale, scale)
                    synchronized(game) { game.draw(cv) }
                    cv.restore()
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
            frames++
            fpsTimer += dt
            if (fpsTimer >= 1f) { game.fps = frames; frames = 0; fpsTimer = 0f }
            val spent = (System.nanoTime() - start) / 1_000_000L
            if (spent < 8L) try { Thread.sleep(8L - spent) } catch (_: InterruptedException) { }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        synchronized(game) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val i = e.actionIndex
                    game.touch(0, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until e.pointerCount) game.touch(1, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val i = e.actionIndex
                    game.touch(2, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
                }
                MotionEvent.ACTION_CANCEL -> {
                    for (i in 0 until e.pointerCount) game.touch(2, e.getPointerId(i), -1000f, -1000f)
                }
            }
        }
        return true
    }
}
