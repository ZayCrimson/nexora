package com.nexora.autoclicker

import android.content.Context

object TargetStore {
    private const val FILE = "nexora_preferences"
    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<TargetConfig> {
        val p = prefs(c)
        val starts = listOf(100L, 100L, 100L, 150L)
        val durations = listOf(200L, 100L, 900L, 800L)
        return (1..4).map { id ->
            TargetConfig(
                id, p.getFloat("x$id", .38f + (id - 1) * .08f),
                p.getFloat("y$id", .55f),
                p.getLong("start$id", starts[id - 1]),
                p.getLong("end$id", 25L),
                p.getLong("duration$id", durations[id - 1]),
                p.getBoolean("enabled$id", true)
            )
        }.toMutableList()
    }

    fun save(c: Context, targets: List<TargetConfig>) {
        val e = prefs(c).edit()
        targets.forEach { t ->
            e.putFloat("x${t.id}", t.x).putFloat("y${t.id}", t.y)
                .putLong("start${t.id}", t.startDelay)
                .putLong("end${t.id}", t.endDelay)
                .putLong("duration${t.id}", t.duration)
                .putBoolean("enabled${t.id}", t.enabled)
        }
        e.apply()
    }
}
