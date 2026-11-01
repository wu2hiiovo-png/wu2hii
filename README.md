# CleanX 2.1.0

中文 Android 存储清理工具，提供普通 SAF 模式和 Magisk Root 增强模式，并采用 Prismal 液态玻璃视觉材质。

## UI / Motion
- Prismal v1.0.8 (MIT) supplies OpenGL ES liquid-glass refraction, blur, rim lighting and spring interaction primitives.
- CleanX uses a single liquid-glass hero panel to keep GPU cost controlled.
- Buttons use press compression and spring release; content uses staggered entrance; scan results fade/slide in; selected rows receive a subtle scale and tint response.
- The visual system uses dark blue gradients, translucent surfaces and high-contrast text.

## Features
- 普通模式：用户通过系统 Storage Access Framework 选择目录，扫描临时文件、日志、备份和疑似缓存。
- Root 增强模式：在 Magisk 明确授权后，扫描指定应用 cache 目录中的普通文件。
- 文件逐项显示名称、路径、大小和来源；默认不勾选。
- 仅手动勾选并二次确认后删除。
- 显示内部存储容量，并提供系统存储管理入口。

## Safety
- Root mode only considers ordinary files under allowlisted cache directories.
- It does not scan or delete databases, shared preferences, account data, APKs, system partitions, Android/obb or unknown directories.
- It never recursively removes an entire cache directory.
- Cache deletion can cause apps to recreate or redownload temporary resources.

## Open-source attribution
- Prismal by styropyr0, MIT License, v1.0.8: https://github.com/styropyr0/Prismal
- Integrated as a binary dependency: `com.github.styropyr0:Prismal:v1.0.8`
- See upstream LICENSE for full terms.

## Build
GitHub Actions builds a debug APK, uploads an Actions artifact, and publishes it to GitHub Releases.

- Actions: https://github.com/wu2hiiovo-png/wu2hii/actions
- Releases: https://github.com/wu2hiiovo-png/wu2hii/releases

## Toolbox 3.4.0 visual update
- Haptic feedback is tied to successful button presses / interactive card activation, not arbitrary screen contact or scrolling.
- Dashboard, tool detail pages and CleanX use staggered entrance animations and press/release motion.
- The app uses one fixed, cool-toned Rei Ayanami mecha-style wallpaper. It is center-cropped to the screen container without stretching, cached locally after download, and remains stationary while foreground UI elements animate.
- The wallpaper is remotely loaded from its source page and is not bundled in the APK; the underlying artwork remains copyrighted by its respective owner:
  - https://hdqwalls.com/wallpaper/2160x3840/rei-ayanami-neon-genesis-evangelion-5k
- Scroll edge bounce and vertical slide motion are disabled/reduced to avoid dizziness. Page transitions keep the wallpaper layer stationary and animate only foreground content.
