package song.vault.ui.profile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import song.vault.MainActivity
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
            // Only update UI if user is not null
            user?.let {
                binding.tvDisplayName.text = it.displayName ?: "User"
                binding.tvEmail.text = it.email

                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(it.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.ivProfileImage)
                }

            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.btnMyPosts.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_myPostsFragment)
        }

        binding.btnSearchSongs.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_youtubeSearchFragment)
        }

        binding.btnFeed.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_feedFragment)
        }

        binding.btnTrending.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_trendingFragment)
        }

        binding.btnMyVaults.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_profileFragment_to_vaultListFragment)
        }

        binding.btnLogout.setOnClickListener { view ->
            playDigitalAnimation(view) {
                profileViewModel.logout()
                activity?.let { currentActivity ->
                    val intent = Intent(currentActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    currentActivity.finish()
                }
            }
        }
    }

    private fun animateAndNavigate(view: View, destinationId: Int) {
        playDigitalAnimation(view) {
            findNavController().navigate(destinationId)
        }
    }

    private fun playDigitalAnimation(view: View, onComplete: () -> Unit) {
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.digital_press)
        view.startAnimation(animation)
        Handler(Looper.getMainLooper()).postDelayed({
            onComplete()
        }, 200)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
