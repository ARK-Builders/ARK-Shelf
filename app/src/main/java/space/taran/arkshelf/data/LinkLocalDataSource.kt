package space.taran.arkshelf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.UserPreferences
import space.taran.arkshelf.presentation.listChildren
import space.taran.arkshelf.presentation.withContextAndLock
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import kotlin.io.path.createTempFile
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory

private class LinkWithPath(val link: Link, val path: Path)

class LinkLocalDataSource @Inject constructor(
    private val preferences: UserPreferences,
    private val appCtx: Context
) {
    private val klaxon = Klaxon()
    private val latestByFolder = mutableMapOf<Path, MutableList<LinkWithPath>>()
    private val loadMoreMutex = Mutex()
    private val cacheDir = appCtx.cacheDir.resolve(CACHE_IMAGES_DIR).toPath().createDirectories()

    fun createLinkFile(link: Link, basePath: Path) {
        val jsonFile = generateJsonFile(link)
        val savePath = basePath.resolve(Path("${sha512(link.url)}.link"))
        zipFiles(jsonFile, link.imagePath, savePath)
    }

    suspend fun loadMore(): List<Link> = withContextAndLock(
        Dispatchers.Default,
        loadMoreMutex
    ) {
        val linkFolder = preferences.getLinkFolder()
            ?: return@withContextAndLock emptyList()


        val newLinksJobs = linkFolder
            .listChildren()
            .filter {
                it.extension == "link" && !it.isDirectory()
            }
            .sortedByDescending { it.getLastModifiedTime() }
            .minus(latestByFolder[linkFolder]?.map { it.path } ?: emptyList())
            .take(BATCH_SIZE)
            .map { path ->
                async {
                    val link = parseLinkFile(path) ?: return@async null
                    LinkWithPath(link, path)
                }
            }
            .toList()

        val newLinks = newLinksJobs.awaitAll().filterNotNull()

        latestByFolder[linkFolder]?.let {
            it.addAll(newLinks)
        } ?: let {
            latestByFolder[linkFolder] = newLinks.toMutableList()
        }

        return@withContextAndLock latestByFolder[linkFolder]
            ?.map { it.link }
            ?: emptyList()
    }

    private fun generateJsonFile(link: Link): Path {
        val file = createTempFile()
        val jsonLink = JsonLink(link.url, link.title, link.desc)
        val content = klaxon.toJsonString(jsonLink)
        file.writeText(content)
        return file
    }

    private fun sha512(string: String): String {
        return MessageDigest.getInstance("SHA-512")
            .digest(string.toByteArray(Charsets.UTF_8))
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun zipFiles(json: Path, image: Path?, output: Path) {
        val out = ZipOutputStream(output.outputStream())
        zipFile(JSON_FILE, json, out)
        image?.let { zipFile(IMAGE_FILE, it, out) }
        out.close()
    }

    private fun zipFile(name: String, path: Path, output: ZipOutputStream) {
        val entry = ZipEntry(name)
        path.inputStream().use {
            output.putNextEntry(entry)
            it.copyTo(output)
            output.closeEntry()
        }
    }

    private fun parseLinkFile(path: Path): Link? = try {
        val zip = ZipFile(path.toFile())
        val entries = zip
            .entries()
            .toList()

        val jsonEntry = entries.find { entry -> entry.name == JSON_FILE }
        val jsonLink = klaxon.parse<JsonLink>(zip.getInputStream(jsonEntry))!!

        val imageCacheName = Path(sha512(jsonLink.url))
        val imagePath = cacheDir.resolve(imageCacheName)

        if (!cacheDir.contains(imageCacheName)) {
            restoreImageFile(zip, entries, imagePath)
        }

        Link(jsonLink.title, jsonLink.desc, imagePath, jsonLink.url)
    } catch (e: Exception) {
        null
    }

    private fun restoreImageFile(
        zip: ZipFile,
        entries: List<ZipEntry>,
        imagePath: Path
    ) {
        val imageEntry = entries.find { entry -> entry.name == IMAGE_FILE }
        imageEntry?.let {
            val bitmap = BitmapFactory.decodeStream(zip.getInputStream(imageEntry))
            imagePath.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 14
        private const val CACHE_IMAGES_DIR = "images"
        private const val IMAGE_FILE = "link.png"
        private const val JSON_FILE = "link.json"
    }
}

private data class JsonLink(val url: String, val title: String, val desc: String)
