# Game Replay Pro (游戏高光回录大师)

> **专为移动端电竞玩家打造的低功耗、零掉帧、后台无感“精彩时刻即时回录”助手。**  
> 媲美 PC 端 **NVIDIA ShadowPlay (GeForce Experience)** 与 **Xbox Game Bar** 的核心回录体验。

---

## 📖 项目简介 (Project Overview)

**Game Replay Pro** 不是传统的手动开始/停止的录屏软件，而是一套**智能滑动循环视频回录系统**。

在游戏对局中，玩家无需提前手动开启录屏占用几十吉字节存储空间。应用在后台持续维护一个极小开销的轻量滑动缓存环（如前 30 秒至 2 分钟）。当玩家在游戏中完成“极限五杀”、“绝地反杀”或“精彩进球”后，只需：
1. **轻触半透明电竞悬浮球**，或
2. **下拉通知栏点击控制中心「游戏回录」快捷开关**，或
3. **按下设定的音量快捷组合键**

应用立即将**刚刚发生的前 N 秒游戏精彩画面与系统原生音效**无损导出并保存至本地视频库中，整个回录过程完全静默进行，不跳出弹窗、不卡顿游戏画面、不打断激烈团战。

---

## ✨ 核心特性 (Key Features)

### 1. 🔄 智能滑动时间循环缓冲 (Circular Buffer Engine)
- **零存储压力**：采用自研环形队列（Ring Queue）与 10 秒微切片（Chunk）机制，仅在内存与专用临时缓存区保留设定时长的最近画面，自动轮转覆盖过期片段。
- **动态配额守护**：内置智能存储防护网（Smart Storage Guard），防止磁盘碎片膨胀。

### 2. ⚡ 极速一键保存与无损拼流 (Instant Clip Muxing)
- **瞬时抓取**：轻触悬浮球即可将环形缓冲内的全部切片在后台极速抽取并进行精确时间戳（PTS）无损拼流，直接生成标准的 MP4 高清视频。
- **微震感反馈**：触发瞬间提供短促线性触觉反馈（Haptic Vibration），无需切屏即可确认保存完成。

### 3. 📲 手机控制中心快捷磁贴 (Quick Settings Tile)
- **系统级集成**：支持将「游戏回录」添加到 Android 顶栏下拉控制中心（Quick Settings Tile）。
- **Android 14+ 免打扰穿透**：底层接入 `MediaProjectionConfig.createConfigForDefaultDisplay()`，绕过系统单应用截取弹窗，一键直达全屏沉浸录制。
- **双向联动**：磁贴图标实时指示回录状态（空闲待机 / 录制中），再次点击即可安全停止回录与悬浮窗。

### 4. 🎧 高保真原生音频采集 (Esports Audio Engine)
- **内录游戏原声（Android 10+）**：通过 `AudioPlaybackCaptureConfiguration` 捕获游戏内部 BGM、打击声效和开黑语音，杜绝环境风噪与杂音。
- **解说麦克风混音支持**：支持麦克风旁白采集，适配电竞解说与游戏直播复盘。
- **音频流防饥饿守护**：集成自研 PCM 时钟补偿与保底合成机制，在游戏切图或静音期间依然平滑推流，彻底杜绝视频无声、音画不同步问题。

### 5. 🎯 沉浸悬浮球与智能避让 (Floating Action Ball)
- **微型低侵入**：支持 36dp/48dp/60dp 尺寸微调与 30%~100% 透明度调节。
- **自动幽灵避让**：闲置 3 秒后悬浮球自动变浅微化，绝不遮挡游戏技能按键与视野。
- **全屏拖拽磁吸**：自由拖动停靠屏幕边缘，支持长按快速调出功能快捷菜单。

### 6. 🎮 游戏模式与自动启动 (Game Mode & Auto Detection)
- **已装游戏感知**：自动识别《王者荣耀》《和平精英》《原神》《英雄联盟手游》等主流手游。
- **一键优化启动**：在游戏模式中提供沉浸免打扰开关与优先级调度，保证高画质与低延迟。

---

## 🛠 技术架构 (Architecture & Tech Stack)

本应用遵循 **Modern Android Architecture** 与 **Clean Architecture** 规范开发，全模块采用 Kotlin 与声明式 Jetpack Compose：

```
┌──────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                │
│   DashboardScreen  |  ReplayLibraryScreen  |  SettingsScreen │
│            VideoPlayerDialog  |  FloatingOverlay             │
└──────────────────────────────┬───────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼───────────────────────────────┐
│              ViewModel Layer (AAC ViewModel)                 │
│                      ReplayViewModel                         │
└──────────────────────────────┬───────────────────────────────┘
                               │ Coroutines / Repository Pattern
┌──────────────────────────────▼───────────────────────────────┐
│                      Domain / Service Layer                  │
│   ReplayRecorderService (Foreground)                         │
│   FloatingOverlayService (Window Manager)                    │
│   ReplayTileService (Quick Settings Tile)                    │
└──────────────────────────────┬───────────────────────────────┘
                               │ Hardware Pipelines
┌──────────────────────────────▼───────────────────────────────┐
│                      Core Media Engine                       │
│   ScreenRecordingEngine (MediaProjection + MediaCodec)       │
│   AudioRecordingEngine (AudioPlaybackCapture + AAC Codec)    │
│   CircularBufferManager (Sliding Chunk Ring Buffer)          │
│   ReplayMuxer (Multi-track MP4 Extraction & Demuxing)        │
└──────────────────────────────┬───────────────────────────────┘
                               │ Local Persistence
┌──────────────────────────────▼───────────────────────────────┐
│               Data Layer (Room + DataStore)                  │
│       AppDatabase  |  ReplayDao  |  ReplaySettings           │
└──────────────────────────────────────────────────────────────┘
```

- **开发语言**：100% Kotlin
- **UI 框架**：Jetpack Compose + Material 3 (M3)
- **底层录屏**：Android MediaProjection API + VirtualDisplay
- **硬件编解码**：MediaCodec（HEVC / H.265 硬件编码优化，支持 AVC / H.264 兼容模式）
- **底层音频**：AudioRecord + AudioPlaybackCapture API + AAC (Advanced Audio Coding)
- **视频复用**：MediaMuxer + MediaExtractor
- **数据库**：Android Jetpack Room Database
- **后台常驻**：Android Foreground Service（支持 Android 14 API 34+ 类型声明）

---

## 📂 项目工程目录 (Project Structure)

```
app/src/main/java/com/example/
├── MainActivity.kt                      # 应用主入口，权限调度与音量快捷键监听
├── GameReplayApplication.kt             # 全局 Application，初始化通道与数据库依赖
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room 数据库配置
│   │   ├── ReplayClip.kt                # 回录视频实体模型 (Entity)
│   │   └── ReplayDao.kt                 # 视频记录增删查改 DAO
│   ├── model/
│   │   └── ReplaySettings.kt            # 回录参数配置 (码率/帧率/分辨率/时长/音频)
│   └── repository/
│       └── ReplayRepository.kt          # 数据持久化与高光切片保存仓库
├── engine/
│   ├── AudioRecordingEngine.kt          # 高性能系统原声/麦克风 AAC 采集编码引擎
│   ├── CircularBufferManager.kt         # 滑动循环切片缓冲队列管理器
│   ├── FallbackVideoGenerator.kt        # 离线环境与测试保底高光发生器
│   ├── GameDetectionManager.kt          # 本地安装游戏嗅探与快速启动管理器
│   ├── ReplayMuxer.kt                   # 极速多切片音视频无损提取与合成复用器
│   └── ScreenRecordingEngine.kt         # 硬件级 MediaCodec 录屏流处理核心
├── service/
│   ├── FloatingOverlayService.kt        # 悬浮电竞闪电球服务与触控手势处理
│   ├── ReplayRecorderService.kt         # 后台前台录制服务与切片轮转调度
│   ├── ReplayTilePromptActivity.kt      # 控制中心磁贴免弹窗透明授权中转器
│   └── ReplayTileService.kt             # 手机下拉控制中心快捷磁贴 (QS Tile)
├── ui/
│   ├── components/
│   │   └── CommonCyberComponents.kt     # 电竞科技风卡片、标签、按钮通用组件
│   ├── screens/
│   │   ├── DashboardScreen.kt           # 控制台大厅 (状态指示、启停开关、硬件仪表盘)
│   │   ├── GameModeScreen.kt            # 游戏模式专属面板 (支持一键快速启动游戏)
│   │   ├── ReplayLibraryScreen.kt       # 精彩高光库 (浏览、多选批量管理、系统分享)
│   │   ├── SettingsScreen.kt            # 详细录制参数设置 (画质、音频、悬浮球、存储)
│   │   └── VideoPlayerDialog.kt         # 原生高清硬件回放弹窗 (进度拖拽、循环播放)
│   └── theme/
│       ├── Color.kt                     # 暗夜深黑、电竞蓝、赛博紫科技配色
│       ├── Theme.kt                     # Material 3 动态科技主题配置
│       └── Type.kt                      # 电竞数字化排版字体样式
└── util/
    ├── MediaExportHelper.kt             # 系统媒体相册共享与导出工具
    ├── ScreenDimensionHelper.kt         # 屏幕物理分辨率与横竖屏自适应计算
    └── VideoThumbnailHelper.kt          # 高清视频缩略图高效抽取缓存
```

---

## ⚡ 性能优化与游戏保帧设计 (Performance Engineering)

为了保障游戏在 60fps/90fps/120fps 满帧运行，本应用在架构设计上执行严格的性能准则：

1. **GPU 硬件编码优先**：利用 Android `MediaCodec` 直接从 Surface 编码，不经过 CPU 像素拷贝（Zero Memory Copy）。
2. **线程隔离与核心让渡**：录屏引擎独立在 `HandlerThread` 中运行，并设定 `THREAD_PRIORITY_BACKGROUND`，将 CPU 高性能大核心全部留给游戏主线程。
3. **音视频 PTS 时钟解耦**：音频流与视频流在切片生成阶段各自按微秒时间轴递增，杜绝跨时钟源导致的空转重试与画面卡顿。
4. **VBR 动态能耗调节**：静止画面自动降低瞬时码率，综合能耗相比全量持续录屏降低高达 45%。
5. **智能自适应分辨率**：根据屏幕当前横竖屏动态匹配物理分辨率，不拉伸、不变形、无黑边。

---

## 🔒 权限使用声明 (Permissions)

| 权限名称 | 用途说明 |
|:---|:---|
| `RECORD_AUDIO` | 用于通过系统接口捕获游戏原生音频（或麦克风语音录制） |
| `FOREGROUND_SERVICE` | 保持游戏对局中后台回录循环引擎不被系统意外杀掉 |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Android 14+ 规范：执行屏幕捕获时合法维持前台服务 |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14+ 规范：录制中访问音频时合法维持前台服务 |
| `SYSTEM_ALERT_WINDOW` | 用于在游戏画面上层显示即时保存高光的悬浮闪电球 |
| `POST_NOTIFICATIONS` | Android 13+ 用于在状态栏常驻显示录制引擎状态通知 |
| `VIBRATE` | 触按悬浮球成功保存瞬间提供手感明确的微震动提示 |

---

## 🚀 快速上手与使用步骤 (User Guide)

1. **首次授权**：打开应用，授予通知、悬浮窗以及音频权限。
2. **开启回录**：
   - 方式 A：在应用首页点击「游戏回录」开关，授权全屏录制。
   - 方式 B：下拉手机通知中心，点击「游戏回录」快捷开关直接启动。
3. **进入对局**：打开《王者荣耀》或任意游戏，屏幕边缘会出现微型悬浮球。
4. **截取精彩高光**：在游戏出现击杀或精彩操作后，**轻点一下悬浮球**，屏幕将弹出轻量提示，上一段对局瞬间已安全保存。
5. **回放与分享**：切回应用「视频库」，即可直接全屏硬件播放精彩剪辑，或一键分享到社交平台。
