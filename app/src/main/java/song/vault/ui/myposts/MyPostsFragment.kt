package song.vault.ui.myposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentMyPostsBinding

class MyPostsFragment : Fragment() {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!

    private val myPostsViewModel: MyPostsViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        MyPostsViewModel.Factory(app.postRepository, app.userRepository)
    }

    private lateinit var postsAdapter: MyPostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        postsAdapter = MyPostsAdapter(
            onDeleteClick = { post ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Post")
                    .setMessage("Are you sure you want to delete this post?")
                    .setPositiveButton("Delete") { _, _ ->
                        myPostsViewModel.deletePost(post.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    private fun setupObservers() {
        myPostsViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            binding.tvEmptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPosts.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
        }

        myPostsViewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is SyncState.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                }
                is SyncState.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is SyncState.Idle -> {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }

        myPostsViewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DeleteState.Success -> {
                    Snackbar.make(binding.root, "Post deleted", Snackbar.LENGTH_SHORT).show()
                    myPostsViewModel.resetDeleteState()
                }
                is DeleteState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    myPostsViewModel.resetDeleteState()
                }
                else -> {}
            }
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            myPostsViewModel.syncPosts()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
