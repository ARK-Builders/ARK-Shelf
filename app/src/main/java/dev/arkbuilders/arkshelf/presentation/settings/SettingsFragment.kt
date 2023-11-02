package dev.arkbuilders.arkshelf.presentation.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.arkbuilders.arkfilepicker.ArkFilePickerConfig
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import dev.arkbuilders.arkfilepicker.presentation.onArkPathPicked
import kotlinx.coroutines.launch
import dev.arkbuilders.arkshelf.R
import dev.arkbuilders.arkshelf.databinding.FragmentSettingsBinding
import dev.arkbuilders.arkshelf.di.DIManager
import javax.inject.Inject

class SettingsFragment: Fragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)

    @Inject
    lateinit var factory: SettingsViewModelFactory
    private val viewModel: SettingsViewModel by viewModels {
        factory
    }

    override fun onAttach(context: Context) {
        DIManager.component.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initResultListener()
        observeViewModel()
    }

    private fun initUI() = with(binding) {
        btnChangePath.setOnClickListener {
            val config = ArkFilePickerConfig(
                mode = ArkFilePickerMode.FOLDER,
                titleStringId = R.string.pick_link_folder,
                showRoots = true
            )
            ArkFilePickerFragment
                .newInstance(config)
                .show(parentFragmentManager, null)
        }
        ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun initResultListener() {
        parentFragmentManager.onArkPathPicked(lifecycleOwner = this) { path->
            viewModel.onLinkFolderPathChanged(path)
        }
    }

    private fun observeViewModel() = lifecycleScope.launch {
        viewModel.stateFlow.collect {
            binding.tvLinkFolderPath.text = it.linkFolder
        }
    }
}