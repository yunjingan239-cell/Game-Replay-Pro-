# Game Replay Pro — Instant Replay Architecture & Implementation Plan

Game Replay Pro is a high-performance instant replay Android utility for mobile gamers, continuously buffering recent gameplay in rolling hardware-encoded chunks so players can capture game-winning moments with a single tap of an unobtrusive floating micro-dot without interrupting gameplay.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during the interactive clarification phase:
> - **Buffer Strategy**: Rolling segmented video chunks (e.g., 10s MP4 rolling segments in app cache), pruned circularly according to the selected buffer duration (30s, 60s, 120s) and merged asynchronously on demand via Android `MediaMuxer`/`MediaExtractor`.
> - **Audio Engine**: Internal system/game audio capture (via Android 10+ `AudioPlaybackCaptureConfiguration`) with toggleable microphone commentary overlay.
> - **Floating Controller**: Minimalist glowing micro-dot with drag-to-reposition physics that expands on touch for quick actions and single-tap instant save.

---

### 1. Overview & Core Concept

- **What It Does**: Runs a low-overhead Android Foreground Service that utilizes `MediaProjection` and hardware `MediaCodec` (H.264/AVC) to write rolling temporary video chunks into a bounded circular ring buffer. When the player triggers a replay (from the in-game floating micro-dot, notification action, or main dashboard), the system stitches the active buffer window into a permanent MP4 highlight and indexes it in a local Room database.
- **Target Audience / Persona**: Competitive and casual mobile gamers (MOBA, FPS, Battle Royale, rhythm games) who want to clip their clutch moments without suffering frame drops, battery drain, or screen clutter.
- **Key Value**: Zero gameplay disruption, guaranteed bounded memory/storage footprint (rolling chunk rotation), and instant 1-tap capture with rich local management (trim, rename, share, delete, preview).

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Onboarding & Permission Gateway**: Simple cyberpunk-styled permissions checklist (Screen Capture / MediaProjection, Overlay permission for Floating Dot, Notifications for Foreground Service, optional Mic).
2. **Dashboard (Flight Control)**:
   - High-tech HUD displaying real-time replay status (● BUFFERS ACTIVE / STANDBY), current buffer size (e.g., 60s / 6 chunks), estimated RAM/VRAM footprint, and encoder profile.
   - Master Arming Switch: One-tap toggle to start/stop the background replay service.
   - Big Action Trigger: Glowing "CLIP LAST 60S" instant trigger.
   - Game Profile selector (e.g., "Honor of Kings", "PUBG Mobile", "Genshin Impact", "General Game").
3. **In-Game Overlay (Minimalist Micro-Dot)**:
   - Floating translucent neon micro-dot (44dp target, draggable, edge-snapping).
   - Single tap: Triggers instant clipping with subtle haptic feedback and glowing pulse animation without opening an intrusive window.
   - Long press / Expand: Unfurls a mini radial / pill HUD (save 30s, save 60s, toggle mic, open app).
4. **Replay Library**:
   - Grid and list view of saved clips with video duration badge, resolution, file size, game tag, and timestamp.
   - In-app video preview player with play/pause, seek scrubber, rename dialog, native system sharing, and delete confirmation.
5. **Settings & Hardware Tuner**:
   - Buffer Duration slider: 30s, 60s, 120s, 180s.
   - Resolution presets: 720p (Performance), 1080p (Standard), Native (Ultra).
   - Frame Rate: 30 FPS vs 60 FPS.
   - Video Bitrate: 4 Mbps, 8 Mbps, 12 Mbps, 16 Mbps.
   - Floating button size, opacity, and auto-hide timeout settings.

#### Visual Identity & Theme
- **Aesthetic Direction**: Futuristic Esports / Cyberpunk Tech HUD (inspired by GeForce Experience, Steam Deck UI, and Razer Synapse).
- **Color Palette**:
  - Background Base: `#0B0E14` (Deep obsidian dark)
  - Surface Containers: `#141824` with `#1E2438` borders
  - Primary Accent: `#00E5FF` (Electric Cyan)
  - Secondary Accent: `#7C4DFF` (Cyber Purple)
  - Recording Pulse / Warning: `#FF1744` (Vibrant Crimson)
  - Text: High contrast `#F0F4F8` primary, `#8B9BB4` secondary
- **Typography**: Clean, geometric monospace-accented headers with modern sans-serif body typography for crisp readability on mobile displays.
- **Motion & Micro-interactions**: Pulsing status beacon for active recording, spring physics for floating dot movement, smooth bottom sheets for quick actions, and snackbar confirmation toasts with clip thumbnails.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Rolling Segmented Muxing vs. Continuous Single File Rewriting**
  - *Chosen Approach*: Record short 10-second MP4 chunks sequentially in a rotating ring queue (`tmp_chunk_0.mp4`, `tmp_chunk_1.mp4`, etc.). When buffer limit is reached, the oldest chunk is deleted and replaced.
  - *Why*: Eliminates the catastrophic risk of MP4 file header corruption if an app or game crashes mid-stream, minimizes I/O spikes, and allows instantaneous merging of the exact desired time window (e.g., last 3 chunks for 30s, last 6 chunks for 60s).
  - *Alternatives Considered*: Single circular file or in-memory raw frame buffer. Memory buffers would crash games due to high RAM overhead (OOM), while single active files risk total recording loss on game crashes.
- **Decision 2: Foreground Service Architecture with MediaProjection**
  - *Chosen Approach*: Persistent Foreground Service with `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` to ensure Android OS never kills the buffer engine during intense gaming sessions.
  - *Why*: Meets Android 14/15/16 background execution requirements and provides ongoing user notification controls.
- **Decision 3: Local Persistence via Room Database**
  - *Chosen Approach*: Room Database stores replay metadata (id, title, relativePath, absolutePath, durationMs, resolution, fileSize, gameTitle, timestamp, thumbnailUri).
  - *Why*: Device-local instant replay records should remain strictly on the device with zero cloud latency and no sign-in barriers, with immediate offline accessibility.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Game Replay Pro App                           │
│                                                                        │
│   ┌───────────────────────────┐    ┌───────────────────────────────┐   │
│   │       MainActivity        │    │    FloatingOverlayService     │   │
│   │  (Jetpack Compose UI)     │    │  (System WindowManager Dot)   │   │
│   │ - DashboardScreen         │    │  - Draggable Micro-dot        │   │
│   │ - ReplayLibraryScreen     │    │  - Single-Tap Save Signal     │   │
│   │ - SettingsScreen          │    │  - Quick Menu Expansion       │   │
│   └─────────────┬─────────────┘    └───────────────┬───────────────┘   │
│                 │                                  │                   │
│                 ▼                                  ▼                   │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                     ReplayViewModel                            │   │
│   │   StateFlow<ReplayUiState>, SettingsState, ClipList            │   │
│   └──────────────────────┬─────────────────────────────────────────┘   │
│                          │                                             │
│       Intent Commands    │             Data Flow                       │
│                          ▼                                             │
│   ┌────────────────────────────────┐    ┌──────────────────────────┐   │
│   │     ReplayRecorderService      │    │     ReplayRepository     │   │
│   │    (Foreground Service)        │    │   (Room Database + I/O)  │   │
│   │                                │    │                          │   │
│   │  ┌──────────────────────────┐  │    │  ┌────────────────────┐  │   │
│   │  │ MediaProjectionManager   │  │    │  │ ReplayDao (Room)   │  │   │
│   │  └────────────┬─────────────┘  │    │  └──────────┬─────────┘  │   │
│   │               ▼                │    │             │            │   │
│   │  ┌──────────────────────────┐  │    │             ▼            │   │
│   │  │ VirtualDisplay & Codec   │  │    │  ┌────────────────────┐  │   │
│   │  │ (MediaCodec Hardware)    │  │    │  │ ReplayClipEntity   │  │   │
│   │  └────────────┬─────────────┘  │    │  └────────────────────┘  │   │
│   │               ▼                │    └──────────────────────────┘   │
│   │  ┌──────────────────────────┐  │                                   │
│   │  │ CircularChunkManager     │──┼───────────────┐                   │
│   │  │ (Rolling 10s MP4 ring)   │  │               │                   │
│   │  └────────────┬─────────────┘  │               │ On Replay Save    │
│   │               │                │               ▼                   │
│   │               ▼                │     ┌──────────────────────┐      │
│   │  ┌──────────────────────────┐  │     │ ClipStitcher & Saver │      │
│   │  │ ChunkMerger / Exporter   │──┼────►│ (Saves final MP4 to  │      │
│   │  └────────────┬─────────────┘  │     │  Movies/GameReplays) │      │
│   └────────────────────────────────┘     └──────────────────────┘      │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Entities & Room Schema
- `ReplayClip`:
  - `id: Long` (Primary Key, auto-generate)
  - `title: String` (e.g., "Ace Clutch #01")
  - `filePath: String`
  - `durationSeconds: Int`
  - `fileSizeBytes: Long`
  - `resolution: String` (e.g. "1920x1080")
  - `fps: Int` (e.g. 60)
  - `gameName: String` (e.g. "Honor of Kings")
  - `createdAt: Long` (Epoch millis)
  - `isFavorite: Boolean`

#### Interactive Component & State Mapping
- **Replay Switch Component**:
  - Validates permissions (Overlay + MediaProjection).
  - Fires MediaProjection intent launcher.
  - Updates `isRecordingActive: Boolean`, starts foreground notification with stop/save actions.
- **Micro-Dot Component**:
  - Touch listener tracks drag gestures; snaps smoothly to screen edges.
  - Quick tap emits broadcast to `ReplayRecorderService.ACTION_SAVE_CLIP`.
  - Service triggers `ChunkMerger`, saves final MP4, notifies UI, inserts Room record.
- **Clip Player & Manager**:
  - Full playback dialog with seek bar, thumbnail generation, rename dialog, direct share intent (`ACTION_SEND`), and file deletion.
