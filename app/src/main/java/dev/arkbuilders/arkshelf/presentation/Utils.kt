package dev.arkbuilders.arkshelf.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.arkbuilders.arkshelf.BuildConfig
import dev.arkbuilders.arkshelf.presentation.main.MainActivity
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.isHidden
import kotlin.io.path.listDirectoryEntries

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

val INTERNAL_STORAGE = Path("/storage/emulated/0")

fun listDevices(): List<Path> =
    App.instance.getExternalFilesDirs(null)
        .toList()
        .filterNotNull()
        .filter { it.exists() }
        .map {
            it.toPath().toRealPath()
                .takeWhile { part ->
                    part != ANDROID_DIRECTORY
                }
                .fold(ROOT_PATH) { parent, child ->
                    parent.resolve(child)
                }
        }

fun Path.listChildren(): List<Path> = listDirectoryEntries().filter { !it.isHidden() }

fun Fragment.hideKeyboard() {
    val activity = requireActivity()
    val inputMethodManager: InputMethodManager = activity.getSystemService(
        AppCompatActivity.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(
        activity.currentFocus!!.windowToken,
        0
    )
}



fun Fragment.askWritePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val packageUri =
            Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        val intent =
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                packageUri
            )
        startActivityForResult(intent, MainActivity.REQUEST_CODE_ALL_FILES_ACCESS)
    } else {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            MainActivity.REQUEST_CODE_PERMISSIONS
        )
    }
}

fun isWritePermGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            App.instance,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

suspend fun <T> withContextAndLock(
    context: CoroutineContext,
    mutex: Mutex,
    block: suspend CoroutineScope.() -> T
): T = withContext(context) {
    mutex.withLock {
        block()
    }
}

fun CoroutineScope.launchWithMutex(
    mutex: Mutex,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    mutex.withLock {
        block()
    }
}