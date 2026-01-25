package song.vault.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        FeedViewModel.Factory(app.postRepository)
    }

    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupRefresh()
        setupGenreFilter()
        setupObservers()

        // Initial load
        feedViewModel.syncFeed()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { postId ->
                // Navigate to post detail
                findNavController().navigate(
                    R.id.action_feedFragment_to_postDetailFragment,
                    Bundle().apply { putString("postId", postId) }
                )
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            feedViewModel.syncFeed()
        }
    }

    private fun setupGenreFilter() {
        binding.btnFilterGenre.setOnClickListener {
            GenreFilterBottomSheet(
                onGenreSelected = { genre ->
                    feedViewModel.filterByGenre(genre)
                }
            ).show(childFragmentManager, "GenreFilterBottomSheet")
        }
    }

    private fun setupObservers() {
        // Observe sync state
        feedViewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedViewModel.SyncState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is FeedViewModel.SyncState.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                }
                is FeedViewModel.SyncState.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is FeedViewModel.SyncState.Idle -> {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }

        // Observe selected genre
        feedViewModel.selectedGenre.observe(viewLifecycleOwner) { genre ->
            binding.tvGenreLabel.text = if (genre == null) "All Genres" else genre
        }

        // Observe filtered posts
        feedViewModel.filteredPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            binding.tvEmptyState.isVisible = posts.isEmpty()
            binding.rvFeed.isVisible = posts.isNotEmpty()
        }

        // Also observe all posts initially
        feedViewModel.allPosts.observe(viewLifecycleOwner) { posts ->
            if (feedViewModel.selectedGenre.value == null) {
                postAdapter.submitList(posts)
                binding.tvEmptyState.isVisible = posts.isEmpty()
                binding.rvFeed.isVisible = posts.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
