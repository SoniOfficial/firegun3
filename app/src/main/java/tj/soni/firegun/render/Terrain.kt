package tj.soni.firegun.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import tj.soni.firegun.game.World
import kotlin.math.cos
import kotlin.math.sin

object Terrain {
    private const val SCALE = 0.5f

    fun build(theme: Int, seed: Long): Bitmap {
        val bw = (World.W * SCALE).toInt()
        val bh = (World.H * SCALE).toInt()
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(SCALE, SCALE)
        val rnd = java.util.Random(seed)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val base: Int; val light: Int; val dark: Int; val dirt: Int; val dirtLight: Int
        when (theme) {
            1 -> { base = 0xFFC9A66B.toInt(); light = 0xFFD9BC82.toInt(); dark = 0xFFB68F55.toInt(); dirt = 0xFF8F6E45.toInt(); dirtLight = 0xFFA98457.toInt() }
            2 -> { base = 0xFF2B4A2E.toInt(); light = 0xFF35583A.toInt(); dark = 0xFF213A25.toInt(); dirt = 0xFF3E352B.toInt(); dirtLight = 0xFF51463A.toInt() }
            else -> { base = 0xFF4E8F2F.toInt(); light = 0xFF61A63A.toInt(); dark = 0xFF3F7A27.toInt(); dirt = 0xFF8B6A42.toInt(); dirtLight = 0xFFA58558.toInt() }
        }
        c.drawColor(base)
        p.style = Paint.Style.FILL
        for (i in 0 until 160) {
            p.color = if (rnd.nextBoolean()) light else dark
            p.alpha = 45
            val x = rnd.nextFloat() * World.W
            val y = rnd.nextFloat() * World.H
            val r = 120f + rnd.nextFloat() * 260f
            c.drawOval(x - r, y - r * 0.65f, x + r, y + r * 0.65f, p)
        }
        for (i in 0 until 320) {
            p.color = if (rnd.nextBoolean()) light else dark
            p.alpha = 90
            val x = rnd.nextFloat() * World.W
            val y = rnd.nextFloat() * World.H
            val r = 30f + rnd.nextFloat() * 90f
            c.drawOval(x - r, y - r * 0.7f, x + r, y + r * 0.7f, p)
        }
        // road
        val path = Path()
        var y = -40f
        path.moveTo(World.roadX(y), y)
        while (y < World.H + 40f) { y += 30f; path.lineTo(World.roadX(y), y) }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.alpha = 255
        p.color = dark; p.strokeWidth = 190f; c.drawPath(path, p)
        p.color = dirt; p.strokeWidth = 160f; c.drawPath(path, p)
        p.color = dirtLight; p.strokeWidth = 110f; c.drawPath(path, p)
        // road details
        p.style = Paint.Style.FILL
        for (i in 0 until 700) {
            val yy = rnd.nextFloat() * World.H
            val xx = World.roadX(yy) + (rnd.nextFloat() - 0.5f) * 170f
            p.color = if (rnd.nextBoolean()) dirt else 0xFF6B5A44.toInt()
            p.alpha = 150
            c.drawCircle(xx, yy, 2f + rnd.nextFloat() * 5f, p)
        }
        // tufts / flowers
        p.strokeWidth = 2f
        for (i in 0 until 1400) {
            val x = rnd.nextFloat() * World.W
            val yy = rnd.nextFloat() * World.H
            if (kotlin.math.abs(x - World.roadX(yy)) < 95f) continue
            p.style = Paint.Style.STROKE
            p.color = dark; p.alpha = 200
            val a = rnd.nextFloat() * 6.28f
            c.drawLine(x, yy, x + cos(a) * 6f, yy - 7f + sin(a) * 2f, p)
            c.drawLine(x, yy, x + 5f, yy - 6f, p)
            if (theme == 0 && i % 9 == 0) {
                p.style = Paint.Style.FILL
                p.color = if (i % 2 == 0) 0xFFFFEB3B.toInt() else 0xFFFFFFFF.toInt(); p.alpha = 255
                c.drawCircle(x + 4f, yy - 7f, 2.6f, p)
            }
        }
        p.style = Paint.Style.FILL
        for (i in 0 until 1800) {
            val x = rnd.nextFloat() * World.W
            val yy = rnd.nextFloat() * World.H
            p.color = if (rnd.nextBoolean()) 0xFF8A8F96.toInt() else 0xFFCFC8B0.toInt()
            p.alpha = 140
            c.drawOval(x, yy, x + 3f + rnd.nextFloat() * 5f, yy + 2f + rnd.nextFloat() * 3f, p)
        }
        return bmp
    }
}
