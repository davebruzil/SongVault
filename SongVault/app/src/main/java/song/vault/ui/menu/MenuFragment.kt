package song.vault.ui.menu

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
import androidx.navigation.fragment.findNavController
import song.vault.MainActivity
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.databinding.FragmentMenuBinding
import song.vault.ui.profile.ProfileViewModel

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
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
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnProfile.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_profileFragment)
        }

        binding.btnMyPosts.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_myPostsFragment)
        }

        binding.btnSearchSongs.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_youtubeSearchFragment)
        }

        binding.btnFeed.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_feedFragment)
        }

        binding.btnTrending.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_trendingFragment)
        }

        binding.btnMyVaults.setOnClickListener { view ->
            animateAndNavigate(view, R.id.action_menuFragment_to_vaultListFragment)
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
