package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable

@Serializable
data class DoctorListing(
    val id: String,
    val name: String,
    val surname: String,
    val discipline: String? = null,
    val phone: String? = null,
    val hourly_rate: Double? = null,
    val profile_image_url: String? = null,
    val operating_hours: String? = null,
    val location: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDoctorsScreen(
    onBack: () -> Unit,
    onSelectDoctor: (DoctorListing) -> Unit,
    fetchDoctors: suspend () -> Result<List<DoctorListing>>
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0B1828)
    val teal = Color(0xFF0D9488)
    val muted = Color(0xFF64748B)

    var allDoctors by remember { mutableStateOf<List<DoctorListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDiscipline by remember { mutableStateOf<String?>(null) }
    var disciplineMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = fetchDoctors()
        isLoading = false
        result
            .onSuccess { allDoctors = it }
            .onFailure { loadError = it.message ?: "Failed to load doctors." }
    }

    val disciplines = remember(allDoctors) {
        allDoctors.mapNotNull { it.discipline }.distinct().sorted()
    }

    val filteredDoctors = remember(allDoctors, searchQuery, selectedDiscipline) {
        allDoctors.filter { doc ->
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    "${doc.name} ${doc.surname}".lowercase().contains(q) ||
                    (doc.discipline ?: "").lowercase().contains(q) ||
                    (doc.location ?: "").lowercase().contains(q)
            val matchesDiscipline =
                selectedDiscipline == null || doc.discipline == selectedDiscipline
            matchesQuery && matchesDiscipline
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Find a Doctor",
                        color = navy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = navy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // ---- SEARCH ----
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, specialty, or location…") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = muted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ---- DISCIPLINE FILTER ----
            Box {
                OutlinedTextField(
                    value = selectedDiscipline ?: "All Specialties",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { disciplineMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = disciplineMenuExpanded,
                    onDismissRequest = { disciplineMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Specialties") },
                        onClick = { selectedDiscipline = null; disciplineMenuExpanded = false }
                    )
                    disciplines.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d) },
                            onClick = { selectedDiscipline = d; disciplineMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${filteredDoctors.size} doctor${if (filteredDoctors.size != 1) "s" else ""}",
                color = teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ---- RESULTS ----
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = teal)
                    }
                }

                loadError != null -> {
                    Text(loadError ?: "", color = Color(0xFFEF4444), fontSize = 13.sp)
                }

                filteredDoctors.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No doctors found matching your search.",
                            color = muted,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredDoctors, key = { it.id }) { doc ->
                            DoctorCard(doc, navy, teal, muted, onClick = { onSelectDoctor(doc) })
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(
    doc: DoctorListing,
    navy: Color,
    teal: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(navy),
                    contentAlignment = Alignment.Center
                ) {
                    if (!doc.profile_image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = doc.profile_image_url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        val initials =
                            "${doc.name.firstOrNull() ?: ' '}${doc.surname.firstOrNull() ?: ' '}"
                        Text(
                            initials.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Dr. ${doc.name} ${doc.surname}",
                        color = navy,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        doc.discipline ?: "General Practitioner",
                        color = muted,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            doc.location?.let {
                DetailRow(Icons.Outlined.LocationOn, it, muted)
            }
            doc.operating_hours?.let {
                DetailRow(Icons.Outlined.Schedule, it, muted)
            }
            doc.phone?.let {
                DetailRow(Icons.Outlined.Phone, it, muted)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0FDF9))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Consultation Rate", color = muted, fontSize = 11.sp)
                Text(
                    doc.hourly_rate?.let { "R %.2f/hr".format(it) } ?: "Rate on request",
                    color = teal, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(teal)
            ) {
                Text(
                    "Book Now",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 12.5.sp)
    }
}
