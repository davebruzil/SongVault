package song.vault.ui.youtube

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.databinding.FragmentYoutubeSearchBinding

class YouTubeSearchFragment : Fragment() {

    private var _binding: FragmentYoutubeSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: YouTubeSearchViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        YouTubeSearchViewModel.Factory(app.youTubeRepository)
    }

    private lateinit var adapter: YouTubeVideoAdapter
    private var youTubePlayerView: YouTubePlayerView? = null
    private var youTubePlayer: YouTubePlayer? = null
    private var currentVideo: YouTubeVideo? = null
    private var isPlaying = false
    private var playerReady = false
    private var videoDuration = 0f
    private var currentTime = 0f
    private var isSeeking = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (!isSeeking && videoDuration > 0) {
                val progress = ((currentTime / videoDuration) * 100).toInt()
                binding.seekBar.progress = progress
                binding.tvCurrentTime.text = formatTime(currentTime)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSearch()
        setupRecyclerView()
        setupYouTubePlayer()
        setupPlayerControls()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        binding.tilSearch.setEndIconOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text?.toString()?.trim() ?: return
        if (query.isNotEmpty()) {
            viewModel.searchSongs(query)
        }
    }

    private fun setupRecyclerView() {
        adapter = YouTubeVideoAdapter { video ->
            viewModel.selectVideo(video)
        }
        binding.rvVideos.adapter = adapter
    }

    private fun setupYouTubePlayer() {
        youTubePlayerView = YouTubePlayerView(requireContext()).apply {
            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    Log.d("YouTubeSearch", "Player ready")
                    youTubePlayer = player
                    playerReady = true

                    currentVideo?.let {
                        player.loadVideo(it.videoId, 0f)
                    }
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    Log.d("YouTubeSearch", "State: $state")
                    when (state) {
                        PlayerConstants.PlayerState.PLAYING -> {
                            isPlaying = true
                            updatePlayPauseButton()
                            handler.post(updateSeekBar)
                        }
                        PlayerConstants.PlayerState.PAUSED -> {
                            isPlaying = false
                            updatePlayPauseButton()
                        }
                        PlayerConstants.PlayerState.ENDED -> {
                            isPlaying = false
                            updatePlayPauseButton()
                            handler.removeCallbacks(updateSeekBar)
                        }
                        else -> {}
                    }
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    currentTime = second
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    videoDuration = duration
                    binding.tvDuration.text = formatTime(duration)
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    Log.e("YouTubeSearch", "Player error: $error")
                    Toast.makeText(context, "Cannot play this video", Toast.LENGTH_SHORT).show()
                }
            })
        }

        lifecycle.addObserver(youTubePlayerView!!)
        binding.youtubePlayerContainer.addView(youTubePlayerView)
    }

    private fun setupPlayerControls() {
        binding.btnPlayPause.setOnClickListener {
            val player = youTubePlayer ?: return@setOnClickListener
            if (isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        binding.btnForward.setOnClickListener {
            youTubePlayer?.let { player ->
                val newTime = (currentTime + 10f).coerceAtMost(videoDuration)
                player.seekTo(newTime)
            }
        }

        binding.btnRewind.setOnClickListener {
            youTubePlayer?.let { player ->
                val newTime = (currentTime - 10f).coerceAtLeast(0f)
                player.seekTo(newTime)
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && videoDuration > 0) {
                    val seekTime = (progress / 100f) * videoDuration
                    binding.tvCurrentTime.text = formatTime(seekTime)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val seekTime = (it.progress / 100f) * videoDuration
                    youTubePlayer?.seekTo(seekTime)
                }
                isSeeking = false
            }
        })
    }

    private fun updatePlayPauseButton() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun formatTime(seconds: Float): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return "$mins:${secs.toString().padStart(2, '0')}"
    }

    private fun playVideo(video: YouTubeVideo) {
        currentVideo = video
        currentTime = 0f
        videoDuration = 0f
        Log.d("YouTubeSearch", "Playing: ${video.title}")

        // Update player UI
        binding.tvPlayerTitle.text = video.title
        binding.tvPlayerChannel.text = video.channelName
        binding.tvCurrentTime.text = "0:00"
        binding.tvDuration.text = "0:00"
        binding.seekBar.progress = 0

        if (playerReady) {
            youTubePlayer?.loadVideo(video.videoId, 0f)
        }
    }

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is SearchState.Loading
            binding.tvError.isVisible = state is SearchState.Error
            binding.tvEmpty.isVisible = state is SearchState.Idle

            if (state is SearchState.Error) {
                binding.tvError.text = state.message
            }
        }

        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            adapter.submitList(videos)
            binding.tvEmpty.isVisible = videos.isEmpty() && viewModel.searchState.value is SearchState.Success
            if (binding.tvEmpty.isVisible) {
                binding.tvEmpty.text = "No songs found"
            }
        }

        viewModel.selectedVideo.observe(viewLifecycleOwner) { video ->
            binding.playerCard.isVisible = video != null
            video?.let { playVideo(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBar)
        youTubePlayerView?.release()
        youTubePlayerView = null
        _binding = null
    }
}
