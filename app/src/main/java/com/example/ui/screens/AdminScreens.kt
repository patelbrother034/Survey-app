package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.model.*
import com.example.ui.widgets.InteractiveMap
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Users", "Tasks", "Entries", "Map & GIS", "Reports")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Field Survey Pro HQ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Central Administrator Terminal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { idx, label ->
                    val icon = when (idx) {
                        0 -> Icons.Default.Dashboard
                        1 -> Icons.Default.People
                        2 -> Icons.Default.Folder
                        3 -> Icons.Default.Inbox
                        4 -> Icons.Default.Map
                        5 -> Icons.Default.Assessment
                        else -> Icons.Default.Info
                    }
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { 
                            Icon(
                                imageVector = icon, 
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = label, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        }
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AdminStatsDashboard(viewModel)
                1 -> AdminUserManagement(viewModel)
                2 -> AdminTaskManagement(viewModel)
                3 -> AdminEntriesReview(viewModel)
                4 -> AdminMapAndGis(viewModel)
                5 -> AdminReportsModule(viewModel)
            }
        }
    }
}

// ==== 0. STATISTICS DASHBOARD ====
@Composable
fun AdminStatsDashboard(viewModel: MainViewModel) {
    val usersList by viewModel.users.collectAsState()
    val tasksList by viewModel.tasks.collectAsState()
    val assignmentsList by viewModel.assignments.collectAsState()
    val entriesList by viewModel.entries.collectAsState()
    val gpsPointsList by viewModel.gpsPoints.collectAsState()

    val totalUsers = usersList.size
    val totalFiles = tasksList.size
    val assignedFiles = tasksList.count { it.status == "Assigned" || it.status == "In Progress" }
    val completedFiles = tasksList.count { it.status == "Completed" }
    val pendingFiles = tasksList.count { it.status == "Pending" }
    val totalEntries = entriesList.size
    val totalGps = gpsPointsList.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Time
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        "Local Field Operations Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Everything synchronized in SQLite sandbox on device storage. No internet required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Row of Stats Matrix
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Users", totalUsers.toString(), Icons.Default.People, Color(0xFF42A5F5), Modifier.weight(1f))
                    StatCard("Survey Files", totalFiles.toString(), Icons.Default.Folder, Color(0xFF66BB6A), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Completed", completedFiles.toString(), Icons.Default.CheckCircle, Color(0xFF66BB6A), Modifier.weight(1f))
                    StatCard("Pending", pendingFiles.toString(), Icons.Default.PendingActions, Color(0xFFFFA726), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Submissions", totalEntries.toString(), Icons.Default.FactCheck, Color(0xFFEC407A), Modifier.weight(1f))
                    StatCard("GPS Points", totalGps.toString(), Icons.Default.PinDrop, Color(0xFFAB47BC), Modifier.weight(1f))
                }
            }
        }

        // Donut Pie chart & line visualizations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Completion Rate Analytics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom Hand Drawn Donut Pie Chart Canvas
                        Canvas(modifier = Modifier.size(110.dp)) {
                            val activeAngle = if (totalFiles > 0) (completedFiles.toFloat() / totalFiles.toFloat()) * 360f else 0f
                            val pendingAngle = 360f - activeAngle

                            drawArc(
                                color = Color(0xFF66BB6A),
                                startAngle = -90f,
                                sweepAngle = activeAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFFFFA726),
                                startAngle = -90f + activeAngle,
                                sweepAngle = pendingAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Column {
                            LegendRow("Completed ($completedFiles)", Color(0xFF66BB6A))
                            LegendRow("InProgress/Assigned ($assignedFiles)", Color(0xFF42A5F5))
                            LegendRow("Pending Pure ($pendingFiles)", Color(0xFFFFA726))
                        }
                    }
                }
            }
        }

        // Horizontal Bar Chart representing user contributions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Surveyor Performance Profiles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (entriesList.isEmpty()) {
                        Text("No entries recorded yet to aggregate statistics.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        val grouped = entriesList.groupBy { it.surveyorName }
                        grouped.forEach { (name, items) ->
                            val score = items.size
                            Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .height(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF26A69A).copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((score / 50f).coerceAtMost(1f))
                                            .background(Color(0xFF26A69A))
                                    )
                                }
                                Text(
                                    "$score entries",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.3f).padding(start = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Feed Item
        item {
            Text("Recent Activities Feed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (entriesList.isEmpty()) {
            item {
                Text("No recent submissions.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(entriesList.take(6)) { item ->
                ActivityLogCard(item)
            }
        }
    }
}

@Composable
fun StatCard(label: String, valStr: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(valStr, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun LegendRow(label: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.LightGray)
    }
}

@Composable
fun ActivityLogCard(entry: SurveyEntry) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFFE0F2F1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Survey submitted for ${entry.name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Surveyor: ${entry.surveyorName} • Village: ${entry.village}",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

// ==== 1. ADMIN USER MANAGEMENT ====
@Composable
fun AdminUserManagement(viewModel: MainViewModel) {
    val usersList by viewModel.users.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Dialog state controllers
    var uid by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var uRealName by remember { mutableStateOf("") }
    var upassword by remember { mutableStateOf("") }
    var umobile by remember { mutableStateOf("") }
    var udept by remember { mutableStateOf("") }
    var udesig by remember { mutableStateOf("") }
    var ustatus by remember { mutableStateOf("Active") }
    var isEditing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("User Management Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Admin can create, deactivate, and remove surveyors.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(usersList) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(
                                    if (u.status == "Active") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (u.status == "Active") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ID: ${u.id} • Dept: ${u.department}", fontSize = 11.sp, color = Color.LightGray)
                                Text("Desg: ${u.designation} • Mob: ${u.mobileNumber}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = if (u.status == "Active") Color(0xFFE8F5E9) else Color(0xFFFDE0DB),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        u.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (u.status == "Active") Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        // Edit
                                        uid = u.id
                                        uname = u.username
                                        uRealName = u.name
                                        upassword = u.password
                                        umobile = u.mobileNumber
                                        udept = u.department
                                        udesig = u.designation
                                        ustatus = u.status
                                        isEditing = true
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { viewModel.toggleUserStatus(u.id) }) {
                                        Icon(
                                            if (u.status == "Active") Icons.Default.Block else Icons.Default.Check,
                                            contentDescription = "Toggle status",
                                            tint = if (u.status == "Active") Color.Yellow else Color.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (u.id != "USR-001" && u.id != "USR-002") {
                                        IconButton(onClick = { viewModel.deleteUser(u.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating button to trigger creation
        FloatingActionButton(
            onClick = {
                uid = "USR-0" + (usersList.size + 1)
                uname = ""
                uRealName = ""
                upassword = "123"
                umobile = ""
                udept = ""
                udesig = ""
                ustatus = "Active"
                isEditing = false
                showDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add User", tint = Color.White)
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (isEditing) "Modify Surveyor Account" else "Enlist New Surveyor", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = uid, onValueChange = { if (!isEditing) uid = it }, label = { Text("User ID") }, enabled = !isEditing, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = uRealName, onValueChange = { uRealName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = uname, onValueChange = { uname = it }, label = { Text("Username login") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = upassword, onValueChange = { upassword = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = umobile, onValueChange = { umobile = it }, label = { Text("Mobile Phone") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = udept, onValueChange = { udept = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = udesig, onValueChange = { udesig = it }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            if (uid.isNotBlank() && uname.isNotBlank() && uRealName.isNotBlank()) {
                                val userObj = User(uid, uRealName, uname, upassword, umobile, udept, udesig, ustatus, "USER")
                                if (isEditing) viewModel.updateUser(userObj) else viewModel.addUser(userObj)
                                showDialog = false
                            } else {
                                viewModel.showToast("All key parameters are required.")
                            }
                        }
                    ) {
                        Text("Save Details")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// ==== 2. ADMIN TASK/FILE MANAGEMENT ====
@Composable
fun AdminTaskManagement(viewModel: MainViewModel) {
    val tasksList by viewModel.tasks.collectAsState()
    val usersList by viewModel.users.collectAsState()
    val assignmentsList by viewModel.assignments.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var taskToAssign by remember { mutableStateOf<SurveyTask?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var fNum by remember { mutableStateOf("") }
    var fName by remember { mutableStateOf("") }
    var fDesc by remember { mutableStateOf("") }
    var fVillage by remember { mutableStateOf("") }
    var fTaluka by remember { mutableStateOf("") }
    var fDistrict by remember { mutableStateOf("") }
    var fPriority by remember { mutableStateOf("High") }
    var fStatus by remember { mutableStateOf("Pending") }
    var isEditing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("File & Task Master Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Admin creates files to map specific villages and assigns surveyors.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(tasksList) { t ->
                    val matchingAssign = assignmentsList.find { it.fileNumber == t.fileNumber }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.fileNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                Surface(
                                    color = when (t.priority) {
                                        "High" -> Color(0x33E53935)
                                        "Medium" -> Color(0x33FFA726)
                                        else -> Color(0x3343A047)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        t.priority + " Priority",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (t.priority) {
                                            "High" -> Color(0xFFEF5350)
                                            "Medium" -> Color(0xFFFFB74D)
                                            else -> Color(0xFF81C784)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(t.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(t.description, fontSize = 11.sp, color = Color.LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Village: ${t.village}", fontSize = 11.sp, color = Color.LightGray)
                                Text("Taluka: ${t.taluka}", fontSize = 11.sp, color = Color.LightGray)
                                Text("District: ${t.district}", fontSize = 11.sp, color = Color.LightGray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0x11FFFFFF))
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Status: ${t.status}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (t.status == "Completed") Color.Green else Color.Yellow
                                    )
                                    if (matchingAssign != null) {
                                        Text(
                                            "Surveyor: ${matchingAssign.assignedUserName} (${matchingAssign.completionPercentage}%)",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    } else {
                                        Text("Surveyor: Unassigned", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        // Edit Dialogue
                                        fNum = t.fileNumber
                                        fName = t.fileName
                                        fDesc = t.description
                                        fVillage = t.village
                                        fTaluka = t.taluka
                                        fDistrict = t.district
                                        fPriority = t.priority
                                        fStatus = t.status
                                        isEditing = true
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit File", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }

                                    Button(
                                        onClick = {
                                            taskToAssign = t
                                            showAssignDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (matchingAssign != null) "Reassign" else "Assign", fontSize = 11.sp, color = Color.White)
                                    }

                                    IconButton(onClick = { viewModel.deleteTask(t.fileNumber) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Floating button trigger
        FloatingActionButton(
            onClick = {
                fNum = "FL-" + (1000 + tasksList.size + 1)
                fName = ""
                fDesc = ""
                fVillage = ""
                fTaluka = ""
                fDistrict = ""
                fPriority = "High"
                fStatus = "Pending"
                isEditing = false
                showDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Task/File", tint = Color.White)
        }

        // Add/Edit Task Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (isEditing) "Edit File details" else "Register New Survey File", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = fNum, onValueChange = { if (!isEditing) fNum = it }, label = { Text("File Number Index") }, enabled = !isEditing, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("Survey Project Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fDesc, onValueChange = { fDesc = it }, label = { Text("Task Description Details") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fVillage, onValueChange = { fVillage = it }, label = { Text("Village name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fTaluka, onValueChange = { fTaluka = it }, label = { Text("Taluka") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fDistrict, onValueChange = { fDistrict = it }, label = { Text("District") }, modifier = Modifier.fillMaxWidth())

                        Text("Priority Scale Rating", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                FilterChip(
                                    selected = fPriority == p,
                                    onClick = { fPriority = p },
                                    label = { Text(p) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            if (fNum.isNotBlank() && fName.isNotBlank() && fVillage.isNotBlank()) {
                                val taskObj = SurveyTask(
                                    fileNumber = fNum,
                                    fileName = fName,
                                    description = fDesc,
                                    village = fVillage,
                                    taluka = fTaluka,
                                    district = fDistrict,
                                    dueDate = System.currentTimeMillis() + 8 * 24 * 60 * 60 * 1000L,
                                    priority = fPriority,
                                    status = fStatus
                                )
                                if (isEditing) viewModel.editTask(taskObj) else viewModel.createTask(taskObj)
                                showDialog = false
                            } else {
                                viewModel.showToast("Required parameters (File ID, Title, Location) are missing.")
                            }
                        }
                    ) {
                        Text("Record File")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Assignment assignment selector dialog
        if (showAssignDialog) {
            val surveyors = usersList.filter { it.role == "USER" && it.status == "Active" }

            AlertDialog(
                onDismissRequest = { showAssignDialog = false },
                title = { Text("Assign File: ${taskToAssign?.fileNumber}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose an Active Surveyor Agent to map this sector:")
                        Spacer(modifier = Modifier.height(4.dp))
                        if (surveyors.isEmpty()) {
                            Text("No active surveyors available currently in the directory.", color = Color.Red, fontSize = 12.sp)
                        } else {
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(surveyors) { s ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.assignTask(taskToAssign!!.fileNumber, s.id)
                                                showAssignDialog = false
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(s.name, fontWeight = FontWeight.Bold)
                                                Text("Dept: ${s.department}", fontSize = 11.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAssignDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// ==== 3. ADMIN ENTRIES REVIEW ====
@Composable
fun AdminEntriesReview(viewModel: MainViewModel) {
    val entriesList by viewModel.entries.collectAsState()
    val usersList by viewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var userFilter by remember { mutableStateOf<String?>(null) }
    var villageFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    var expandedUser by remember { mutableStateOf(false) }
    var expandedVillage by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    // Aggregate values for Filter lists
    val surveyorNames = remember(usersList) { usersList.filter { it.role == "USER" }.map { it.name } }
    val uniqueVillages = remember(entriesList) { entriesList.map { it.village }.distinct() }
    val categories = listOf("Agriculture", "Infrastructure", "Social", "Economic")

    val filteredEntries = remember(entriesList, searchQuery, userFilter, villageFilter, categoryFilter) {
        entriesList.filter { ent ->
            val matchTxt = searchQuery.isBlank() || ent.name.contains(searchQuery, true) || ent.surveyorName.contains(searchQuery, true)
            val matchUser = userFilter == null || ent.surveyorName == userFilter
            val matchVillage = villageFilter == null || ent.village == villageFilter
            val matchCategory = categoryFilter == null || ent.category == categoryFilter
            matchTxt && matchUser && matchVillage && matchCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Survey Submissions Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Search, filter, examine GPS coordinates, and delete local submissions.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Outline
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by surveyor or respondent name", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown filter triggers row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // User filter dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedUser = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (userFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(userFilter ?: "Surveyor", fontSize = 11.sp, color = if (userFilter != null) Color.White else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                    DropdownMenuItem(text = { Text("All Surveyors") }, onClick = { userFilter = null; expandedUser = false })
                    surveyorNames.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { userFilter = s; expandedUser = false })
                    }
                }
            }

            // Village filter dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedVillage = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (villageFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(villageFilter ?: "Village", fontSize = 11.sp, color = if (villageFilter != null) Color.White else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedVillage, onDismissRequest = { expandedVillage = false }) {
                    DropdownMenuItem(text = { Text("All Villages") }, onClick = { villageFilter = null; expandedVillage = false })
                    uniqueVillages.forEach { v ->
                        DropdownMenuItem(text = { Text(v) }, onClick = { villageFilter = v; expandedVillage = false })
                    }
                }
            }

            // Category filter dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedCategory = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (categoryFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(categoryFilter ?: "Category", fontSize = 11.sp, color = if (categoryFilter != null) Color.White else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                    DropdownMenuItem(text = { Text("All Categories") }, onClick = { categoryFilter = null; expandedCategory = false })
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { categoryFilter = c; expandedCategory = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No survey submissions found matching these filters.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                Text(
                                    SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(entry.timestamp),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Respondent: ${entry.name} (${entry.gender}, Age ${entry.age})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Father: ${entry.fatherName} • Mobile: ${entry.mobileNumber}", fontSize = 11.sp, color = Color.LightGray)
                            Text("Sector village: ${entry.village}, ${entry.district} • Address: ${entry.address}", fontSize = 11.sp, color = Color.LightGray)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(entry.remarks, fontSize = 12.sp, color = Color.White, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = Color(0x11FFFFFF))
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Surveyor: ${entry.surveyorName}", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                    Text("Coords: ${String.format("%.5f", entry.latitude)}, ${String.format("%.5f", entry.longitude)} (±${String.format("%.1f", entry.accuracy)}m)", fontSize = 11.sp, color = Color(0xFF81C784))
                                }

                                Row {
                                    IconButton(onClick = { viewModel.deleteSurveyEntry(entry.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Submission", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==== 4. ADMIN TAB: MAP AND GIS ====
@Composable
fun AdminMapAndGis(viewModel: MainViewModel) {
    val entriesList by viewModel.entries.collectAsState()
    val gpsPointsList by viewModel.gpsPoints.collectAsState()
    val usersList by viewModel.users.collectAsState()

    var userFilter by remember { mutableStateOf<String?>(null) }
    var villageFilter by remember { mutableStateOf<String?>(null) }

    var expandedUser by remember { mutableStateOf(false) }
    var expandedVillage by remember { mutableStateOf(false) }

    val surveyorNames = remember(usersList) { usersList.filter { it.role == "USER" }.map { it.name } }
    val uniqueVillages = remember(entriesList) { entriesList.map { it.village }.distinct() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedUser = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (userFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(userFilter ?: "Filter Surveyor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                    DropdownMenuItem(text = { Text("All Surveyors") }, onClick = { userFilter = null; expandedUser = false })
                    surveyorNames.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { userFilter = s; expandedUser = false })
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedVillage = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (villageFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(villageFilter ?: "Filter Village", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedVillage, onDismissRequest = { expandedVillage = false }) {
                    DropdownMenuItem(text = { Text("All Villages") }, onClick = { villageFilter = null; expandedVillage = false })
                    uniqueVillages.forEach { v ->
                        DropdownMenuItem(text = { Text(v) }, onClick = { villageFilter = v; expandedVillage = false })
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            // Draw Interactive custom Map canvas
            InteractiveMap(
                entries = entriesList,
                gpsPoints = gpsPointsList,
                filterUser = userFilter,
                filterVillage = villageFilter
            )
        }
    }
}

// ==== 5. REPORTS EXPORTER MODULE ====
@Composable
fun AdminReportsModule(viewModel: MainViewModel) {
    val entriesList by viewModel.entries.collectAsState()
    val usersList by viewModel.users.collectAsState()

    var userFilter by remember { mutableStateOf<String?>(null) }
    var villageFilter by remember { mutableStateOf<String?>(null) }

    var expandedUser by remember { mutableStateOf(false) }
    var expandedVillage by remember { mutableStateOf(false) }

    val surveyorNames = remember(usersList) { usersList.filter { it.role == "USER" }.map { it.name } }
    val uniqueVillages = remember(entriesList) { entriesList.map { it.village }.distinct() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    "Local Dynamic Reports Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Generate reports in JSON and CSV format stored securely in local app storage inside the tablet/mobile sandboxed filesystem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Text("Select Filter Criteria to limit rows:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedUser = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (userFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(userFilter ?: "All Surveyors", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                    DropdownMenuItem(text = { Text("All Surveyors") }, onClick = { userFilter = null; expandedUser = false })
                    surveyorNames.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { userFilter = s; expandedUser = false })
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedVillage = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (villageFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(villageFilter ?: "All Villages", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expandedVillage, onDismissRequest = { expandedVillage = false }) {
                    DropdownMenuItem(text = { Text("All Villages") }, onClick = { villageFilter = null; expandedVillage = false })
                    uniqueVillages.forEach { v ->
                        DropdownMenuItem(text = { Text(v) }, onClick = { villageFilter = v; expandedVillage = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    "Target Records Count: ${entriesList.filter { (userFilter == null || it.surveyorName == userFilter) && (villageFilter == null || it.village == villageFilter) }.size} entries",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.generateCSVReport(userFilter, villageFilter, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Filtered CSV File", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.generateJSONReport(userFilter, villageFilter, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Filtered JSON File", color = Color.White)
                }
            }
        }

        // Info box indicating offline location details
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "File storage writes outputs inside the secure local system sandbox in Device/storage/Android/data/com.example/files/Documents.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
