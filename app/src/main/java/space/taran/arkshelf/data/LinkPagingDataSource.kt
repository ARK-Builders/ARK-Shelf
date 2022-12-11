package space.taran.arkshelf.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.presentation.searchedit.LinkItemModel

class LinkPagingDataSource(private val linkRepo: LinkRepo) :
    PagingSource<Int, LinkItemModel>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LinkItemModel> {
        return try {
            val currentLoadingPageKey = params.key ?: 1
            val result = linkRepo.loadFiles(currentLoadingPageKey)
            val prevKey =
                if (currentLoadingPageKey == 1) null else currentLoadingPageKey - 1
            val nextKey = if (result.second) currentLoadingPageKey + 1 else null

            LoadResult.Page(
                result.first.map { LinkItemModel(it, false) },
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, LinkItemModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}