package song.vault.ui.vault

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import song.vault.R
import song.vault.SongVaultApplication
import song.vault.data.local.entity.VaultEntity
import song.vault.data.remote.youtube.YouTubeVideo

class AddToVaultDialog : DialogFragment() {

    private var video: YouTubeVideo? = null
    private var onSongAdded: (() -> Unit)? = null

    companion object {
        fun newInstance(video: YouTubeVideo, onSongAdded: () -> Unit = {}): AddToVaultDialog {
            return AddToVaultDialog().apply {
                this.video = video
                this.onSongAdded = onSongAdded
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val app = requireActivity().application as SongVaultApplication
        val vaultRepository = app.vaultRepository
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        // Create main layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        // Title
        val titleView = TextView(context).apply {
            text = "Add to Vault"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(titleView)

        // Song info
        val songInfo = TextView(context).apply {
            text = video?.title ?: "Unknown Song"
            textSize = 14f
            setTextColor(context.getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 24)
        }
        layout.addView(songInfo)

        // Vaults list
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            minimumHeight = 200
        }
        layout.addView(recyclerView)

        // Create new vault button
        val createButton = MaterialButton(context).apply {
            text = "Create New Vault"
            setOnClickListener {
                showCreateVaultDialog(vaultRepository, userId)
            }
        }
        layout.addView(createButton)

        // Load vaults
        val adapter = VaultListAdapter { vault ->
            addSongToVault(vaultRepository, vault)
        }
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            val vaults = vaultRepository.getVaultsByUserOnce(userId)
            adapter.submitList(vaults)

            if (vaults.isEmpty()) {
                val emptyText = TextView(context).apply {
                    text = "No vaults yet. Create one!"
                    textSize = 14f
                    setPadding(0, 16, 0, 16)
                }
                layout.addView(emptyText, 2)
            }
        }

        return AlertDialog.Builder(context)
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun showCreateVaultDialog(vaultRepository: song.vault.data.repository.VaultRepository, userId: String) {
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
                    lifecycleScope.launch {
                        val vaultId = vaultRepository.createVault(name, userId)
                        Toast.makeText(context, "Vault '$name' created!", Toast.LENGTH_SHORT).show()

                        // Add song to the new vault
                        video?.let { v ->
                            vaultRepository.addSongToVault(vaultId, v)
                            Toast.makeText(context, "Added to '$name'", Toast.LENGTH_SHORT).show()
                            onSongAdded?.invoke()
                        }
                        dismiss()
                    }
                } else {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSongToVault(vaultRepository: song.vault.data.repository.VaultRepository, vault: VaultEntity) {
        val v = video ?: return

        lifecycleScope.launch {
            // Check if already in vault
            if (vaultRepository.isSongInVault(vault.id, v.videoId)) {
                Toast.makeText(context, "Already in '${vault.name}'", Toast.LENGTH_SHORT).show()
            } else {
                vaultRepository.addSongToVault(vault.id, v)
                Toast.makeText(context, "Added to '${vault.name}'", Toast.LENGTH_SHORT).show()
                onSongAdded?.invoke()
            }
            dismiss()
        }
    }

    // Simple adapter for vault list
    private class VaultListAdapter(
        private val onVaultClick: (VaultEntity) -> Unit
    ) : RecyclerView.Adapter<VaultListAdapter.VaultViewHolder>() {

        private var vaults: List<VaultEntity> = emptyList()

        fun submitList(list: List<VaultEntity>) {
            vaults = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 32, 32, 32)
                textSize = 16f
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            return VaultViewHolder(textView)
        }

        override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
            val vault = vaults[position]
            holder.bind(vault, onVaultClick)
        }

        override fun getItemCount() = vaults.size

        class VaultViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
            fun bind(vault: VaultEntity, onClick: (VaultEntity) -> Unit) {
                textView.text = vault.name
                textView.setOnClickListener { onClick(vault) }
            }
        }
    }
}
