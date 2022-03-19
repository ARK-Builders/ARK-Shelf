package space.taran.arkshelf.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import space.taran.arkshelf.domain.UserPreferences

data class SettingsState(val linkFolder: String?)

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    val stateFlow: MutableStateFlow<SettingsState> =
        MutableStateFlow(SettingsState(userPreferences.getLinkFolder()?.toString()))

    fun onLinkFolderPathChanged() {
        stateFlow.value = SettingsState(userPreferences.getLinkFolder()?.toString())
    }
}