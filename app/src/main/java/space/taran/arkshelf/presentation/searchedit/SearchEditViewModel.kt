package space.taran.arkshelf.presentation.searchedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import space.taran.arkshelf.data.LinkPagingDataSource
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences
import java.nio.file.Path
import javax.inject.Named

data class SearchEditState(
    val url: String,
    val inputError: Throwable?,
    val screen: SearchEditScreen,
    val link: Link?,
    val isExternalUrl: Boolean,
    val linkFolder: Path?
) {
    init {
        when (screen) {
            SearchEditScreen.SEARCH -> require(link == null) {
                "Link must be null in search screen"
            }
            SearchEditScreen.EDIT -> require(link != null) {
                "Link must be parsed before going to edit screen"
            }
        }
    }

    companion object {
        fun initial(externalUrl: String?, preferences: UserPreferences) =
            SearchEditState(
                url = externalUrl ?: "",
                inputError = null,
                screen = SearchEditScreen.SEARCH,
                link = null,
                isExternalUrl = externalUrl != null,
                linkFolder = preferences.getLinkFolder()
            )
    }
}

enum class SearchEditScreen {
    SEARCH, EDIT
}

data class LinkItemModel(
    val link: Link,
    var isExpanded: Boolean
)

sealed class SearchEditSideEffect {
    object AskLinkFolder : SearchEditSideEffect()
    object CloseApp : SearchEditSideEffect()
    object LinkFolderChanged : SearchEditSideEffect()
}

class SearchEditViewModel(
    private val externalUrl: String?,
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences,
    private val pageSize: Int
) : ViewModel(), ContainerHost<SearchEditState, SearchEditSideEffect> {

    override val container: Container<SearchEditState, SearchEditSideEffect> =
        container(SearchEditState.initial(externalUrl, preferences))

    val listData = Pager(PagingConfig(pageSize = pageSize)) {
        LinkPagingDataSource(linkRepo)
    }.flow.cachedIn(viewModelScope)

    init {
        externalUrl?.let { onUrlPicked(externalUrl) }
    }

    fun onInputChanged(url: String) = intent {
        reduce {
            state.copy(url = url)
        }
    }

    fun onUrlPicked(url: String) = intent {
        val result = linkRepo.parse(formatUrl(url))
        reduce {
            result.fold(
                onSuccess = { link ->
                    state.copy(
                        link = link,
                        inputError = null,
                        screen = SearchEditScreen.EDIT
                    )
                },
                onFailure = { e -> state.copy(inputError = e) }
            )
        }
    }

    fun onSaveBtnClick(title: String, desc: String) = intent {
        if (preferences.getLinkFolder() == null) {
            postSideEffect(SearchEditSideEffect.AskLinkFolder)
            return@intent
        }
        state.link!!.title = title
        state.link!!.desc = desc
        linkRepo.generateFile(state.link!!, preferences.getLinkFolder()!!)
        if (state.isExternalUrl)
            postSideEffect(SearchEditSideEffect.CloseApp)
        else {
            reduce {
                state.copy(screen = SearchEditScreen.SEARCH, link = null)
            }
        }
    }

    fun handleShareIntent(url: String) = intent {
        reduce {
            state.copy(
                url = url,
                isExternalUrl = true
            )
        }
        onUrlPicked(url)
    }

    fun onLinkFolderChanged(folder: Path) = viewModelScope.launch {
        preferences.setLinkFolder(folder)
        intent {
            reduce {
                state.copy(linkFolder = folder)
            }
            postSideEffect(SearchEditSideEffect.LinkFolderChanged)
        }
    }

    fun onBackClick() = intent {
        when (state.screen) {
            SearchEditScreen.SEARCH -> postSideEffect(SearchEditSideEffect.CloseApp)
            SearchEditScreen.EDIT -> reduce {
                state.copy(
                    screen = SearchEditScreen.SEARCH,
                    link = null,
                    isExternalUrl = false
                )
            }
        }
    }

    private fun formatUrl(url: String): String {
        if (url.startsWith("https://") || url.startsWith("http://"))
            return url

        return "http://$url"
    }
}

class SearchEditViewModelFactory @AssistedInject constructor(
    @Assisted private val externalUrl: String?,
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences,
    @Named("PAGE_SIZE") private val pageSize: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchEditViewModel(externalUrl, linkRepo, preferences, pageSize) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted externalUrl: String?
        ): SearchEditViewModelFactory
    }
}
