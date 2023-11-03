package dev.arkbuilders.arkshelf.presentation.searchedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import dev.arkbuilders.arkshelf.domain.Link
import dev.arkbuilders.arkshelf.domain.LinkRepo
import dev.arkbuilders.arkshelf.domain.UserPreferences
import java.nio.file.Path

data class SearchEditState(
    val url: String,
    val inputError: Throwable?,
    val screen: SearchEditScreen,
    val link: Link?,
    val isExternalUrl: Boolean,
    val linkFolder: Path?,
    val links: List<Link>? = null
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
    private val preferences: UserPreferences
) : ViewModel(), ContainerHost<SearchEditState, SearchEditSideEffect> {

    override val container: Container<SearchEditState, SearchEditSideEffect> =
        container(SearchEditState.initial(externalUrl, preferences))

    init {
        externalUrl?.let { onUrlPicked(externalUrl) }
        intent {
            val links = linkRepo.getLinksFromFolder()
            reduce { state.copy(links = links) }
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
        if (state.isExternalUrl) {
            viewModelScope.launch(Dispatchers.IO) {
                linkRepo.generateResource(state.link!!, preferences.getLinkFolder()!!)
            }
            postSideEffect(SearchEditSideEffect.CloseApp)
        } else {
            linkRepo.generateResource(state.link!!, preferences.getLinkFolder()!!)
            val links = state.links?.let { oldLinks ->
                val mutLinks = oldLinks.toMutableList()
                mutLinks.add(0, state.link!!)
                mutLinks
            }
            reduce {
                state.copy(screen = SearchEditScreen.SEARCH, link = null, links = links)
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
            postSideEffect(SearchEditSideEffect.LinkFolderChanged)
            val links = linkRepo.getLinksFromFolder()
            reduce {
                state.copy(linkFolder = folder, links = links)
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
    private val preferences: UserPreferences,
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
