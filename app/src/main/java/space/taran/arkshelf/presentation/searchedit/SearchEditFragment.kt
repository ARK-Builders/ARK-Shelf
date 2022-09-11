package space.taran.arkshelf.presentation.searchedit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.viewmodel.observe
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.ArkFilePickerMode
import space.taran.arkfilepicker.onArkPathPicked
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.FragmentSearchEditBinding
import space.taran.arkshelf.databinding.ItemLinkBinding
import space.taran.arkshelf.databinding.ItemProgressBinding
import space.taran.arkshelf.di.DIManager
import space.taran.arkshelf.domain.NoInternetException
import space.taran.arkshelf.presentation.RelaxedTransitionListener
import space.taran.arkshelf.presentation.askWritePermissions
import space.taran.arkshelf.presentation.hideKeyboard
import space.taran.arkshelf.presentation.isWritePermGranted
import space.taran.arkshelf.presentation.launchWithMutex
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

    private val linksAdapter = ItemAdapter<LinkItem>()
    private val footerProgressAdapter = ItemAdapter<ProgressItem>()
    private val loadMoreMutex = Mutex()

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
        val fastAdapter = FastAdapter.with(listOf(linksAdapter, footerProgressAdapter))
        rvLatest.adapter = fastAdapter
        rvLatest.loadSkeleton(R.layout.item_link_skeleton) {
            itemCount(16)
        }
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
        val loadMoreListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                lifecycleScope.launchWithMutex(loadMoreMutex) {
                    val progressItem = ProgressItem()
                    footerProgressAdapter.add(progressItem)
                    viewModel.onLoadMore()
                    footerProgressAdapter.clear()
                }
            }
        }
        rvLatest.addOnScrollListener(loadMoreListener)
    }

    private fun render(state: SearchEditState) = lifecycleScope.launch {
        state.linkFolder?.let {
            binding.btnFolder.text = it.fileName.toString()
            binding.tvLatestLinks.isVisible = true
        } ?: let {
            binding.tvLatestLinks.isVisible = false
            binding.btnFolder.text = getString(R.string.pick_link_folder)
        }

        when (state.screen) {
            SearchEditScreen.SEARCH -> {
                if (binding.inputUrl.editText!!.text.toString() != state.url)
                    binding.inputUrl.editText!!.setText(state.url)
                state.inputError?.let { handleException(it) }
                    ?: let { binding.inputUrl.error = null }
                state.latestLinks?.let {
                    binding.rvLatest.hideSkeleton()
                    FastAdapterDiffUtil[linksAdapter] =
                        state.latestLinks.map { LinkItem(requireContext(), it) }
                }
                if (binding.root.currentState == R.id.motion_edit) {
                    binding.root.setTransition(R.id.motion_edit, R.id.motion_search)
                    binding.root.transitionToEnd {
                        binding.root.setTransition(
                            R.id.motion_search,
                            R.id.motion_latest
                        )
                        binding.root.transitionToEnd {}
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
                linksAdapter.setNewList(emptyList())
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
        parentFragmentManager.onArkPathPicked(lifecycleOwner = this) { path->
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

private class LinkItem(
    private val context: Context,
    private val model: LinkItemModel
) : AbstractBindingItem<ItemLinkBinding>() {
    override var identifier: Long
        get() = model.link.hashCode().toLong()
        set(value) {}
    override val type = 0

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemLinkBinding.inflate(inflater, parent, false).apply {
        motionItem.setTransitionListener(RelaxedTransitionListener(
            onTransitionCompleted = {
                motionItem.post {
                    motionItem.requestLayout()
                }
            }
        ))
    }

    override fun bindView(
        binding: ItemLinkBinding,
        payloads: List<Any>
    ) = with(binding) {
        reset(model.isExpanded, binding)

        val link = model.link
        tvTitle.text = link.title
        tvUrl.text = link.url
        tvDesc.text = link.desc

        binding.motionItem.setOnClickListener {
            if (motionItem.progress != 0f && motionItem.progress != 1f)
                return@setOnClickListener

            if (model.isExpanded)
                motionItem.transitionToStart()
            else
                motionItem.transitionToEnd()
            model.isExpanded = !model.isExpanded
        }

        btnOpen.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(link.url)
            context.startActivity(
                Intent.createChooser(intent, "View the link with:")
            )
        }
        btnCopy.setOnClickListener {
            val clipboard =
                context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", link.url)
            clipboard.setPrimaryClip(clip)
        }
        btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, link.url)
            intent.type = "text/plain"

            context.startActivity(
                Intent.createChooser(intent, "Share the link with:")
            )
        }

        Glide
            .with(ivPreview)
            .load(link.imagePath?.toFile())
            .override(PREVIEW_SIZE)
            .into(ivPreview)

        Glide
            .with(ivThumbnail)
            .load(link.imagePath?.toFile())
            .override(THUMBNAIL_SIZE)
            .transform(CenterCrop(), RoundedCorners(8))
            .into(ivThumbnail)

        return@with
    }

    private fun reset(
        isExpanded: Boolean,
        binding: ItemLinkBinding
    ) = with(binding) {
        motionItem.progress = if (isExpanded) 1f else 0f
        root.post { root.requestLayout() }
    }

    companion object {
        private const val PREVIEW_SIZE = 600
        private const val THUMBNAIL_SIZE = 200
    }
}

private class ProgressItem: AbstractBindingItem<ItemProgressBinding>() {
    override val type = 1
    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemProgressBinding.inflate(inflater, parent, false)
}