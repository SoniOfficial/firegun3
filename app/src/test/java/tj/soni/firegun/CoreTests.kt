package tj.soni.firegun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tj.soni.firegun.core.MathX
import tj.soni.firegun.data.MapStore
import tj.soni.firegun.data.MissionType
import tj.soni.firegun.data.Missions
import tj.soni.firegun.data.SaveManager
import tj.soni.firegun.data.Upgrade
import tj.soni.firegun.data.Weapons

class WeaponsTest {
    @Test fun idsAreSequential() { Weapons.all.forEachIndexed { i, w -> assertEquals(i, w.id) } }
    @Test fun pistolIsFree() { assertEquals(0, Weapons.get(0).price) }
    @Test fun fiveWeaponsWithPositiveStats() {
        assertEquals(5, Weapons.all.size)
        for (w in Weapons.all) { assertTrue(w.damage > 0f); assertTrue(w.fireDelay > 0f); assertTrue(w.magSize > 0); assertTrue(w.reloadTime > 0f) }
    }
}

class MissionsTest {
    @Test fun tenMissionsInOrder() {
        assertEquals(10, Missions.all.size)
        Missions.all.forEachIndexed { i, m -> assertEquals(i + 1, m.id); assertTrue(m.target > 0); assertEquals(4, m.weights.size) }
    }
    @Test fun bossMissionsHaveBosses() {
        for (m in Missions.all) if (m.type == MissionType.BOSS) { assertTrue(m.bosses > 0); assertEquals(m.bosses, m.target) }
    }
}

class SaveManagerTest {
    @Test fun defaults() {
        val s = SaveManager(MapStore())
        assertEquals(0, s.coins)
        assertEquals(1, s.unlockedMission)
        assertTrue(s.owns(0))
        assertFalse(s.owns(1))
    }
    @Test fun buyWeaponNeedsCoins() {
        val s = SaveManager(MapStore())
        assertFalse(s.buyWeapon(1))
        s.coins = 1200
        assertTrue(s.buyWeapon(1))
        assertEquals(200, s.coins)
        assertTrue(s.owns(1))
        assertFalse(s.buyWeapon(1))
    }
    @Test fun missionProgress() {
        val s = SaveManager(MapStore())
        s.completeMission(1, 2)
        s.completeMission(1, 1)
        assertEquals(2, s.stars(1))
        assertEquals(2, s.unlockedMission)
        s.completeMission(10, 3)
        assertEquals(10, s.unlockedMission)
    }
    @Test fun upgradesCostAndCap() {
        val s = SaveManager(MapStore())
        s.coins = 100000
        repeat(SaveManager.MAX_UPGRADE) { assertTrue(s.buyUpgrade(Upgrade.HEALTH)) }
        assertFalse(s.buyUpgrade(Upgrade.HEALTH))
    }
    @Test fun chestsAndGrenades() {
        val s = SaveManager(MapStore())
        assertEquals(0, s.openChest(10))
        s.chests = 1
        assertEquals(110, s.openChest(10))
        assertEquals(110, s.coins)
        s.coins = 1000
        assertTrue(s.buyGrenadePack())
        assertEquals(6, s.grenades)
        s.buyGrenadePack()
        assertFalse(s.buyGrenadePack())
    }
    @Test fun resetClearsEverything() {
        val s = SaveManager(MapStore())
        s.coins = 500; s.completeMission(1, 3)
        s.resetAll()
        assertEquals(0, s.coins); assertEquals(1, s.unlockedMission)
    }
}

class MathXTest {
    @Test fun clampAndDist() {
        assertEquals(5f, MathX.clamp(9f, 0f, 5f), 0.001f)
        assertEquals(5f, MathX.dist(0f, 0f, 3f, 4f), 0.001f)
    }
    @Test fun circleRect() {
        assertTrue(MathX.circleHitsRect(5f, 5f, 3f, 0f, 0f, 10f, 10f))
        assertTrue(MathX.circleHitsRect(12f, 5f, 3f, 0f, 0f, 10f, 10f))
        assertFalse(MathX.circleHitsRect(20f, 5f, 3f, 0f, 0f, 10f, 10f))
    }
}

class KingdomDataTest {
    @Test fun buildingCostsAndHp() {
        for (b in tj.soni.firegun.kingdom.BType.values()) { assertTrue(b.hp > 0f); assertTrue(b.w > 0f) }
        assertTrue(tj.soni.firegun.kingdom.BType.HOUSE.cost < tj.soni.firegun.kingdom.BType.STABLE.cost)
    }
    @Test fun castlesInsideWorld() {
        assertEquals(4, tj.soni.firegun.kingdom.KingdomWorld.CASTLE_POS.size)
        for (c in tj.soni.firegun.kingdom.KingdomWorld.CASTLE_POS) {
            assertTrue(c[0] > 300f && c[0] < tj.soni.firegun.kingdom.KingdomWorld.W - 300f)
            assertTrue(c[1] > 300f && c[1] < tj.soni.firegun.kingdom.KingdomWorld.H - 300f)
        }
    }
    @Test fun unitTypesHaveStats() {
        for (u in tj.soni.firegun.kingdom.UType.values()) { assertTrue(u.hp > 0f); assertTrue(u.speed > 0f); assertTrue(u.cd > 0f) }
        assertTrue(tj.soni.firegun.kingdom.UType.ARCHER.ranged)
    }
    @Test fun crownsPersist() {
        val s = SaveManager(MapStore())
        s.crowns = 2; s.conquered = 5
        assertEquals(2, s.crowns); assertEquals(5, s.conquered)
    }
}
