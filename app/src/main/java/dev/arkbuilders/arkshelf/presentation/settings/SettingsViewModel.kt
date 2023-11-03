package dev.arkbuilders.arkshelf.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import dev.arkbuilders.arkshelf.domain.UserPreferences
import java.nio.file.Path
import javax.inject.Inject

data class SettingsState(val linkFolder: String?)

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    val stateFlow: MutableStateFlow<SettingsState> =
        MutableStateFlow(SettingsState(userPreferences.getLinkFolder()?.toString()))

    fun onLinkFolderPathChanged(path: Path) {
        userPreferences.setLinkFolder(path)
        stateFlow.value = SettingsState(userPreferences.getLinkFolder()?.toString())
    }
}

class SettingsViewModelFactory @Inject constructor(
    private val userPreferences: UserPreferences
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(userPreferences) as T
    }
}