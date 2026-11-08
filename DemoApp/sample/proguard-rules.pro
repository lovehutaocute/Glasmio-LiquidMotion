# Glasmio Demo release rules.
#
# Log calls are stripped rather than shipped: every removed call also removes its
# format string, and draw-path logging is the one place where a debug leftover
# would cost both code size and frame time.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
