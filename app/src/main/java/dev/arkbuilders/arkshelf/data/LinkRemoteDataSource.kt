package dev.arkbuilders.arkshelf.data

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import dev.arkbuilders.arklib.fetchLinkData
import dev.arkbuilders.arkshelf.domain.Link
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

class LinkRemoteDataSource @Inject constructor(
    private val context: Context
) {
    fun parse(url: String): Result<Link> {
        try {
            val linkData = fetchLinkData(url)
                ?: return Result.failure(Exception("Link data not available"))

            val imagePath = try {
                if (linkData.imageUrl.isNotEmpty())
                    downloadImage(linkData.imageUrl)
                else null
            } catch (e: Exception) {
                null
            }

            return Result.success(
                Link(
                    linkData.title,
                    linkData.desc,
                    imagePath,
                    url
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
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