package song.vault.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as SongVaultApplication
        ProfileViewModel.Factory(app.userRepository, app.postRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        profileViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                return@observe
            }

            binding.tvDisplayName.text = user.displayName ?: "User"
            binding.tvEmail.text = user.email

            user.profileImageUrl?.let { url ->
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivProfileImage)
            }

            profileViewModel.loadPostCount(user.uid)
        }

        profileViewModel.postCount.observe(viewLifecycleOwner) { count ->
            binding.tvPostCount.text = count.toString()
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.btnMyPosts.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myPostsFragment)
        }

        binding.btnSearchSongs.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_youtubeSearchFragment)
        }

        binding.btnLogout.setOnClickListener {
            profileViewModel.logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
