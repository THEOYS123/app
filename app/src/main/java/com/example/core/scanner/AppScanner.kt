package com.example.core.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.core.logging.AppLogger
import com.example.core.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppScanner {

    private const val TAG = "AppScanner"

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val pm = context.packageManager
        val list = mutableListOf<AppInfo>()

        try {
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getInstalledPackages(0)
            }

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val appName = try {
                    appInfo.loadLabel(pm).toString()
                } catch (_: Exception) {
                    pkg.packageName
                }

                val apkPath = appInfo.sourceDir ?: ""
                val splitApks = appInfo.splitSourceDirs?.toList() ?: emptyList()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val size = if (apkPath.isNotEmpty()) {
                    try { File(apkPath).length() } catch (_: Exception) { 0L }
                } else 0L

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else 0

                list.add(
                    AppInfo(
                        packageName = pkg.packageName,
                        appName = appName,
                        versionName = pkg.versionName ?: "N/A",
                        versionCode = vCode,
                        apkPath = apkPath,
                        splitApkPaths = splitApks,
                        isSystemApp = isSystem,
                        sizeBytes = size,
                        installTime = pkg.firstInstallTime,
                        updateTime = pkg.lastUpdateTime,
                        targetSdkVersion = appInfo.targetSdkVersion,
                        minSdkVersion = minSdk
                    )
                )
            }

            val duration = System.currentTimeMillis() - startTime
            AppLogger.i(TAG, "Scanned ${list.size} installed packages in ${duration}ms", duration)
            list.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying installed applications", e)
            emptyList()
        }
    }
}
