package space.taran.arkshelf.presentation

import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import space.taran.arkshelf.R
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
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

fun iconForExtension(ext: String): Int {
    val drawableID = App.instance.resources
        .getIdentifier(
            "ic_file_$ext",
            "drawable",
            App.instance.packageName
        )

    return if (drawableID > 0) drawableID
    else R.drawable.ic_file
}

fun AppCompatActivity.hideKeyboard() {
    val inputMethodManager: InputMethodManager = getSystemService(
        AppCompatActivity.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(
        currentFocus!!.windowToken,
        0
    )
}

fun Long.formatSize(): String {
    if (this <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}