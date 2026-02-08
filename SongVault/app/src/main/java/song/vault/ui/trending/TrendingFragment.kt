package song.vault.ui.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.snackbar.Snackbar
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentTrendingBinding
import song.vault.ui.youtube.YouTubeVideoAdapter

class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private val binding get() = _binding!!

    private val trendingViewModel: TrendingViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        TrendingViewModel.Factory(app.youTubeRepository)
    }

    private lateinit var videoAdapter: YouTubeVideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        // Load trending on first view
        trendingViewModel.loadTrendingMusic()
    }

    private fun setupRecyclerView() {
        videoAdapter = YouTubeVideoAdapter(
            onVideoClick = { video ->
                // TODO: Navigate to create post with this video
                Snackbar.make(binding.root, "Creating post from trending: ${video.title}", Snackbar.LENGTH_SHORT).show()
            },
            onPostClick = { video ->
                // TODO: Navigate to create post with this video
                Snackbar.make(binding.root, "Creating post from trending: ${video.title}", Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.rvTrending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }
    }

    private fun setupObservers() {
        trendingViewModel.trendingVideos.observe(viewLifecycleOwner) { videos ->
            videoAdapter.submitList(videos)
            binding.tvEmptyState.isVisible = videos.isEmpty()
            binding.rvTrending.isVisible = videos.isNotEmpty()
        }

        trendingViewModel.loadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TrendingViewModel.LoadState.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is TrendingViewModel.LoadState.Success -> {
                    binding.progressBar.isVisible = false
                }
                is TrendingViewModel.LoadState.Error -> {
                    binding.progressBar.isVisible = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.isVisible = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
