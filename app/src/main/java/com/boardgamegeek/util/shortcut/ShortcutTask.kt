package com.boardgamegeek.util.shortcut

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.ShortcutUtils
import com.boardgamegeek.util.StringUtils
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException

abstract class ShortcutTask @JvmOverloads constructor(context: Context?, thumbnailUrl: String? = null) : AsyncTask<Void, Void, Void>() {
    @SuppressLint("StaticFieldLeak") protected val context: Context? = context?.applicationContext
    private val thumbnailUrl: String? = HttpUtils.ensureScheme(thumbnailUrl)

    protected abstract val shortcutName: String
    protected abstract val id: String
    protected open val shortcutIconResId: Int
        get() = R.mipmap.ic_launcher_foreground

    protected abstract fun createIntent(): Intent?

    override fun doInBackground(vararg params: Void): Void? {
        if (context == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createShortcutForOreo()
        } else {
            val intent = createIntent() ?: return null
            val shortcutIntent = ShortcutUtils.createShortcutIntent(context, shortcutName, intent, shortcutIconResId)
            if (!TextUtils.isEmpty(thumbnailUrl)) {
                val bitmap = fetchThumbnail()
                if (bitmap != null) {
                    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
                }
            }
            context.sendBroadcast(shortcutIntent)
        }
        return null
    }

    @RequiresApi(api = VERSION_CODES.O)
    private fun createShortcutForOreo() {
        if (context == null) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val builder = ShortcutInfo.Builder(context, id)
                    .setShortLabel(StringUtils.limitText(shortcutName, ShortcutUtils.SHORT_LABEL_LENGTH))
                    .setLongLabel(StringUtils.limitText(shortcutName, ShortcutUtils.LONG_LABEL_LENGTH))
                    .setIntent(createIntent())
            if (!TextUtils.isEmpty(thumbnailUrl)) {
                val bitmap = fetchThumbnail()
                if (bitmap != null) {
                    builder.setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                } else {
                    builder.setIcon(Icon.createWithResource(context, shortcutIconResId))
                }
            } else {
                builder.setIcon(Icon.createWithResource(context, shortcutIconResId))
            }
            shortcutManager.requestPinShortcut(builder.build(), null)
        }
    }

    private fun fetchThumbnail(): Bitmap? {
        var bitmap: Bitmap? = null
        val file = ShortcutUtils.getThumbnailFile(context!!, thumbnailUrl!!)
        if (file != null && file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.absolutePath)
        } else {
            try {
                bitmap = Picasso.with(context)
                        .load(thumbnailUrl)
                        .resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
                        .centerCrop()
                        .get()
            } catch (e: IOException) {
                Timber.e(e, "Error downloading the thumbnail.")
            }

        }
        if (bitmap != null && file != null) {
            try {
                val out = BufferedOutputStream(FileOutputStream(file))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()
            } catch (e: IOException) {
                Timber.e(e, "Error saving the thumbnail file.")
            }

        }
        return bitmap
    }
}
