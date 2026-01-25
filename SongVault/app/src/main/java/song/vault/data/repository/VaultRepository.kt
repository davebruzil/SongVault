package song.vault.data.repository

import kotlinx.coroutines.flow.Flow
import song.vault.data.local.dao.VaultDao
import song.vault.data.local.entity.VaultEntity
import song.vault.data.local.entity.VaultSongEntity
import song.vault.data.remote.youtube.YouTubeVideo

class VaultRepository(private val vaultDao: VaultDao) {

    // Vault operations
    suspend fun createVault(name: String, userId: String): Long {
        val vault = VaultEntity(name = name, userId = userId)
        return vaultDao.createVault(vault)
    }

    fun getVaultsByUser(userId: String): Flow<List<VaultEntity>> {
        return vaultDao.getVaultsByUser(userId)
    }

    suspend fun getVaultsByUserOnce(userId: String): List<VaultEntity> {
        return vaultDao.getVaultsByUserOnce(userId)
    }

    suspend fun deleteVault(vaultId: Long) {
        vaultDao.deleteVaultById(vaultId)
    }

    // Song operations
    suspend fun addSongToVault(vaultId: Long, video: YouTubeVideo): Long {
        val song = VaultSongEntity(
            vaultId = vaultId,
            videoId = video.videoId,
            title = video.title,
            channelName = video.channelName,
            thumbnailUrl = video.thumbnailUrl
        )
        return vaultDao.addSongToVault(song)
    }

    fun getSongsByVault(vaultId: Long): Flow<List<VaultSongEntity>> {
        return vaultDao.getSongsByVault(vaultId)
    }

    suspend fun getSongsByVaultOnce(vaultId: Long): List<VaultSongEntity> {
        return vaultDao.getSongsByVaultOnce(vaultId)
    }

    suspend fun isSongInVault(vaultId: Long, videoId: String): Boolean {
        return vaultDao.isSongInVault(vaultId, videoId)
    }

    suspend fun removeSongFromVault(vaultId: Long, videoId: String) {
        vaultDao.removeSongFromVault(vaultId, videoId)
    }

    suspend fun getSongCount(vaultId: Long): Int {
        return vaultDao.getSongCountInVault(vaultId)
    }
}
