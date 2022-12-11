package space.taran.arkshelf.presentation.searchedit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import space.taran.arkshelf.databinding.ItemProgressBinding

class LoaderAdapter : LoadStateAdapter<LoaderViewHolder>() {
    override fun onBindViewHolder(holder: LoaderViewHolder, loadState: LoadState) {
        holder.binding.progressCircular.isVisible = loadState is LoadState.Loading
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ) = LoaderViewHolder(
        ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )
}

class LoaderViewHolder(val binding: ItemProgressBinding) :
    RecyclerView.ViewHolder(binding.root)