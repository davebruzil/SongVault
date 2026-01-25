package song.vault.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.data.local.relation.PostWithUser
import song.vault.databinding.ItemPostCardBinding

class PostAdapter(
    private val onPostClick: (String) -> Unit = {}  // postId
) : ListAdapter<PostWithUser, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onPostClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: ItemPostCardBinding,
        private val onPostClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(postWithUser: PostWithUser) {
            val post = postWithUser.post
            val user = postWithUser.user

            // Load thumbnail
            Picasso.get()
                .load(post.musicThumbnailUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.ivMusicThumbnail)

            // Set music info
            binding.tvMusicTitle.text = post.musicTitle
            binding.tvMusicArtist.text = post.musicArtist

            // Set user info
            binding.tvUserName.text = user?.displayName ?: "Unknown User"

            // Load user profile image
            if (!user?.profileImageUrl.isNullOrBlank()) {
                Picasso.get()
                    .load(user?.profileImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.ivUserProfile)
            }

            // Set genre if available
            binding.tvGenre.text = post.genre ?: "Other"
            binding.tvGenre.visibility = if (post.genre != null) android.view.View.VISIBLE else android.view.View.GONE

            // Set caption if available
            binding.tvCaption.text = post.caption ?: ""
            binding.tvCaption.visibility = if (!post.caption.isNullOrBlank()) android.view.View.VISIBLE else android.view.View.GONE

            // Click listener
            binding.root.setOnClickListener {
                onPostClick(post.id)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostWithUser>() {
        override fun areItemsTheSame(oldItem: PostWithUser, newItem: PostWithUser): Boolean {
            return oldItem.post.id == newItem.post.id
        }

        override fun areContentsTheSame(oldItem: PostWithUser, newItem: PostWithUser): Boolean {
            return oldItem == newItem
        }
    }
}
