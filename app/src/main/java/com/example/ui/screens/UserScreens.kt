package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.widgets.InteractiveMap
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMainDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeSurveyTask by remember { mutableStateOf<SurveyTask?>(null) }
    var isSurveyWizardActive by remember { mutableStateOf(false) }

    val tabs = listOf("Dashboard", "My Route Tracking", "Field Settings")

    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSurveyWizardActive) "Survey Form Wizard" else "Surveyor Assistant Terminal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isSurveyWizardActive) "File: ${activeSurveyTask?.fileNumber ?: "Independent Entry"}" else "Field Surveyor: ${currentUser?.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    if (isSurveyWizardActive) {
                        IconButton(onClick = { isSurveyWizardActive = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (!isSurveyWizardActive) {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (!isSurveyWizardActive) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { idx, label ->
                        val icon = when (idx) {
                            0 -> Icons.Default.Dashboard
                            1 -> Icons.Default.Map
                            else -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isSurveyWizardActive) {
                MultiStepSurveyWizard(
                    viewModel = viewModel,
                    linkedTask = activeSurveyTask,
                    onWizardCompleted = {
                        isSurveyWizardActive = false
                        activeSurveyTask = null
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> UserStatsDashboard(
                        viewModel = viewModel,
                        onStartSurvey = { task ->
                            activeSurveyTask = task
                            isSurveyWizardActive = true
                        }
                    )
                    1 -> UserRouteTrackingScreen(viewModel)
                    2 -> UserSettingsScreen(viewModel)
                }
            }
        }
    }
}

// ==== 1. SURVEYOR STATS HOMEPAGE ====
@Composable
fun UserStatsDashboard(
    viewModel: MainViewModel,
    onStartSurvey: (SurveyTask?) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val tasksList by viewModel.tasks.collectAsState()
    val assignmentsList by viewModel.assignments.collectAsState()
    val entriesList by viewModel.entries.collectAsState()

    val myAssignedFiles = remember(assignmentsList, currentUser) {
        assignmentsList.filter { it.assignedUserId == currentUser?.id }
    }

    val myPendingCount = remember(myAssignedFiles) {
        myAssignedFiles.count { it.currentStatus == "Assigned" || it.currentStatus == "By hand in-progress" || it.currentStatus == "In Progress" }
    }

    val myCompletedCount = remember(myAssignedFiles) {
        myAssignedFiles.count { it.currentStatus == "Completed" }
    }

    val myTotalDrafts = remember(entriesList, currentUser) {
        entriesList.count { it.surveyorId == currentUser?.id && it.isDraft }
    }

    val mySubmittedEntries = remember(entriesList, currentUser) {
        entriesList.count { it.surveyorId == currentUser?.id && !it.isDraft }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "Good day, ${currentUser?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Department: ${currentUser?.department} • Status: ${currentUser?.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Surveyor Stat Grid Matrix
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniUserStatCard("Assigned Files", myAssignedFiles.size.toString(), Icons.Default.Folder, Color(0xFF42A5F5), Modifier.weight(1f))
                    MiniUserStatCard("Pending Survey", myPendingCount.toString(), Icons.Default.AssignmentLate, Color(0xFFFFB74D), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniUserStatCard("Drafts Saved", myTotalDrafts.toString(), Icons.Default.Drafts, Color(0xFF81C784), Modifier.weight(1f))
                    MiniUserStatCard("Submitted", mySubmittedEntries.toString(), Icons.Default.CloudQueue, Color(0xFFEC407A), Modifier.weight(1f))
                }
            }
        }

        // Quick Actions Dashboard Panel
        item {
            Text("Quick Action Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButtonWithLabel(
                        icon = Icons.Default.NoteAdd,
                        label = "New Survey",
                        color = Color(0xFF00B0FF),
                        onClick = { onStartSurvey(null) }
                    )
                    IconButtonWithLabel(
                        icon = Icons.Default.MyLocation,
                        label = "Pin Location",
                        color = Color(0xFFFF1744),
                        onClick = {
                            // Capture GPS coordinates mock
                            val baseLat = 23.2156
                            val baseLng = 72.6369
                            val offLat = (Math.random() - 0.5) * 0.05
                            val offLng = (Math.random() - 0.5) * 0.05
                            viewModel.addGpsPoint(
                                GpsPoint(
                                    surveyEntryId = null,
                                    userId = currentUser?.id ?: "USR-002",
                                    latitude = baseLat + offLat,
                                    longitude = baseLng + offLng,
                                    accuracy = 3.5f,
                                    timestamp = System.currentTimeMillis(),
                                    tag = "tracking"
                                )
                            )
                            viewModel.showToast("GPS Coordinate register stored!")
                        }
                    )
                    IconButtonWithLabel(
                        icon = Icons.Default.Refresh,
                        label = "Sync Offline",
                        color = Color(0xFF00E676),
                        onClick = {
                            viewModel.showToast("Database already state synced (Offline First sandbox active)")
                        }
                    )
                }
            }
        }

        // Assigned files listing
        item {
            Text("Assigned Files Allocated", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (myAssignedFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active survey tasks assigned currently.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(myAssignedFiles) { assignment ->
                val associatedTask = tasksList.find { it.fileNumber == assignment.fileNumber }
                if (associatedTask != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(associatedTask.fileNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                Surface(
                                    color = if (associatedTask.status == "Completed") Color(0x224CAF50) else Color(0x22FFA726),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        associatedTask.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (associatedTask.status == "Completed") Color(0xFF41C300) else Color(0xFFFFA726),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(associatedTask.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(associatedTask.description, fontSize = 11.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Village: ${associatedTask.village}", fontSize = 11.sp, color = Color.Gray)
                                Text("Priority: ${associatedTask.priority}", fontSize = 11.sp, color = Color.Gray)
                            }

                            if (associatedTask.status != "Completed") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateTaskProgress(associatedTask.fileNumber, "In Progress")
                                        onStartSurvey(associatedTask)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Start Survey Form", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniUserStatCard(label: String, valStr: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(valStr, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun IconButtonWithLabel(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==== 2. SURVEYOR ROUTE TRACKING SCREEN ====
@Composable
fun UserRouteTrackingScreen(viewModel: MainViewModel) {
    val entriesList by viewModel.entries.collectAsState()
    val gpsPointsList by viewModel.gpsPoints.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val myGpsPoints = remember(gpsPointsList, currentUser) {
        gpsPointsList.filter { it.userId == currentUser?.id }
    }

    val totalPoints = myGpsPoints.size
    val lastPing = if (myGpsPoints.isEmpty()) "Never" else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(myGpsPoints.last().timestamp))

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Interactive Routing GIS Map", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Visualizing historic route tracks & submissions.", fontSize = 11.sp, color = Color.Gray)
                }

                Surface(color = Color(0x334CAF50), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "Pings: $totalPoints • Last: $lastPing",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            InteractiveMap(
                entries = entriesList,
                gpsPoints = gpsPointsList,
                filterUser = currentUser?.id
            )
        }
    }
}

// ==== 3. MODERN MULTI-STEP SURVEY FORM WIZARD ====
@Composable
fun MultiStepSurveyWizard(
    viewModel: MainViewModel,
    linkedTask: SurveyTask?,
    onWizardCompleted: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 5
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()

    // Step 1 parameters
    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var mNum by remember { mutableStateOf("") }
    var age by remember { mutableFloatStateOf(40f) }
    var gender by remember { mutableStateOf("Male") }

    // Step 2 boundaries
    var state by remember { mutableStateOf("Gujarat") }
    var district by remember { mutableStateOf(linkedTask?.district ?: "Gandhinagar") }
    var taluka by remember { mutableStateOf(linkedTask?.taluka ?: "Taluka North") }
    var village by remember { mutableStateOf(linkedTask?.village ?: "") }
    var address by remember { mutableStateOf("") }

    // Step 3 boundaries
    var category by remember { mutableStateOf("Agriculture") }
    var remarks by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Step 4 parameters (attachments mockup)
    var attachmentPreviewCount by remember { mutableIntStateOf(2) }

    // Step 5 coordinates parameters (GPS Lock)
    var isGpsLocked by remember { mutableStateOf(false) }
    var latval by remember { mutableDoubleStateOf(0.0) }
    var lngval by remember { mutableDoubleStateOf(0.0) }
    var accval by remember { mutableFloatStateOf(0.0f) }
    var loadingGps by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator Progress Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Progress tracker", fontSize = 11.sp, color = Color.Gray)
                    Text("Step $step of $totalSteps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = step.toFloat() / totalSteps.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF1E352B)
                )
            }
        }

        Divider(color = Color(0x33FFFFFF))

        // Dynamic Wizard Stage Rendering
        when (step) {
            1 -> {
                Text("Step 1: Respondent Personal Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Respondent Full Name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Father's Name") }, leadingIcon = { Icon(Icons.Default.AccountBox, null) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mNum, onValueChange = { mNum = it }, label = { Text("Mobile Contact No.") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())

                Column {
                    Text("Respondent Age: ${age.toInt()} years", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(
                        value = age,
                        onValueChange = { age = it },
                        valueRange = 18f..100f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Column {
                    Text("Gender Designation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                }
            }

            2 -> {
                Text("Step 2: Geographical Boundary Locations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District Jurisdiction") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = taluka, onValueChange = { taluka = it }, label = { Text("Taluka boundary core") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village Town") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Detailed Residence Street Address") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
            }

            3 -> {
                Text("Step 3: Survey Classification Parameters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                Text("Categorized Division", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Agriculture", "Infrastructure", "Social", "Economic").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Official Surveyor Remarks") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Internal Survey Draft Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
            }

            4 -> {
                Text("Step 4: Image Attachments Mock Client", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Simulated camera captures for offline records verification.", fontSize = 12.sp, color = Color.Gray)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { attachmentPreviewCount++ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Mock Photo", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { if (attachmentPreviewCount > 0) attachmentPreviewCount-- },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove Photo", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (attachmentPreviewCount == 0) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("No attachments loaded currently.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    // Modern non-lazy gridded representation to safely co-exist within any Scrollable parent
                    val chunks = (1..attachmentPreviewCount).toList().chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { index ->
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.1f)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "VERIFY_$index.JPG",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill the Row properly if there are fewer than 3 items in the chunk
                                val paddingCount = 3 - chunk.size
                                if (paddingCount > 0) {
                                    repeat(paddingCount) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            5 -> {
                Text("Step 5: GPS Location Capture Lock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Mandatory geographical validation for mobile field submissions.", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (loadingGps) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Triangulating Satellite Logs...", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                        } else if (isGpsLocked) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("HIGH PRESICION LAT-LNG BOUND", fontWeight = FontWeight.Bold, color = Color.Green, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Latitude: ${String.format("%.6f", latval)}° N", fontSize = 13.sp, color = Color.White)
                            Text("Longitude: ${String.format("%.6f", lngval)}° E", fontSize = 13.sp, color = Color.White)
                            Text("Spatial Accuracy Tolerances: ±${String.format("%.1f", accval)} m", fontSize = 11.sp, color = Color.LightGray)
                        } else {
                            Icon(Icons.Default.LocationDisabled, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No satellite spatial logs lock established.", fontSize = 11.sp, color = Color.Yellow)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                loadingGps = true
                                isGpsLocked = false
                                // Simulate play services GPS logs callback bounds asynchronously
                                val baseLat = 23.2156
                                val baseLng = 72.6369
                                val random = Random()
                                latval = baseLat + (random.nextDouble() - 0.5) * 0.04
                                lngval = baseLng + (random.nextDouble() - 0.5) * 0.04
                                accval = 3.2f + random.nextFloat() * 4.0f
                                
                                scope.launch {
                                    delay(1200)
                                    loadingGps = false
                                    isGpsLocked = true
                                    viewModel.showToast("Local GPS locked at 4.2m precision tolerance.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trigger Location Capture Logs")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation actions block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (step > 1) step-- else onWizardCompleted()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (step == 1) "Exit Wizard" else "Back")
            }

            // Draft capability on all screens
            OutlinedButton(
                onClick = {
                    val entryObj = SurveyEntry(
                        fileNumber = linkedTask?.fileNumber,
                        surveyorId = currentUser?.id ?: "USR-002",
                        surveyorName = currentUser?.name ?: "Field Surveyor",
                        name = name.ifBlank { "Unfinished Draft" },
                        fatherName = fatherName,
                        mobileNumber = mNum,
                        age = age.toInt(),
                        gender = gender,
                        state = state,
                        district = district,
                        taluka = taluka,
                        village = village,
                        address = address,
                        category = category,
                        remarks = remarks,
                        notes = notes,
                        imagesCsv = (1..attachmentPreviewCount).joinToString(",") { "device_mock_uri_$it" },
                        latitude = if (isGpsLocked) latval else 0.0,
                        longitude = if (isGpsLocked) lngval else 0.0,
                        accuracy = if (isGpsLocked) accval else 0.0f,
                        timestamp = System.currentTimeMillis(),
                        isDraft = true
                    )
                    viewModel.submitSurvey(entryObj)
                    onWizardCompleted()
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Draft Layout")
            }

            if (step < totalSteps) {
                Button(
                    onClick = {
                        // Progression checklist checks
                        val validationMsg = when (step) {
                            1 -> if (name.isBlank() || fatherName.isBlank()) " respondent details required" else null
                            2 -> if (village.isBlank()) "Please state respondent village name" else null
                            else -> null
                        }

                        if (validationMsg != null) {
                            viewModel.showToast(validationMsg)
                        } else {
                            step++
                        }
                    }
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    onClick = {
                        if (!isGpsLocked) {
                            viewModel.showToast("Geographical GPS validator lock required before submit.")
                        } else {
                            val entryObj = SurveyEntry(
                                fileNumber = linkedTask?.fileNumber,
                                surveyorId = currentUser?.id ?: "USR-002",
                                surveyorName = currentUser?.name ?: "Field Surveyor",
                                name = name.ifBlank { "Respondent Record" },
                                fatherName = fatherName,
                                mobileNumber = mNum,
                                age = age.toInt(),
                                gender = gender,
                                state = state,
                                district = district,
                                taluka = taluka,
                                village = village,
                                address = address,
                                category = category,
                                remarks = remarks,
                                notes = notes,
                                imagesCsv = (1..attachmentPreviewCount).joinToString(",") { "device_mock_uri_$it" },
                                latitude = latval,
                                longitude = lngval,
                                accuracy = accval,
                                timestamp = System.currentTimeMillis(),
                                isDraft = false
                            )
                            viewModel.submitSurvey(entryObj)
                            onWizardCompleted()
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit Offline", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==== 4. SURVEYOR SETTINGS CORE ====
@Composable
fun UserSettingsScreen(viewModel: MainViewModel) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val gpsFreq by viewModel.gpsFrequency.collectAsState()
    val optLang by viewModel.language.collectAsState()
    val autoReport by viewModel.autoExportDrafts.collectAsState()

    var expandedGps by remember { mutableStateOf(false) }
    var expandedLang by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Field Settings Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Modify local application specifications offline.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Dark theme Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Visual App Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Toggle Dark Mode aesthetics.", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = isDark,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }
        }

        // GPS capture pings frequency select dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.6f)) {
                    Text("GPS Pin Frequency", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Set route-tracking GPS interval.", fontSize = 11.sp, color = Color.Gray)
                }
                Box(modifier = Modifier.weight(0.4f)) {
                    Button(
                        onClick = { expandedGps = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(gpsFreq, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = expandedGps, onDismissRequest = { expandedGps = false }) {
                        listOf("Every 5 seconds", "Every 30 seconds", "Every 2 minutes", "Manual Click Only").forEach { f ->
                            DropdownMenuItem(text = { Text(f) }, onClick = {
                                viewModel.setGpsFrequency(f)
                                expandedGps = false
                                viewModel.showToast("GPS tracking frequency: $f")
                            })
                        }
                    }
                }
            }
        }

        // Language Select setting dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.6f)) {
                    Text("Local Language Selection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Sets the primary UI vocabulary.", fontSize = 11.sp, color = Color.Gray)
                }
                Box(modifier = Modifier.weight(0.4f)) {
                    Button(
                        onClick = { expandedLang = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(optLang, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = expandedLang, onDismissRequest = { expandedLang = false }) {
                        listOf("English", "Hindi", "Gujarati").forEach { l ->
                            DropdownMenuItem(text = { Text(l) }, onClick = {
                                viewModel.setLanguage(l)
                                expandedLang = false
                                viewModel.showToast("Language changed to $l")
                            })
                        }
                    }
                }
            }
        }

        // Auto backup toggle setting
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Instant Auto-Export drafts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Auto generates files immediately.", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = autoReport,
                    onCheckedChange = { viewModel.setAutoExportDrafts(it) }
                )
            }
        }

        // Database cleaning sandbox button
        Button(
            onClick = {
                viewModel.showToast("Offline local storage cached cleaned: 0 bytes reclaimed.")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Offline Images Cached", color = Color.White)
        }
    }
}
