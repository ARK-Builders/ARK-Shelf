package space.taran.arkshelf.data

import space.taran.arkshelf.domain.Link
import java.nio.file.Path
import kotlin.io.path.*
import space.taran.arklib.createLinkFile as createLinkFileN


class LinkLocalDataSource() {
    fun createLinkFile(link: Link, basePath: Path) {
        createLinkFileN(link.title, link.desc, link.url, basePath.pathString, true)
    }
}