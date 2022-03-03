package space.taran.arkshelf.presentation.folderpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import space.taran.arkshelf.domain.UserPreferences
import space.taran.arkshelf.presentation.listChildren
import space.taran.arkshelf.presentation.listDevices
import java.nio.file.Path
import kotlin.io.path.isDirectory

data class FolderPickerState(
    val devices: List<Path>,
    val selectedDevicePos: Int,
    val currentPath: Path,
    val files: List<Path>
)

class FolderPickerViewModel(private val preferences: UserPreferences) :
    ViewModel() {

    val stateFlow = MutableStateFlow(initialState())

    fun onDeviceSelected(selectedDevicePos: Int) {
        val selectedDevice = stateFlow.value.devices[selectedDevicePos]
        if (stateFlow.value.currentPath.startsWith(selectedDevice))
            return
        stateFlow.value = stateFlow.value.copy(
            selectedDevicePos = selectedDevicePos,
            currentPath = selectedDevice,
            files = formatChildren(selectedDevice)
        )
    }

    fun onPathPicked() {
        preferences.setLastSavePath(stateFlow.value.currentPath)
    }

    fun onItemClick(path: Path) = viewModelScope.launch {
        if (!path.isDirectory())
            return@launch
        stateFlow.value =
            stateFlow.value.copy(currentPath = path, files = formatChildren(path))
    }

    private fun initialState(): FolderPickerState {
        val devices = listDevices()
        val currentPath = preferences.getLastSavePath() ?: devices[0]
        val selectedDevice =
            devices.find { currentPath.startsWith(it) } ?: devices[0]
        val selectedDevicePos = devices.indexOf(selectedDevice)
        return FolderPickerState(
            devices,
            selectedDevicePos,
            currentPath,
            formatChildren(currentPath)
        )
    }

    private fun formatChildren(path: Path): List<Path> {
        val (dirs, files) = path.listChildren().partition {
            it.isDirectory()
        }

        val children = mutableListOf<Path>()
        children.addAll(dirs.sorted())
        children.addAll(files.sorted())

        return children
    }

    fun onBackClick(): Boolean {
        val state = stateFlow.value
        val isDevice = state.devices.any { device -> device == state.currentPath }
        if (isDevice)
            return false
        val parent = stateFlow.value.currentPath.parent

        stateFlow.value =
            stateFlow.value.copy(
                currentPath = parent,
                files = formatChildren(parent)
            )
        return true
    }
}