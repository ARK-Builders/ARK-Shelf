package space.taran.arkshelf.data

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import space.taran.arkshelf.domain.Link
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream
import space.taran.arklib.fetchLinkData


class LinkRemoteDataSource(
    private val context: Context,
) {

    fun parse(url: String): Result<Link> {
        return try {
            val linkData = fetchLinkData(url) ?: return Result.failure( Exception("Link data not available"))
            val imagePath = try {
                linkData.imageUrl?.let {
                    println("Yeah")
                    downloadImage(it)
                }
            } catch (e: Exception) {
                null
            }
            Result.success(Link(linkData.title, linkData.desc, imagePath, linkData.url))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadImage(url: String): Path {
        val file = createTempFile()

        val bitmap = Glide.with(context)
            .asBitmap()
            .load(url)
            .submit()
            .get()

        val out = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()

        return file
    }
}