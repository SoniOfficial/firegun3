package tj.soni.firegun.core

import kotlin.math.atan2
import kotlin.math.sqrt

object MathX {
    fun clamp(v: Float, lo: Float, hi: Float): Float = if (v < lo) lo else if (v > hi) hi else v
    fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        return sqrt(dx * dx + dy * dy)
    }
    fun angleTo(ax: Float, ay: Float, bx: Float, by: Float): Float = atan2(by - ay, bx - ax)
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Shortest-turn interpolation between two angles (radians). */
    fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var d = b - a
        while (d > Math.PI) d -= (2 * Math.PI).toFloat()
        while (d < -Math.PI) d += (2 * Math.PI).toFloat()
        return a + d * t
    }

    fun circleHitsRect(cx: Float, cy: Float, r: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
        val px = clamp(cx, rx, rx + rw)
        val py = clamp(cy, ry, ry + rh)
        val dx = cx - px
        val dy = cy - py
        return dx * dx + dy * dy < r * r
    }
}
