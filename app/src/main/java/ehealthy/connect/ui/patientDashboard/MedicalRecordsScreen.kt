package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
@Serializable
data class MedicalRecord(
    val id: String,
    val record_type: String? = null,
    val medications: List<String>? = null,
    val notes: String? = null,
    val created_at: String,
    val doctor_id: String? = null
)

data class MedicalRecordDisplay(
    val record: MedicalRecord,
    val doctorName: String,
    val doctorPhoto: String? = null
)

enum class RecordSort(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(
    onBack: () -> Unit,
    fetchRecords: suspend () -> Result<List<MedicalRecordDisplay>>
) {
    var records by remember { mutableStateOf<List<MedicalRecordDisplay>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf(RecordSort.NEWEST) }

    LaunchedEffect(Unit) {
        val result = fetchRecords()
        isLoading = false
        result
            .onSuccess { records = it }
            .onFailure { loadError = it.message ?: "Failed to load medical records." }
    }

    val recordTypes = remember(records) {
        records.mapNotNull { it.record.record_type }.distinct().sorted()
    }

    val displayedRecords = remember(records, selectedType, sortOrder) {
        val filtered = if (selectedType == null) records
        else records.filter { it.record.record_type == selectedType }
        when (sortOrder) {
            RecordSort.NEWEST -> filtered.sortedByDescending { it.record.created_at }
            RecordSort.OLDEST -> filtered.sortedBy { it.record.created_at }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Medical Records",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Your complete health history",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            // ---- FILTERS ----
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    "FILTER BY TYPE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All Records", fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                    recordTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    type.replaceFirstChar { it.uppercase() },
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "SORT",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordSort.entries.forEach { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { sortOrder = order },
                            label = { Text(order.label, fontSize = 13.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }
                }
            }

            // ---- RESULTS ----
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                loadError != null -> {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        message = loadError ?: "Something went wrong.",
                        subtitle = "Pull down to try again, or check your connection."
                    )
                }

                displayedRecords.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        message = if (records.isEmpty()) "No medical records found yet." else "No records match your filter.",
                        subtitle = if (records.isEmpty()) "Records from your consultations will appear here." else "Try a different type or clear the filter."
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedRecords, key = { it.record.id }) { item ->
                            RecordCard(item)
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(item: MedicalRecordDisplay) {
    val record = item.record
    val dateLabel = remember(record.created_at) { formatRecordDate(record.created_at) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        (record.record_type ?: "prescription").uppercase(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(dateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.doctorPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = item.doctorPhoto,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "Prescribed by",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                    Text(
                        item.doctorName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val meds = record.medications.orEmpty()
            if (meds.isEmpty()) {
                Text(
                    "No medications listed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meds.forEach { med ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                med,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            record.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

private fun formatRecordDate(isoTimestamp: String): String {
    return try {
        // created_at from Supabase looks like "2026-01-15T09:30:00+00:00" — take the date part
        val datePart = isoTimestamp.substringBefore("T")
        val (year, month, day) = datePart.split("-")
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "${months[month.toInt() - 1]} ${day.toInt()}, $year"
    } catch (e: Exception) {
        isoTimestamp
    }
}