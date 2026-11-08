package io.github.neoruaa.glasmio.sample

import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.neoruaa.glasmio.glass.render.GlassSamplingPolicy

/**
 * Turns passive frame-timing reports into two load-shedding levers:
 *
 *  - [GlassSamplingPolicy.globalScale]: glass backdrops rasterize at reduced resolution.
 *  - the window's preferred refresh rate: the display (and every animation clock under
 *    it) runs at a lower mode, so a loaded device spends less energy per menu motion.
 *
 * Levels degrade fast (a few hundred milliseconds of sustained jank) and recover slowly
 * (a dwell of calm frames per step), which keeps flapping near the threshold at bay.
 * Reports arrive from the FrameMetrics callback thread; effects apply on the main thread.
 */
internal object GlassPerformanceGovernor {

    const val LEVEL_FULL = 0
    const val LEVEL_REDUCED = 1   // 0.75x sampling, refresh capped at 60 Hz
    const val LEVEL_MINIMAL = 2   // 0.5x sampling, refresh capped at 30 Hz

    var level by mutableIntStateOf(LEVEL_FULL)
        private set

    val samplingScale: Float
        get() = when (level) {
            LEVEL_REDUCED -> 0.75f
            LEVEL_MINIMAL -> 0.5f
            else -> 1f
        }

    private const val HEAVY_RATIO = 1.4            // frame budget overrun per level
    private const val DEGRADE_FRAMES = 24          // ~0.4 s of 60 Hz jank
    private const val RECOVER_FRAMES = 240         // ~4 s of calm frames per step down
    private const val IDLE_RECOVER_MS = 4_000L     // no frames at all also recovers

    private val mainHandler = Handler(Looper.getMainLooper())
    private var heavyRun = 0
    private var calmRun = 0
    private var lastFrameAt = 0L
    private var lastRefreshPeriod = 0L
    private var window: Window? = null
    private var appliedRate = 0f

    /** Called from the FrameMetrics listener thread for every rendered frame. */
    fun reportFrame(durationNanos: Long?, refreshPeriodNanos: Long) {
        val now = System.nanoTime()
        lastFrameAt = now
        lastRefreshPeriod = refreshPeriodNanos
        val heavy = durationNanos != null &&
            refreshPeriodNanos > 0 &&
            durationNanos > refreshPeriodNanos * HEAVY_RATIO
        var nextLevel = Int.MIN_VALUE
        synchronized(this) {
            if (heavy) {
                heavyRun++
                calmRun = 0
                if (heavyRun >= DEGRADE_FRAMES && level < LEVEL_MINIMAL) {
                    nextLevel = level + 1
                    heavyRun = 0
                    calmRun = 0
                }
            } else {
                calmRun++
                if (calmRun >= RECOVER_FRAMES && level > LEVEL_FULL) {
                    nextLevel = level - 1
                    calmRun = 0
                    heavyRun = 0
                }
            }
        }
        if (nextLevel != Int.MIN_VALUE) {
            val target = nextLevel
            mainHandler.post { applyLevel(target) }
        }
    }

    /** Arms idle recovery while the host is resumed. Call from a lifecycle-aware place. */
    fun attach(window: Window) {
        this.window = window
        mainHandler.postDelayed(idleCheck, IDLE_RECOVER_MS)
    }

    fun detach() {
        window = null
        mainHandler.removeCallbacks(idleCheck)
    }

    private val idleCheck = object : Runnable {
        override fun run() {
            val idleFor = System.nanoTime() - lastFrameAt
            if (lastFrameAt != 0L && idleFor > IDLE_RECOVER_MS * 1_000_000 && level > LEVEL_FULL) {
                applyLevel(level - 1)
            }
            mainHandler.postDelayed(this, IDLE_RECOVER_MS)
        }
    }

    private fun applyLevel(target: Int) {
        if (target == level) return
        level = target
        GlassSamplingPolicy.globalScale = samplingScale
        val host = window ?: return
        // Only step the display down from its current mode; leaving 0 restores the default.
        val capHz = when (target) {
            LEVEL_REDUCED -> 60f
            LEVEL_MINIMAL -> 30f
            else -> 0f
        }
        val rate = when {
            capHz == 0f -> 0f
            else -> lowestModeAtMost(host, capHz)
        }
        if (rate != appliedRate) {
            appliedRate = rate
            host.attributes = host.attributes.apply { preferredRefreshRate = if (rate > 0f) rate else 0f }
        }
    }

    /** Pick the slowest supported display mode at or under [capHz]; 0f when none fits. */
    private fun lowestModeAtMost(window: Window, capHz: Float): Float {
        val display = window.decorView.display ?: return capHz
        val modes = display.supportedModes
            .map { it.refreshRate }
            .filter { it.isFinite() && it in 1f..capHz + 0.01f }
        return modes.minOrNull() ?: capHz
    }
}
