package io.github.mobdev.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.data.LOCAL_ID_PREFIX
import io.github.mobdev.data.Message
import io.github.mobdev.data.MessageData
import io.github.mobdev.databinding.ItemMessageImageBinding
import io.github.mobdev.databinding.ItemMessageTextBinding
import io.github.mobdev.network.ApiClient

class MessagesAdapter(
    private val onImageClick: (String) -> Unit,
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    inner class TextViewHolder(private val binding: ItemMessageTextBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isPending = message.id.startsWith(LOCAL_ID_PREFIX)
            binding.textFrom.text = message.from
            binding.textBody.text = (message.data as MessageData.Text).text
            val visibility = if (isPending) android.view.View.VISIBLE else android.view.View.GONE
            binding.pendingBar.visibility = visibility
            binding.textPending.visibility = visibility
            binding.root.setBackgroundColor(
                if (isPending) 0x1FF44336.toInt() else android.graphics.Color.TRANSPARENT
            )
        }
    }

    inner class ImageViewHolder(private val binding: ItemMessageImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textFrom.text = message.from
            val link = (message.data as MessageData.Image).link
            binding.imageThumb.load("${ApiClient.BASE_IMAGE_URL}thumb/$link")
            binding.imageThumb.setOnClickListener { onImageClick(link) }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).data is MessageData.Text) TYPE_TEXT else TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TEXT) {
            TextViewHolder(ItemMessageTextBinding.inflate(inflater, parent, false))
        } else {
            ImageViewHolder(ItemMessageImageBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TextViewHolder -> holder.bind(getItem(position))
            is ImageViewHolder -> holder.bind(getItem(position))
        }
    }

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_IMAGE = 1

        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message) =
                oldItem == newItem
        }
    }
}
