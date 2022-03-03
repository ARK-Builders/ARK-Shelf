package space.taran.arkshelf.domain

import java.nio.file.Path

data class Link(
    var title: String,
    var desc: String,
    val imagePath: Path?,
    val url: String
)