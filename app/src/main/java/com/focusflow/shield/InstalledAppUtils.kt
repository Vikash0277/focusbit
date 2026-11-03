package com.focusflow.shield

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

object InstalledAppUtils {

    fun isEmergencyPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower == "android" ||
               lower == "com.android.systemui" ||
               lower.contains("dialer") ||
               lower.contains("telecom") ||
               lower.contains("phone") ||
               lower.contains("emergency") ||
               lower.contains("cellbroadcast") ||
               lower.contains("sos") ||
               lower.contains("safety")
    }

    fun getInstalledLauncherApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val myPackage = context.packageName

        return resolveInfos
            .filter {
                val pkg = it.activityInfo.packageName
                pkg != myPackage && !isEmergencyPackage(pkg)
            }
            .map { resolveInfo ->
                val appName = resolveInfo.loadLabel(pm).toString()
                val packageName = resolveInfo.activityInfo.packageName
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                InstalledAppInfo(appName, packageName, icon, isSystem)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        return try {
            val width = drawable.intrinsicWidth.coerceIn(48, 256)
            val height = drawable.intrinsicHeight.coerceIn(48, 256)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
