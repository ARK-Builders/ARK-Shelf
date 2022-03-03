package space.taran.arkshelf.domain

import java.nio.file.Path

interface LinkRepo {
    suspend fun parse(url: String): Result<Link>
    suspend fun generateFile(link: Link, basePath: Path)
}