package space.taran.arkshelf.presentation.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.FragmentSettingsBinding
import space.taran.arkshelf.presentation.folderpicker.FolderPickerDialogFragment

class SettingsFragment: Fragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)
    private val viewModel: SettingsViewModel by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initResultListener()
        observeViewModel()
    }

    private fun initUI() = with(binding) {
        btnChangePath.setOnClickListener {
            val folderPicker = FolderPickerDialogFragment()
            folderPicker.show(
                parentFragmentManager,
                FolderPickerDialogFragment.TAG
            )
        }
        ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun initResultListener() {
        setFragmentResultListener(
            FolderPickerDialogFragment.REQUEST_KEY_PATH_PICKED
        ) { _, _ ->
            viewModel.onLinkFolderPathChanged()
        }
    }

    private fun observeViewModel() = lifecycleScope.launch {
        viewModel.stateFlow.collect {
            binding.tvLinkFolderPath.text = it.linkFolder
        }
    }
}