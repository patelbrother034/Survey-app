package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

class SurveyRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val taskDao = db.taskDao()
    private val assignmentDao = db.assignmentDao()
    private val entryDao = db.surveyEntryDao()
    private val gpsDao = db.gpsPointDao()

    val usersFlow: Flow<List<User>> = userDao.getAllUsersFlow()
    val tasksFlow: Flow<List<SurveyTask>> = taskDao.getAllTasksFlow()
    val assignmentsFlow: Flow<List<Assignment>> = assignmentDao.getAllAssignmentsFlow()
    val entriesFlow: Flow<List<SurveyEntry>> = entryDao.getAllEntriesFlow()
    val gpsPointsFlow: Flow<List<GpsPoint>> = gpsDao.getAllGpsPointsFlow()

    fun getGpsPointsByUserIdFlow(userId: String): Flow<List<GpsPoint>> {
        return gpsDao.getGpsPointsByUserIdFlow(userId)
    }

    // User Operations
    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun deleteUserById(id: String) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id)
    }

    // Task Operations
    suspend fun createTask(task: SurveyTask) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun deleteModelTask(fileNumber: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskByFileNumber(fileNumber)
        assignmentDao.deleteAssignmentByFileNumber(fileNumber)
    }

    // Assignment Operations
    suspend fun assignTask(assignment: Assignment) = withContext(Dispatchers.IO) {
        assignmentDao.insertAssignment(assignment)
        // Update task status as Assigned
        taskDao.getTaskByFileNumber(assignment.fileNumber)?.let { existingTask ->
            taskDao.insertTask(existingTask.copy(status = "Assigned"))
        }
    }

    suspend fun updateTaskStatusOnly(fileNumber: String, status: String) = withContext(Dispatchers.IO) {
        taskDao.getTaskByFileNumber(fileNumber)?.let { existingTask ->
            taskDao.insertTask(existingTask.copy(status = status))
        }
    }

    suspend fun reassignTask(fileNumber: String, userId: String, userName: String) = withContext(Dispatchers.IO) {
        val assignment = Assignment(
            fileNumber = fileNumber,
            assignedUserId = userId,
            assignedUserName = userName,
            assignedDate = System.currentTimeMillis(),
            completionPercentage = 0,
            currentStatus = "Assigned"
        )
        assignmentDao.insertAssignment(assignment)
        taskDao.getTaskByFileNumber(fileNumber)?.let { existingTask ->
            taskDao.insertTask(existingTask.copy(status = "Assigned"))
        }
    }

    // Survey Entry & GPS Operations
    suspend fun saveSurveyEntry(entry: SurveyEntry): Long = withContext(Dispatchers.IO) {
        val entryId = entryDao.insertEntry(entry)

        // Store active coordinates as an explicit GPS log
        if (entry.latitude != 0.0 && entry.longitude != 0.0) {
            gpsDao.insertGpsPoint(
                GpsPoint(
                    surveyEntryId = entryId.toInt(),
                    userId = entry.surveyorId,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    accuracy = entry.accuracy,
                    timestamp = entry.timestamp,
                    tag = "survey"
                )
            )
        }

        // If it's a concrete layout submission (not draft), we check if there's a fileNumber associated
        if (!entry.isDraft && entry.fileNumber != null) {
            taskDao.getTaskByFileNumber(entry.fileNumber)?.let { existingTask ->
                taskDao.insertTask(existingTask.copy(status = "Completed"))
            }
            assignmentDao.getAllAssignments().find { it.fileNumber == entry.fileNumber }?.let { existingAssign ->
                assignmentDao.insertAssignment(existingAssign.copy(completionPercentage = 100, currentStatus = "Completed"))
            }
        }

        entryId
    }

    suspend fun deleteSurveyEntryById(id: Int) = withContext(Dispatchers.IO) {
        entryDao.deleteEntryById(id)
    }

    suspend fun addGpsPoint(point: GpsPoint) = withContext(Dispatchers.IO) {
        gpsDao.insertGpsPoint(point)
    }

    // Database Seed Verification
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val count = userDao.getAllUsers().size
        if (count == 0) {
            Log.d("SurveyRepository", "Database is empty! Generating preloaded sample data...")
            seedData()
        }
    }

    private suspend fun seedData() {
        // 1. Create 10 Users (1 admin, 9 surveyors)
        val users = listOf(
            User("USR-001", "Administrator", "admin", "123", "9876543210", "Administration", "Chief Director", "Active", "ADMIN"),
            User("USR-002", "Surveyor Agent One", "user", "123", "9876543211", "Field Assessment", "Senior Surveyor", "Active", "USER"),
            User("USR-003", "John Surveyor", "john", "123", "9876543212", "Agriculture Surveying", "Junior Officer", "Active", "USER"),
            User("USR-004", "Alice Smith", "alice", "123", "9876543213", "Civil Works Department", "Inspecting Engineer", "Active", "USER"),
            User("USR-005", "Bobby Vance", "bobby", "123", "9876543214", "Revenue Department", "Tehsildar Assistant", "Active", "USER"),
            User("USR-006", "Sarah Connor", "sarah", "123", "9876543215", "Forestry Conservation", "Forest Inspector", "Active", "USER"),
            User("USR-007", "Clara Oswald", "clara", "123", "9876543216", "Urban Planning Division", "Municipal Planner", "Active", "USER"),
            User("USR-008", "David Miller", "david", "123", "9876543217", "Property Assessment", "Valuer", "Disabled", "USER"),
            User("USR-009", "Emily Watson", "emily", "123", "9876543218", "Water Commission", "Hydro-analyst", "Active", "USER"),
            User("USR-010", "Frank Castle", "frank", "123", "9876543219", "Risk Management", "Security Auditor", "Active", "USER")
        )
        userDao.insertUsers(users)

        // 2. Create 20 Task Files
        val priorities = listOf("High", "Medium", "Low")
        val districts = listOf("Gandhinagar", "Ahmedabad", "Surat", "Rajkot", "Vadodara")
        val talukas = listOf("Taluka North", "Taluka South", "Taluka East", "Taluka West", "Central")
        val villages = listOf("Sardarnagar", "Rampur", "Krishnanagar", "Navjivan Village", "Gokul", "Vaikunth", "Sundarban")

        val baseTime = System.currentTimeMillis()
        val dayInMs = 24 * 60 * 60 * 1000L

        val tasks = ArrayList<SurveyTask>()
        for (i in 1..20) {
            val district = districts[i % districts.size]
            val taluka = talukas[i % talukas.size]
            val village = villages[i % villages.size]
            val priority = priorities[i % priorities.size]
            val status = when (i) {
                in 1..5 -> "Pending"
                in 6..11 -> "Assigned"
                in 12..16 -> "In Progress"
                else -> "Completed"
            }

            tasks.add(
                SurveyTask(
                    fileNumber = "FL-10$i",
                    fileName = "Infrastructure Assessment Proj $i",
                    description = "Comprehensive regional and agricultural land checking for file ID $i inside $village, detailing local public amenities validation.",
                    village = village,
                    taluka = taluka,
                    district = district,
                    dueDate = baseTime + (i * 2 * dayInMs),
                    priority = priority,
                    status = status
                )
            )
        }
        taskDao.insertTasks(tasks)

        // 3. Create Assignments matching tasks
        // Let's assign tasks 6 to 20
        // Surveyor users USR-002 (idx 1) up to USR-007 (idx 6)
        val assignedUsersList = users.filter { it.role == "USER" && it.status == "Active" }
        for (i in 6..20) {
            val u = assignedUsersList[(i - 6) % assignedUsersList.size]
            val cStatus = when (i) {
                in 6..11 -> "Assigned"
                in 12..16 -> "In Progress"
                else -> "Completed"
            }
            val percentage = when (cStatus) {
                "Assigned" -> 0
                "In Progress" -> 45
                else -> 100
            }
            assignmentDao.insertAssignment(
                Assignment(
                    fileNumber = "FL-10$i",
                    assignedUserId = u.id,
                    assignedUserName = u.name,
                    assignedDate = baseTime - ((20 - i) * dayInMs),
                    completionPercentage = percentage,
                    currentStatus = cStatus
                )
            )
        }

        // 4. Generate 50 Survey Entries
        // Center coordinates roughly around Gandhinagar, Gujarat (23.2156° N, 72.6369° E)
        val baseLat = 23.2156
        val baseLng = 72.6369
        val rand = Random(42) // Seeded random for consistency

        val categories = listOf("Agriculture", "Infrastructure", "Social", "Economic")
        val names = listOf("Ramesh Patel", "Suresh Mehta", "Amit Shah", "Narendra Modi", "Geeta Ben", "Rahul Sharma", "Savita Devi", "Vinod Khanna", "Vijay Rupani", "Anand Patel")
        val fatherNames = listOf("Kantilal Patel", "Manish Mehta", "Dinesh Shah", "Damodardas Modi", "Dhaval Ben", "Mohan Sharma", "Devanand Devi", "Kishore Khanna", "Babubhai Rupani", "Hasmukh Patel")
        val genders = listOf("Male", "Female", "Other")

        val surveyEntriesList = ArrayList<SurveyEntry>()
        val gpsPointsToInsert = ArrayList<GpsPoint>()

        for (i in 1..50) {
            val subName = names[i % names.size] + " $i"
            val father = fatherNames[i % fatherNames.size]
            val num = "91234567${i % 10}${i % 10}"
            val age = 22 + (i % 55)
            val gender = genders[i % genders.size]

            val dIdx = i % districts.size
            val vIdx = i % villages.size
            val cIdx = i % categories.size

            // Let's calculate a small random spatial offset (around 0.05 degrees ~ 5-6 km max)
            val offLat = (rand.nextDouble() - 0.5) * 0.1
            val offLng = (rand.nextDouble() - 0.5) * 0.1
            val lat = baseLat + offLat
            val lng = baseLng + offLng

            val sUserId = assignedUsersList[i % assignedUsersList.size].id
            val sUserName = assignedUsersList[i % assignedUsersList.size].name
            val linkTask = if (i <= 10) "FL-10${10 + i}" else null // Link some submissions directly to files 11-20
            val draft = i == 49 || i == 50 // last two are drafts

            val entryObj = SurveyEntry(
                fileNumber = linkTask,
                surveyorId = sUserId,
                surveyorName = sUserName,
                name = subName,
                fatherName = father,
                mobileNumber = num,
                age = age,
                gender = gender,
                state = "Gujarat",
                district = districts[dIdx],
                taluka = talukas[i % talukas.size],
                village = villages[vIdx],
                address = "Sector ${i % 30}, Plot No ${10 + i}, ${villages[vIdx]}",
                category = categories[cIdx],
                remarks = "Submitting official verification regarding crop/boundary status $i for community review.",
                notes = "Local land owners agreed on proposed mapping and checked structural configurations.",
                imagesCsv = "uri_image_${i}_1,uri_image_${i}_2",
                latitude = lat,
                longitude = lng,
                accuracy = 4.2f + (i % 10) * 0.5f,
                timestamp = baseTime - (i * 4 * 60 * 60 * 1000L), // spaced hours apart
                isDraft = draft
            )

            surveyEntriesList.add(entryObj)
        }

        // Insert we have to insert sequentially to guarantee getting ID back
        for (i in 0 until surveyEntriesList.size) {
            val ent = surveyEntriesList[i]
            val insertedId = entryDao.insertEntry(ent)

            // Let's create tracking routes!
            // Each surveyor will have a route history of 5 points spaced geographically
            val surveyorId = ent.surveyorId
            val subLat = ent.latitude
            val subLng = ent.longitude

            // Add the coordinate for the survey point
            gpsPointsToInsert.add(
                GpsPoint(
                    surveyEntryId = insertedId.toInt(),
                    userId = surveyorId,
                    latitude = subLat,
                    longitude = subLng,
                    accuracy = ent.accuracy,
                    timestamp = ent.timestamp,
                    tag = "survey"
                )
            )

            // Add additional trailing historical tracking points (route logs) to build beautiful connected lines
            for (p in 1..4) {
                gpsPointsToInsert.add(
                    GpsPoint(
                        surveyEntryId = null,
                        userId = surveyorId,
                        latitude = subLat - (p * 0.003),
                        longitude = subLng - (p * 0.002),
                        accuracy = ent.accuracy + p * 0.8f,
                        timestamp = ent.timestamp - (p * 20 * 60 * 1000L), // 20-minute gap, prior
                        tag = "tracking"
                    )
                )
            }
        }

        gpsDao.insertGpsPoints(gpsPointsToInsert)
        Log.d("SurveyRepository", "Successfully populated DB: 10 Users, 20 Tasks, 50 Survey Entries, and tracking GPS routes.")
    }
}
