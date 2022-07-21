package space.taran.arkshelf.data

import com.beust.klaxon.Klaxon
import space.taran.arkshelf.domain.Link
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import kotlin.io.path.createTempFile
import java.security.MessageDigest


class LinkLocalDataSource() {
    private val klaxon = Klaxon()

    fun createLinkFile(link: Link, basePath: Path) {
        val jsonFile = generateJsonFile(link)
        val savePath = basePath.resolve(Path("${sha512(link.url)}.link"))
        zipFiles(jsonFile, link.imagePath, savePath)
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

    companion object {
        private const val IMAGE_FILE = "link.png"
        private const val JSON_FILE = "link.json"
    }
}

private data class JsonLink(val url: String, val title: String, val desc: String)