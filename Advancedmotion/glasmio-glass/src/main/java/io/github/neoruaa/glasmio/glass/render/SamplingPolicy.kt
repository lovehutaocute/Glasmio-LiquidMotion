package io.github.neoruaa.glasmio.glass.render

/**
 * Process-wide backdrop sampling policy. Hosts under sustained load (thermal, low-end
 * devices, battery savers) can lower [globalScale] so every glass surface rasterizes its
 * backdrop at reduced resolution. The frost hides the resample; the effect passes and
 * their RenderEffect chains run on proportionally fewer pixels.
 *
 * Changes take effect on each surface's next draw. Draws happen whenever glass content
 * animates, so an idle screen keeps its last resolution until the next motion — which is
 * exactly when the sampling cost returns.
 */
object GlassSamplingPolicy {
    @Volatile
    var globalScale: Float = 1f
        set(value) {
            field = if (value.isFinite() && value > 0f) value.coerceIn(0.25f, 1f) else 1f
        }
}

/** Invalid policies retain full resolution; supported source textures span 1/4x through 1x. */
internal fun resolveBackdropSamplingScale(requestedScale: Float): Float {
    val global = GlassSamplingPolicy.globalScale
    val combined = if (global < 1f) requestedScale * global else requestedScale
    return if (combined.isFinite()) combined.coerceIn(0.25f, 1f) else 1f
}
