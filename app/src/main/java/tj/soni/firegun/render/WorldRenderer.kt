package tj.soni.firegun.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import tj.soni.firegun.data.Weapons
import tj.soni.firegun.kingdom.Grain
import tj.soni.firegun.game.EType
import tj.soni.firegun.game.ObType
import tj.soni.firegun.game.PickType
import tj.soni.firegun.game.World
import kotlin.math.sin

class WorldRenderer(private val world: World) {
    private val terrain: Bitmap = Terrain.build(world.mission?.theme ?: 0, (world.mission?.id ?: 99).toLong() * 31L)
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val src = RectF()
    private val rnd = java.util.Random()
    private val vig = Paint()
    private var vigW = 0f

    private fun vis(x: Float, y: Float, m: Float): Boolean =
        x > world.camX - m && x < world.camX + world.viewW + m && y > world.camY - m && y < world.camY + world.viewH + m

    fun draw(c: Canvas, vw: Float, vh: Float) {
        val w = world
        w.viewW = vw / World.ZOOM
        w.viewH = vh / World.ZOOM
        val sx = if (w.shake > 0f) (rnd.nextFloat() - 0.5f) * w.shake else 0f
        val sy = if (w.shake > 0f) (rnd.nextFloat() - 0.5f) * w.shake else 0f
        c.save()
        c.scale(World.ZOOM, World.ZOOM)
        c.translate(-w.camX + sx, -w.camY + sy)

        src.set(0f, 0f, World.W, World.H)
        c.drawBitmap(terrain, null, src, bmpPaint)
        c.drawRect(w.camX, w.camY, w.camX + w.viewW, w.camY + w.viewH, Grain.grainPaint())
        p.style = Paint.Style.STROKE; p.strokeWidth = 8f; p.color = 0xFF1B2330.toInt()
        c.drawRect(0f, 0f, World.W, World.H, p)

        p.style = Paint.Style.FILL
        for (d in w.decals) {
            if (!vis(d.x, d.y, 80f)) continue
            p.color = 0x55000000; c.drawCircle(d.x, d.y, d.r, p)
            p.color = 0x44000000; c.drawCircle(d.x, d.y, d.r * 0.6f, p)
        }

        for (o in w.obstacles) {
            if (o.type == ObType.TREE || !vis(o.x + o.w / 2, o.y + o.h / 2, 200f)) continue
            when (o.type) {
                ObType.ROCK -> Sprites.rock(c, o.x, o.y, o.w, o.h)
                ObType.CRATE -> Sprites.crate(c, o.x, o.y, o.w, o.h, o.variant)
                ObType.BARRIER -> Sprites.barrier(c, o.x, o.y, o.w, o.h, o.variant)
                ObType.BUILDING -> Sprites.building(c, o.x, o.y, o.w, o.h, o.variant)
                ObType.HELIPAD -> Sprites.helipad(c, o.x, o.y, o.w, o.h)
                ObType.TANK -> Sprites.tank(c, o.x, o.y, o.w, o.h, o.variant)
                else -> {}
            }
        }

        for (k in w.pickups) {
            if (!vis(k.x, k.y, 40f)) continue
            if (k.life < 4f && ((k.life * 6).toInt() % 2 == 0)) continue
            val by = k.y + sin(k.t * 5f) * 3f
            when (k.type) {
                PickType.COIN -> Sprites.coin(c, k.x, by, 8f)
                PickType.HEALTH -> Sprites.medkit(c, k.x, by)
                PickType.AMMO -> Sprites.ammoBox(c, k.x, by)
                PickType.ARMOR -> Sprites.armorPick(c, k.x, by)
            }
        }

        for (g in w.grenades) Sprites.grenade(c, g.x, g.y, ((g.fuse * 8f).toInt() % 2 == 0))

        // sniper lasers
        for (e in w.enemies) {
            if (e.charging && !e.dying) Sprites.lineP(c, e.x, e.y, w.player.x, w.player.y, 0x88FF1744.toInt(), 2f)
        }

        for (e in w.enemies) {
            if (!vis(e.x, e.y, 120f)) continue
            val style = when (e.type) {
                EType.BASIC -> Sprites.BASIC; EType.FAST -> Sprites.FAST; EType.HEAVY -> Sprites.HEAVY
                EType.SNIPER -> Sprites.SNIPER; EType.BOSS -> Sprites.BOSS
            }
            val gl = when (e.type) { EType.SNIPER -> 40f; EType.BOSS -> 30f; EType.HEAVY -> 28f; EType.FAST -> 14f; else -> 22f }
            val s = e.type.radius / 17f
            if (e.dying) {
                val a = 1f - e.dyingT / 0.7f
                c.saveLayerAlpha(e.x - 90f, e.y - 90f, e.x + 90f, e.y + 90f, (255 * a).toInt().coerceIn(0, 255))
                Sprites.soldier(c, e.x, e.y, e.angle + e.dyingT * 6f, 0f, s * (1f - e.dyingT * 0.4f), style, gl, false, false)
                c.restore()
            } else {
                if (e.type == EType.BOSS) Sprites.ring(c, e.x, e.y, e.r + 12f + sin(w.time * 6f) * 3f, 0x66FF1744.toInt(), 4f)
                Sprites.soldier(c, e.x, e.y, e.angle, e.walk, s, style, gl, e.shotFlash > 0f, e.hitFlash > 0f)
                if (e.hp < e.maxHp || e.type == EType.BOSS) {
                    val bw = e.r * 2.2f
                    val by = e.y - e.r - 14f
                    Sprites.rectP(c, e.x - bw / 2 - 1, by - 1, e.x + bw / 2 + 1, by + 5, 0xCC000000.toInt())
                    Sprites.rectP(c, e.x - bw / 2, by, e.x - bw / 2 + bw * (e.hp / e.maxHp).coerceIn(0f, 1f), by + 4, 0xFFFF3D3D.toInt())
                }
            }
        }

        val pl = w.player
        val wd = Weapons.get(pl.weaponId)
        if (pl.dead) {
            val a = (1f - pl.deadT / 1.3f).coerceIn(0f, 1f)
            c.saveLayerAlpha(pl.x - 70f, pl.y - 70f, pl.x + 70f, pl.y + 70f, (255 * a).toInt())
            Sprites.soldier(c, pl.x, pl.y, pl.angle + pl.deadT * 5f, 0f, 1f - pl.deadT * 0.3f, Sprites.PLAYER, wd.gunLen, false, false)
            c.restore()
        } else if (pl.invuln <= 0f || ((pl.invuln * 10f).toInt() % 2 == 0)) {
            Sprites.soldier(c, pl.x, pl.y, pl.angle, pl.walk, 1f, Sprites.PLAYER, wd.gunLen, pl.flashT > 0f, pl.hurtT > 0f)
            if (pl.reloadT > 0f) {
                val frac = 1f - pl.reloadT / wd.reloadTime
                Sprites.rectP(c, pl.x - 22, pl.y - 36, pl.x + 22, pl.y - 31, 0xAA000000.toInt())
                Sprites.rectP(c, pl.x - 21, pl.y - 35, pl.x - 21 + 42 * frac, pl.y - 32, 0xFFFFD54F.toInt())
            }
        }

        for (b in w.bullets) if (vis(b.x, b.y, 30f)) Sprites.bullet(c, b.x, b.y, b.vx, b.vy, b.color, b.size, b.fromPlayer)

        for (o in w.obstacles) {
            if (o.type != ObType.TREE || !vis(o.x, o.y, 100f)) continue
            Sprites.tree(c, o.x + o.w / 2, o.y + o.h / 2, o.variant)
        }

        for (q in w.particles) {
            if (!vis(q.x, q.y, 100f)) continue
            val t = (q.life / q.maxLife).coerceIn(0f, 1f)
            when (q.kind) {
                0 -> { p.style = Paint.Style.FILL; p.color = withAlpha(q.color, (255 * t).toInt()); c.drawCircle(q.x, q.y, q.size * (0.4f + 0.6f * t), p) }
                1 -> { p.style = Paint.Style.FILL; p.color = withAlpha(q.color, (130 * t).toInt()); c.drawCircle(q.x, q.y, q.size, p) }
                else -> { p.style = Paint.Style.STROKE; p.strokeWidth = 8f * t + 1f; p.color = withAlpha(q.color, (255 * t).toInt()); c.drawCircle(q.x, q.y, q.size, p) }
            }
        }

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.textSize = 18f
        p.isFakeBoldText = true
        for (t in w.texts) {
            if (!vis(t.x, t.y, 40f)) continue
            val a = (t.t / 0.8f).coerceIn(0f, 1f)
            p.color = withAlpha(0xFF000000.toInt(), (200 * a).toInt()); c.drawText(t.text, t.x + 1.5f, t.y + 1.5f, p)
            p.color = withAlpha(t.color, (255 * a).toInt()); c.drawText(t.text, t.x, t.y, p)
        }
        p.isFakeBoldText = false
        c.restore()

        if ((w.mission?.theme ?: 0) == 2) {
            p.style = Paint.Style.FILL
            p.color = 0x6609133A
            c.drawRect(0f, 0f, vw, vh, p)
        }
        if (vigW != vw) {
            vigW = vw
            vig.shader = RadialGradient(vw / 2f, vh / 2f, vw * 0.65f, 0x00000000, 0x77000000, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, vw, vh, vig)
        if (w.player.hurtT > 0f) {
            p.style = Paint.Style.FILL
            p.color = withAlpha(0xFFFF0000.toInt(), (90 * (w.player.hurtT / 0.2f)).toInt())
            c.drawRect(0f, 0f, vw, vh, p)
        }
    }

    fun release() { terrain.recycle() }

    private fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
}
