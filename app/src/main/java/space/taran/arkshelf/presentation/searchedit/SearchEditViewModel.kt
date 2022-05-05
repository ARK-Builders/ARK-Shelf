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

    data class Edit(val link: Link, val isExtraUrl: Boolean) : SearchEditState()
}

sealed class SearchEditAction {
    object AskLinkFolder: SearchEditAction()
    object CloseApp: SearchEditAction()
}

class SearchEditViewModel(
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences
) : ViewModel() {
    val stateFlow: MutableStateFlow<SearchEditState> =
        MutableStateFlow(SearchEditState.Search("", null))
    val actionsFlow: MutableSharedFlow<SearchEditAction> = MutableSharedFlow()

    fun onUrlPicked(url: String, isExtraUrl: Boolean) = viewModelScope.launch {
        val result = linkRepo.parse(formatUrl(url))
        result.onSuccess { link ->
            stateFlow.value = SearchEditState.Edit(link, isExtraUrl)
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
        val state = stateFlow.value as SearchEditState.Edit
        state.link.title = title
        state.link.desc = desc
        linkRepo.generateFile(state.link, preferences.getLinkFolder()!!)
        if (state.isExtraUrl)
            actionsFlow.emit(SearchEditAction.CloseApp)
        else
            stateFlow.value = SearchEditState.Search("")
    }

    fun handleShareIntent(url: String) {
        stateFlow.value = SearchEditState.Search(url)
        onUrlPicked(url, isExtraUrl = true)
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