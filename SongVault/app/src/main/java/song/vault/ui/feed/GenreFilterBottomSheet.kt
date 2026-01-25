package song.vault.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import song.vault.databinding.BottomSheetGenreFilterBinding
import song.vault.util.Genre

class GenreFilterBottomSheet(
    private val onGenreSelected: (String?) -> Unit = {}  // null = "All"
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGenreFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGenreFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChips()
    }

    private fun setupChips() {
        // "All" chip
        val allChip = Chip(requireContext()).apply {
            text = "All Genres"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                onGenreSelected(null)
                dismiss()
            }
        }
        binding.chipGroup.addView(allChip)

        // Genre chips
        Genre.values().forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.displayName
                isCheckable = true
                setOnClickListener {
                    onGenreSelected(genre.displayName)
                    dismiss()
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
