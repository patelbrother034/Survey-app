package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SurveyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SurveyRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SurveyRepository(database)
        
        // Ensure Database is initialized and populated with sample data
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // 1. Core Reactive Flows from Database
    val users: StateFlow<List<User>> = repository.usersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<SurveyTask>> = repository.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<Assignment>> = repository.assignmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<SurveyEntry>> = repository.entriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gpsPoints: StateFlow<List<GpsPoint>> = repository.gpsPointsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Authentication & Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // 3. Application Preferences / Settings States
    private val _isDarkMode = MutableStateFlow(true) // Dark Mode by default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow("English")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _gpsFrequency = MutableStateFlow("Every 5 seconds")
    val gpsFrequency: StateFlow<String> = _gpsFrequency.asStateFlow()

    private val _autoExportDrafts = MutableStateFlow(false)
    val autoExportDrafts: StateFlow<Boolean> = _autoExportDrafts.asStateFlow()

    // Active Toast Notifications state
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setGpsFrequency(freq: String) {
        _gpsFrequency.value = freq
    }

    fun setAutoExportDrafts(enabled: Boolean) {
        _autoExportDrafts.value = enabled
    }

    // 4. Session Operations
    fun login(username: String, password: String): Boolean {
        _loginError.value = null

        // 1. Check Hardcoded Mock Logins first as described in request
        if (username.equals("admin", ignoreCase = true) && password == "123") {
            val adminUser = User(
                id = "USR-001",
                name = "Chief Admin Office",
                username = "admin",
                password = "123",
                mobileNumber = "9988776655",
                department = "Central Survey HQ",
                designation = "Survey Commissioner",
                status = "Active",
                role = "ADMIN"
            )
            _currentUser.value = adminUser
            showToast("Welcome Administrator")
            return true
        } else if (username.equals("user", ignoreCase = true) && password == "123") {
            val genericUser = User(
                id = "USR-002",
                name = "Field Surveyor",
                username = "user",
                password = "123",
                mobileNumber = "9988776654",
                department = "Field Operations",
                designation = "Senior Inspector",
                status = "Active",
                role = "USER"
            )
            _currentUser.value = genericUser
            showToast("Welcome Surveyor Agent")
            return true
        }

        // 2. Query dynamically added Users in DB
        var success = false
        viewModelScope.launch {
            val foundUser = repository.getUserByUsername(username)
            if (foundUser != null) {
                if (foundUser.password == password) {
                    if (foundUser.status == "Disabled") {
                        _loginError.value = "Your account is disabled. Contact Admin."
                        showToast("Account Disabled")
                    } else {
                        _currentUser.value = foundUser
                        showToast("Welcome ${foundUser.name}")
                        success = true
                    }
                } else {
                    _loginError.value = "Invalid Username or Password."
                }
            } else {
                _loginError.value = "Invalid Username or Password."
            }
        }
        return success
    }

    fun logout() {
        _currentUser.value = null
        showToast("Logged Out Successfully")
    }

    // 5. User Management Module (Admin)
    fun addUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
            showToast("User added: ${user.name}")
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
            showToast("User details updated: ${user.name}")
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUserById(userId)
            showToast("User deleted: $userId")
        }
    }

    fun toggleUserStatus(userId: String) {
        viewModelScope.launch {
            val userList = users.value
            val match = userList.find { it.id == userId }
            if (match != null) {
                val newStatus = if (match.status == "Active") "Disabled" else "Active"
                repository.insertUser(match.copy(status = newStatus))
                showToast("User status changed to $newStatus")
            }
        }
    }

    // 6. Task & Assignment Management Module (Admin)
    fun createTask(task: SurveyTask) {
        viewModelScope.launch {
            repository.createTask(task)
            showToast("Created File: ${task.fileNumber}")
        }
    }

    fun editTask(task: SurveyTask) {
        viewModelScope.launch {
            repository.createTask(task)
            showToast("Updated File: ${task.fileNumber}")
        }
    }

    fun deleteTask(fileNumber: String) {
        viewModelScope.launch {
            repository.deleteModelTask(fileNumber)
            showToast("Deleted File tasks & assignments: $fileNumber")
        }
    }

    fun assignTask(fileNumber: String, userId: String) {
        viewModelScope.launch {
            val userMatch = users.value.find { it.id == userId }
            if (userMatch != null) {
                val assignObj = Assignment(
                    fileNumber = fileNumber,
                    assignedUserId = userId,
                    assignedUserName = userMatch.name,
                    assignedDate = System.currentTimeMillis(),
                    completionPercentage = 0,
                    currentStatus = "Assigned"
                )
                repository.assignTask(assignObj)
                showToast("Assigned $fileNumber to ${userMatch.name}")
            }
        }
    }

    fun reassignTask(fileNumber: String, userId: String) {
        viewModelScope.launch {
            val userMatch = users.value.find { it.id == userId }
            if (userMatch != null) {
                repository.reassignTask(fileNumber, userId, userMatch.name)
                showToast("Reassigned file $fileNumber to ${userMatch.name}")
            }
        }
    }

    fun updateTaskProgress(fileNumber: String, status: String) {
        viewModelScope.launch {
            repository.updateTaskStatusOnly(fileNumber, status)
        }
    }

    // 7. Survey Entries Operations
    fun deleteSurveyEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteSurveyEntryById(id)
            showToast("Deleted survey entry #$id")
        }
    }

    fun submitSurvey(entry: SurveyEntry) {
        viewModelScope.launch {
            repository.saveSurveyEntry(entry)
            showToast("Survey Submitted Successfully")
        }
    }

    fun addGpsPoint(point: GpsPoint) {
        viewModelScope.launch {
            repository.addGpsPoint(point)
        }
    }

    // 8. Custom Document Reports Exporter
    fun generateCSVReport(filterUser: String? = null, filterVillage: String? = null, filterCategory: String? = null): String {
        val entryList = entries.value
        val filtered = entryList.filter { ent ->
            val uMatch = filterUser == null || ent.surveyorName.contains(filterUser, true) || ent.surveyorId == filterUser
            val vMatch = filterVillage == null || ent.village.contains(filterVillage, true)
            val cMatch = filterCategory == null || ent.category.equals(filterCategory, true)
            uMatch && vMatch && cMatch
        }

        val csvBuilder = java.lang.StringBuilder()
        csvBuilder.append("ID,File Number,Surveyor,Name,Father's Name,Phone,Age,Gender,Village,Taluka,District,Category,Latitude,Longitude,Timestamp,Draft\n")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        filtered.forEach { ent ->
            val fields = listOf(
                ent.id.toString(),
                ent.fileNumber ?: "N/A",
                ent.surveyorName,
                ent.name.replace(",", " "),
                ent.fatherName.replace(",", " "),
                ent.mobileNumber,
                ent.age.toString(),
                ent.gender,
                ent.village.replace(",", " "),
                ent.taluka.replace(",", " "),
                ent.district.replace(",", " "),
                ent.category,
                ent.latitude.toString(),
                ent.longitude.toString(),
                sdf.format(Date(ent.timestamp)),
                ent.isDraft.toString()
            )
            csvBuilder.append(fields.joinToString(","))
            csvBuilder.append("\n")
        }

        val result = csvBuilder.toString()
        saveLocalReportFile("SurveyReport_${System.currentTimeMillis()}.csv", result)
        return result
    }

    fun generateJSONReport(filterUser: String? = null, filterVillage: String? = null, filterCategory: String? = null): String {
        val entryList = entries.value
        val filtered = entryList.filter { ent ->
            val uMatch = filterUser == null || ent.surveyorName.contains(filterUser, true) || ent.surveyorId == filterUser
            val vMatch = filterVillage == null || ent.village.contains(filterVillage, true)
            val cMatch = filterCategory == null || ent.category.equals(filterCategory, true)
            uMatch && vMatch && cMatch
        }

        val jsonBuilder = java.lang.StringBuilder()
        jsonBuilder.append("[\n")
        filtered.forEachIndexed { idx, ent ->
            jsonBuilder.append("  {\n")
            jsonBuilder.append("    \"id\": ${ent.id},\n")
            jsonBuilder.append("    \"fileNumber\": \"${ent.fileNumber ?: "N/A"}\",\n")
            jsonBuilder.append("    \"surveyorName\": \"${ent.surveyorName}\",\n")
            jsonBuilder.append("    \"name\": \"${ent.name}\",\n")
            jsonBuilder.append("    \"fatherName\": \"${ent.fatherName}\",\n")
            jsonBuilder.append("    \"phone\": \"${ent.mobileNumber}\",\n")
            jsonBuilder.append("    \"age\": ${ent.age},\n")
            jsonBuilder.append("    \"gender\": \"${ent.gender}\",\n")
            jsonBuilder.append("    \"village\": \"${ent.village}\",\n")
            jsonBuilder.append("    \"taluka\": \"${ent.taluka}\",\n")
            jsonBuilder.append("    \"district\": \"${ent.district}\",\n")
            jsonBuilder.append("    \"category\": \"${ent.category}\",\n")
            jsonBuilder.append("    \"latitude\": ${ent.latitude},\n")
            jsonBuilder.append("    \"longitude\": ${ent.longitude},\n")
            jsonBuilder.append("    \"timestamp\": ${ent.timestamp},\n")
            jsonBuilder.append("    \"isDraft\": ${ent.isDraft}\n")
            jsonBuilder.append("  }")
            if (idx < filtered.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("]")

        val result = jsonBuilder.toString()
        saveLocalReportFile("SurveyReport_${System.currentTimeMillis()}.json", result)
        return result
    }

    private fun saveLocalReportFile(fileName: String, content: String) {
        val app = getApplication<Application>()
        try {
            // Save in cache directory or external files directory to keep sandbox friendly
            val dir = app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
                ?: app.cacheDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            val writer = FileWriter(file)
            writer.write(content)
            writer.close()
            Log.d("MainViewModel", "Saved offline local report files to path: ${file.absolutePath}")
            showToast("Saved Report: $fileName")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving report file", e)
            showToast("Report Export Failed: ${e.message}")
        }
    }
}
