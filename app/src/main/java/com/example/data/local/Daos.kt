package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasksFlow(): Flow<List<SurveyTask>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<SurveyTask>

    @Query("SELECT * FROM tasks WHERE fileNumber = :fileNumber LIMIT 1")
    suspend fun getTaskByFileNumber(fileNumber: String): SurveyTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: SurveyTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<SurveyTask>)

    @Query("DELETE FROM tasks WHERE fileNumber = :fileNumber")
    suspend fun deleteTaskByFileNumber(fileNumber: String)
}

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments")
    fun getAllAssignmentsFlow(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments")
    suspend fun getAllAssignments(): List<Assignment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Query("DELETE FROM assignments WHERE fileNumber = :fileNumber")
    suspend fun deleteAssignmentByFileNumber(fileNumber: String)
}

@Dao
interface SurveyEntryDao {
    @Query("SELECT * FROM survey_entries ORDER BY timestamp DESC")
    fun getAllEntriesFlow(): Flow<List<SurveyEntry>>

    @Query("SELECT * FROM survey_entries ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<SurveyEntry>

    @Query("SELECT * FROM survey_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): SurveyEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SurveyEntry): Long

    @Query("DELETE FROM survey_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
}

@Dao
interface GpsPointDao {
    @Query("SELECT * FROM gps_points ORDER BY timestamp ASC")
    fun getAllGpsPointsFlow(): Flow<List<GpsPoint>>

    @Query("SELECT * FROM gps_points WHERE userId = :userId ORDER BY timestamp ASC")
    fun getGpsPointsByUserIdFlow(userId: String): Flow<List<GpsPoint>>

    @Query("SELECT * FROM gps_points ORDER BY timestamp ASC")
    suspend fun getAllGpsPoints(): List<GpsPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoint(point: GpsPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoints(points: List<GpsPoint>)

    @Query("DELETE FROM gps_points")
    suspend fun clearAllGpsPoints()
}
