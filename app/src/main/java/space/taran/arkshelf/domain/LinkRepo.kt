package space.taran.arkshelf.domain

import kotlinx.coroutines.flow.StateFlow
import space.taran.arkshelf.data.LinkPage
import java.nio.file.Path

interface LinkRepo {
    suspend fun parse(url: String): Result<Link>
    suspend fun generateFile(link: Link, basePath: Path)
    suspend fun loadFiles(page: Int): LinkPage
}