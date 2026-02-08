package song.vault.ui.youtube

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.databinding.ItemYoutubeVideoBinding

class YouTubeVideoAdapter(
    private val onVideoClick: (YouTubeVideo) -> Unit,
    private val onPostClick: (YouTubeVideo) -> Unit
) : ListAdapter<YouTubeVideo, YouTubeVideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemYoutubeVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemYoutubeVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: YouTubeVideo) {
            binding.apply {
                tvTitle.text = video.title
                tvChannel.text = video.channelName

                Picasso.get()
                    .load(video.thumbnailUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(ivThumbnail)

                root.setOnClickListener {
                    onVideoClick(video)
                }

                btnPost.setOnClickListener {
                    onPostClick(video)
                }
            }
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<YouTubeVideo>() {
        override fun areItemsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem.videoId == newItem.videoId
        }

        override fun areContentsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem == newItem
        }
    }
}
