package space.taran.arkshelf.presentation.folderpicker

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.FragmentFolderPickerBinding
import space.taran.arkshelf.presentation.INTERNAL_STORAGE
import kotlin.io.path.name

class FolderPickerDialogFragment : DialogFragment() {

    private val binding by viewBinding(FragmentFolderPickerBinding::bind)
    private val viewModel: FolderPickerViewModel by inject()
    private var filesAdapter: FoldersRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_folder_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initBackButtonListener()
        observeState()
    }

    private fun initUI() = binding.apply {
        rvRootsDialog.layoutManager = LinearLayoutManager(requireContext())
        filesAdapter = FoldersRVAdapter(viewModel)
        rvRootsDialog.adapter = filesAdapter

        btnCancel.setOnClickListener {
            dismiss()
        }
        btnPick.setOnClickListener {
            viewModel.onPathPicked()
            setFragmentResult(REQUEST_KEY_PATH_PICKED, bundleOf())
            dismiss()
        }
    }

    private fun observeState() = lifecycleScope.launch {
        viewModel.stateFlow.collect { state ->
            val menuItems = state.devices.map {
                if (it == INTERNAL_STORAGE)
                    getString(R.string.internal_storage)
                else
                    it.name
            }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                menuItems
            )
            binding.tvCurrentPath.requestLayout()
            binding.tvCurrentPath.post {
                binding.scrollPath.fullScroll(ScrollView.FOCUS_RIGHT)
            }
            binding.tvMenu.doAfterTextChanged {}
            binding.tvMenu.setText(menuItems[state.selectedDevicePos])
            binding.tvMenu.setAdapter(adapter)
            binding.tvMenu.dismissDropDown()
            binding.tvMenu.doAfterTextChanged {
                val pos = menuItems.indexOf(it.toString())
                viewModel.onDeviceSelected(pos)
            }

            binding.tvCurrentPath.text = state.currentPath.toString()

            filesAdapter?.files = state.files
            filesAdapter?.notifyDataSetChanged()
        }
    }

    private fun initBackButtonListener() {
        requireDialog().setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                keyEvent.action == KeyEvent.ACTION_UP
            ) {
                if (!viewModel.onBackClick())
                    dismiss()
            }
            return@setOnKeyListener true
        }
    }

    companion object {
        const val TAG = "folderPicker"
        const val REQUEST_KEY_PATH_PICKED = "pathPickedFolderPicker"
    }
}