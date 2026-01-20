package song.vault.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentFeedBinding
import song.vault.util.Genre

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: FeedAdapter

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

        val postDao = SongVaultApplication.postDatabase.postDao()
        val factory = FeedViewModelFactory(postDao)
        viewModel = ViewModelProvider(this, factory).get(FeedViewModel::class.java)

        setupRecyclerView()
        setupGenreChips()
        setupSearchView()
        observeData()

        binding.createPostFab.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_createPost)
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = SongVaultApplication.currentUser?.uid ?: ""
        adapter = FeedAdapter(
            onPostClick = { post ->
                val action = FeedFragmentDirections.actionFeedToPostDetail(post.id)
                findNavController().navigate(action)
            },
            onEditClick = { post ->
                val action = FeedFragmentDirections.actionFeedToCreatePost(post.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { post ->
                viewModel.deletePost(post.id)
            },
            currentUserId = currentUserId
        )

        binding.postsRecyclerView.apply {
            adapter = this@FeedFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupGenreChips() {
        binding.genreChipsGroup.removeAllViews()
        val allChip = Chip(requireContext()).apply {
            text = "All Genres"
            isCheckable = true
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setSelectedGenre(null)
                }
            }
        }
        binding.genreChipsGroup.addView(allChip)

        for (genre in Genre.values()) {
            val chip = Chip(requireContext()).apply {
                text = genre.displayName
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        allChip.isChecked = false
                        viewModel.setSelectedGenre(genre)
                    }
                }
            }
            binding.genreChipsGroup.addView(chip)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.filteredPosts.collect { posts ->
                adapter.submitList(posts)
                binding.emptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
