package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.Paint
import tj.soni.firegun.core.Game
import tj.soni.firegun.data.SaveManager
import tj.soni.firegun.data.SfxId
import tj.soni.firegun.data.Upgrade
import tj.soni.firegun.data.Weapons

class WeaponsScreen(g: Game) : ButtonScreen(g) {
    private var tab = 0
    private var msg = ""
    private var msgT = 0f
    private val rnd = java.util.Random()

    init { build() }

    private fun say(s: String) { msg = s; msgT = 2f }

    private fun build() {
        buttons.clear()
        addBack()
        val w = g.vw
        val h = g.vh
        val s = g.save
        val tw = 230f
        val ty = h - 74f
        val names = listOf("WEAPONS", "UPGRADES", "ITEMS")
        val icons = intArrayOf(3, 14, 13)
        for (i in 0 until 3) {
            buttons.add(Btn(w / 2f - 1.5f * tw - 20f + i * (tw + 20f), ty, tw, 56f, names[i], if (tab == i) BtnStyle.GOLD else BtnStyle.DARK, icons[i], 22f) { tab = i; build() })
        }
        when (tab) {
            0 -> {
                val gap = 14f
                val cw = (w - 56f - 4 * gap) / 5f
                for (wd in Weapons.all) {
                    val x = 28f + wd.id * (cw + gap)
                    val owned = s.owns(wd.id)
                    val equipped = s.selectedWeapon == wd.id
                    val label = if (equipped) "EQUIPPED" else if (owned) "EQUIP" else "BUY"
                    val style = if (equipped) BtnStyle.GRAY else if (owned) BtnStyle.BLUE else BtnStyle.GOLD
                    val b = Btn(x + 14f, 92f + CH - 70f, cw - 28f, 52f, label, style, 0, 22f) {
                        if (owned) { s.equip(wd.id); say("${wd.name} equipped") }
                        else if (s.buyWeapon(wd.id)) { g.sound.play(SfxId.PICKUP); say("${wd.name} purchased!") }
                        else say("Not enough coins")
                        build()
                    }
                    b.enabled = !equipped
                    buttons.add(b)
                }
            }
            1 -> {
                val cw = (w - 56f - 40f) / 3f
                for ((i, u) in Upgrade.values().withIndex()) {
                    val x = 28f + i * (cw + 20f)
                    val maxed = s.upgradeLevel(u) >= SaveManager.MAX_UPGRADE
                    val b = Btn(x + 30f, 92f + CH - 70f, cw - 60f, 52f, if (maxed) "MAX" else "UPGRADE ${s.upgradeCost(u)}", BtnStyle.GOLD, 0, 22f) {
                        if (s.buyUpgrade(u)) { g.sound.play(SfxId.PICKUP); say("${u.title} upgraded!") } else say("Not enough coins")
                        build()
                    }
                    b.enabled = !maxed
                    buttons.add(b)
                }
            }
            else -> {
                val cw = 400f
                val x1 = w / 2f - cw - 20f
                val x2 = w / 2f + 20f
                val full = s.grenades >= SaveManager.MAX_GRENADES
                val b1 = Btn(x1 + 40f, 92f + CH - 70f, cw - 80f, 52f, if (full) "FULL" else "BUY ${SaveManager.GRENADE_PACK_PRICE}", BtnStyle.GOLD, 0, 22f) {
                    if (s.buyGrenadePack()) { g.sound.play(SfxId.PICKUP); say("+3 grenades") } else say("Not enough coins")
                    build()
                }
                b1.enabled = !full
                buttons.add(b1)
                val b2 = Btn(x2 + 40f, 92f + CH - 70f, cw - 80f, 52f, "OPEN CHEST", BtnStyle.GREEN, 0, 22f) {
                    val r = s.openChest(rnd.nextInt(301))
                    if (r > 0) { g.sound.play(SfxId.PICKUP); say("Chest opened: +$r coins!") } else say("No chests. Complete missions!")
                    build()
                }
                b2.enabled = s.chests > 0
                buttons.add(b2)
            }
        }
    }

    override fun update(dt: Float) { if (msgT > 0f) msgT -= dt }

    override fun draw(c: Canvas, w: Float, h: Float) {
        header(c, w, "WEAPONS SHOP")
        val s = g.save
        when (tab) {
            0 -> {
                val gap = 14f
                val cw = (w - 56f - 4 * gap) / 5f
                for (wd in Weapons.all) {
                    val x = 28f + wd.id * (cw + gap)
                    val y = 92f
                    val equipped = s.selectedWeapon == wd.id
                    Ui.panel(c, x, y, cw, CH, 0xEE18222F.toInt(), if (equipped) Ui.GOLD else Ui.BORDER)
                    Ui.text(c, wd.name, x + cw / 2, y + 38f, 24f, Ui.WHITE, Paint.Align.CENTER)
                    c.drawRoundRect(x + 12, y + 54f, x + cw - 12, y + 150f, 10f, 10f, Ui.fillP(0xFF0F161F.toInt()))
                    Ui.weaponIcon(c, wd.id, x + cw / 2, y + 102f, 2.0f, 0xFFC9D1DB.toInt())
                    val dmg = wd.damage * (if (wd.pellets > 1) wd.pellets * 0.6f else 1f)
                    stat(c, x + 16f, y + 176f, cw - 32f, "DMG", dmg / 100f, 0xFFE53935.toInt())
                    stat(c, x + 16f, y + 214f, cw - 32f, "RATE", (1f / wd.fireDelay) / 14f, 0xFFFFB300.toInt())
                    stat(c, x + 16f, y + 252f, cw - 32f, "MAG", wd.magSize / 80f, 0xFF43A047.toInt())
                    stat(c, x + 16f, y + 290f, cw - 32f, "RELOAD", 1f - (wd.reloadTime - 0.8f) / 2.6f, 0xFF42A5F5.toInt())
                    if (s.owns(wd.id)) Ui.text(c, "OWNED", x + cw / 2, y + 354f, 22f, 0xFF7CE07C.toInt(), Paint.Align.CENTER)
                    else {
                        Ui.coin(c, x + cw / 2 - 40f, y + 347f, 12f)
                        Ui.text(c, wd.price.toString(), x + cw / 2 - 20f, y + 356f, 24f, Ui.GOLD)
                    }
                }
            }
            1 -> {
                val cw = (w - 56f - 40f) / 3f
                val info = listOf("+20 max health per level", "+20 max armor per level", "+10% weapon damage per level")
                for ((i, u) in Upgrade.values().withIndex()) {
                    val x = 28f + i * (cw + 20f)
                    Ui.panel(c, x, 92f, cw, CH, 0xEE18222F.toInt())
                    Ui.text(c, u.title.uppercase(), x + cw / 2, 136f, 30f, Ui.GOLD, Paint.Align.CENTER, 5f)
                    when (u) {
                        Upgrade.HEALTH -> Ui.heart(c, x + cw / 2, 205f, 38f)
                        Upgrade.ARMOR -> tj.soni.firegun.render.Sprites.shield(c, x + cw / 2, 205f, 38f, 0xFF42A5F5.toInt(), 0xFF0D47A1.toInt())
                        Upgrade.DAMAGE -> Ui.weaponIcon(c, 1, x + cw / 2, 205f, 1.6f, 0xFFC9D1DB.toInt())
                    }
                    val lv = s.upgradeLevel(u)
                    for (k in 0 until SaveManager.MAX_UPGRADE) {
                        val px = x + cw / 2 + (k - 2) * 40f
                        c.drawRoundRect(px - 15f, 262f, px + 15f, 282f, 5f, 5f, Ui.fillP(if (k < lv) Ui.GOLD else 0xFF2B3544.toInt()))
                    }
                    Ui.text(c, "Level $lv / ${SaveManager.MAX_UPGRADE}", x + cw / 2, 318f, 22f, Ui.WHITE, Paint.Align.CENTER)
                    Ui.text(c, info[i], x + cw / 2, 352f, 19f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
                }
            }
            else -> {
                val cw = 400f
                val x1 = w / 2f - cw - 20f
                val x2 = w / 2f + 20f
                Ui.panel(c, x1, 92f, cw, CH, 0xEE18222F.toInt())
                Ui.text(c, "GRENADE PACK", x1 + cw / 2, 136f, 30f, Ui.GOLD, Paint.Align.CENTER, 5f)
                Ui.grenadeIcon(c, x1 + cw / 2, 215f, 46f)
                Ui.text(c, "+3 grenades", x1 + cw / 2, 300f, 24f, Ui.WHITE, Paint.Align.CENTER)
                Ui.text(c, "You have ${s.grenades} / ${SaveManager.MAX_GRENADES}", x1 + cw / 2, 336f, 20f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
                Ui.panel(c, x2, 92f, cw, CH, 0xEE18222F.toInt())
                Ui.text(c, "SUPPLY CHEST", x2 + cw / 2, 136f, 30f, Ui.GOLD, Paint.Align.CENTER, 5f)
                Ui.icon(c, 12, x2 + cw / 2, 215f, 50f, Ui.GOLD)
                Ui.text(c, "100-400 coins inside", x2 + cw / 2, 300f, 24f, Ui.WHITE, Paint.Align.CENTER)
                Ui.text(c, "Chests: ${s.chests} (earned in missions)", x2 + cw / 2, 336f, 20f, 0xFFB7C3D6.toInt(), Paint.Align.CENTER)
            }
        }
        if (msgT > 0f) {
            Ui.panel(c, w / 2 - 230f, h - 128f, 460f, 42f, 0xEE0A101A.toInt(), Ui.GOLD, 21f)
            Ui.textC(c, msg, w / 2, h - 107f, 22f, Ui.WHITE)
        }
        drawButtons(c)
    }

    private fun stat(c: Canvas, x: Float, y: Float, w: Float, label: String, frac: Float, color: Int) {
        Ui.text(c, label, x, y, 16f, 0xFFB7C3D6.toInt())
        c.drawRoundRect(x, y + 6f, x + w, y + 18f, 6f, 6f, Ui.fillP(0xFF0D121A.toInt()))
        c.drawRoundRect(x, y + 6f, x + w * frac.coerceIn(0.06f, 1f), y + 18f, 6f, 6f, Ui.fillP(color))
    }

    companion object { private const val CH = 420f }
}
