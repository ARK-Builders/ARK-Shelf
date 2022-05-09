package space.taran.arkshelf.data

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import space.taran.arkshelf.domain.Link
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

// https://ogp.me/
class LinkRemoteDataSource(
    private val context: Context,
    private val client: OkHttpClient
) {


    fun parse(url: String): Result<Link> {
        try {
            val request: Request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).execute().use {
                return parseBody(it.body!!.string(), it.request.url.toString())
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseBody(body: String, url: String): Result<Link> {
        val doc = Jsoup.parse(body)
        val metas = doc.getElementsByTag("meta")
        val ogpMap = metas.map { element ->
            element.attr("property") to element.attr("content")
        }.toMap()

        val title = parseTitle(doc, ogpMap)
        val desc = parseDesc(doc, ogpMap)
        val image = parseImage(doc, ogpMap)
        val imagePath = try {
            image?.let { downloadImage(it) }
        } catch (e: Exception) {
            null
        }

        return Result.success(Link(title, desc, imagePath, url))
    }

    private fun parseTitle(doc: Document, ogpMap: Map<String, String>): String {
        return ogpMap[OGP_TITLE] ?: doc.title()
    }

    private fun parseDesc(doc: Document, ogpMap: Map<String, String>): String {
        return ogpMap[OGP_DESC] ?: ""
    }

    private fun parseImage(doc: Document, ogpMap: Map<String, String>): String? {
        return ogpMap[OGP_IMAGE] ?: let {
            val iconElement =
                doc.head().select("link[href~=.*\\.(ico|png)]").firstOrNull()
            iconElement?.attr("href")
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

    companion object {
        private const val OGP_TITLE = "og:title"
        private const val OGP_IMAGE = "og:image"
        private const val OGP_DESC = "og:description"
    }
}