package song.vault.ui.youtube

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.fragment.findNavController
import song.vault.SongVaultApplication
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.databinding.FragmentYoutubeSearchBinding
import song.vault.ui.vault.AddToVaultDialog
import song.vault.util.AudioExtractor
import song.vault.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Random search prompts for inspiration
    private val randomPrompts = listOf(
        // Genre Mashups
        "math rock with trumpet like American Football",
        "post-rock that actually sings in their native language",
        "jazz fusion with black metal vocals",
        "baroque pop with heavy distortion",
        "synthwave but with acoustic instruments",
        "emo rap that sounds like it was recorded in a bedroom in 2006",
        "folk punk with cello",
        "kawaii metal with actual good production",
        "dreampop with breakbeats",
        "bluegrass played like black metal",
        "trip-hop with opera vocals",
        "lo-fi beats but made with 80s synths",
        "post-punk with saxophone solos",
        "shoegaze with trap drums",
        "jungle with classical string arrangements",
        // Geographic/Regional
        "Brazilian indie that isn't bossa nova",
        "Russian rock that came out after 2010",
        "Japanese math rock that isn't toe",
        "Korean shoegaze with female vocals",
        "Icelandic electronic that isn't Björk",
        "French hip hop that samples old movies",
        "Ethiopian jazz revival",
        "Australian psych rock that's actually trippy",
        "Canadian post-rock that feels like winter",
        "Mexican punk with folk instruments",
        "Swedish death metal with clean vocals",
        "Polish experimental electronic",
        "Thai surf rock",
        "South African protest music that isn't apartheid era",
        "Indonesian dream pop",
        // Vibe/Scenario
        "music for coding at 4am when everything works",
        "music for staring at the ceiling at 2pm",
        "music for driving through empty desert at sunrise",
        "music that sounds like a rainy day in a Japanese city",
        "music for when you're nostalgic for a place you've never been",
        "music that feels like finding an old VHS tape in an abandoned house",
        "music for walking alone at night in a city that isn't yours",
        "music that sounds like 90s anime opening credits",
        "music for cooking complicated recipes",
        "music that feels like a 3am gas station",
        "music for when you want to feel like a cyberpunk character",
        "music that sounds like summer childhood memories",
        "music for late night train rides",
        "music that feels like discovering an ancient temple",
        "music for when you're homesick for a fictional place",
        // Era/Style Crossovers
        "2000s pop punk revival that doesn't suck",
        "90s trip-hop made with modern production",
        "70s funk recorded with lo-fi aesthetics",
        "80s synthpop with dark lyrics",
        "2020s hyperpop but actually listenable",
        "classical music that sounds like video game boss fights",
        "boombap beats with ambient textures",
        "vaporwave that's actually musically interesting",
        "nu-metal revival without the cringe",
        "riot grrrl but make it electronic",
        // Instrument Focused
        "bands where the bass is the lead instrument",
        "music with prominent glockenspiel",
        "songs where the drummer sings",
        "music with accordion that isn't polka",
        "bands that use sitar in non-psychedelic rock",
        "electronic music made mostly with modular synths",
        "music where the clarinet is secretly the coolest part",
        "bands with dual drummers",
        "music that features theremin prominently",
        "guitar music where they don't use distortion pedals"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var typewriterJob: Job? = null

    // Intro text content
    private val introTitle = "[ DISCOVER NEW MUSIC ]"
    private val introDesc = "Use your own words to find songs.\nDescribe a vibe, a feeling, a memory.\nNo need for exact titles or artists."
    private val introExamples = listOf(
        "\"sad songs for 3am drives\"",
        "\"music that sounds like rain on windows\"",
        "\"japanese city pop vibes\"",
        "\"something to code to at midnight\"",
        "\"songs my cool older sister would play\""
    )

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

        // Keep screen on while in this fragment
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupToolbar()
        setupSearch()
        setupRecyclerView()
        setupPlayerControls()
        setupLoadMore()
        observeViewModel()
        startTypewriterAnimation()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Generate button - only fills search field, does NOT trigger search
        binding.btnGenerate.setOnClickListener {
            val randomPrompt = randomPrompts.random()
            binding.etSearch.setText(randomPrompt)
            binding.etSearch.setSelection(randomPrompt.length)
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        binding.btnSearch.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text?.toString()?.trim() ?: return
        if (query.isNotEmpty()) {
            // Hide generate button and intro section after first search
            binding.btnGenerate.visibility = View.GONE
            hideIntroSection()
            viewModel.searchSongs(query)
        }
    }

    private fun setupRecyclerView() {
        adapter = YouTubeVideoAdapter { video ->
            viewModel.selectVideo(video)
        }
        binding.rvVideos.adapter = adapter
    }

    private var isAtBottom = false

    private fun setupLoadMore() {
        binding.btnLoadMore.setOnClickListener {
            viewModel.loadMore()
        }

        // Show Load More button only when scrolled to bottom
        binding.rvVideos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Consider "at bottom" if within last 2 items
                isAtBottom = lastVisibleItem >= totalItemCount - 2

                // Update button visibility based on scroll position AND canLoadMore
                updateLoadMoreVisibility()
            }
        })
    }

    private fun updateLoadMoreVisibility() {
        val canLoad = viewModel.canLoadMore.value == true
        binding.btnLoadMore.isVisible = canLoad && isAtBottom
    }

    private fun showAddToVaultDialog() {
        val video = currentVideo ?: return
        AddToVaultDialog.newInstance(video) {
            Toast.makeText(context, "Song saved to vault!", Toast.LENGTH_SHORT).show()
        }.show(childFragmentManager, "add_to_vault")
    }

    private fun setupPlayerControls() {
        // Add to vault button
        binding.btnAddToVault.setOnClickListener {
            showAddToVaultDialog()
        }

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

            // Optimized load control for faster playback start
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000,   // Min buffer (2 sec) - reduced for faster start
                    30000,  // Max buffer (30 sec)
                    500,    // Buffer for playback (0.5 sec) - start playing sooner
                    1000    // Buffer for rebuffer (1 sec)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            // Create and configure ExoPlayer with optimized settings
            exoPlayer = ExoPlayer.Builder(ctx)
                .setLoadControl(loadControl)
                .build().apply {
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
                binding.tvEmpty.text = "[ NO RESULTS FOUND ]"
            }

            // Pre-extract first 2 videos for faster playback
            if (videos.isNotEmpty()) {
                lifecycleScope.launch {
                    videos.take(2).forEach { video ->
                        AudioExtractor.preExtract(video.videoId)
                    }
                }
            }
        }

        viewModel.selectedVideo.observe(viewLifecycleOwner) { video ->
            binding.playerCard.isVisible = video != null
            video?.let { playVideo(it) }
        }

        // Observe Load More state
        viewModel.canLoadMore.observe(viewLifecycleOwner) { _ ->
            updateLoadMoreVisibility()
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLoadMore.isEnabled = !isLoading
            binding.btnLoadMore.text = if (isLoading) "[ LOADING... ]" else "[ LOAD MORE ]"
        }
    }

    private fun startTypewriterAnimation() {
        typewriterJob?.cancel()
        typewriterJob = lifecycleScope.launch {
            // Clear text first
            binding.tvIntroTitle.text = ""
            binding.tvIntroDesc.text = ""
            binding.tvIntroExamples.text = ""
            binding.introSection.visibility = View.VISIBLE

            // Type title
            typeText(binding.tvIntroTitle, introTitle, 40)

            // Small delay before description
            delay(300)

            // Type description
            typeText(binding.tvIntroDesc, introDesc, 25)

            // Small delay before examples
            delay(400)

            // Type examples one by one
            val examplesText = StringBuilder()
            for ((index, example) in introExamples.withIndex()) {
                for (char in example) {
                    examplesText.append(char)
                    binding.tvIntroExamples.text = examplesText.toString()
                    delay(20)
                }
                if (index < introExamples.size - 1) {
                    examplesText.append("\n")
                    binding.tvIntroExamples.text = examplesText.toString()
                    delay(200)
                }
            }
        }
    }

    private suspend fun typeText(textView: android.widget.TextView, text: String, delayMs: Long) {
        val builder = StringBuilder()
        for (char in text) {
            builder.append(char)
            textView.text = builder.toString()
            delay(delayMs)
        }
    }

    private fun hideIntroSection() {
        typewriterJob?.cancel()
        binding.introSection.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        updatePlayPauseButton()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Allow screen to turn off when leaving
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        typewriterJob?.cancel()
        releasePlayer()
        _binding = null
    }
}
