package song.vault.ui.createpost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import android.app.AlertDialog
import com.squareup.picasso.Picasso
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentCreatePostBinding
import song.vault.util.Genre
import song.vault.util.Resource

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val args: CreatePostFragmentArgs by navArgs()

    private lateinit var viewModel: CreatePostViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postRepository = SongVaultApplication.postRepository
        val youTubeRepository = SongVaultApplication.youTubeRepository
        val vmFactory = androidx.lifecycle.ViewModelProvider.NewInstanceFactory().run {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val handle = androidx.lifecycle.SavedStateHandle().apply {
                        args.postId?.let { set("postId", it) }
                    }
                    return CreatePostViewModel(postRepository, youTubeRepository, handle) as T
                }
            }
        }
        viewModel = ViewModelProvider(this, vmFactory).get(CreatePostViewModel::class.java)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.musicLinkInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.setMusicLink(binding.musicLinkInput.text.toString())
            }
        }

        binding.titleInput.setOnTextChangedListener { text ->
            viewModel.setTitle(text.toString())
        }

        binding.artistInput.setOnTextChangedListener { text ->
            viewModel.setArtist(text.toString())
        }

        binding.captionInput.setOnTextChangedListener { text ->
            viewModel.setCaption(text.toString())
        }

        binding.genreSelector.setOnClickListener {
            showGenreBottomSheet()
        }

        binding.publishButton.setOnClickListener {
            if (validateInput()) {
                viewModel.createPost()
            }
        }

        observeData()
    }

    private fun showGenreBottomSheet() {
        val genres = Genre.values().map { it.displayName to it }.toTypedArray()
        val genreNames = genres.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Genre")
            .setItems(genreNames) { _, which ->
                viewModel.setGenre(genres[which].second)
            }
            .show()
    }

    private fun validateInput(): Boolean {
        val title = binding.titleInput.text.toString()
        val artist = binding.artistInput.text.toString()
        val link = binding.musicLinkInput.text.toString()
        val genre = viewModel.selectedGenre.value

        return when {
            title.isBlank() -> {
                Toast.makeText(requireContext(), "Please enter song title", Toast.LENGTH_SHORT).show()
                false
            }
            artist.isBlank() -> {
                Toast.makeText(requireContext(), "Please enter artist name", Toast.LENGTH_SHORT).show()
                false
            }
            link.isBlank() -> {
                Toast.makeText(requireContext(), "Please enter music link", Toast.LENGTH_SHORT).show()
                false
            }
            genre == null -> {
                Toast.makeText(requireContext(), "Please select a genre", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.selectedGenre.collect { genre ->
                binding.genreSelector.text = genre?.displayName ?: "Select Genre"
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.thumbnailUrl.collect { url ->
                if (url.isNotBlank()) {
                    Picasso.get().load(url).into(binding.thumbnailPreview)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.createStatus.collect { status ->
                when (status) {
                    is Resource.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "Post created successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${status.message}",
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
