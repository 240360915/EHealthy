package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ehealthy.connect.data.patient.PatientHealthProfile
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.CareFileRepository
import ehealthy.connect.data.PatientCareNote

/** A live overview of the patient's existing profile and doctor-authored encounter notes.
 * No second patient row is created. Additional medical records/prescriptions remain on
 * their existing screens so we don't silently duplicate or alter those records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientCareFileScreen(
    onBack: () -> Unit,
    onEditHealthProfile: () -> Unit,
    onOpenMedicalRecords: () -> Unit
) {
    var profile by remember { mutableStateOf<PatientHealthProfile?>(null) }
    var notes by remember { mutableStateOf<List<PatientCareNote>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notesError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        PatientRepository.getMyHealthProfile()
            .onSuccess { profile = it }
            .onFailure { error = it.message ?: "Unable to load your health profile." }
        CareFileRepository.getMyCareNotes()
            .onSuccess { notes = it; notesError = null }
            .onFailure {
        notesError = "Unable to load consultation history. Please try again."
    }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("My Care File") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { insets ->
        when {
            loading -> Column(Modifier.padding(insets).fillMaxSize().padding(24.dp)) {
                CircularProgressIndicator()
                Text("Loading your care file…")
            }
            profile == null -> Column(Modifier.padding(insets).padding(24.dp)) {
                Text(error ?: "Your patient profile is unavailable.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onBack) { Text("Go back") }
            }
            else -> {
                val p = profile!!
                LazyColumn(
                    modifier = Modifier.padding(insets).fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("${p.patientName}'s care file", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Patient file ID: ${p.id}", style = MaterialTheme.typography.bodySmall)
                        Text("Information is reused securely from your existing profile and consultations.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onEditHealthProfile, modifier = Modifier.fillMaxWidth()) { Text("Update health profile") }
                        OutlinedButton(onClick = onOpenMedicalRecords, modifier = Modifier.fillMaxWidth()) { Text("View medical records and prescriptions") }
                    }
                    item {
                        PatientFileSection("Medical background") {
                            PatientFileValue("Blood group", p.blood_group)
                            PatientFileValue("Allergies", p.allergies)
                            PatientFileValue("Chronic conditions", p.chronic)
                            PatientFileValue("Current medication", p.medication)
                            PatientFileValue("Previous surgeries", p.surgeries)
                            PatientFileValue("Family medical history", p.family_medical_history)
                        }
                    }
                    item {
                        PatientFileSection("Latest reported symptoms") {
                            PatientFileValue("Symptoms", p.current_symptoms)
                            PatientFileValue("Duration", p.symptom_duration)
                            PatientFileValue("Severity (out of 10)", p.symptom_severity?.toString())
                            PatientFileValue("Profile last updated", p.health_profile_updated_at)
                        }
                    }
                    item {
                        Text("Consultation history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Only doctor-authored summaries linked to your consultations appear below.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (notesError != null) {
                        item { Text("Couldn't load consultation history: $notesError", color = MaterialTheme.colorScheme.error) }
                    } else if (notes.isEmpty()) {
                        item { Text("No consultation summaries have been added yet.") }
                    } else {
                        items(notes, key = { it.appointment_id }) { note ->
                            PatientFileSection("${note.visit_date ?: "Consultation"} • ${note.doctor_name ?: "Doctor"}") {
                                PatientFileValue("Consultation summary", note.consultation_summary)
                                PatientFileValue("Care plan", note.care_plan)
                                Text("Last updated: ${note.updated_at ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientFileSection(title: String, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PatientFileValue(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value?.takeIf { it.isNotBlank() } ?: "Not provided", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(9.dp))
}
