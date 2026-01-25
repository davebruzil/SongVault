package song.vault.ui.post.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentCreatePostBinding
import song.vault.ui.feed.GenreFilterBottomSheet
import song.vault.data.remote.youtube.N8nClient

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val createViewModel: PostCreateViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        val n8nService = N8nClient.webhookService
        PostCreateViewModel.Factory(app.postRepository, app.youTubeRepository, n8nService)
    }

    private var selectedGenre: String? = null

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

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnPasteLink.setOnClickListener {
            val link = binding.etMusicLink.text.toString().trim()
            if (link.isNotEmpty()) {
                createViewModel.autoFetchMetadata(link)
            } else {
                Snackbar.make(binding.root, "Please enter a music link", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectGenre.setOnClickListener {
            GenreFilterBottomSheet(
                onGenreSelected = { genre ->
                    selectedGenre = genre
                    binding.tvGenreSelected.text = genre ?: "Other"
                }
            ).show(childFragmentManager, "GenreFilterBottomSheet")
        }

        binding.btnCreatePost.setOnClickListener {
            val title = binding.etMusicTitle.text.toString().trim()
            val artist = binding.etMusicArtist.text.toString().trim()
            val link = binding.etMusicLink.text.toString().trim()
            val caption = binding.etCaption.text.toString().trim()

            if (title.isEmpty() || artist.isEmpty() || link.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createViewModel.createPost(
                musicTitle = title,
                musicArtist = artist,
                musicThumbnailUrl = createViewModel.musicMetadata.value?.thumbnailUrl,
                musicExternalUrl = link,
                musicSource = "YouTube",  // or detect from link
                caption = if (caption.isEmpty()) null else caption,
                genre = selectedGenre
            )
        }
    }

    private fun setupObservers() {
        createViewModel.autoFetchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostCreateViewModel.AutoFetchState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is PostCreateViewModel.AutoFetchState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Metadata fetched!", Snackbar.LENGTH_SHORT).show()
                }
                is PostCreateViewModel.AutoFetchState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is PostCreateViewModel.AutoFetchState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        createViewModel.musicMetadata.observe(viewLifecycleOwner) { video ->
            if (video != null) {
                binding.etMusicTitle.setText(video.title)
                binding.etMusicArtist.setText(video.channelName)
                
                Picasso.get()
                    .load(video.thumbnailUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.ivThumbnail)
                
                binding.ivThumbnail.visibility = View.VISIBLE
            }
        }

        createViewModel.createState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostCreateViewModel.CreateState.Loading -> {
                    binding.btnCreatePost.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is PostCreateViewModel.CreateState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreatePost.isEnabled = true
                    Snackbar.make(binding.root, "Post created!", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is PostCreateViewModel.CreateState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreatePost.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is PostCreateViewModel.CreateState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreatePost.isEnabled = true
                }
            }
        }

        createViewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            errors["title"]?.let { binding.etMusicTitle.error = it }
            errors["artist"]?.let { binding.etMusicArtist.error = it }
            errors["link"]?.let { binding.etMusicLink.error = it }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
