# Glasmio Liquid Motion

基于 [NEORUAA/Glasmio](https://github.com/NEORUAA/Glasmio)（Apache-2.0）整理的液态玻璃菜单动效工程，拆分为两部分：

```
仓库根目录/
├── Advancedmotion/   # 仅液态动效相关代码（4 个库模块，可独立编译），用于移植到其他 App
└── DemoApp/          # 完整演示 Android App 源代码（全部 7 个模块 + sample）
```

| 目录 | 内容 | 构建产物 |
|---|---|---|
| `Advancedmotion/` | `glasmio-core`（含 `MenuMotionConfig` 参数中心）、`glasmio-icons`、`glasmio-glass`（液态形变弹簧引擎）、`glasmio-ui`（`GlasmioMenu` 等菜单组件） | 库模块 AAR |
| `DemoApp/` | 上表全部模块 + `glasmio-scaffold` + `glasmio-preference` + `sample`（Menus 演示页、Motion Lab 调参页） | 演示 APK |

两个目录的 4 个共享模块内容完全一致；`DemoApp` 是功能事实来源，`Advancedmotion` 是它的动效依赖闭包快照。

## 构建方法

要求：Android Studio（AGP 9.4.1 / Kotlin 2.4.21 / JDK 21），或命令行：

```bash
# 构建演示 App（输出 DemoApp/sample/build/outputs/apk/）
cd DemoApp
./gradlew :sample:assembleDebug

# 单独编译动效库
cd Advancedmotion
./gradlew build
```

`Advancedmotion` 的 `settings.gradle.kts` 只 include 四个动效模块，不依赖 `sample` / `scaffold` / `preference`，可整体拷贝进任何 Gradle 工程复用。

## 液态动效接入

见 [`Advancedmotion/README.md`](Advancedmotion/README.md)。

## 许可

Apache License 2.0（继承上游 Glasmio）。
