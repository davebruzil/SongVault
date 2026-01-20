package song.vault.ui.postdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentPostDetailBinding
import song.vault.util.Resource

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var viewModel: PostDetailViewModel

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

        val postDao = SongVaultApplication.postDatabase.postDao()
        val postRepository = SongVaultApplication.postRepository
        val factory = PostDetailViewModelFactory(postDao, postRepository)
        viewModel = ViewModelProvider(this, factory).get(PostDetailViewModel::class.java)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.loadPost(args.postId)
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.post.collect { post ->
                if (post != null) {
                    with(binding) {
                        if (!post.musicThumbnailUrl.isNullOrBlank()) {
                            Picasso.get()
                                .load(post.musicThumbnailUrl)
                                .into(coverArt)
                        }
                        musicTitle.text = post.musicTitle
                        musicArtist.text = post.musicArtist
                        userNameDetail.text = post.userDisplayName ?: "Unknown"
                        genreDetail.text = post.genre ?: "No Genre"
                        sourceDetail.text = post.musicSource ?: "Unknown"
                        descriptionDetail.text = post.caption ?: "No description"

                        val isOwnPost = SongVaultApplication.currentUser?.uid == post.userId
                        editButton.visibility = if (isOwnPost) View.VISIBLE else View.GONE
                        deleteButton.visibility = if (isOwnPost) View.VISIBLE else View.GONE

                        editButton.setOnClickListener {
                            val action = PostDetailFragmentDirections.actionPostDetailToCreatePost(post.id)
                            findNavController().navigate(action)
                        }

                        deleteButton.setOnClickListener {
                            viewModel.deletePost(post.id)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.deleteStatus.collect { status ->
                when (status) {
                    is Resource.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "Post deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Delete failed: ${status.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is Resource.Loading -> {
                    }
                    null -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
