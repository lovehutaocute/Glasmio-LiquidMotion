package io.github.neoruaa.glasmio.sample

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.neoruaa.glasmio.core.IosTypography
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Passive FPS readout fixed to the host's bottom-left corner.
 *
 * The old counter recomputed its whole statistic from a freshly copied + sorted 2400-slot
 * ring every second and reported a single activity-window total, so the label jumped by
 * whole frames, stalled at 0 while the UI idled, and allocated on every tick. This rewrite:
 *
 *  - keeps one fixed ring of (timestamp, duration) pairs written on the callback thread
 *    with zero steady-state allocation;
 *  - recomputes on a 500 ms cadence over a true sliding 1 s window, so the value tracks
 *    motion within two ticks instead of lagging a full second;
 *  - only publishes when the displayed integers change, keeping recomposition at rest
 *    while the UI itself is idle;
 *  - feeds every normalized frame duration to [GlassPerformanceGovernor], which drives
 *    the adaptive sampling scale and refresh cap.
 */
@Composable
internal fun DemoFpsCounter(modifier: Modifier = Modifier) {
    val window = LocalActivity.current?.window ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var fps by remember(window) { mutableIntStateOf(0) }
    var lowFps by remember(window) { mutableIntStateOf(-1) }

    DisposableEffect(window, lifecycle) {
        val mainHandler = Handler(Looper.getMainLooper())
        var stopMonitoring: (() -> Unit)? = null

        fun startMonitoring() {
            if (stopMonitoring != null) return
            fps = 0
            lowFps = -1
            val active = AtomicBoolean(true)
            val callbackThread = HandlerThread("Glasmio FPS").apply { start() }
            val callbackHandler = Handler(callbackThread.looper)
            val display = window.decorView.display
            fun refreshPeriod(): Long {
                val rate = display?.refreshRate?.takeIf { it.isFinite() && it > 0f } ?: 60f
                return (1_000_000_000.0 / rate).roundToLong()
            }

            val capacity = 512
            val timestamps = LongArray(capacity)
            val durations = LongArray(capacity)
            var head = 0      // next write slot
            var count = 0     // live samples, at most `capacity`
            val listener = Window.OnFrameMetricsAvailableListener { _, metrics, droppedReports ->
                if (!active.get()) return@OnFrameMetricsAvailableListener
                val duration = activeFrameDurationNanos(
                    totalDuration = metrics.getMetric(FrameMetrics.TOTAL_DURATION),
                    deadline = metrics.getMetric(FrameMetrics.DEADLINE),
                    refreshPeriod = refreshPeriod(),
                    firstDraw = metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L,
                ) ?: return@OnFrameMetricsAvailableListener
                timestamps[head] = System.nanoTime()
                durations[head] = duration
                head = (head + 1) % capacity
                if (count < capacity) count++
                GlassPerformanceGovernor.reportFrame(duration, refreshPeriod())
            }
            val stats = FrameStats()
            var lastPublishedFps = -1
            var lastPublishedLow = -1
            val update = object : Runnable {
                override fun run() {
                    if (!active.get()) return
                    val now = System.nanoTime()
                    stats.compute(timestamps, durations, count, head, now)
                    val measuredFps = stats.fps
                    val measuredLow = stats.onePercentLow
                    // Publish only on change: an idle UI keeps both the text and the
                    // recomposition pipeline at rest.
                    if (measuredFps != lastPublishedFps || measuredLow != lastPublishedLow) {
                        val publishFps = measuredFps
                        val publishLow = measuredLow
                        mainHandler.post {
                            if (active.get()) {
                                fps = publishFps
                                lowFps = publishLow
                            }
                        }
                        lastPublishedFps = measuredFps
                        lastPublishedLow = measuredLow
                    }
                    callbackHandler.postDelayed(this, 500L)
                }
            }
            window.addOnFrameMetricsAvailableListener(listener, callbackHandler)
            callbackHandler.postDelayed(update, 500L)
            GlassPerformanceGovernor.attach(window)
            stopMonitoring = {
                active.set(false)
                callbackHandler.removeCallbacks(update)
                mainHandler.removeCallbacksAndMessages(null)
                window.removeOnFrameMetricsAvailableListener(listener)
                GlassPerformanceGovernor.detach()
                callbackThread.quitSafely()
            }
        }

        fun stop() {
            stopMonitoring?.invoke()
            stopMonitoring = null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startMonitoring()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) startMonitoring()
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }

    Text(
        text = "FPS $fps · 1% Low ${if (lowFps >= 0) lowFps.toString() else "—"}",
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.White,
        style = IosTypography.caption1.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
    )
}

/** Sliding-window aggregates reused across updates; [compute] allocates nothing. */
private class FrameStats {
    var fps = 0
        private set
    var onePercentLow = -1
        private set

    private val windowDurations = LongArray(600)

    fun compute(timestamps: LongArray, durations: LongArray, count: Int, head: Int, nowNanos: Long) {
        // Frames rendered within the last second define the current rate.
        val cutoffFps = nowNanos - 1_000_000_000L
        var frames = 0
        for (index in 0 until count) {
            val slot = (head - 1 - index + timestamps.size * 2) % timestamps.size
            if (timestamps[slot] < cutoffFps) break
            frames++
        }
        fps = frames.coerceAtMost(999)

        // One-percent-low over the last five seconds of real frame durations.
        val cutoffLow = nowNanos - 5_000_000_000L
        var samples = 0
        for (index in 0 until count) {
            val slot = (head - 1 - index + timestamps.size * 2) % timestamps.size
            if (timestamps[slot] < cutoffLow) break
            if (samples < windowDurations.size) {
                windowDurations[samples] = durations[slot]
                samples++
            }
        }
        onePercentLow = if (samples >= 100) onePercentLow(windowDurations, samples) else -1
    }
}

/** Normalize pipeline budget to one refresh interval, retaining all deadline overrun. */
internal fun activeFrameDurationNanos(
    totalDuration: Long,
    deadline: Long,
    refreshPeriod: Long,
    firstDraw: Boolean,
): Long? {
    if (firstDraw || totalDuration <= 0L || deadline <= 0L || refreshPeriod <= 0L) return null
    // Vsync arrival gaps include intentional idle time. TOTAL_DURATION includes UI delay
    // and GPU completion; DEADLINE accounts for the system's overlapping pipeline budget.
    return refreshPeriod + (totalDuration - deadline).coerceAtLeast(0L)
}

/** Reciprocal mean of the slowest one percent of [durations] within [count] samples. */
internal fun onePercentLowFps(durations: LongArray): Int = onePercentLow(durations, durations.size)

private fun onePercentLow(durations: LongArray, count: Int): Int {
    if (count < 100) return -1
    // Sort a copy so the reusable window buffer keeps its ring order.
    val sorted = durations.copyOf(count)
    sorted.sort()
    val slowCount = (count + 99) / 100
    var slowTotal = 0.0
    for (index in count - slowCount until count) slowTotal += sorted[index]
    return (1_000_000_000.0 * slowCount / slowTotal).roundToInt()
}
