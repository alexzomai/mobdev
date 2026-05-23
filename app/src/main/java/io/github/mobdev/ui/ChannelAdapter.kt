package io.github.mobdev.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.databinding.ItemChannelBinding

class ChannelAdapter(
    selectedChannel: String?,
    private val onClick: (String) -> Unit,
) : ListAdapter<String, ChannelAdapter.ViewHolder>(DIFF) {

    private var selected: String? = selectedChannel

    fun setSelected(channel: String?) {
        val old = selected
        selected = channel
        if (old != channel) {
            val oldIdx = currentList.indexOf(old)
            val newIdx = currentList.indexOf(channel)
            if (oldIdx >= 0) notifyItemChanged(oldIdx)
            if (newIdx >= 0) notifyItemChanged(newIdx)
        }
    }

    inner class ViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String) {
            binding.textChannelName.text = name
            binding.root.isSelected = name == selected
            binding.root.setOnClickListener { onClick(name) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }
}
