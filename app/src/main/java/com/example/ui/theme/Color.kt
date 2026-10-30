package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// GAME REPLAY PRO — Unified Minimalist Design System Color Tokens
// 70% Minimal Professional Utility + 20% Modern Native OS + 10% Light Gaming Tech
// ============================================================================

// Core Surfaces & Backgrounds
val PureWhite = Color(0xFFFFFFFF)
val AppBackground = Color(0xFFF5F7FA)           // 页面背景: #F5F7FA
val AppBackgroundGradientTop = Color(0xFFF0F5FF) // 顶部极轻量浅蓝过渡
val CardBackground = Color(0xFFFFFFFF)          // 卡片背景: #FFFFFF
val CardElevated = Color(0xFFFFFFFF)
val SubtleSurface = Color(0xFFF7F9FC)           // 卡片内次级容器背景

// Brand Primary Blue
val PrimaryBlue = Color(0xFF1677FF)             // 品牌主蓝: #1677FF
val PrimaryBlueDark = Color(0xFF0958D9)
val PrimaryBlueLight = Color(0xFFEAF3FF)        // 浅蓝强调背景: #EAF3FF
val PrimaryBlueHover = Color(0xFF4096FF)

// Typography Hierarchy
val TextTitle = Color(0xFF202733)               // 主文字: #202733
val TextBody = Color(0xFF4A5568)                // 正文深灰
val TextMuted = Color(0xFF8993A1)               // 次级文字: #8993A1
val TextLight = Color(0xFFC4CAD4)               // 禁用/占位文字

// Dividers & Borders
val BorderSubtle = Color(0xFFE8EDF3)            // 分割线/细边框: #E8EDF3
val BorderMedium = Color(0xFFD8E0EA)
val ShadowAmbient = Color(0x0A000000)

// Semantic Status Colors
val SoftGreen = Color(0xFF36B56A)               // 成功/运行状态: #36B56A
val SoftGreenBg = Color(0xFFEDF9F2)
val SoftGreenBorder = Color(0xFFB7EBCD)

val SoftOrange = Color(0xFFF5A623)              // 警告/提示状态: #F5A623
val SoftOrangeBg = Color(0xFFFFF8EC)

val SoftRed = Color(0xFFE55353)                 // 错误/危险操作: #E55353
val SoftRedBg = Color(0xFFFDF0F0)

// Unified Secondary Accents (mapped to calm brand blue/neutral to avoid multi-color clutter)
val SoftPurple = PrimaryBlue
val SoftPurpleBg = PrimaryBlueLight

// Legacy alias mappings for compatibility
val ElectricCyan = PrimaryBlue
val CyberBlack = TextTitle
val CyberSurfaceDark = AppBackground
val CyberSurfaceCard = CardBackground
val CyberSurfaceElevated = CardElevated
val CyberBorder = BorderSubtle
val CyberBorderGlow = BorderMedium
val CyberPurple = PrimaryBlue
val NeonGreen = SoftGreen
val RadiantCrimson = SoftRed
val TextPrimary = TextTitle
val TextSecondary = TextBody
