package com.example.engine

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log

data class InstalledGame(
    val title: String,
    val packageName: String,
    val isSystemGame: Boolean = false,
    val category: String = "游戏",
    val isRunning: Boolean = false
)

class GameDetectionManager(private val context: Context) {

    private val tag = "GameDetectionManager"

    // Curated high-profile mobile games for fast and accurate identification
    private val knownGamePackages = mapOf(
        "com.tencent.tmgp.sgame" to "王者荣耀",
        "com.tencent.tmgp.pubgmhd" to "和平精英",
        "com.miHoYo.Yuanshen" to "原神",
        "com.miHoYo.hkrpg" to "崩坏：星穹铁道",
        "com.tencent.lolm" to "英雄联盟手游",
        "com.tencent.tmgp.cf" to "穿越火线：枪战王者",
        "com.tencent.tmgp.cod" to "使命召唤手游",
        "com.tencent.tmgp.jgame" to "金铲铲之战",
        "com.netease.party" to "蛋仔派对",
        "com.tencent.tmgp.hyrz" to "火影忍者",
        "com.epicgames.portal" to "Fortnite",
        "com.activision.callofduty.shooter" to "Call of Duty: Mobile",
        "com.riotgames.league.wildrift" to "Wild Rift"
    )

    /**
     * Scans and returns all detected installed games on the user's device.
     */
    fun getInstalledGames(): List<InstalledGame> {
        val detected = mutableListOf<InstalledGame>()
        val pm = context.packageManager

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)

            for (resolveInfo in resolvedApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) continue

                val appInfo = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkgName, 0)
                    }
                } catch (_: Exception) {
                    continue
                }

                if (!appInfo.enabled) continue

                val appLabel = resolveInfo.loadLabel(pm).toString()

                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                            (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }

                val knownTitle = knownGamePackages[pkgName]
                if (isGame || knownTitle != null) {
                    val gameTitle = knownTitle ?: appLabel
                    if (detected.none { it.packageName == pkgName }) {
                        detected.add(
                            InstalledGame(
                                title = gameTitle,
                                packageName = pkgName,
                                isSystemGame = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to scan installed apps: ${e.message}")
        }

        // Explicitly probe known esports titles directly by package name to catch any missed launcher intents,
        // strictly verifying that the game is ACTUALLY installed and launchable on the device
        for ((pkgName, knownTitle) in knownGamePackages) {
            if (detected.any { it.packageName == pkgName }) continue
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkgName, 0)
                }
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null && appInfo.enabled) {
                    detected.add(
                        InstalledGame(
                            title = knownTitle,
                            packageName = pkgName,
                            isSystemGame = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    )
                }
            } catch (_: PackageManager.NameNotFoundException) {
                // Game is NOT installed on this device: strictly do not display as requested by user
            } catch (_: Exception) {}
        }

        return detected
    }

    /**
     * Fallback returns empty list: games that are not installed on device will strictly NOT be displayed.
     */
    fun getDefaultSupportedGames(): List<InstalledGame> {
        return emptyList()
    }

    /**
     * Attempts to launch the specified game.
     */
    fun launchGame(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error launching game package: $packageName", e)
            false
        }
    }
}
