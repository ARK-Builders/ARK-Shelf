package space.taran.arkshelf.presentation.searchedit

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.viewmodel.observe
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import space.taran.arkfilepicker.presentation.onArkPathPicked
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.FragmentSearchEditBinding
import space.taran.arkshelf.di.DIManager
import space.taran.arkshelf.domain.NoInternetException
import space.taran.arkshelf.presentation.askWritePermissions
import space.taran.arkshelf.presentation.hideKeyboard
import space.taran.arkshelf.presentation.isWritePermGranted
import space.taran.arkshelf.presentation.main.MainActivity
import java.net.UnknownHostException
import javax.inject.Inject

class SearchEditFragment : Fragment(R.layout.fragment_search_edit) {
    private val binding by viewBinding(FragmentSearchEditBinding::bind)

    @Inject
    lateinit var factory: SearchEditViewModelFactory.Factory
    private val viewModel: SearchEditViewModel by viewModels {
        factory.create(arguments?.getString(URL_KEY))
    }

    lateinit var linkListAdapter: LinkListAdapter

    override fun onAttach(context: Context) {
        DIManager.component.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initResultListener()
        checkWritePermissions()
        viewModel.observe(this, ::render, ::handleSideEffect)
        handleShareIntent()
    }

    private fun initUI() = with(binding) {
        linkListAdapter = LinkListAdapter(requireContext())
        rvLatest.adapter = linkListAdapter
        rvLatest.loadSkeleton(R.layout.item_link_skeleton) {
            itemCount(16)
        }
        rvLatest.itemAnimator = null
        root.setTransition(R.id.motion_search, R.id.motion_latest)
        inputUrl.editText!!.apply {
            doAfterTextChanged {
                viewModel.onInputChanged(it.toString())
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    inputUrl.clearFocus()
                    viewModel.onUrlPicked(
                        inputUrl.editText?.text.toString()
                    )
                }
                true
            }
        }
        btnFolder.setOnClickListener {
            val config = ArkFilePickerConfig(
                mode = ArkFilePickerMode.FOLDER,
                titleStringId = R.string.pick_link_folder,
                showRoots = true
            )
            ArkFilePickerFragment
                .newInstance(config)
                .show(parentFragmentManager, null)
        }
        btnGenerate.setOnClickListener {
            if (!isWritePermGranted()) {
                askWritePermissions()
                return@setOnClickListener
            }

            viewModel.onSaveBtnClick(
                inputTitle.editText!!.text.toString(),
                inputDesc.editText!!.text.toString()
            )
        }
        btnSettings.setOnClickListener {
            (requireActivity() as MainActivity).navigateToSettings()
        }
        requireActivity().onBackPressedDispatcher.addCallback(this@SearchEditFragment) {
            viewModel.onBackClick()
        }
    }

    private fun render(state: SearchEditState) = lifecycleScope.launch {
        state.linkFolder?.let {
            binding.btnFolder.text = it.fileName.toString()
            binding.tvLatestLinks.isVisible = true
        } ?: let {
            binding.tvLatestLinks.isVisible = false
            binding.btnFolder.text = getString(R.string.pick_link_folder)
        }
        state.links?.let {
            linkListAdapter.setLinks(it)
            binding.rvLatest.hideSkeleton()
        }

        when (state.screen) {
            SearchEditScreen.SEARCH -> {
                if (binding.inputUrl.editText!!.text.toString() != state.url)
                    binding.inputUrl.editText!!.setText(state.url)
                state.inputError?.let { handleException(it) }
                    ?: let { binding.inputUrl.error = null }
                if (binding.root.currentState == R.id.motion_edit) {
                    binding.root.setTransition(R.id.motion_edit, R.id.motion_search)
                    binding.root.transitionToEnd {
                        binding.root.setTransition(R.id.motion_search, R.id.motion_latest)
                        binding.root.transitionToEnd()
                    }
                }
            }
            SearchEditScreen.EDIT -> {
                binding.tvUrl.text = state.link!!.url
                binding.inputTitle.editText!!.setText(state.link.title)
                binding.inputDesc.editText!!.setText(state.link.desc)
                binding.ivPreviewEdit.setImageDrawable(null)
                loadImage(state)
                binding.root.transitionToState(R.id.motion_edit)
            }
        }
    }

    private fun handleSideEffect(effect: SearchEditSideEffect) {
        when (effect) {
            SearchEditSideEffect.AskLinkFolder -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.specify_link_folder),
                    Toast.LENGTH_SHORT
                ).show()
                (requireActivity() as MainActivity).navigateToSettings()
            }
            SearchEditSideEffect.CloseApp -> {
                requireActivity().finish()
            }
            SearchEditSideEffect.LinkFolderChanged -> {
                binding.rvLatest.loadSkeleton(R.layout.item_link_skeleton) {
                    itemCount(16)
                }
            }
        }
    }

    private fun handleException(exception: Throwable) = when (exception) {
        is UnknownHostException -> {
            binding.inputUrl.error = getString(R.string.unknown_host)
        }
        is NoInternetException -> {
            binding.inputUrl.error = getString(R.string.no_internet)
        }
        else -> {
            binding.inputUrl.error = exception.message
        }
    }

    private fun handleShareIntent() {
        arguments?.getString(URL_KEY)?.let { url ->
            viewModel.handleShareIntent(url)
        }
    }

    private fun initResultListener() {
        parentFragmentManager.onArkPathPicked(lifecycleOwner = this) { path ->
            viewModel.onLinkFolderChanged(path)
        }
        setFragmentResultListener(REQUEST_SHARE_URL_KEY) { _, bundle ->
            bundle.getString(URL_KEY)?.let { url ->
                viewModel.handleShareIntent(url)
            }
        }
    }

    private suspend fun loadImage(state: SearchEditState) =
        withContext(Dispatchers.IO) {
            if (state.link!!.imagePath == null)
                return@withContext

            val bitmap = Glide.with(this@SearchEditFragment)
                .asBitmap()
                .load(state.link.imagePath!!.toFile())
                .override(1000)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit()
                .get()

            withContext(Dispatchers.Main) {
                binding.ivPreviewEdit.setImageBitmap(bitmap)
            }
        }

    private fun checkWritePermissions() {
        if (!isWritePermGranted())
            askWritePermissions()
    }

    companion object {
        const val REQUEST_SHARE_URL_KEY = "shareUrl"
        const val TAG = "searchEdit"
        const val URL_KEY = "url"

        fun newInstance(url: String? = null) = SearchEditFragment().apply {
            url?.let {
                arguments = Bundle().apply {
                    putString(URL_KEY, url)
                }
            }
        }
    }
}
