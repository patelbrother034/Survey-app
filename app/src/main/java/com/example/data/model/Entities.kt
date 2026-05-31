package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val password: String,
    val mobileNumber: String,
    val department: String,
    val designation: String,
    val status: String, // "Active", "Disabled"
    val role: String // "ADMIN", "USER"
)

@Entity(tableName = "tasks")
data class SurveyTask(
    @PrimaryKey val fileNumber: String, // e.g., "FL-01"
    val fileName: String,
    val description: String,
    val village: String,
    val taluka: String,
    val district: String,
    val dueDate: Long,
    val priority: String, // "High", "Medium", "Low"
    val status: String // "Pending", "Assigned", "In Progress", "Completed"
)

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey val fileNumber: String,
    val assignedUserId: String,
    val assignedUserName: String,
    val assignedDate: Long,
    val completionPercentage: Int,
    val currentStatus: String // "Assigned", "In Progress", "Completed"
)

@Entity(tableName = "survey_entries")
data class SurveyEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileNumber: String?,
    val surveyorId: String,
    val surveyorName: String,
    val name: String,
    val fatherName: String,
    val mobileNumber: String,
    val age: Int,
    val gender: String,
    val state: String,
    val district: String,
    val taluka: String,
    val village: String,
    val address: String,
    val category: String, // "Agriculture", "Infrastructure", "Social", "Economic", "Other"
    val remarks: String,
    val notes: String,
    val imagesCsv: String, // Comma separated list of mock image URIs or local file paths
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isDraft: Boolean
)

@Entity(tableName = "gps_points")
data class GpsPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surveyEntryId: Int?,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val tag: String // "survey", "tracking"
)
