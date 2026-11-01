# CleanX 2.0

中文 Android 存储清理工具，提供普通 SAF 模式和 Magisk Root 增强模式。

## 功能
- 普通模式：用户通过系统 Storage Access Framework 选择目录，扫描临时文件、日志、备份和疑似缓存。
- Root 增强模式：在 Magisk 明确授权后，扫描以下已知缓存目录中的普通文件：
  - /data/user/0/*/cache
  - /data/user_de/0/*/cache
  - /storage/emulated/0/Android/data/*/cache
- 文件逐项显示名称、路径、大小和来源；默认不勾选。
- 仅手动勾选并二次确认后删除。
- 显示内部存储容量，并提供系统存储管理入口。

## 安全边界
- Root 模式只对路径位于上述白名单 cache 目录中的普通文件执行删除。
- 不扫描或删除数据库、shared_prefs、账号数据、应用 APK、系统分区、Android/obb 或未知目录。
- 不递归删除整个缓存目录，只删除用户勾选的单个文件。
- Magisk 未授权或设备无 su 时，Root 模式不可用。
- 清理缓存可能使应用下次启动重新生成缓存、重新下载资源或短暂变慢。删除前请检查路径。

## Android 存储限制
普通模式受 Scoped Storage 和 SAF 用户授权约束。Android 11+ 禁止 SAF 选择 Android/data、Android/obb 等部分目录；Root 模式用于在用户设备已 Root 且明确授权时访问有限的缓存目录。

## 构建
GitHub Actions 自动构建 debug APK，并上传 Actions artifact 和 GitHub Release。

- Actions: https://github.com/wu2hiiovo-png/wu2hii/actions
- Releases: https://github.com/wu2hiiovo-png/wu2hii/releases
