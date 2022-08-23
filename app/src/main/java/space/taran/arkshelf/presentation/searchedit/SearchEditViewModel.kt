package space.taran.arkshelf.presentation.searchedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences

sealed class SearchEditState {
    data class Search(val url: String, val inputError: Throwable? = null) :
        SearchEditState()

    data class Edit(val link: Link, val isExtraUrl: Boolean) : SearchEditState()

    companion object {
        fun initial() = Search("")
    }
}

sealed class SearchEditSideEffect {
    object AskLinkFolder: SearchEditSideEffect()
    object CloseApp: SearchEditSideEffect()
}

class SearchEditViewModel(
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences
) : ViewModel(), ContainerHost<SearchEditState, SearchEditSideEffect> {

    override val container: Container<SearchEditState, SearchEditSideEffect> =
        container(SearchEditState.initial())

    fun onUrlPicked(url: String, isExtraUrl: Boolean) = intent {
        val result = linkRepo.parse(formatUrl(url))
        reduce {
            result.fold(
                onSuccess = { link -> SearchEditState.Edit(link, isExtraUrl) },
                onFailure = { e -> SearchEditState.Search(url, e) }
            )
        }
    }

    fun onSaveBtnClick(title: String, desc: String) = intent {
        if (preferences.getLinkFolder() == null) {
            postSideEffect(SearchEditSideEffect.AskLinkFolder)
            return@intent
        }
        val state = state as SearchEditState.Edit
        state.link.title = title
        state.link.desc = desc
        linkRepo.generateFile(state.link, preferences.getLinkFolder()!!)
        if (state.isExtraUrl)
            postSideEffect(SearchEditSideEffect.CloseApp)
        else {
            reduce {
                SearchEditState.Search("")
            }
        }
    }

    fun handleShareIntent(url: String) = intent {
        reduce { SearchEditState.Search(url) }
        onUrlPicked(url, isExtraUrl = true)
    }

    fun onBackClick() = intent {
        when(state) {
            is SearchEditState.Search -> {
                postSideEffect(SearchEditSideEffect.CloseApp)
            }
            is SearchEditState.Edit -> {
                val url = (state as SearchEditState.Edit).link.url
                reduce { SearchEditState.Search(url) }
            }
        }
    }

    private fun formatUrl(url: String): String {
        if (url.startsWith("https://") || url.startsWith("http://"))
            return url

        return "http://$url"
    }
}