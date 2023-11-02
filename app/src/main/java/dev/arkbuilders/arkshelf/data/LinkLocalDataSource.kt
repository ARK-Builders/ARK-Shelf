package dev.arkbuilders.arkshelf.data

import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.arklib.arkPreviews
import dev.arkbuilders.arklib.getLinkHash
import dev.arkbuilders.arklib.loadLinkFile
import dev.arkbuilders.arkshelf.domain.Link
import dev.arkbuilders.arkshelf.domain.UserPreferences
import dev.arkbuilders.arkshelf.presentation.listChildren
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import dev.arkbuilders.arklib.createLinkFile as createLinkFileN


class LinkLocalDataSource @Inject constructor(
    private val preferences: UserPreferences,
) {
    suspend fun createLinkResource(resource: Link, basePath: Path) {
        val roots = FoldersRepo.instance.provideFolders().keys
        val root = roots.find { basePath.startsWith(it) } ?: basePath
        Timber.d("creating link[$resource] file")
        createLinkFileN(
            root,
            resource.title,
            resource.desc,
            resource.url,
            basePath,
            downloadPreview = false
        )
        resource.imagePath?.copyTo(linkPreviewPath(root, resource.url))
    }

    suspend fun getLinksFromFolder(): List<Link> = withContext(Dispatchers.IO) {
        val linkFolder = preferences.getLinkFolder()
            ?: return@withContext emptyList()

        val parseLinksJob = linkFolder
            .listChildren()
            .filter {
                !it.isDirectory() && it.extension == "link"
            }
            .sortedByDescending { it.getLastModifiedTime() }
            .also { Timber.d("found ${it.size} links inside $linkFolder") }
            .map {
                async {
                    parseLinkResource(it, linkFolder)
                }
            }


        return@withContext parseLinksJob.awaitAll().filterNotNull()
    }

    private suspend fun parseLinkResource(path: Path, folder: Path): Link? = try {
        val roots = FoldersRepo.instance.provideFolders().keys.toMutableList()
        roots.add(folder)
        val root = roots.find { path.startsWith(it) } ?: error("Root not found")
        val linkData = loadLinkFile(root, path)
        val imagePath = linkPreviewPath(root, linkData.url)
            .let {
                if (it.exists()) it else null
            }
        Timber.d("link[$path] parsed")
        Link(linkData.title, linkData.desc, imagePath, linkData.url)
    } catch (e: Exception) {
        Timber.d("parse link[$path] failed: ${e.message}")
        null
    }

    private fun linkPreviewPath(root: Path, url: String) = root
        .arkFolder()
        .arkPreviews()
        .createDirectories()
        .resolve(getLinkHash(url))

}
