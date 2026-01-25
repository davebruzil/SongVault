package song.vault.ui.vault

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.data.local.entity.VaultSongEntity
import song.vault.databinding.FragmentVaultDetailBinding
import song.vault.util.AudioExtractor
import song.vault.util.Resource

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VaultDetailFragment : Fragment() {

    private var _binding: FragmentVaultDetailBinding? = null
    private val binding get() = _binding!!

    private var vaultId: Long = 0
    private var vaultName: String = ""

    private lateinit var adapter: SongAdapter
    private var songs: List<VaultSongEntity> = emptyList()
    private var currentSongIndex = -1

    private var exoPlayer: ExoPlayer? = null
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
        _binding = FragmentVaultDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Keep screen on
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get arguments
        vaultId = arguments?.getLong("vaultId") ?: 0
        vaultName = arguments?.getString("vaultName") ?: "Vault"

        setupToolbar()
        setupRecyclerView()
        setupPlayerControls()
        loadSongs()
    }

    private fun setupToolbar() {
        binding.toolbar.title = vaultName
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(
            onSongClick = { song, position ->
                currentSongIndex = position
                playSong(song)
            },
            onSongLongClick = { song ->
                showDeleteSongDialog(song)
            }
        )
        binding.rvSongs.adapter = adapter
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

        binding.btnNext.setOnClickListener {
            playNext()
        }

        binding.btnPrevious.setOnClickListener {
            playPrevious()
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

    private fun loadSongs() {
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository

        binding.progressBar.isVisible = true

        lifecycleScope.launch {
            vaultRepository.getSongsByVault(vaultId).collectLatest { songList ->
                binding.progressBar.isVisible = false
                songs = songList
                binding.tvEmpty.isVisible = songs.isEmpty()
                binding.rvSongs.isVisible = songs.isNotEmpty()
                adapter.submitList(songs)
            }
        }
    }

    private fun playSong(song: VaultSongEntity) {
        Log.d("VaultDetail", "Playing: ${song.title}")

        isExtracting = true
        binding.playerCard.isVisible = true
        binding.tvPlayerTitle.text = song.title
        binding.tvPlayerChannel.text = song.channelName
        binding.tvCurrentTime.text = "0:00"
        binding.tvDuration.text = "Loading..."
        binding.seekBar.progress = 0
        binding.playerProgress.isVisible = true

        releasePlayer()

        val app = requireActivity().application as SongVaultApplication
        if (!app.isYtDlpReady) {
            Toast.makeText(context, "Audio extractor initializing...", Toast.LENGTH_SHORT).show()
            binding.playerProgress.isVisible = false
            isExtracting = false
            return
        }

        lifecycleScope.launch {
            when (val result = AudioExtractor.extractAudioUrl(song.videoId)) {
                is Resource.Success -> {
                    val audioInfo = result.data
                    playAudioUrl(audioInfo.audioUrl, audioInfo.headers)
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Failed: ${result.message}", Toast.LENGTH_LONG).show()
                    binding.playerProgress.isVisible = false
                    isExtracting = false
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun playAudioUrl(audioUrl: String, headers: Map<String, String>) {
        try {
            val ctx = context ?: return

            val userAgent = headers["User-Agent"] ?: "Mozilla/5.0"

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(headers)
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(audioUrl))

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2000, 30000, 500, 1000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayer = ExoPlayer.Builder(ctx)
                .setLoadControl(loadControl)
                .build().apply {
                    setMediaSource(mediaSource)

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                Player.STATE_READY -> {
                                    isExtracting = false
                                    binding.playerProgress.isVisible = false
                                    binding.tvDuration.text = formatTime(duration)
                                    play()
                                    handler.post(updateSeekBar)
                                    updatePlayPauseButton()
                                }
                                Player.STATE_ENDED -> {
                                    binding.seekBar.progress = 100
                                    updatePlayPauseButton()
                                    // Auto-play next
                                    playNext()
                                }
                                Player.STATE_BUFFERING -> {}
                                Player.STATE_IDLE -> {}
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            updatePlayPauseButton()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Toast.makeText(context, "Playback error: ${error.message}", Toast.LENGTH_LONG).show()
                            isExtracting = false
                            binding.playerProgress.isVisible = false
                        }
                    })

                    prepare()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            isExtracting = false
            binding.playerProgress.isVisible = false
        }
    }

    private fun playNext() {
        if (songs.isEmpty() || currentSongIndex < 0) return
        currentSongIndex = (currentSongIndex + 1) % songs.size
        playSong(songs[currentSongIndex])
    }

    private fun playPrevious() {
        if (songs.isEmpty() || currentSongIndex < 0) return
        currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
        playSong(songs[currentSongIndex])
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

    private fun showDeleteSongDialog(song: VaultSongEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Song")
            .setMessage("Remove '${song.title}' from this vault?")
            .setPositiveButton("Remove") { _, _ ->
                deleteSong(song)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSong(song: VaultSongEntity) {
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository

        lifecycleScope.launch {
            vaultRepository.removeSongFromVault(vaultId, song.videoId)
            Toast.makeText(context, "Song removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updateSeekBar)
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        updatePlayPauseButton()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        releasePlayer()
        _binding = null
    }

    // Adapter for songs
    private class SongAdapter(
        private val onSongClick: (VaultSongEntity, Int) -> Unit,
        private val onSongLongClick: (VaultSongEntity) -> Unit
    ) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

        private var songs: List<VaultSongEntity> = emptyList()

        fun submitList(list: List<VaultSongEntity>) {
            songs = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vault_song, parent, false)
            return SongViewHolder(view)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(songs[position], position, onSongClick, onSongLongClick)
        }

        override fun getItemCount() = songs.size

        class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val tvChannel: TextView = itemView.findViewById(R.id.tvChannel)

            fun bind(
                song: VaultSongEntity,
                position: Int,
                onClick: (VaultSongEntity, Int) -> Unit,
                onLongClick: (VaultSongEntity) -> Unit
            ) {
                tvTitle.text = song.title
                tvChannel.text = song.channelName

                if (song.thumbnailUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(song.thumbnailUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivThumbnail)
                }

                itemView.setOnClickListener { onClick(song, position) }
                itemView.setOnLongClickListener {
                    onLongClick(song)
                    true
                }
            }
        }
    }
}
