package dev.arkbuilders.arkshelf.domain

import java.nio.file.Path

interface LinkRepo {
    suspend fun parse(url: String): Result<Link>
    suspend fun generateResource(link: Link, basePath: Path)
    suspend fun getLinksFromFolder(): List<Link>
}