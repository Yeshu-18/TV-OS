package com.firelite.tv.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, cached dynamic icon and banner loader for installed Android packages.
 * Automatically resolves leanback TV banners or application icons directly from PackageManager
 * preserving true aspect ratios without distortion.
 */
object AppIconLoader {

  private val cache = ConcurrentHashMap<String, ImageBitmap>()

  fun getAppIcon(context: Context, packageName: String?): ImageBitmap? {
    if (packageName.isNullOrEmpty()) return null

    cache[packageName]?.let { return it }

    val packagesToTry = when (packageName) {
      "com.amazon.firebat" -> listOf("com.amazon.firebat", "com.amazon.amazonvideo.livingroom")
      "com.amazon.firetv.youtube" -> listOf("com.amazon.firetv.youtube", "com.google.android.youtube.tv")
      else -> listOf(packageName)
    }

    val pm = context.packageManager
    for (pkg in packagesToTry) {
      try {
        val drawable: Drawable = pm.getApplicationBanner(pkg) ?: pm.getApplicationIcon(pkg)
        val bitmap = drawableToBitmap(drawable)
        val imageBitmap = bitmap.asImageBitmap()
        cache[packageName] = imageBitmap
        return imageBitmap
      } catch (_: Exception) {}
    }

    return null
  }

  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      return drawable.bitmap
    }
    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bitmap
  }
}
