package tj.soni.firegun.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import tj.soni.firegun.R
import tj.soni.firegun.data.SfxId

class SoundManager(context: Context) {
    var enabled = true
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = HashMap<SfxId, Int>()
    private val lastPlay = HashMap<SfxId, Long>()

    init {
        val map = mapOf(
            SfxId.PISTOL to R.raw.sfx_pistol, SfxId.RIFLE to R.raw.sfx_rifle,
            SfxId.SHOTGUN to R.raw.sfx_shotgun, SfxId.SNIPER to R.raw.sfx_sniper,
            SfxId.MG to R.raw.sfx_mg, SfxId.EXPLOSION to R.raw.sfx_explosion,
            SfxId.RELOAD to R.raw.sfx_reload, SfxId.HIT to R.raw.sfx_hit,
            SfxId.CLICK to R.raw.sfx_click, SfxId.EMPTY to R.raw.sfx_empty,
            SfxId.PICKUP to R.raw.sfx_pickup, SfxId.VICTORY to R.raw.sfx_victory,
            SfxId.GAMEOVER to R.raw.sfx_gameover
        )
        for ((k, v) in map) ids[k] = pool.load(context, v, 1)
    }

    fun play(id: SfxId, volume: Float = 1f) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - (lastPlay[id] ?: 0L) < 35L) return
        lastPlay[id] = now
        val sid = ids[id] ?: return
        pool.play(sid, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()
}
