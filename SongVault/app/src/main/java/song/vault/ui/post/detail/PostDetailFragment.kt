package song.vault.ui.post.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentPostDetailBinding

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val postDetailViewModel: PostDetailViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        PostDetailViewModel.Factory(app.postRepository, app.userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId")
        if (postId != null) {
            postDetailViewModel.loadPost(postId)
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        postDetailViewModel.postWithUser.observe(viewLifecycleOwner) { postWithUser ->
            if (postWithUser != null) {
                val post = postWithUser.post
                val user = postWithUser.user

                // Load thumbnail
                Picasso.get()
                    .load(post.musicThumbnailUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.ivCoverArt)

                // Set music info
                binding.tvMusicTitle.text = post.musicTitle
                binding.tvMusicArtist.text = post.musicArtist
                binding.tvDescription.text = post.caption ?: "No description"

                // Set user info
                binding.tvUserName.text = user?.displayName ?: "Unknown User"
                if (!user?.profileImageUrl.isNullOrBlank()) {
                    Picasso.get()
                        .load(user?.profileImageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.ivUserProfile)
                }

                // Set genre
                binding.tvGenre.text = post.genre ?: "Other"

                // Open link button
                binding.btnOpenMusic.setOnClickListener {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(post.musicExternalUrl)
                    )
                    startActivity(intent)
                }
            }
        }

        postDetailViewModel.isOwner.observe(viewLifecycleOwner) { isOwner ->
            binding.btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
            binding.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
        }

        postDetailViewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostDetailViewModel.DeleteState.Loading -> {
                    binding.btnDelete.isEnabled = false
                }
                is PostDetailViewModel.DeleteState.Success -> {
                    Snackbar.make(binding.root, "Post deleted", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is PostDetailViewModel.DeleteState.Error -> {
                    binding.btnDelete.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener {
            // TODO: Navigate to edit post fragment
            Snackbar.make(binding.root, "Edit feature coming soon", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ ->
                    postDetailViewModel.deletePost(postDetailViewModel.postWithUser.value?.post?.id ?: "")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
