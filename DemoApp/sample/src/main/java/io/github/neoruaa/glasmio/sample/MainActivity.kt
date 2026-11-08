package io.github.neoruaa.glasmio.sample

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.fonts.Font as NativeFont
import android.graphics.fonts.FontFamily as NativeFontFamily
import android.graphics.fonts.FontStyle as NativeFontStyle
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.*
import io.github.neoruaa.glasmio.glass.*
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.*
import io.github.neoruaa.glasmio.preference.*
import io.github.neoruaa.glasmio.scaffold.*
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationOperation
import io.github.neoruaa.glasmio.scaffold.navigation.GlasmioNavDisplay
import io.github.neoruaa.glasmio.scaffold.navigation.GlasmioNavHeaderMotion
import androidx.navigation3.runtime.NavEntry
import io.github.neoruaa.glasmio.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

private data class SampleAssets(
    val symbolTypeface: Typeface?,
    val typography: Typography,
    val wallpapers: SampleWallpapers,
)

/**
 * Sample wallpapers decode on demand. Startup only pays for the appearance it shows:
 * each 1206x2622 ARGB bitmap is ~12 MB, and decoding both on every cold start doubled
 * peak memory and the IO the first frame waits on. The counter-appearance decodes the
 * first time a page needs it and is then retained for the process lifetime.
 */
private class SampleWallpapers(private val context: Context, initialDark: Boolean) {
    private var light: ImageBitmap? = null
    private var dark: ImageBitmap? = null

    init {
        if (initialDark) dark = decode(R.drawable.ios27_wallpaper_dark)
        else light = decode(R.drawable.ios27_wallpaper_light)
    }

    @Synchronized
    fun of(isDark: Boolean): ImageBitmap = (if (isDark) dark else light) ?: run {
        val decoded = decode(if (isDark) R.drawable.ios27_wallpaper_dark else R.drawable.ios27_wallpaper_light)
        if (isDark) dark = decoded else light = decoded
        decoded
    }

    private fun decode(resource: Int): ImageBitmap =
        requireNotNull(BitmapFactory.decodeResource(context.resources, resource)).apply { prepareToDraw() }.asImageBitmap()
}

/** Process-owned immutable assets; no Activity, View, or Context is retained. */
private object SampleAssetCache {
    private var assets: SampleAssets? = null

    @Synchronized
    fun load(applicationContext: Context, dark: Boolean): SampleAssets {
        assets?.let { return it }
        val baseFont = try {
            NativeFont.Builder(applicationContext.assets, "glasmio_symbols.ttf").build()
        } catch (_: IOException) {
            null
        }
        val italicFont = if (baseFont == null) null else try {
            NativeFont.Builder(applicationContext.assets, "glasmio_italic.ttf").build()
        } catch (_: IOException) {
            null
        }
        return SampleAssets(
            symbolTypeface = baseFont?.let(::sampleTypeface),
            typography = baseFont?.let { sampleTypography(it, italicFont) } ?: glasmioTypography(),
            wallpapers = SampleWallpapers(applicationContext, dark),
        ).also { assets = it }
    }
}

private val LocalSampleAssets = staticCompositionLocalOf<SampleAssets> { error("Sample assets are not loaded") }

class MainActivity : ComponentActivity() {
    private var appDarkAppearance: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPreferences = DemoPreferences.load(this,
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        appDarkAppearance = initialPreferences.dark
        io.github.neoruaa.glasmio.glass.render.configureGlassDynamicRange(window, initialPreferences.hdrOutput)
        val systemBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { resources ->
            appDarkAppearance ?: ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        }
        enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        setContent {
            // Persistent host settings take precedence over an old saved-instance snapshot.
            var preferences by remember { mutableStateOf(initialPreferences) }
            fun updatePreferences(next: DemoPreferences) {
                if (next == preferences) return
                val outputChanged = next.hdrOutput != preferences.hdrOutput
                next.save(applicationContext)
                preferences = next
                if (outputChanged) recreate()
            }
            val dark = preferences.dark
            val reduceTransparency = preferences.reduceTransparency
            val context = LocalContext.current.applicationContext
            val fontResolver = LocalFontFamilyResolver.current
            val assets by produceState<SampleAssets?>(null, context, fontResolver) {
                value = withContext(Dispatchers.IO) {
                    val cachedAssets = SampleAssetCache.load(context, dark)
                    val typography = cachedAssets.typography
                    listOf(
                        typography.displayLarge, typography.displayMedium, typography.displaySmall,
                        typography.headlineLarge, typography.headlineMedium, typography.headlineSmall,
                        typography.titleLarge, typography.titleMedium, typography.titleSmall,
                        typography.bodyLarge, typography.bodyMedium, typography.bodySmall,
                        typography.labelLarge, typography.labelMedium, typography.labelSmall,
                    ).mapNotNull { it.fontFamily }.distinct().forEach { fontResolver.preload(it) }
                    cachedAssets
                }
            }
            val darkAppearance = dark
            SideEffect {
                appDarkAppearance = darkAppearance
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkAppearance
                    isAppearanceLightNavigationBars = !darkAppearance
                }
            }
            val loadedAssets = assets
            if (loadedAssets == null) {
                Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color.White))
            } else {
                GlasmioIconProvider(symbolTypeface = loadedAssets.symbolTypeface) {
                    GlasmioTheme(
                        isDark = dark,
                        typography = loadedAssets.typography,
                        appearance = GlassAppearance(reduceTransparency = reduceTransparency),
                    ) {
                        CompositionLocalProvider(
                            LocalSampleAssets provides loadedAssets,
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                MenusScreen(
                                    dark, { updatePreferences(preferences.copy(dark = it)) },
                                    reduceTransparency, { updatePreferences(preferences.copy(reduceTransparency = it)) },
                                    preferences.hdrEffects, { updatePreferences(preferences.copy(hdrEffects = it)) },
                                    preferences.fpsCounter, { updatePreferences(preferences.copy(fpsCounter = it)) },
                                )
                                if (preferences.fpsCounter) DemoFpsCounter(
                                    Modifier.align(Alignment.BottomStart)
                                        .navigationBarsPadding()
                                        .padding(start = 16.dp, bottom = 16.dp)
                                        .testTag("demo-fps-counter"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every variation retains the same native font buffer; the asset is read only once. */
private fun sampleTypeface(font: NativeFont): Typeface =
    Typeface.CustomFallbackBuilder(NativeFontFamily.Builder(font).build()).setStyle(font.style).build()

private class SampleFont(
    override val weight: FontWeight,
    val typeface: Typeface,
    override val style: FontStyle = FontStyle.Normal,
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = SampleFontLoader,
    variationSettings = FontVariation.Settings(),
)

private object SampleFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface = (font as SampleFont).typeface
    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface = loadBlocking(context, font)
}

/** Compose sees real weight entries; their Typefaces clone the shared native Font data. */
private fun sampleTypography(baseFont: NativeFont, italicFont: NativeFont?): Typography {
    val base = glasmioTypography()
    val families = mutableMapOf<Float, FontFamily>()
    val typefaces = mutableMapOf<Triple<Float, Float, FontStyle>, Typeface>()
    val weights = listOf(400 to 400f, 500 to 510f, 510 to 510f, 590 to 590f, 591 to 590.7999878f, 600 to 590f, 700 to 700f)
    val fontStyles = if (italicFont == null) listOf(FontStyle.Normal) else listOf(FontStyle.Normal, FontStyle.Italic)
    fun resolve(style: TextStyle): TextStyle {
        val opticalSize = style.fontSize.value.coerceIn(17f, 28f)
        val family = families.getOrPut(opticalSize) {
            FontFamily(weights.flatMap { (request, axis) ->
                fontStyles.map { fontStyle ->
                    // Named SF Pro Italic instances carry a YAXS value as well as weight.
                    val (weightAxis, italicYAxis) = if (fontStyle == FontStyle.Italic) {
                        when (request) {
                            500, 510 -> 508f to 436f
                            590, 591, 600 -> 590.800003f to 419.199997f
                            700 -> 700f to 430f
                            else -> axis to 400f
                        }
                    } else axis to 400f
                    val typeface = typefaces.getOrPut(Triple(weightAxis, opticalSize, fontStyle)) {
                        val source = if (fontStyle == FontStyle.Italic) requireNotNull(italicFont) else baseFont
                        val axes = "'wght' $weightAxis, 'opsz' $opticalSize" +
                            if (fontStyle == FontStyle.Italic) ", 'YAXS' $italicYAxis" else ""
                        val variation = NativeFont.Builder(source)
                            .setWeight(weightAxis.roundToInt())
                            .setSlant(if (fontStyle == FontStyle.Italic) NativeFontStyle.FONT_SLANT_ITALIC else NativeFontStyle.FONT_SLANT_UPRIGHT)
                            .setFontVariationSettings(axes)
                            .build()
                        sampleTypeface(variation)
                    }
                    SampleFont(FontWeight(request), typeface, fontStyle)
                }
            })
        }
        return style.copy(fontFamily = family)
    }
    return base.copy(
        displayLarge = resolve(base.displayLarge),
        displayMedium = resolve(base.displayMedium),
        displaySmall = resolve(base.displaySmall),
        headlineLarge = resolve(base.headlineLarge),
        headlineMedium = resolve(base.headlineMedium),
        headlineSmall = resolve(base.headlineSmall),
        titleLarge = resolve(base.titleLarge),
        titleMedium = resolve(base.titleMedium),
        titleSmall = resolve(base.titleSmall),
        bodyLarge = resolve(base.bodyLarge),
        bodyMedium = resolve(base.bodyMedium),
        bodySmall = resolve(base.bodySmall),
        labelLarge = resolve(base.labelLarge),
        labelMedium = resolve(base.labelMedium),
        labelSmall = resolve(base.labelSmall),
    )
}

/**
 * The whole app is the Menus demo: one static entry, no back stack, no sidebar, no
 * bottom bar. The header's merged action group keeps the appearance toggle and the
 * toolbar menu, and gains a settings entry that opens the host preference toggles.
 */
@Composable
private fun MenusScreen(
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    reduceTransparency: Boolean,
    onReduceTransparency: (Boolean) -> Unit,
    hdrEffects: Boolean,
    onHdrEffects: (Boolean) -> Unit,
    fpsCounter: Boolean,
    onFpsCounter: (Boolean) -> Unit,
) {
    val navigationBackdrop = rememberLayerBackdrop()
    val headerEntry = remember { SampleHeaderEntry() }
    // One static entry: the app IS the Menus page. There is no back stack to pop, and
    // the display still provides the entry isolation the menu anchors rely on.
    val backStack = remember { mutableStateListOf("Menus") }
    val headerMotion = remember {
        GlasmioNavHeaderMotion(backStack.last()) { _, _, pop ->
            if (pop) GlasmioNavigationOperation.Pop else GlasmioNavigationOperation.Push
        }
    }
    val currentDark by rememberUpdatedState(dark)
    val currentOnDark by rememberUpdatedState(onDark)
    val currentReduceTransparency by rememberUpdatedState(reduceTransparency)
    val currentOnReduceTransparency by rememberUpdatedState(onReduceTransparency)
    val currentHdrEffects by rememberUpdatedState(hdrEffects)
    val currentOnHdrEffects by rememberUpdatedState(onHdrEffects)
    val currentFpsCounter by rememberUpdatedState(fpsCounter)
    val currentOnFpsCounter by rememberUpdatedState(onFpsCounter)
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSampleHeaderEntry provides headerEntry) {
        GlasmioNavigationScaffold(
            motion = headerMotion,
            contentBackdrop = navigationBackdrop,
            header = {
                GlasmioHeaderSpec(
                    title = "Menus",
                    leading = emptyList(),
                    trailing = listOf(GlasmioHeaderGroup("primary", headerEntry.actions ?: emptyList())),
                    scrollState = headerEntry.scroll,
                )
            },
        ) { headerPadding ->
            CompositionLocalProvider(LocalSampleBodyPadding provides headerPadding) {
                Box(Modifier.fillMaxSize()) {
                    GlasmioNavDisplay(
                        motion = headerMotion,
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                        onBack = { /* single static entry: nothing to pop */ },
                        swipeBackEnabled = false,
                        entryProvider = { entryKey ->
                            NavEntry(key = entryKey, contentKey = entryKey) {
                                CompositionLocalProvider(LocalSampleHeaderEntry provides headerEntry) {
                                    GlasmioBackdropHost(
                                        sampledColor = LocalGlassColors.current.groupedBackground,
                                        modifier = Modifier.fillMaxSize(),
                                        overlayContent = { _ ->
                                            // The page records into its own layer, like a nav
                                            // entry would; reusing the host layer cycles the
                                            // render nodes when an anchor records its foreground.
                                            val pageBackdrop = rememberLayerBackdrop()
                                            CompositionLocalProvider(LocalBlurBackdrop provides pageBackdrop) {
                                                Box(Modifier.fillMaxSize().glassBackdropSource(pageBackdrop).trackBackdropPosition(pageBackdrop)) {
                                                    MenusDemo(
                                                        bottomPadding = bottomPadding,
                                                        dark = currentDark,
                                                        onDark = currentOnDark,
                                                        reduceTransparency = currentReduceTransparency,
                                                        onReduceTransparency = currentOnReduceTransparency,
                                                        hdrEffects = currentHdrEffects,
                                                        onHdrEffects = currentOnHdrEffects,
                                                        fpsCounter = currentFpsCounter,
                                                        onFpsCounter = currentOnFpsCounter,
                                                    )
                                                }
                                                GlasmioPageTitle(entryKey)
                                            }
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private data class SamplePageLayout(
    val title: String,
    val padding: PaddingValues,
    val listState: LazyListState,
    val collapseProgress: Float,
)

@Composable
private fun SamplePinnedPage(
    title: String,
    bottomPadding: Dp,
    content: @Composable (SamplePageLayout) -> Unit,
) {
    val state = rememberLazyListState()
    // The title collapses on a breakpoint; the top bar material keeps tracking the scroll offset.
    val animatedCollapse = rememberGlasmioHeaderCollapse(state)
    val animatedEdge = rememberGlasmioHeaderScrollEdge(state)
    val entry = LocalSampleHeaderEntry.current
    val currentCollapse = rememberUpdatedState(animatedCollapse)
    val scroll = remember {
        GlasmioHeaderScrollState({ currentCollapse.value })
    }
    SideEffect { entry.scroll = scroll }
    val hostPadding = LocalSampleBodyPadding.current
    val bodyPadding = PaddingValues(
        start = IosPageHorizontalPadding, top = hostPadding.calculateTopPadding(),
        end = IosPageHorizontalPadding, bottom = bottomPadding + 24.dp,
    )
    GlasmioPageBody(
        contentPadding = bodyPadding,
        topBarBlurProgress = animatedEdge,
    ) { padding ->
        content(SamplePageLayout(title, padding, state, animatedCollapse))
    }
}

@Composable
private fun SampleLargeTitle(title: String, collapseProgress: Float) {
    Text(
        title,
        style = IosTypography.largeTitle,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = 1f - collapseProgress
                scaleX = 1f - 0.04f * collapseProgress
                scaleY = scaleX
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .blur(6.dp * collapseProgress)
            .offset(y = (-10).dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun DemoList(layout: SamplePageLayout, content: LazyListScope.() -> Unit) {
    LazyColumn(
        state = layout.listState,
        modifier = Modifier.fillMaxSize().consumeWindowInsets(layout.padding),
        contentPadding = layout.padding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "sample-large-title") { SampleLargeTitle(layout.title, layout.collapseProgress) }
        content()
    }
}

@Composable
private fun MenusDemo(
    bottomPadding: Dp,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    reduceTransparency: Boolean,
    onReduceTransparency: (Boolean) -> Unit,
    hdrEffects: Boolean,
    onHdrEffects: (Boolean) -> Unit,
    fpsCounter: Boolean,
    onFpsCounter: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var settingsMenu by remember { mutableStateOf(false) }
    var motionSettings by remember { mutableStateOf(false) }
    var selection by rememberSaveable { mutableStateOf("Default") }
    var alert by rememberSaveable { mutableStateOf(false) }
    var inputAlert by rememberSaveable { mutableStateOf(false) }
    var nestedMenu by remember { mutableStateOf(false) }
    val nestedAnchor = rememberGlasmioMenuAnchorState()
    val toolbarAnchor = rememberGlasmioMenuAnchorState()
    val settingsAnchor = rememberGlasmioMenuAnchorState()
    val headerEntry = LocalSampleHeaderEntry.current
    val headerActions = remember(dark, toolbarAnchor, settingsAnchor) {
        listOf(
            GlasmioHeaderAction("appearance.toggle", if (dark) "sun.max" else "moon", if (dark) "Light appearance" else "Dark appearance", { onDark(!dark) }) {
                SfIcon(if (dark) "sun.max" else "moon", null)
            },
            GlasmioHeaderAction("settings", "gearshape", "Settings", { settingsMenu = true }, menuAnchor = settingsAnchor) {
                SfIcon("gearshape", null)
            },
            GlasmioHeaderAction("menu.options", "ellipsis", "Menu", { expanded = true }, menuAnchor = toolbarAnchor) { SfIcon("ellipsis", null) },
        )
    }
    SideEffect { headerEntry.actions = headerActions }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    SamplePinnedPage(
        title = "Menus",
        bottomPadding = bottomPadding,
    ) { layout ->
        DemoList(layout) {
            item {
                GlasmioGroupedList {
                    GlasmioListRow("Pop-up Button", showTopSeparator = false, trailing = {
                        GlasmioPopUpButton(selection.takeIf { it in listOf("Default", "Recent", "Name") } ?: "Default", listOf("Default", "Recent", "Name"), { selection = it }, { it })
                    })
                    GlasmioListRow("Alert", onClick = { alert = true })
                    GlasmioListRow("Input Alert", onClick = { inputAlert = true })
                }
            }
            item {
                GlasmioButton(onClick = { nestedMenu = true }, modifier = Modifier.glasmioMenuAnchor(nestedAnchor), size = GlasmioButtonSize.Medium) { Text("Cascading Menu") }
            }
        }
    }
    GlasmioMenu(
        expanded = expanded, onExpandedChange = { expanded = it }, itemCount = 4,
        menuAnchorState = toolbarAnchor, externalAnchorBounds = toolbarAnchor.bounds,
        anchor = {},
    ) { backdrop, close ->
        GlasmioMenuItem("Favorite", { selection = "Favorite"; close() }, systemName = "heart", backdrop = backdrop)
        GlasmioMenuItem("Duplicate", { selection = "Duplicate"; close() }, systemName = "plus.square.on.square", backdrop = backdrop)
        GlasmioMenuItem("Unavailable", {}, enabled = false, backdrop = backdrop)
        GlasmioMenuItem("Delete", { selection = "Delete"; close() }, destructive = true, systemName = "trash", backdrop = backdrop)
    }
    GlasmioMenu(
        expanded = settingsMenu, onExpandedChange = { settingsMenu = it }, itemCount = 4,
        menuAnchorState = settingsAnchor, externalAnchorBounds = settingsAnchor.bounds,
        anchor = {},
    ) { backdrop, close ->
        GlasmioMenuItem(
            if (fpsCounter) "FPS Overlay ✓" else "FPS Overlay",
            { onFpsCounter(!fpsCounter); close() },
            systemName = if (fpsCounter) "checkmark" else "circle",
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            if (hdrEffects) "HDR Output ✓" else "HDR Output",
            { onHdrEffects(!hdrEffects); close() },
            systemName = if (hdrEffects) "checkmark" else "circle",
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            if (reduceTransparency) "Reduce Transparency ✓" else "Reduce Transparency",
            { onReduceTransparency(!reduceTransparency); close() },
            systemName = if (reduceTransparency) "checkmark" else "circle",
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            "Motion Lab",
            { motionSettings = true; close() },
            systemName = "gearshape",
            backdrop = backdrop,
        )
    }
    if (motionSettings) {
        MotionSettingsPage(onClose = { motionSettings = false })
    }
    if (nestedMenu) {
        GlasmioCascadingMenu(
            anchorBounds = nestedAnchor.bounds,
            menuAnchorState = nestedAnchor,
            items = listOf(
                GlasmioCascadingMenuItem("Sort By", systemName = "arrow.up.arrow.down", children = listOf(
                    GlasmioCascadingMenuItem("Name", onClick = { selection = "Name" }),
                    GlasmioCascadingMenuItem("Recent", onClick = { selection = "Recent" }),
                )),
                GlasmioCascadingMenuItem("Favorite", systemName = "heart", onClick = { selection = "Favorite" }),
            ),
            title = "Options",
            expandedDescription = "Expanded",
            collapsedDescription = "Collapsed",
            onDismiss = { nestedMenu = false },
        )
    }
    GlasmioAlertDialog(
        visible = alert,
        onDismissRequest = { alert = false },
        title = "Save Changes?",
        message = "Your changes will appear in this sample.",
        buttons = listOf(
            GlasmioAlertButtonSpec("Cancel", { alert = false }, GlasmioAlertButtonRole.Cancel),
            GlasmioAlertButtonSpec("Save", { alert = false }),
        ),
    )
    GlasmioAlertDialog(
        visible = inputAlert,
        onDismissRequest = { inputAlert = false },
        title = "Name",
        buttons = listOf(
            GlasmioAlertButtonSpec("Cancel", { inputAlert = false }, GlasmioAlertButtonRole.Cancel),
            GlasmioAlertButtonSpec("Done", { inputAlert = false }, enabled = name.text.isNotBlank()),
        ),
    ) {
        GlasmioAlertFieldGroup { GlasmioAlertTextField(name, { name = it }, "Name") }
    }
}
