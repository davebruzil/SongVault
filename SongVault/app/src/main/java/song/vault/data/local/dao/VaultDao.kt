package song.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import song.vault.data.local.entity.VaultEntity
import song.vault.data.local.entity.VaultSongEntity

@Dao
interface VaultDao {

    // Vault operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createVault(vault: VaultEntity): Long

    @Query("SELECT * FROM vaults WHERE userId = :userId ORDER BY createdAt DESC")
    fun getVaultsByUser(userId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getVaultsByUserOnce(userId: String): List<VaultEntity>

    @Query("SELECT * FROM vaults WHERE id = :vaultId")
    suspend fun getVaultById(vaultId: Long): VaultEntity?

    @Delete
    suspend fun deleteVault(vault: VaultEntity)

    @Query("DELETE FROM vaults WHERE id = :vaultId")
    suspend fun deleteVaultById(vaultId: Long)

    // Vault songs operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToVault(song: VaultSongEntity): Long

    @Query("SELECT * FROM vault_songs WHERE vaultId = :vaultId ORDER BY addedAt DESC")
    fun getSongsByVault(vaultId: Long): Flow<List<VaultSongEntity>>

    @Query("SELECT * FROM vault_songs WHERE vaultId = :vaultId ORDER BY addedAt DESC")
    suspend fun getSongsByVaultOnce(vaultId: Long): List<VaultSongEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM vault_songs WHERE vaultId = :vaultId AND videoId = :videoId)")
    suspend fun isSongInVault(vaultId: Long, videoId: String): Boolean

    @Query("DELETE FROM vault_songs WHERE vaultId = :vaultId AND videoId = :videoId")
    suspend fun removeSongFromVault(vaultId: Long, videoId: String)

    @Delete
    suspend fun deleteSong(song: VaultSongEntity)

    @Query("SELECT COUNT(*) FROM vault_songs WHERE vaultId = :vaultId")
    suspend fun getSongCountInVault(vaultId: Long): Int
}
