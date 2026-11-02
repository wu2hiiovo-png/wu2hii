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


## 3.6.0 新增工具
- SecureRandom 密码生成器（可选字符集和长度）
- URL 编码/解码、JSON 格式化/压缩与校验
- 文本 HEX / 二进制互转、摩斯电码转换
- 图片 JPEG 压缩与缩放（SAF 选择输入和输出，原图不覆盖）
- 文件 SHA-256 校验

以上功能在设备本地处理；图片输出使用系统文件创建器。


## 3.7.0 壁纸自定义
- 新增个性化 / 壁纸设置入口，可通过 Android 系统图片选择器设置本地图片。
- 使用 Storage Access Framework 持久化读取授权，壁纸 URI 保存在应用私有偏好设置中，不上传图片。
- 支持恢复默认背景；图片按屏幕容器比例居中裁剪，不拉伸。


## 3.8.0 外观设置
- 首页右上角统一使用「设置」入口，不再单独占用首页个性化分类。
- 图标颜色与透明度、文字颜色与透明度可分别调整，并保存在本机。
- 设置页可直接更换或恢复默认壁纸。


## 3.9.0 视频链接工具
- 新增 Instagram、TikTok、X 平台选择和链接域名识别。
- 打开页面时尝试读取剪贴板；支持手动粘贴和重新读取。
- 当前通过第三方 Cobalt 网页解析服务继续下载，需在网页内粘贴链接；并非内置原生解析引擎。
- 不要求提供社交平台账号密码；仅用于公开且有权保存的内容。


## 4.0.0 网络工具
- 新增网络诊断页面：本机网络连接状态、网络类型、IP 与系统 DNS 服务器。
- 新增 DNS 域名解析查询。
- 新增 Cloudflare HTTPS 连通性与往返耗时测试。
- 新增约 5 MB 的 Cloudflare 下载测速；仅用户主动点击时运行，并显示数据量和平均 Mbps。
- 不新增备忘录功能；网络测试不会在后台自动运行。
