package com.boardgamegeek.ui

import android.Manifest.permission
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.events.ExportFinishedEvent
import com.boardgamegeek.events.ExportProgressEvent
import com.boardgamegeek.events.ImportFinishedEvent
import com.boardgamegeek.events.ImportProgressEvent
import com.boardgamegeek.export.*
import com.boardgamegeek.ui.widget.DataStepRow
import com.boardgamegeek.ui.widget.DataStepRow.Listener
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.TaskUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import hugo.weaving.DebugLog
import kotlinx.android.synthetic.main.fragment_data.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.ctx
import timber.log.Timber

class DataFragment : Fragment(), Listener {
    private var currentType = ""

    @DebugLog
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createDataRow(TYPE_COLLECTION_VIEWS, R.string.backup_type_collection_view, R.string.backup_description_collection_view)
        createDataRow(TYPE_GAMES, R.string.backup_type_game, R.string.backup_description_game)
        createDataRow(TYPE_USERS, R.string.backup_type_user, R.string.backup_description_user)
    }

    private fun createDataRow(type: String, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        val row = DataStepRow(context)
        row.setListener(this)
        row.bind(type, typeResId, descriptionResId)
        row.tag = type
        fileTypesView.addView(row)
    }

    @DebugLog
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    @DebugLog
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun getExportTask(type: String, uri: Uri): JsonExportTask<*>? {
        return when (type) {
            TYPE_COLLECTION_VIEWS -> CollectionViewExportTask(ctx, uri)
            TYPE_GAMES -> GameExportTask(ctx, uri)
            TYPE_USERS -> UserExportTask(ctx, uri)
            else -> null
        }
    }

    private fun getImportTask(type: String, uri: Uri): JsonImportTask<*>? {
        return when (type) {
            TYPE_COLLECTION_VIEWS -> CollectionViewImportTask(ctx, uri)
            TYPE_GAMES -> GameImportTask(ctx, uri)
            TYPE_USERS -> UserImportTask(ctx, uri)
            else -> null
        }
    }

    override fun onExportClicked(type: String) {
        if (FileUtils.shouldUseDefaultFolders()) {
            DialogUtils.createConfirmationDialog(activity, R.string.msg_export_confirmation
            ) { _, _ ->
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(ctx, permission.WRITE_EXTERNAL_STORAGE)) {
                    performExport(type, Uri.EMPTY)
                } else {
                    if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
                        showSnackbar(R.string.msg_export_permission_rationale)
                    }
                    requestPermissions(arrayOf(permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS)
                }
            }.show()
        } else {
            currentType = type
            startActivityForResult(createIntent(type, Intent.ACTION_CREATE_DOCUMENT), REQUEST_EXPORT)
        }
    }

    override fun onImportClicked(type: String) {
        if (FileUtils.shouldUseDefaultFolders()) {
            DialogUtils.createConfirmationDialog(activity, R.string.msg_import_confirmation
            ) { _, _ -> performImport(type, Uri.EMPTY) }.show()
        } else {
            currentType = type
            startActivityForResult(createIntent(type, Intent.ACTION_OPEN_DOCUMENT), REQUEST_IMPORT)
        }
    }

    private fun createIntent(type: String, action: String): Intent {
        val intent = Intent(action)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/json"
        intent.putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(type))
        return intent
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || !isAdded || data == null) return

        val uri = data.data ?: return

        try {
            val modeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            ctx.contentResolver.takePersistableUriPermission(uri, modeFlags)
        } catch (e: SecurityException) {
            Timber.e(e, "Could not persist URI permissions for '$uri'.")
        }

        when (requestCode) {
            REQUEST_EXPORT -> performExport(currentType, uri)
            REQUEST_IMPORT -> performImport(currentType, uri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performExport(currentType, Uri.EMPTY)
            } else {
                showSnackbar(R.string.msg_export_permission_denied)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @DebugLog
    private fun performExport(type: String, uri: Uri) {
        val task = getExportTask(type, uri)
        when (task) {
            null -> Timber.i("No task found for $type")
            else -> {
                findRow(type)?.initProgressBar()
                TaskUtils.executeAsyncTask(task)
                logAnswer("Export")
            }
        }
    }

    @DebugLog
    private fun performImport(type: String, uri: Uri) {
        val task = getImportTask(type, uri)
        when (task) {
            null -> Timber.i("No task found for $type")
            else -> {
                findRow(type)?.initProgressBar()
                TaskUtils.executeAsyncTask(task)
                logAnswer("Import")
            }
        }
    }

    private fun logAnswer(action: String) {
        Answers.getInstance().logCustom(CustomEvent(ANSWERS_EVENT_NAME).putCustomAttribute(ANSWERS_ATTRIBUTE_KEY_ACTION, action))
    }

    @DebugLog
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ExportFinishedEvent) {
        findRow(event.type)?.hideProgressBar()
        notifyEnd(event.errorMessage, R.string.msg_export_success, R.string.msg_export_failed)
    }

    @DebugLog
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ImportFinishedEvent) {
        findRow(event.type)?.hideProgressBar()
        notifyEnd(event.errorMessage, R.string.msg_import_success, R.string.msg_import_failed)
    }

    @DebugLog
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ExportProgressEvent) {
        findRow(event.type)?.updateProgressBar(event.totalCount, event.currentCount)
    }

    @DebugLog
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ImportProgressEvent) {
        findRow(event.type)?.updateProgressBar(event.totalCount, event.currentCount)
    }

    private fun findRow(type: String): DataStepRow? {
        // todo refactor
        (0 until fileTypesView.childCount)
                .map { fileTypesView.getChildAt(it) }
                .filter { it != null && it.tag == type }
                .forEach { return it as DataStepRow }
        return null
    }

    private fun notifyEnd(errorMessage: String, @StringRes successResId: Int, @StringRes failureResId: Int) {
        val message = when {
            errorMessage.isEmpty() -> getString(successResId)
            else -> "${getString(failureResId)} - $errorMessage"
        }
        showSnackbar(message)
    }

    private fun showSnackbar(@StringRes messageResId: Int) {
        val view = view
        if (view != null) longSnackbar(view, messageResId)
    }

    private fun showSnackbar(message: String) {
        val view = view
        if (view != null) longSnackbar(view, message)
    }

    companion object {
        private val REQUEST_EXPORT = 1000
        private val REQUEST_IMPORT = 2000
        private val REQUEST_PERMISSIONS = 3000
        private val ANSWERS_EVENT_NAME = "DataManagement"
        private val ANSWERS_ATTRIBUTE_KEY_ACTION = "Action"
    }
}
