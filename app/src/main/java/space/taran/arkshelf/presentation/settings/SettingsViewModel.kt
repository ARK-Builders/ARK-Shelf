package space.taran.arkshelf.presentation.settings

import androidx.lifecycle.ViewModel
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import space.taran.arkshelf.domain.UserPreferences

data class SettingsState(val linkFolder: String?)

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    val stateFlow: MutableStateFlow<SettingsState> =
        MutableStateFlow(SettingsState(userPreferences.getLinkFolder()?.toString()))

    fun onLinkFolderPathChanged(path: Path) {
        userPreferences.setLinkFolder(path)
        stateFlow.value = SettingsState(userPreferences.getLinkFolder()?.toString())
    }
}