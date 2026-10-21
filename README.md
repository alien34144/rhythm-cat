# 节奏猫 · 我的音游

一款可以**导入自己下载的歌**、**配自己的背景图（含 GIF 动图）**的休闲音游。

整个游戏只有一个 `index.html`，没有构建步骤、没有框架、没有外部依赖。

## 三种打开方式

### 1. 网页版（最快）

打开 GitHub Pages 地址，加到手机主屏幕，就是一个全屏 App。

### 2. 安卓 APK

每次推送到 `main`，GitHub Actions 会自动编译出 APK：

> 仓库页 → Actions → 最新一次 Build APK → 页面底部 Artifacts → 下载 `rhythm-cat-apk`

也可以在 Release 里找。装之前需要在手机上允许「安装未知来源应用」。

APK 安装后离线可用，不需要网络。

### 3. 本地跑

```bash
python -m http.server 8000
```

然后手机连同一个 WiFi，浏览器打开 `http://<你电脑的IP>:8000`。

## 玩法

1. 选一个 mp3（或点「先用演示曲试试手感」）
2. 选一张背景图 —— GIF 会真的动
3. 自动识别 BPM，不准就点「敲击测速」跟着节奏敲几下
4. 选难度，开始

四指按屏幕下方的四条轨道，或者电脑上按 `D` `F` `J` `K`。

## ⚠️ 关于音乐格式

网易云 `.ncm`、QQ音乐 `.qmc0/.mflac`、酷狗 `.kgm`、酷我 `.kwm` 都是**加密格式**，
任何播放器都读不了。先用 ncmdump / um-cnv 这类工具转成 `.mp3` 再导入。

游戏里选到加密文件会直接提示你。

## 目录结构

```
index.html                完整游戏（唯一需要改的文件）
manifest.json             PWA 清单
sw.js                     离线缓存
icon.svg / icon-maskable.svg
android/                  极薄的 WebView 壳，只负责全屏 + 文件选择器
.github/workflows/        云端编译 APK
```

## 改点什么

| 想改的 | 改哪儿 |
| --- | --- |
| 判定宽容度 | `index.html` 里 `W_PERFECT` / `W_GOOD` / `W_MISS` |
| 音符生成密度 | `buildChart()` 里按难度分布的 `p` |
| 轨道数量 | `LANES = 4`（配色和键位表也要跟着改） |
| 下落速度 | 设置页的滑条，或 `G.travel` |
| 配色 | CSS 变量 `--c0` ~ `--c3` 和 `LANE_COLORS` |
