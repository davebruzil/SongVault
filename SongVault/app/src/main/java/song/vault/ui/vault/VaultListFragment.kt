package song.vault.ui.vault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.data.local.entity.VaultEntity
import song.vault.databinding.FragmentVaultListBinding

class VaultListFragment : Fragment() {

    private var _binding: FragmentVaultListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: VaultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVaultListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadVaults()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = VaultAdapter(
            onVaultClick = { vault ->
                // Navigate to vault detail
                val bundle = bundleOf(
                    "vaultId" to vault.id,
                    "vaultName" to vault.name
                )
                findNavController().navigate(
                    R.id.action_vaultListFragment_to_vaultDetailFragment,
                    bundle
                )
            },
            onVaultLongClick = { vault ->
                showDeleteDialog(vault)
            }
        )
        binding.rvVaults.adapter = adapter
    }

    private fun setupFab() {
        binding.fabCreateVault.setOnClickListener {
            showCreateVaultDialog()
        }
    }

    private fun loadVaults() {
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        binding.progressBar.isVisible = true

        lifecycleScope.launch {
            vaultRepository.getVaultsByUser(userId).collectLatest { vaults ->
                binding.progressBar.isVisible = false
                binding.tvEmpty.isVisible = vaults.isEmpty()
                binding.rvVaults.isVisible = vaults.isNotEmpty()
                adapter.submitList(vaults)
            }
        }
    }

    private fun showCreateVaultDialog() {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = "Vault name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(context)
            .setTitle("Create New Vault")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createVault(name)
                } else {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createVault(name: String) {
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        lifecycleScope.launch {
            vaultRepository.createVault(name, userId)
            Toast.makeText(context, "Vault '$name' created!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(vault: VaultEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Vault")
            .setMessage("Delete '${vault.name}' and all its songs?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVault(vault)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVault(vault: VaultEntity) {
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository

        lifecycleScope.launch {
            vaultRepository.deleteVault(vault.id)
            Toast.makeText(context, "Vault deleted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter
    private class VaultAdapter(
        private val onVaultClick: (VaultEntity) -> Unit,
        private val onVaultLongClick: (VaultEntity) -> Unit
    ) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

        private var vaults: List<VaultEntity> = emptyList()

        fun submitList(list: List<VaultEntity>) {
            vaults = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VaultViewHolder(view)
        }

        override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
            holder.bind(vaults[position], onVaultClick, onVaultLongClick)
        }

        override fun getItemCount() = vaults.size

        class VaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(android.R.id.text1)
            private val tvInfo: TextView = itemView.findViewById(android.R.id.text2)

            fun bind(
                vault: VaultEntity,
                onClick: (VaultEntity) -> Unit,
                onLongClick: (VaultEntity) -> Unit
            ) {
                tvName.text = vault.name
                tvName.textSize = 18f
                tvInfo.text = "Tap to view songs"

                itemView.setOnClickListener { onClick(vault) }
                itemView.setOnLongClickListener {
                    onLongClick(vault)
                    true
                }
            }
        }
    }
}
