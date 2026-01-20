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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.fragment.findNavController
import song.vault.SongVaultApplication
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.databinding.FragmentYoutubeSearchBinding
import song.vault.util.AudioExtractor
import song.vault.util.Resource
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class YouTubeSearchFragment : Fragment() {

    private var _binding: FragmentYoutubeSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: YouTubeSearchViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        YouTubeSearchViewModel.Factory(app.youTubeRepository)
    }

    private lateinit var adapter: YouTubeVideoAdapter
    private var exoPlayer: ExoPlayer? = null
    private var currentVideo: YouTubeVideo? = null
    private var isExtracting = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    val duration = player.duration.coerceAtLeast(1L)
                    val progress = ((player.currentPosition * 100) / duration).toInt()
                    binding.seekBar.progress = progress
                    binding.tvCurrentTime.text = formatTime(player.currentPosition)
                }
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

    private fun setupPlayerControls() {
        binding.btnPlayPause.setOnClickListener {
            if (isExtracting) return@setOnClickListener

            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                    handler.post(updateSeekBar)
                }
                updatePlayPauseButton()
            }
        }

        binding.btnForward.setOnClickListener {
            exoPlayer?.let { player ->
                val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
                player.seekTo(newPos)
            }
        }

        binding.btnRewind.setOnClickListener {
            exoPlayer?.let { player ->
                val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
                player.seekTo(newPos)
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    exoPlayer?.let { player ->
                        val seekPos = (progress * player.duration) / 100
                        binding.tvCurrentTime.text = formatTime(seekPos)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { sb ->
                    exoPlayer?.let { player ->
                        val seekPos = (sb.progress * player.duration) / 100
                        player.seekTo(seekPos)
                    }
                }
            }
        })
    }

    private fun updatePlayPauseButton() {
        val isPlaying = exoPlayer?.isPlaying == true
        binding.btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun formatTime(millis: Long): String {
        val secs = (millis / 1000).toInt()
        val mins = secs / 60
        val remainingSecs = secs % 60
        return "$mins:${remainingSecs.toString().padStart(2, '0')}"
    }

    private fun playVideo(video: YouTubeVideo) {
        Log.d("YouTubeSearch", "=== PLAY VIDEO CALLED ===")
        Log.d("YouTubeSearch", "Video: ${video.title}")
        Log.d("YouTubeSearch", "Video ID: ${video.videoId}")

        currentVideo = video
        isExtracting = true

        // Update UI
        binding.tvPlayerTitle.text = video.title
        binding.tvPlayerChannel.text = video.channelName
        binding.tvCurrentTime.text = "0:00"
        binding.tvDuration.text = "Loading..."
        binding.seekBar.progress = 0
        binding.playerProgress.isVisible = true
        updatePlayPauseButton()

        // Release previous player
        releasePlayer()

        // Check if yt-dlp is ready
        val app = requireActivity().application as SongVaultApplication
        if (!app.isYtDlpReady) {
            Toast.makeText(context, "Audio extractor initializing...", Toast.LENGTH_SHORT).show()
            binding.playerProgress.isVisible = false
            isExtracting = false
            return
        }

        // Extract audio URL using yt-dlp
        lifecycleScope.launch {
            Log.d("YouTubeSearch", "Extracting audio for: ${video.videoId}")

            when (val result = AudioExtractor.extractAudioUrl(video.videoId)) {
                is Resource.Success -> {
                    val audioInfo = result.data
                    Log.d("YouTubeSearch", "=== EXTRACTION SUCCESS ===")
                    Log.d("YouTubeSearch", "Audio URL length: ${audioInfo.audioUrl.length}")
                    playAudioUrl(audioInfo.audioUrl, audioInfo.headers)
                }
                is Resource.Error -> {
                    Log.e("YouTubeSearch", "=== EXTRACTION FAILED ===")
                    Log.e("YouTubeSearch", "Error: ${result.message}")
                    Toast.makeText(context, "Failed: ${result.message}", Toast.LENGTH_LONG).show()
                    binding.playerProgress.isVisible = false
                    isExtracting = false
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun playAudioUrl(audioUrl: String, headers: Map<String, String>) {
        Log.d("YouTubeSearch", "=== STARTING EXOPLAYER ===")
        Log.d("YouTubeSearch", "URL: ${audioUrl.take(100)}...")

        try {
            val ctx = context ?: return

            // Log headers for debugging
            Log.d("YouTubeSearch", "Using headers: $headers")

            // Use User-Agent from yt-dlp headers (important for 403 prevention)
            val userAgent = headers["User-Agent"] ?: "Mozilla/5.0"
            Log.d("YouTubeSearch", "User-Agent: $userAgent")

            // Create data source factory with exact headers from yt-dlp
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(headers)
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)

            // Create media source
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(audioUrl))

            // Create and configure ExoPlayer
            exoPlayer = ExoPlayer.Builder(ctx).build().apply {
                setMediaSource(mediaSource)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                Log.d("YouTubeSearch", "=== EXOPLAYER READY ===")
                                Log.d("YouTubeSearch", "Duration: ${duration}ms")
                                isExtracting = false
                                binding.playerProgress.isVisible = false
                                binding.tvDuration.text = formatTime(duration)
                                play()
                                handler.post(updateSeekBar)
                                updatePlayPauseButton()
                            }
                            Player.STATE_ENDED -> {
                                Log.d("YouTubeSearch", "Playback ended")
                                binding.seekBar.progress = 100
                                updatePlayPauseButton()
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d("YouTubeSearch", "Buffering...")
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseButton()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("YouTubeSearch", "=== EXOPLAYER ERROR ===")
                        Log.e("YouTubeSearch", "Error: ${error.message}")
                        Log.e("YouTubeSearch", "Cause: ${error.cause?.message}")
                        Toast.makeText(context, "Playback error: ${error.message}", Toast.LENGTH_LONG).show()
                        isExtracting = false
                        binding.playerProgress.isVisible = false
                    }
                })

                prepare()
            }
        } catch (e: Exception) {
            Log.e("YouTubeSearch", "Error creating ExoPlayer: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            isExtracting = false
            binding.playerProgress.isVisible = false
        }
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updateSeekBar)
        exoPlayer?.release()
        exoPlayer = null
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
        exoPlayer?.pause()
        updatePlayPauseButton()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
