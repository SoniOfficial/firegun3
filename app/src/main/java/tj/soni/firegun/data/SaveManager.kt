package tj.soni.firegun.data

import android.content.Context

interface KeyValueStore {
    fun getInt(key: String, def: Int): Int
    fun putInt(key: String, v: Int)
    fun getString(key: String, def: String): String
    fun putString(key: String, v: String)
    fun clearAll()
}

class PrefsStore(context: Context) : KeyValueStore {
    private val p = context.getSharedPreferences("firegun_save", Context.MODE_PRIVATE)
    override fun getInt(key: String, def: Int) = p.getInt(key, def)
    override fun putInt(key: String, v: Int) { p.edit().putInt(key, v).apply() }
    override fun getString(key: String, def: String) = p.getString(key, def) ?: def
    override fun putString(key: String, v: String) { p.edit().putString(key, v).apply() }
    override fun clearAll() { p.edit().clear().apply() }
}

class MapStore : KeyValueStore {
    private val m = HashMap<String, Any>()
    override fun getInt(key: String, def: Int) = (m[key] as? Int) ?: def
    override fun putInt(key: String, v: Int) { m[key] = v }
    override fun getString(key: String, def: String) = (m[key] as? String) ?: def
    override fun putString(key: String, v: String) { m[key] = v }
    override fun clearAll() { m.clear() }
}

enum class Upgrade(val title: String, val key: String) { HEALTH("Health", "up_hp"), ARMOR("Armor", "up_ar"), DAMAGE("Damage", "up_dm") }

class SaveManager(private val s: KeyValueStore) {
    companion object {
        const val MAX_UPGRADE = 5
        const val GRENADE_PACK_PRICE = 150
        const val MAX_GRENADES = 9
    }

    var coins: Int
        get() = s.getInt("coins", 0)
        set(v) = s.putInt("coins", v.coerceAtLeast(0))
    var totalScore: Int
        get() = s.getInt("score", 0)
        set(v) = s.putInt("score", v)
    var bestSurvivalWave: Int
        get() = s.getInt("surv_wave", 0)
        set(v) = s.putInt("surv_wave", v)
    var bestSurvivalScore: Int
        get() = s.getInt("surv_score", 0)
        set(v) = s.putInt("surv_score", v)
    var unlockedMission: Int
        get() = s.getInt("unlocked", 1)
        set(v) = s.putInt("unlocked", v.coerceIn(1, Missions.all.size))
    var ownedMask: Int
        get() = s.getInt("owned", 1)
        set(v) = s.putInt("owned", v or 1)
    var selectedWeapon: Int
        get() = s.getInt("sel_weapon", 0)
        set(v) = s.putInt("sel_weapon", v)
    var grenades: Int
        get() = s.getInt("grenades", 3)
        set(v) = s.putInt("grenades", v.coerceIn(0, MAX_GRENADES))
    var chests: Int
        get() = s.getInt("chests", 0)
        set(v) = s.putInt("chests", v.coerceAtLeast(0))
    var crowns: Int
        get() = s.getInt("crowns", 0)
        set(v) = s.putInt("crowns", v)
    var conquered: Int
        get() = s.getInt("conquered", 0)
        set(v) = s.putInt("conquered", v)
    var soundOn: Boolean
        get() = s.getInt("sound", 1) == 1
        set(v) = s.putInt("sound", if (v) 1 else 0)
    var autoAim: Boolean
        get() = s.getInt("autoaim", 1) == 1
        set(v) = s.putInt("autoaim", if (v) 1 else 0)
    var shakeOn: Boolean
        get() = s.getInt("shake", 1) == 1
        set(v) = s.putInt("shake", if (v) 1 else 0)
    var showFps: Boolean
        get() = s.getInt("fps", 0) == 1
        set(v) = s.putInt("fps", if (v) 1 else 0)

    fun owns(weaponId: Int) = (ownedMask shr weaponId) and 1 == 1
    fun ownedWeapons(): List<Int> = Weapons.all.map { it.id }.filter { owns(it) }

    fun buyWeapon(id: Int): Boolean {
        val w = Weapons.get(id)
        if (owns(id) || coins < w.price) return false
        coins -= w.price
        ownedMask = ownedMask or (1 shl id)
        selectedWeapon = id
        return true
    }

    fun equip(id: Int): Boolean {
        if (!owns(id)) return false
        selectedWeapon = id
        return true
    }

    fun upgradeLevel(u: Upgrade) = s.getInt(u.key, 0)
    fun upgradeCost(u: Upgrade) = 300 * (upgradeLevel(u) + 1)

    fun buyUpgrade(u: Upgrade): Boolean {
        val lv = upgradeLevel(u)
        if (lv >= MAX_UPGRADE || coins < upgradeCost(u)) return false
        coins -= upgradeCost(u)
        s.putInt(u.key, lv + 1)
        return true
    }

    fun buyGrenadePack(): Boolean {
        if (grenades >= MAX_GRENADES || coins < GRENADE_PACK_PRICE) return false
        coins -= GRENADE_PACK_PRICE
        grenades += 3
        return true
    }

    /** Opens one chest, returns coin reward or 0 when none available. */
    fun openChest(roll: Int): Int {
        if (chests <= 0) return 0
        chests -= 1
        val reward = 100 + (roll.coerceIn(0, 300))
        coins += reward
        return reward
    }

    fun stars(missionId: Int) = s.getString("stars", "").split(",").getOrNull(missionId - 1)?.toIntOrNull() ?: 0

    fun completeMission(missionId: Int, stars: Int) {
        val arr = IntArray(Missions.all.size) { stars(it + 1) }
        arr[missionId - 1] = maxOf(arr[missionId - 1], stars)
        s.putString("stars", arr.joinToString(","))
        if (missionId >= unlockedMission) unlockedMission = missionId + 1
    }

    fun resetAll() {
        s.clearAll()
    }
}
