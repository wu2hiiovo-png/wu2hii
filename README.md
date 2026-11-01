# CleanX

CleanX 是一个中文 Android 存储清理工具。

## 功能
- 使用系统 Storage Access Framework 由用户选择可访问的目录。
- 扫描临时文件、日志、备份文件、疑似缓存文件和空文件。
- 用户逐项勾选并二次确认后删除。
- 展示内部存储已用/可用空间。
- 提供系统缓存管理入口。

## 安全边界
Android 11+ 的分区存储限制意味着普通应用不能任意访问其他应用私有缓存、Android/data 或 Android/obb。CleanX 不申请“所有文件访问”权限，不读取其他应用私有数据，不自动删除文件。扫描结果只是候选项，删除前请核对路径和文件名。

## 构建
GitHub Actions 自动构建 debug APK。APK 同时作为 Actions artifact 上传，并发布到 GitHub Release。

- Actions: https://github.com/wu2hiiovo-png/wu2hii/actions
- Releases: https://github.com/wu2hiiovo-png/wu2hii/releases
