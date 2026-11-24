package com.nexora.autoclicker

import android.content.Context

object MultiButtonStore {
    private const val FILE = "nexora_multi_button"
    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<TargetConfig> {
        val p = prefs(c)
        val count = p.getInt("count", 0).coerceIn(0, 40)
        return (1..count).map { id ->
            TargetConfig(
                id = id,
                x = p.getFloat("x$id", .42f + ((id - 1) % 4) * .08f),
                y = p.getFloat("y$id", .48f + ((id - 1) / 4) * .07f),
                startDelay = p.getLong("start$id", 100L),
                endDelay = p.getLong("end$id", 25L),
                duration = p.getLong("duration$id", 200L),
                enabled = p.getBoolean("enabled$id", true)
            )
        }.toMutableList()
    }

    fun save(c: Context, items: List<TargetConfig>) {
        val p = prefs(c).edit().putInt("count", items.size)
        items.forEach { t ->
            p.putFloat("x${t.id}", t.x).putFloat("y${t.id}", t.y)
                .putLong("start${t.id}", t.startDelay)
                .putLong("end${t.id}", t.endDelay)
                .putLong("duration${t.id}", t.duration)
                .putBoolean("enabled${t.id}", t.enabled)
        }
        p.apply()
    }
}
