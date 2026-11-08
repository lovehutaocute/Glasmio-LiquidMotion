# Advancedmotion — 液态菜单动效移植包

从 Glasmio 提取的液态玻璃菜单（liquid glass menu）完整动效依赖闭包。四个 Android 库模块可独立编译，整体拷入其他 Compose App 即可复用。

## 模块与依赖关系

```
glasmio-ui ──> glasmio-glass ──> glasmio-core
                    │                ▲
                    └──> glasmio-icons
```

| 模块 | 液态动效核心文件 |
|---|---|
| `glasmio-core` | `MenuMotionConfig.kt` — **全部动效参数的单一事实来源**（速度/弹性/阻尼/形变/通透度/模糊） |
| `glasmio-glass` | `LiquidMenuMorphMotion.kt` — 二阶弹簧精确解算的可打断形变时间轴；`LiquidMenuMorphSurface.kt` — AGSL 液态形变渲染面 |
| `glasmio-ui` | `IosMenus.kt` — `GlasmioMenu` / `GlasmioMenuItem` 组件与收起重开 tap router |
| `glasmio-icons` | SF Symbols 图标渲染（菜单项图标依赖） |

## 接入方式

1. 把本目录下的 4 个 `glasmio-*` 模块目录拷入目标工程根。
2. 目标工程 `settings.gradle.kts` 增加：

```kotlin
include(":glasmio-core", ":glasmio-icons", ":glasmio-glass", ":glasmio-ui")
```

3. App 模块依赖：

```kotlin
implementation(project(":glasmio-ui"))
```

4. 要求：`minSdk 31`，Compose + JDK 21。AGSL 形变着色器需 API 33+，API 31/32 自动回退高斯磨砂。

## 使用示例

```kotlin
// 1. 建锚点状态
val anchor = rememberGlasmioMenuAnchorState()

// 2. 触发视图挂锚点
Button(
    onClick = { expanded = true },
    modifier = Modifier.glasmioMenuAnchor(anchor),
) { Text("Open") }

// 3. 弹出液态菜单
GlasmioMenu(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    itemCount = 2,                       // 必须等于实际菜单项数，否则底部项被裁剪
    menuAnchorState = anchor,
    externalAnchorBounds = anchor.bounds,
    anchor = {},
) { backdrop, close ->
    GlasmioMenuItem("Favorite", { close() }, systemName = "heart", backdrop = backdrop)
    GlasmioMenuItem("Delete", { close() }, systemName = "trash", backdrop = backdrop, destructive = true)
}
```

## 动效参数（MenuMotionConfig）

所有参数是 Compose 快照状态：引擎在每次弹簧 retarget 与材质解析时读取，**任何线程任何时刻修改都对正在播放的动画立即生效**，无需重建。

| 参数 | 默认 | 范围建议 | 作用 |
|---|---|---|---|
| `speed` | 1f | 0.5–2 | 全局速度：弹簧 response 与时间线延迟都除以它 |
| `bounciness` | 0f | 0–0.5 | 从液态弹簧阻尼比中减去，越大回弹越明显 |
| `damping` | 0f | 0–0.5 | 加到阻尼比上，越大越「重」、越少振荡 |
| `morph` | 1f | 0–1.6 | 液态形变强度：kick 冲量、metaball 融合半径、透镜高度乘数 |
| `transparency` | 1f | 0.6–1.4 | 通透度：经 `clarityFactor` 缩放亮度抬升/白纱/容器 tint |
| `blurStrength` | 1f | 0–2 | 磨砂半径与展开过渡模糊乘数 |

`MenuMotionConfig.reset()` 一键恢复出厂校准；`dampingFor(base)` / `clarityFactor` 是引擎使用的派生函数。

## 实时调参页参考

`DemoApp/sample/src/.../MotionSettingsPage.kt` 是一个完整的调参页实现（六个滑杆 + 预览菜单 + 恢复默认），可直接拷贝进目标 App 改造。

## 与 DemoApp 的同步

`DemoApp/` 是功能事实来源；本目录是它的 4 模块依赖闭包快照（`settings.gradle.kts` 与根构建配置不同，其余源码一致）。修改动效请先改 `DemoApp`，再同步到本目录。
