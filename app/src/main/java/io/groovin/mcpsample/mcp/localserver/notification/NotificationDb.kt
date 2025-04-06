package io.groovin.mcpsample.mcp.localserver.notification

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


@Entity(tableName = "user_notifications")
data class UserNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val text: String,
    val subText: String?,
    val packageName: String,
    val appName: String,
    val datetime: Long
)

@Dao
interface UserNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: UserNotification)
    @Delete
    suspend fun delete(notification: UserNotification)
    @Query("DELETE FROM user_notifications WHERE id = :id")
    suspend fun deleteById(id: Int)
    @Query("DELETE FROM user_notifications WHERE id NOT IN (SELECT id FROM user_notifications ORDER BY datetime DESC LIMIT :limit)")
    suspend fun deleteOldNotifications(limit: Int)
    @Query("SELECT * FROM user_notifications WHERE text LIKE '%' || :content || '%' OR subText LIKE '%' || :content || '%' OR packageName LIKE '%' || :content || '%' ORDER BY datetime ASC")
    suspend fun searchByContent(content: String): List<UserNotification>
    @Query("SELECT * FROM user_notifications WHERE appName LIKE '%' || :appName || '%' ORDER BY datetime ASC")
    suspend fun searchByAppName(appName: String): List<UserNotification>
    @Query("SELECT * FROM user_notifications WHERE appName LIKE '%' || :appName || '%' AND (text LIKE '%' || :content || '%' OR subText LIKE '%' || :content || '%' OR packageName LIKE '%' || :content || '%') ORDER BY datetime ASC")
    suspend fun searchByAppNameWithContent(appName: String, content: String): List<UserNotification>
    @Query("SELECT * FROM user_notifications ORDER BY datetime ASC")
    suspend fun getAll(): List<UserNotification>
}

@Database(entities = [UserNotification::class], version = 1, exportSchema = false)
abstract class UserNotificationDatabase : RoomDatabase() {
    abstract fun userNotificationDao(): UserNotificationDao
}


object NotificationDatabaseProvider {
    private var INSTANCE: UserNotificationDao? = null
    fun getDao(context: Context): UserNotificationDao {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                UserNotificationDatabase::class.java,
                "user_notifications_db"
            ).build()
            val dao = instance.userNotificationDao()
            INSTANCE = dao
            dao
        }
    }
}