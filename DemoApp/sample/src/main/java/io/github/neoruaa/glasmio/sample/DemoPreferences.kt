package io.github.neoruaa.glasmio.sample

import android.content.Context

/** Host settings survive a fresh process; component examples keep their own local state. */
internal data class DemoPreferences(
    val dark: Boolean,
    val reduceTransparency: Boolean = false,
    val hdrEffects: Boolean = false,
    val fpsCounter: Boolean = true,
) {
    val hdrOutput: Boolean get() = hdrEffects && !reduceTransparency

    fun save(context: Context) {
        // apply() publishes to the in-memory store synchronously, so the recreate() that
        // follows an output-mode change still reads the new values, while the disk write
        // stays off the main thread instead of blocking the activity restart.
        context.getSharedPreferences(StoreName, Context.MODE_PRIVATE).edit()
            .putBoolean("dark", dark)
            .putBoolean("reduceTransparency", reduceTransparency)
            .putBoolean("hdrEffects", hdrEffects)
            .putBoolean("fpsCounter", fpsCounter)
            .apply()
    }

    companion object {
        private const val StoreName = "demo-settings"

        fun load(context: Context, systemDark: Boolean): DemoPreferences {
            val values = context.getSharedPreferences(StoreName, Context.MODE_PRIVATE)
            return DemoPreferences(
                dark = values.getBoolean("dark", systemDark),
                reduceTransparency = values.getBoolean("reduceTransparency", false),
                hdrEffects = values.getBoolean("hdrEffects", false),
                fpsCounter = values.getBoolean("fpsCounter", true),
            )
        }
    }
}
