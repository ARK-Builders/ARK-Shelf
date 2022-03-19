package space.taran.arkshelf.data

import android.content.Context
import android.os.Environment
import space.taran.arkshelf.domain.UserPreferences
import java.nio.file.Path
import kotlin.io.path.Path

class UserPreferencesImpl(private val context: Context) : UserPreferences {
    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    override fun getLinkFolder() =
        prefs.getString(SAVE_PATH_KEY, null)?.let { Path(it) }


    override fun setLinkFolder(path: Path) = with(prefs.edit()) {
        putString(SAVE_PATH_KEY, path.toString())
        apply()
    }

    companion object {
        private const val SAVE_PATH_KEY = "savePath"
    }
}