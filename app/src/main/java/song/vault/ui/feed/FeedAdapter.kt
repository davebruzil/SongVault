package song.vault.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.data.local.entity.PostEntity
import song.vault.databinding.ItemPostBinding

class FeedAdapter(
    private val onPostClick: (PostEntity) -> Unit,
    private val onEditClick: (PostEntity) -> Unit,
    private val onDeleteClick: (PostEntity) -> Unit,
    private val currentUserId: String
) : ListAdapter<PostEntity, FeedAdapter.PostViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            with(binding) {
                if (!post.musicThumbnailUrl.isNullOrBlank()) {
                    Picasso.get()
                        .load(post.musicThumbnailUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(musicThumbnail)
                } else {
                    musicThumbnail.setImageResource(R.drawable.ic_launcher_foreground)
                }

                musicTitle.text = post.musicTitle
                musicArtist.text = post.musicArtist
                userName.text = post.userDisplayName ?: "Unknown User"
                genreChip.text = post.genre ?: "No Genre"

                if (post.caption.isNullOrBlank()) {
                    caption.text = "No description"
                } else {
                    caption.text = post.caption
                }

                val isOwnPost = currentUserId == post.userId
                editButton.visibility = if (isOwnPost) android.view.View.VISIBLE else android.view.View.GONE
                deleteButton.visibility = if (isOwnPost) android.view.View.VISIBLE else android.view.View.GONE

                root.setOnClickListener { onPostClick(post) }
                editButton.setOnClickListener { onEditClick(post) }
                deleteButton.setOnClickListener { onDeleteClick(post) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem == newItem
        }
    }
}
