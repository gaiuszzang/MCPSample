package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_memory")
data class UserMemory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val tags: String?,
)

@Dao
interface UserMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UserMemory)
    @Delete
    suspend fun delete(item: UserMemory)
    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun deleteById(id: Int)
    @Query("SELECT * FROM user_memory")
    suspend fun getAllMemory(): List<UserMemory>
    @Query("SELECT * FROM user_memory WHERE tags LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%'")
    suspend fun searchMemory(keyword: String): List<UserMemory>
    @Query("UPDATE user_memory SET content = :newContent WHERE id = :id")
    suspend fun updateContentById(id: Int, newContent: String)
    @Query("UPDATE user_memory SET tags = :tags WHERE id = :id")
    suspend fun updateTagsById(id: Int, tags: String)
}

@Database(entities = [UserMemory::class], version = 1, exportSchema = false)
abstract class UserMemoryDatabase : RoomDatabase() {
    abstract fun userMemoryDao(): UserMemoryDao
}


object UserMemoryDatabaseProvider {
    private var INSTANCE: UserMemoryDao? = null
    fun getDao(context: Context): UserMemoryDao {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                UserMemoryDatabase::class.java,
                "user_memory_db"
            ).build()
            val dao = instance.userMemoryDao()
            INSTANCE = dao
            dao
        }
    }
}
