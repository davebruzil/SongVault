package song.vault.ui.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentTrendingBinding
import song.vault.ui.feed.FeedAdapter

class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TrendingViewModel
    private lateinit var adapter: FeedAdapter

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

        viewModel = ViewModelProvider(this).get(TrendingViewModel::class.java)

        setupRecyclerView()
        setupUI()
        observeData()
        viewModel.loadTrendingContent()
    }

    private fun setupRecyclerView() {
        adapter = FeedAdapter(
            onPostClick = { post ->
                val action = TrendingFragmentDirections.actionTrendingFragmentToPostDetailFragment(post.id)
                findNavController().navigate(action)
            },
            onEditClick = { post ->
                val action = TrendingFragmentDirections.actionTrendingFragmentToCreatePostFragment(post.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { post ->
            },
            currentUserId = SongVaultApplication.currentUser?.uid ?: ""
        )

        binding.trendingRecyclerView.apply {
            adapter = this@TrendingFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupUI() {
        binding.refreshButton.setOnClickListener {
            viewModel.loadTrendingContent()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.trendingPosts.collect { posts ->
                adapter.submitList(posts)
                binding.emptyStateText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                binding.trendingRecyclerView.visibility = if (posts.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
