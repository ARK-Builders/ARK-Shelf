package space.taran.arkshelf.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import space.taran.arkshelf.BuildConfig
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.ActivityMainBinding
import space.taran.arkshelf.domain.NoInternetException
import space.taran.arkshelf.presentation.folderpicker.FolderPickerDialogFragment
import space.taran.arkshelf.presentation.hideKeyboard
import java.net.UnknownHostException

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by viewBinding(ActivityMainBinding::bind)
    private val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        initResultListener()
        checkPerms()
        observeViewModel()
        handleShareIntent()
    }

    private fun initUI() = with(binding) {
        inputUrl.editText!!.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    inputUrl.clearFocus()
                    viewModel.onUrlPicked(inputUrl.editText?.text.toString())
                }
                true
            }
        }
        btnGenerate.setOnClickListener {
            val folderPicker = FolderPickerDialogFragment()
            folderPicker.show(
                supportFragmentManager,
                FolderPickerDialogFragment.TAG
            )
        }
    }

    private fun observeViewModel() = lifecycleScope.launch {
        viewModel.stateFlow.collect { state ->
            when (state) {
                is MainState.Search -> {
                    binding.inputUrl.editText!!.setText(state.url)
                    state.inputError?.let { handleException(it) }
                        ?: let { binding.inputUrl.error = null }
                    binding.root.transitionToStart()
                }
                is MainState.Edit -> {
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

    private fun handleException(exception: Throwable) = when (exception) {
        is UnknownHostException -> {
            binding.inputUrl.error = getString(R.string.unknown_host)
        }
        is NoInternetException -> {
            binding.inputUrl.error = getString(R.string.no_internet)
        }
        else -> {
            binding.inputUrl.error = getString(R.string.invalid_url)
        }
    }

    private fun handleShareIntent() {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                viewModel.handleShareIntent(url)
            }
        }
    }

    private fun initResultListener() = supportFragmentManager
        .setFragmentResultListener(
            FolderPickerDialogFragment.REQUEST_KEY_PATH_PICKED,
            this
        ) { _, _ ->
            viewModel.onSavePathPicked(
                binding.inputTitle.editText!!.text.toString(),
                binding.inputDesc.editText!!.text.toString()
            )
        }

    override fun onBackPressed() {
        if (!viewModel.onBackClick())
            finish()
    }

    private suspend fun loadImage(state: MainState.Edit) =
        withContext(Dispatchers.IO) {
            if (state.link.imagePath == null)
                return@withContext

            val bitmap = Glide.with(this@MainActivity)
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

    private fun checkPerms() {
        if (!isWritePermGranted())
            askWritePerm()
    }

    private fun isWritePermGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun askWritePerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val packageUri =
                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    packageUri
                )
            startActivityForResult(intent, REQUEST_CODE_ALL_FILES_ACCESS)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_ALL_FILES_ACCESS = 1
        private const val REQUEST_CODE_PERMISSIONS = 2
    }
}

