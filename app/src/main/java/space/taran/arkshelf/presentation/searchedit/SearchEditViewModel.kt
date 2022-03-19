package space.taran.arkshelf.presentation.searchedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences

sealed class SearchEditState {
    data class Search(val url: String, val inputError: Throwable? = null) :
        SearchEditState()

    data class Edit(val link: Link) : SearchEditState()
}

sealed class SearchEditAction {
    object AskLinkFolder: SearchEditAction()
}

class SearchEditViewModel(
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences
) : ViewModel() {
    val stateFlow: MutableStateFlow<SearchEditState> =
        MutableStateFlow(SearchEditState.Search("", null))
    val actionsFlow: MutableSharedFlow<SearchEditAction> = MutableSharedFlow()

    fun onUrlPicked(url: String) = viewModelScope.launch {
        val result = linkRepo.parse(formatUrl(url))
        result.onSuccess { link ->
            stateFlow.value = SearchEditState.Edit(link)
        }
        result.onFailure { exception ->
            stateFlow.value = SearchEditState.Search(url, exception)
        }
    }

    fun onSaveBtnClick(title: String, desc: String) = viewModelScope.launch {
        if (preferences.getLinkFolder() == null) {
            actionsFlow.emit(SearchEditAction.AskLinkFolder)
            return@launch
        }
        val link = (stateFlow.value as SearchEditState.Edit).link
        link.title = title
        link.desc = desc
        linkRepo.generateFile(link, preferences.getLinkFolder()!!)
        stateFlow.value = SearchEditState.Search("")
    }

    fun handleShareIntent(url: String) {
        stateFlow.value = SearchEditState.Search(url)
        onUrlPicked(url)
    }

    fun onBackClick(): Boolean {
        with(stateFlow.value) {
            return if (this is SearchEditState.Edit) {
                stateFlow.value = SearchEditState.Search(link.url)
                true
            } else
                false
        }
    }

    private fun formatUrl(url: String): String {
        if (url.startsWith("https://") || url.startsWith("http://"))
            return url

        return "http://$url"
    }
}