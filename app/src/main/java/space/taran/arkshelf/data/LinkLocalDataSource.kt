package space.taran.arkshelf.data

import android.content.Context
import com.beust.klaxon.Klaxon
import space.taran.arkshelf.domain.Link
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

class LinkLocalDataSource(private val context: Context) {
    private val klaxon = Klaxon()
    private val jsonTmpPath =
        Path(context.cacheDir.absolutePath).resolve(Path("link.json"))

    fun createLinkFile(link: Link, basePath: Path) {
        generateJsonFile(link)
        val savePath = basePath.resolve(Path("${formatName(link)}.link"))
        zipFiles(jsonTmpPath, link.imagePath, savePath)
    }

    private fun generateJsonFile(link: Link) {
        jsonTmpPath.deleteIfExists()
        val jsonLink = JsonLink(link.url, link.title, link.desc)
        val content = klaxon.toJsonString(jsonLink)
        jsonTmpPath.writeText(content)
    }

    private fun formatName(link: Link): String {
        return link.url
            .replace("http://", "")
            .replace("https://", "")
            .replace("/", "-")
            .replace(".", "-")
    }

    private fun zipFiles(json: Path, image: Path?, output: Path) {
        val out = ZipOutputStream(output.outputStream())
        zipFile(json, out)
        image?.let { zipFile(it, out) }
        out.close()
    }

    private fun zipFile(path: Path, output: ZipOutputStream) {
        val entry = ZipEntry(path.name)
        path.inputStream().use {
            output.putNextEntry(entry)
            it.copyTo(output)
            output.closeEntry()
        }
    }
}

private data class JsonLink(val url: String, val title: String, val desc: String)