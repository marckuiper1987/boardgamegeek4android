package com.boardgamegeek.export

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.ParcelFileDescriptor
import com.boardgamegeek.R
import com.boardgamegeek.events.ImportFinishedEvent
import com.boardgamegeek.events.ImportProgressEvent
import com.boardgamegeek.export.model.Model
import com.boardgamegeek.util.FileUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

abstract class JsonImportTask<T : Model>(context: Context?, private val type: String, private val uri: Uri?) : AsyncTask<Void, Int, String>() {
    @SuppressLint("StaticFieldLeak") protected val context: Context? = context?.applicationContext
    private val items: MutableList<T>
    private val PROGRESS_TOTAL = 0
    private val PROGRESS_CURRENT = 1

    init {
        items = ArrayList()
    }

    protected open fun initializeImport() {}

    protected abstract fun parseItem(gson: Gson, reader: JsonReader): T

    protected abstract fun importRecord(item: T, version: Int)

    override fun doInBackground(vararg params: Void): String {
        if (context == null) return "Error."

        val fileInputStream: FileInputStream
        var pfd: ParcelFileDescriptor? = null
        if (uri == null) {
            if (!FileUtils.isExtStorageAvailable()) return context.getString(R.string.msg_export_failed_external_unavailable)

            // TODO: Ensure no large database ops are running?

            val importPath = FileUtils.getExportPath()
            if (!importPath.exists()) return context.getString(R.string.msg_import_failed_external_not_exist, importPath)

            val file = FileUtils.getExportFile(type)
            if (!file.exists()) return context.getString(R.string.msg_import_failed_file_not_exist, file)
            if (!file.canRead()) return context.getString(R.string.msg_import_failed_file_not_read, file)

            try {
                fileInputStream = FileInputStream(file)
            } catch (e: FileNotFoundException) {
                val error = context.getString(R.string.msg_import_failed_file_not_exist, file)
                Timber.w(e, error)
                return error
            }

        } else {
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
            } catch (e: FileNotFoundException) {
                val error = context.getString(R.string.msg_import_failed_file_not_exist, uri)
                Timber.w(e, error)
                return error
            }

            if (pfd == null) return context.getString(R.string.msg_export_failed_null_pfd, uri)

            fileInputStream = FileInputStream(pfd.fileDescriptor)
        }

        if (isCancelled) return context.getString(R.string.cancelled)

        initializeImport()
        var version = 0

        var reader: JsonReader? = null
        try {
            reader = JsonReader(InputStreamReader(fileInputStream, "UTF-8"))
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                parseItems(reader)
            } else {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (NAME_TYPE == name) {
                        val type = reader.nextString()
                        if (type != this.type) {
                            return context.getString(R.string.msg_import_failed_wrong_type, this.type, type)
                        }
                    } else if (NAME_VERSION == name) {
                        version = reader.nextInt()
                    } else if (NAME_ITEMS == name) {
                        parseItems(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Timber.w(e, "Importing %s JSON file.", type)
            return context.getString(R.string.msg_import_failed_parse_json)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Timber.w(e, "Failed trying to close the JsonReader")
                }

            }
        }

        for (i in items.indices) {
            publishProgress(items.size, i)
            importRecord(items[i], version)
        }

        FileUtils.closePfd(pfd)

        return ""
    }

    @Throws(IOException::class)
    private fun parseItems(reader: JsonReader) {
        val gson = Gson()
        items.clear()
        reader.beginArray()
        while (reader.hasNext()) {
            items.add(parseItem(gson, reader))
        }
        reader.endArray()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        EventBus.getDefault().post(ImportProgressEvent(
                values[PROGRESS_TOTAL] ?: 1,
                values[PROGRESS_CURRENT] ?: 0,
                type))
    }

    override fun onPostExecute(errorMessage: String) {
        Timber.i(errorMessage)
        EventBus.getDefault().post(ImportFinishedEvent(type, errorMessage))
    }
}
