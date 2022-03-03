package space.taran.arkshelf.domain

import java.nio.file.Path

interface UserPreferences {
    fun getLastSavePath(): Path?
    fun setLastSavePath(path: Path)
}