package dev.arkbuilders.arkshelf.domain

import java.nio.file.Path

interface UserPreferences {
    fun getLinkFolder(): Path?
    fun setLinkFolder(path: Path)
}