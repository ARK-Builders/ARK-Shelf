package space.taran.arkshelf.presentation.searchedit

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.net.ProtocolException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.FragmentSearchEditBinding
import space.taran.arkshelf.domain.NoInternetException
import space.taran.arkshelf.presentation.askWritePermissions
import space.taran.arkshelf.presentation.hideKeyboard
import space.taran.arkshelf.presentation.isWritePermGranted
import space.taran.arkshelf.presentation.main.MainActivity

class SearchEditFragment : Fragment(R.layout.fragment_search_edit) {
    private val binding by viewBinding(FragmentSearchEditBinding::bind)
    private val viewModel: SearchEditViewModel by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initResultListener()
        checkWritePermissions()
        observeViewModel()
        handleShareIntent()
    }

    private fun initUI() = with(binding) {
        inputUrl.editText!!.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    inputUrl.clearFocus()
                    viewModel.onUrlPicked(
                        inputUrl.editText?.text.toString(),
                        isExtraUrl = false
                    )
                }
                true
            }
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
            if (!viewModel.onBackClick()) {
                remove()
                requireActivity().onBackPressed()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                when (state) {
                    is SearchEditState.Search -> {
                        binding.inputUrl.editText!!.setText(state.url)
                        state.inputError?.let { handleException(it) }
                            ?: let { binding.inputUrl.error = null }
                        binding.root.transitionToStart()
                    }
                    is SearchEditState.Edit -> {
                        binding.tvUrl.text = state.link.url
                        binding.inputTitle.editText!!.setText(state.link.title)
                        binding.inputDesc.editText!!.setText(state.link.desc)
                        binding.ivPreview.setImageDrawable(null)
                        loadImage(state)
                        binding.root.transitionToEnd()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.actionsFlow.collect { action ->
                when (action) {
                    is SearchEditAction.AskLinkFolder -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.specify_link_folder),
                            Toast.LENGTH_SHORT
                        ).show()
                        (requireActivity() as MainActivity).navigateToSettings()
                    }
                    SearchEditAction.CloseApp -> {
                        requireActivity().finish()
                    }
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
        setFragmentResultListener(REQUEST_SHARE_URL_KEY) { _, bundle ->
            bundle.getString(URL_KEY)?.let { url ->
                viewModel.handleShareIntent(url)
            }
        }
    }

    private suspend fun loadImage(state: SearchEditState.Edit) =
        withContext(Dispatchers.IO) {
            if (state.link.imagePath == null)
                return@withContext

            val bitmap = Glide.with(this@SearchEditFragment)
                .asBitmap()
                .load(state.link.imagePath.toFile())
                .override(1000)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit()
                .get()

            withContext(Dispatchers.Main) {
                binding.ivPreview.setImageBitmap(bitmap)
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

