package space.taran.arkshelf.presentation.folderpicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.ItemFolderBinding
import space.taran.arkshelf.presentation.formatSize
import space.taran.arkshelf.presentation.iconForExtension
import space.taran.arkshelf.presentation.listChildren
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class FoldersRVAdapter(
    private val viewModel: FolderPickerViewModel
) : RecyclerView.Adapter<FolderViewHolder>() {
    var files = listOf<Path>()

    override fun getItemCount(): Int = files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FolderViewHolder(
            ItemFolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val file = files[position]

        holder.bind(file)

        holder.itemView.setOnClickListener {
            viewModel.onItemClick(file)
        }
    }
}

class FolderViewHolder(val binding: ItemFolderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(path: Path) {
        val context = binding.root.context
        binding.tvName.text = path.name
        if (path.isDirectory()) {
            val childrenCount = path.listChildren().size
            binding.tvDetails.text = context.resources.getQuantityString(
                R.plurals.items,
                childrenCount,
                childrenCount
            )
            Glide.with(binding.iv).clear(binding.iv)
            binding.iv.setImageResource(R.drawable.ic_folder)
        } else {
            binding.tvDetails.text = path.fileSize().formatSize()
            Glide.with(binding.iv)
                .load(path.toFile())
                .override(500)
                .placeholder(iconForExtension(path.extension.lowercase()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.iv)
        }
    }
}
