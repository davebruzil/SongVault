package song.vault.ui.myposts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.data.local.entity.PostEntity
import song.vault.databinding.ItemMyPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsAdapter(
    private val onDeleteClick: (PostEntity) -> Unit
) : ListAdapter<PostEntity, MyPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemMyPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            binding.tvMusicTitle.text = post.musicTitle
            binding.tvMusicArtist.text = post.musicArtist
            binding.tvCaption.text = post.caption ?: ""
            binding.tvGenre.text = post.genre ?: "No genre"

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(post.createdAt))

            post.musicThumbnailUrl?.let { url ->
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(binding.ivThumbnail)
            } ?: run {
                binding.ivThumbnail.setImageResource(R.drawable.ic_music_placeholder)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(post)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem == newItem
        }
    }
}
