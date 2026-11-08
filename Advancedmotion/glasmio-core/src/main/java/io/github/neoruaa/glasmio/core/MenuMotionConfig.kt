package io.github.neoruaa.glasmio.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * 单一事实来源：菜单液态动效、玻璃通透度与光效的所有可调参数都集中在这里。
 *
 * 引擎在每次弹簧 retarget / 材质解析时读取这些快照状态，因此高级设置页滑动时
 * 正在运行的菜单会立即生效，无需重组或重建。所有字段都是相对出厂校准的
 * 倍率或偏置；[reset] 一键恢复默认。[1f] 恒等于原生校准值。
 */
object MenuMotionConfig {
    /** 全局动画速度倍率。弹簧 response 与时间线延迟都会除以该值（>1 更快）。 */
    var speed: Float by mutableFloatStateOf(1f)

    /** 弹性：从液态弹簧的阻尼比中减去的量，越大回弹越明显。 */
    var bounciness: Float by mutableFloatStateOf(0f)

    /** 阻尼：加到液态弹簧阻尼比上的偏置，越大越「重」、越少振荡。 */
    var damping: Float by mutableFloatStateOf(0f)

    /** 液态形变强度：kick 冲量、metaball 融合半径与透镜高度的乘数。 */
    var morph: Float by mutableFloatStateOf(1f)

    /** 通透度：越大，亮度抬升（black floor）、白纱 faceFill 与容器 tint 越薄。 */
    var transparency: Float by mutableFloatStateOf(1f)

    /** 模糊强度：菜单 frost 半径与展开过渡模糊的乘数。 */
    var blurStrength: Float by mutableFloatStateOf(1f)

    /** 把基础阻尼比解析为实际使用的阻尼比。 */
    fun dampingFor(base: Float): Float =
        (base - bounciness + damping).coerceIn(0.15f, 1f)

    /**
     * 面材质减薄系数：transparency 越高越小（1 → 1，1.4 → 0.6，0.6 → 1.4）。
     * 乘到 black floor / faceFill / rollOff / 容器 tint alpha 上。
     */
    val clarityFactor: Float
        get() = (2f - transparency).coerceIn(0.25f, 1.75f)

    fun reset() {
        speed = 1f
        bounciness = 0f
        damping = 0f
        morph = 1f
        transparency = 1f
        blurStrength = 1f
    }
}
