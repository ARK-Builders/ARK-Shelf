package space.taran.arkshelf.presentation.searchedit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.coroutines.CoroutineScope
import space.taran.arkshelf.databinding.ItemLinkBinding
import space.taran.arkshelf.domain.Link

class LinkListAdapter(
    private val context: Context,
) : PagingDataAdapter<LinkItemModel, LinkViewHolder>(LinkDiffUtilCallback) {

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)!!
        if (getItemViewType(position) == VIEW_TYPE_WITH_IMAGE)
            reset(model, binding)

        val link = model.link
        binding.tvTitle.text = link.title
        binding.tvUrl.text = link.url
        binding.tvDesc.text = link.desc

        setClickListeners(holder, model, binding, link)

        Glide
            .with(binding.ivPreview)
            .load(link.imagePath?.toFile())
            .override(PREVIEW_SIZE)
            .into(binding.ivPreview)

        Glide
            .with(binding.ivThumbnail)
            .load(link.imagePath?.toFile())
            .override(THUMBNAIL_SIZE)
            .transform(CenterCrop(), RoundedCorners(8))
            .into(binding.ivThumbnail)
    }

    override fun getItemViewType(position: Int): Int {
        val link = getItem(position)!!.link
        return if (link.imagePath == null)
            VIEW_TYPE_WITHOUT_IMAGE
        else
            VIEW_TYPE_WITH_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LinkViewHolder(
            ItemLinkBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ).also {
            it.binding.apply {
                it.setIsRecyclable(true)
                if (motionItem.progress != 0f && motionItem.progress != 1f)
                    return@apply

                if (viewType == VIEW_TYPE_WITH_IMAGE) {
                    motionItem.progress = 0f
                    root.post { root.requestLayout() }
                }

                if (viewType == VIEW_TYPE_WITHOUT_IMAGE) {
                    motionItem.progress = 1f
                    root.post { root.requestLayout() }
                }
            }
        }

    private fun reset(
        model: LinkItemModel,
        binding: ItemLinkBinding
    ) = with(binding) {
        motionItem.progress = 0f
        model.isExpanded = false
        root.post { root.requestLayout() }
    }

    private fun setClickListeners(
        holder: LinkViewHolder,
        model: LinkItemModel,
        binding: ItemLinkBinding,
        link: Link,
    ) = with(binding) {
        if (link.imagePath != null) {
            motionItem.setOnClickListener {
                if (motionItem.progress != 0f && motionItem.progress != 1f)
                    return@setOnClickListener

                if (model.isExpanded) {
                    motionItem.progress = 1f
                    motionItem.transitionToStart()
                    holder.setIsRecyclable(true)
                } else {
                    motionItem.progress = 0f
                    motionItem.transitionToEnd()
                    holder.setIsRecyclable(false)
                }
                model.isExpanded = !model.isExpanded
            }
        } else {
            motionItem.setOnClickListener {}
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
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
    }

    companion object {
        const val VIEW_TYPE_WITHOUT_IMAGE = 1
        const val VIEW_TYPE_WITH_IMAGE = 2
        private const val PREVIEW_SIZE = 600
        private const val THUMBNAIL_SIZE = 200
    }
}

class LinkViewHolder(val binding: ItemLinkBinding) :
    RecyclerView.ViewHolder(binding.root)

private object LinkDiffUtilCallback: DiffUtil.ItemCallback<LinkItemModel>() {
    override fun areItemsTheSame(
        oldItem: LinkItemModel,
        newItem: LinkItemModel
    ): Boolean {
        return oldItem.link.url == newItem.link.url
    }

    override fun areContentsTheSame(
        oldItem: LinkItemModel,
        newItem: LinkItemModel
    ): Boolean {
        return oldItem == newItem
    }
}