package com.boardgamegeek.export

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.ParcelFileDescriptor
import android.support.v4.content.ContextCompat
import com.boardgamegeek.R
import com.boardgamegeek.events.ExportFinishedEvent
import com.boardgamegeek.events.ExportProgressEvent
import com.boardgamegeek.export.model.Model
import com.boardgamegeek.util.FileUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.*

abstract class JsonExportTask<T : Model>(context: Context?, private val type: String, private val uri: Uri?) : AsyncTask<Void, Int, String>() {
    @SuppressLint("StaticFieldLeak") private val context = context?.applicationContext
    private val PROGRESS_TOTAL = 0
    private val PROGRESS_CURRENT = 1

    protected open val version: Int
        get() = 0

    protected abstract fun getCursor(context: Context): Cursor?

    protected abstract fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter)

    override fun doInBackground(vararg params: Void): String? {
        if (context == null) return "Error."

        if (uri == null) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission.WRITE_EXTERNAL_STORAGE)
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                return context.getString(R.string.msg_export_failed_external_permissions)
            }

            if (!FileUtils.isExtStorageAvailable()) {
                return context.getString(R.string.msg_export_failed_external_unavailable)
            }

            val exportPath = FileUtils.getExportPath()
            if (!exportPath.exists()) {
                if (!exportPath.mkdirs()) {
                    return context.getString(R.string.msg_export_failed_external_not_created, exportPath)
                }
            }
        }

        if (isCancelled) return context.getString(R.string.cancelled)

        val out: OutputStream
        var pfd: ParcelFileDescriptor? = null
        if (uri == null) {
            val file = FileUtils.getExportFile(type)
            try {
                out = FileOutputStream(file)
            } catch (e: FileNotFoundException) {
                val error = context.getString(R.string.msg_export_failed_file_not_found, file)
                Timber.w(e, error)
                return error
            }

        } else {
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "w")
            } catch (e: SecurityException) {
                val error = context.getString(R.string.msg_export_failed_permissions, uri)
                Timber.w(e, error)
                return error
            } catch (e: FileNotFoundException) {
                val error = context.getString(R.string.msg_export_failed_file_not_found, uri)
                Timber.w(e, error)
                return error
            }

            if (pfd == null) {
                return context.getString(R.string.msg_export_failed_null_pfd, uri)
            }

            out = FileOutputStream(pfd.fileDescriptor)
        }

        val cursor = getCursor(context) ?: return context.getString(R.string.msg_export_failed_null_cursor)

        try {
            writeJsonStream(out, cursor)
        } catch (e: Exception) {
            val error = context.getString(R.string.msg_export_failed_write_json)
            Timber.e(e, error)
            return error
        } finally {
            cursor.close()
        }

        FileUtils.closePfd(pfd)

        return null
    }


    override fun onProgressUpdate(vararg values: Int?) {
        EventBus.getDefault().post(ExportProgressEvent(
                values[PROGRESS_TOTAL] ?: 1,
                values[PROGRESS_CURRENT] ?: 0,
                type))
    }

    override fun onPostExecute(errorMessage: String) {
        Timber.i(errorMessage)
        EventBus.getDefault().post(ExportFinishedEvent(type, errorMessage))
    }

    @Throws(IOException::class)
    private fun writeJsonStream(out: OutputStream, cursor: Cursor) {
        if (context == null) {
            Timber.w("context is null")
            return
        }
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        with(JsonWriter(OutputStreamWriter(out, "UTF-8"))) {
            setIndent("  ")

            beginObject()
            name(NAME_TYPE).value(type)
            name(NAME_VERSION).value(version.toLong())
            name(NAME_ITEMS)
            beginArray()

            var numExported = 0
            while (cursor.moveToNext()) {
                if (isCancelled) break
                publishProgress(cursor.count, numExported++)
                try {
                    writeJsonRecord(context, cursor, gson, this)
                } catch (e: RuntimeException) {
                    Timber.e(e)
                }

            }

            endArray()
            endObject()
            close()
        }
    }
}
