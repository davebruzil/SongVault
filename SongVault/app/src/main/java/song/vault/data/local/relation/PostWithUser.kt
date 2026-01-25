package song.vault.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import song.vault.data.local.entity.PostEntity
import song.vault.data.local.entity.UserEntity

data class PostWithUser(
    @Embedded
    val post: PostEntity,
    
    @Relation(
        parentColumn = "userId",
        entityColumn = "uid"
    )
    val user: UserEntity?
)
