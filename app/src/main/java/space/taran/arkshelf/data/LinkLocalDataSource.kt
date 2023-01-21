package space.taran.arkshelf.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.arkFolder
import space.taran.arklib.arkPreviews
import space.taran.arklib.getLinkHash
import space.taran.arklib.loadLinkFile
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.UserPreferences
import space.taran.arkshelf.presentation.listChildren
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import space.taran.arklib.createLinkFile as createLinkFileN

class LinkPage(val links: List<Link>, val hasMore: Boolean)

class LinkLocalDataSource @Inject constructor(
    private val preferences: UserPreferences,
    @Named("PAGE_SIZE") private val pageSize: Int,
) {
    suspend fun createLinkFile(link: Link, basePath: Path) {
        val roots = FoldersRepo.instance.provideFolders().keys
        val root = roots.find { basePath.startsWith(it) } ?: basePath
        createLinkFileN(root, link.title, link.desc, link.url, basePath, true)
    }

    suspend fun loadFiles(page: Int): LinkPage = withContext(Dispatchers.Default) {
        val linkFolder = preferences.getLinkFolder()
            ?: return@withContext LinkPage(emptyList(), false)

        val dropCount = (page - 1) * pageSize
        val hasMore: Boolean

        val newLinksJobs = linkFolder
            .listChildren()
            .filter {
                it.extension == "link" && !it.isDirectory()
            }
            .sortedByDescending { it.getLastModifiedTime() }
            .also { files ->
                hasMore = files.size > dropCount + pageSize
            }
            .drop(dropCount)
            .take(pageSize)
            .map { path ->
                async {
                    parseLinkFile(path) ?: return@async null
                }
            }
            .toList()

        return@withContext LinkPage(
            newLinksJobs.awaitAll().filterNotNull(),
            hasMore
        )
    }

    private suspend fun parseLinkFile(path: Path): Link? = try {
        val roots = FoldersRepo.instance.provideFolders().keys
        val root = roots.find { path.startsWith(it) }!!
        val linkData = loadLinkFile(root, path)
        val imagePath = root
            .arkFolder()
            .arkPreviews()
            .resolve(getLinkHash(linkData.url))
            .let {
                if (it.exists()) it else null
            }

        Link(linkData.title, linkData.desc, imagePath, linkData.url)
    } catch (e: Exception) {
        null
    }
}
