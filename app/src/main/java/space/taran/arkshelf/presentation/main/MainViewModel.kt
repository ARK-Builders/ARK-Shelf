package space.taran.arkshelf.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import space.taran.arkshelf.domain.Link
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences

sealed class MainState {
    data class Search(val url: String, val inputError: Throwable? = null) :
        MainState()

    data class Edit(val link: Link) : MainState()
}

class MainViewModel(
    private val linkRepo: LinkRepo,
    private val preferences: UserPreferences
) : ViewModel() {
    val stateFlow: MutableStateFlow<MainState> =
        MutableStateFlow(MainState.Search("", null))

    fun onUrlPicked(url: String) = viewModelScope.launch {
        val result = linkRepo.parse(formatUrl(url))
        result.onSuccess { link ->
            stateFlow.value = MainState.Edit(link)
        }
        result.onFailure { exception ->
            stateFlow.value = MainState.Search(url, exception)
        }
    }

    fun onSavePathPicked(title: String, desc: String) = viewModelScope.launch {
        val link = (stateFlow.value as MainState.Edit).link
        link.title = title
        link.desc = desc
        linkRepo.generateFile(link, preferences.getLastSavePath()!!)
        stateFlow.value = MainState.Search("")
    }

    fun handleShareIntent(url: String) {
        stateFlow.value = MainState.Search(url)
        onUrlPicked(url)
    }

    fun onBackClick(): Boolean {
        with(stateFlow.value) {
            return if (this is MainState.Edit) {
                stateFlow.value = MainState.Search(link.url)
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