package space.taran.arkshelf.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arkshelf.data.network.NetworkStatus
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.NoInternetException
import java.nio.file.Path
import javax.inject.Inject

class LinkRepoImpl @Inject constructor(
    private val local: LinkLocalDataSource,
    private val remote: LinkRemoteDataSource,
    private val status: NetworkStatus
) : LinkRepo {

    override suspend fun parse(url: String): Result<Link> =
        withContext(Dispatchers.IO) {
            if (status.isOnline()) {
                return@withContext remote.parse(url)
            } else
                return@withContext Result.failure(NoInternetException())
        }

    override suspend fun generateFile(link: Link, basePath: Path) =
        withContext(Dispatchers.IO) {
            local.createLinkFile(link, basePath)
        }

    override suspend fun loadFiles(page: Int) = local.loadFiles(page)

}