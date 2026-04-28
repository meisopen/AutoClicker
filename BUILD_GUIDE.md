# 连点器 Android 应用 - 构建说明

## 方法一：使用 Android Studio（本机构建）

### 前置要求
- 安装 [Android Studio](https://developer.android.com/studio)（含 JDK 17）
- 安装 Android SDK（API 34）

### 步骤
1. 打开 Android Studio → `File → Open` → 选择本目录 `AutoClicker/`
2. 等待 Gradle 同步完成（首次需下载依赖，约 5-10 分钟）
3. 菜单：`Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. APK 生成路径：`app/build/outputs/apk/debug/app-debug.apk`
5. 将 APK 传至手机安装即可

---

## 方法二：使用在线构建（推荐，无需安装任何工具）

### 使用 GitHub Actions 一键构建

1. 注册/登录 [GitHub](https://github.com)
2. 新建仓库，将 `AutoClicker/` 目录内容推送上去
3. 仓库中已包含 `.github/workflows/build.yml`（见下方）
4. 推送后，Actions 自动构建，几分钟后在 `Actions → Artifacts` 下载 APK

---

## 方法三：使用 Appetize / Replit（最快预览）

- 上传源码至 [replit.com](https://replit.com) 的 Android 模板
- 或使用 [Buildozer在线服务](https://buildozer.io)

---

## 安装到手机

1. 手机开启「允许安装未知来源应用」
   - 设置 → 安全 → 未知来源（或在文件管理器中直接点击 APK）
2. 传送 APK 到手机（USB / 微信 / QQ / 蓝牙均可）
3. 点击安装

---

## 首次使用配置

1. 打开「连点器」App
2. 点击「授权」按钮 → 允许悬浮窗权限
3. 点击「启用」按钮 → 进入无障碍设置 → 找到「连点器」→ 开启
4. 返回 App，填写点击坐标（可在开发者选项中开启「指针位置」查看坐标）
5. 调整速度（1-100 次/秒）和触点大小
6. 点击「启动悬浮窗」→ 切换到目标应用
7. 点击悬浮的 ▶ 按钮开始连点，点击 ■ 停止
