package space.taran.arkshelf.domain

import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Path

interface LinkRepo {
    suspend fun parse(url: String): Result<Link>
    suspend fun generateFile(link: Link, basePath: Path)
    suspend fun loadMore(): List<Link>
}