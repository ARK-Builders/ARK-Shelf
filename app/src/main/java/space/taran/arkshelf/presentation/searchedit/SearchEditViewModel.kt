package space.taran.arkshelf.presentation.searchedit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences
import java.nio.file.Path
import kotlin.system.measureTimeMillis

data class SearchEditState(
    val url: String,
    val inputError: Throwable?,
    val screen: SearchEditScreen,
    val latestLinks: List<LinkItemModel>?,
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
                latestLinks = null,
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
    private val preferences: UserPreferences
) : ViewModel(), ContainerHost<SearchEditState, SearchEditSideEffect> {

    override val container: Container<SearchEditState, SearchEditSideEffect> =
        container(SearchEditState.initial(externalUrl, preferences))

    init {
        externalUrl?.let { onUrlPicked(externalUrl) }

        viewModelScope.launch {
            val links = linkRepo.loadMore()
            intent {
                reduce {
                    state.copy(
                        latestLinks = links.toLinkModels(state)
                    )
                }
            }
        }
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

    suspend fun onLoadMore() = withContext(Dispatchers.IO) {
        val links = linkRepo.loadMore()
        intent {
            reduce {
                state.copy(
                    latestLinks = links.toLinkModels(state)
                )
            }
        }
    }

    fun onLinkFolderChanged(folder: Path) = viewModelScope.launch {
        preferences.setLinkFolder(folder)
        intent {
            reduce {
                state.copy(linkFolder = folder)
            }
            postSideEffect(SearchEditSideEffect.LinkFolderChanged)
        }
        val links = linkRepo.loadMore()
        intent {
            reduce {
                state.copy(latestLinks = links.toLinkModels(state))
            }
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
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchEditViewModel(externalUrl, linkRepo, preferences) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted externalUrl: String?
        ): SearchEditViewModelFactory
    }
}

private fun List<Link>.toLinkModels(state: SearchEditState): List<LinkItemModel> {
    if (state.latestLinks == null) return map {
        LinkItemModel(
            it,
            false
        )
    }


    return map { link ->
        val isExpanded = state
            .latestLinks
            .find { it.link == link }
            ?.isExpanded ?: false
        LinkItemModel(
            link,
            isExpanded
        )
    }
}
