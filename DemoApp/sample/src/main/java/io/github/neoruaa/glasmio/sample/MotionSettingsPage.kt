package io.github.neoruaa.glasmio.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.core.MenuMotionConfig
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlasmioMenuAnchorState
import io.github.neoruaa.glasmio.glass.glasmioMenuAnchor
import io.github.neoruaa.glasmio.glass.rememberGlasmioMenuAnchorState
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioSlider

/**
 * 动效高级选项页面。每个滑杆直接写 [MenuMotionConfig] 的快照状态：
 * 引擎在 retarget / 材质解析时读取，滑杆拖动立即影响正在打开或收起的菜单，
 * 「预览菜单」按钮提供就地实测，「恢复默认」一键回到出厂校准。
 */
@Composable
fun MotionSettingsPage(onClose: () -> Unit) {
    val colors = LocalGlassColors.current
    var previewOpen by remember { mutableStateOf(false) }
    val previewAnchor = rememberGlasmioMenuAnchorState()
    // Reading the config fields here subscribes this page to every change,
    // so the value captions and the preview menu items stay live.
    val speed = MenuMotionConfig.speed
    val bounciness = MenuMotionConfig.bounciness
    val damping = MenuMotionConfig.damping
    val morph = MenuMotionConfig.morph
    val transparency = MenuMotionConfig.transparency
    val blurStrength = MenuMotionConfig.blurStrength

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlasmioButton(onClick = onClose, size = GlasmioButtonSize.Medium) { Text("‹ 返回") }
            Spacer(Modifier.width(12.dp))
            Text("动效高级选项", style = IosTypography.title3, fontWeight = FontWeight.Bold, color = colors.content)
        }
        Text(
            "所有参数实时生效；恢复默认回到出厂校准。透明度与模糊仅作用于菜单玻璃。",
            style = IosTypography.footnote,
            color = colors.secondaryContent,
        )
        GlasmioButton(
            onClick = { MenuMotionConfig.reset() },
            size = GlasmioButtonSize.Medium,
        ) { Text("恢复默认") }

        MotionSliderRow("动画速度", speed, 0.5f..2f, "%.2fx") { MenuMotionConfig.speed = it }
        MotionSliderRow("弹性", bounciness, 0f..0.5f, "%.2f") { MenuMotionConfig.bounciness = it }
        MotionSliderRow("阻尼", damping, 0f..0.5f, "%.2f") { MenuMotionConfig.damping = it }
        MotionSliderRow("液态形变", morph, 0f..1.6f, "%.2f") { MenuMotionConfig.morph = it }
        MotionSliderRow("透明度", transparency, 0.6f..1.4f, "%.2f") { MenuMotionConfig.transparency = it }
        MotionSliderRow("模糊强度", blurStrength, 0f..2f, "%.2f") { MenuMotionConfig.blurStrength = it }

        Text("实时预览", style = IosTypography.headline, color = colors.content)
        GlasmioButton(
            onClick = { previewOpen = true },
            modifier = Modifier.glasmioMenuAnchor(previewAnchor),
            size = GlasmioButtonSize.Medium,
        ) { Text("打开预览菜单") }
        Spacer(Modifier.padding(bottom = 24.dp))
    }

    GlasmioMenu(
        expanded = previewOpen,
        onExpandedChange = { previewOpen = it },
        itemCount = 3,
        menuAnchorState = previewAnchor,
        externalAnchorBounds = previewAnchor.bounds,
        anchor = {},
    ) { backdrop, close ->
        GlasmioMenuItem(
            "速度 ${"%.2f".format(MenuMotionConfig.speed)} · 形变 ${"%.2f".format(MenuMotionConfig.morph)}",
            { close() },
            systemName = "heart",
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            "弹性 ${"%.2f".format(MenuMotionConfig.bounciness)} · 阻尼 ${"%.2f".format(MenuMotionConfig.damping)}",
            { close() },
            systemName = "plus.square.on.square",
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            "透明 ${"%.2f".format(MenuMotionConfig.transparency)} · 模糊 ${"%.2f".format(MenuMotionConfig.blurStrength)}",
            { close() },
            systemName = "trash",
            backdrop = backdrop,
        )
    }
}

@Composable
private fun MotionSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    onChange: (Float) -> Unit,
) {
    val colors = LocalGlassColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = IosTypography.body, color = colors.content)
            Spacer(Modifier.weight(1f))
            Text(format.format(value), style = IosTypography.subheadline, color = colors.secondaryContent, fontSize = 13.sp)
        }
        GlasmioSlider(value = value, onValueChange = onChange, valueRange = range)
    }
}
