package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ehealthy.connect.data.CareFileRepository
import ehealthy.connect.data.DoctorCareFile
import kotlinx.coroutines.launch


/** The doctor's appointment-specific view of an existing patient's health file.
 * All private information comes from an authorization-checking Supabase RPC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientCareFileScreen(
    appointmentId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var file by remember(appointmentId) { mutableStateOf<DoctorCareFile?>(null) }
    var summary by remember(appointmentId) { mutableStateOf("") }
    var plan by remember(appointmentId) { mutableStateOf("") }
    var loading by remember(appointmentId) { mutableStateOf(true) }
    var saving by remember(appointmentId) { mutableStateOf(false) }
    var error by remember(appointmentId) { mutableStateOf<String?>(null) }
    var confirmation by remember(appointmentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(appointmentId) {
        loading = true
        error = null
        CareFileRepository.getDoctorCareFile(appointmentId)
            .onSuccess { loaded ->
                file = loaded
                summary = loaded.consultation_summary.orEmpty()
                plan = loaded.care_plan.orEmpty()
            }
            .onFailure { problem -> error = problem.message ?: "Could not load patient file." }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Patient care file") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { insets ->
        when {
            loading -> Column(Modifier.padding(insets).fillMaxSize().padding(24.dp)) {
                CircularProgressIndicator()
                Text("Loading authorised patient information…")
            }
            file == null -> Column(Modifier.padding(insets).padding(24.dp)) {
                Text(error ?: "Patient file is not available.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onBack) { Text("Go back") }
            }
            else -> {
                val careFile = file!!
                LazyColumn(
                    modifier = Modifier.padding(insets).fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(careFile.patient_name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Appointment ${careFile.visit_date.orEmpty()} • ${careFile.visit_time.orEmpty()}")
                        Text("Status: ${careFile.visit_status.orEmpty()}")
                        Text("Patient file ID: ${careFile.patient_id}", style = MaterialTheme.typography.bodySmall)
                        Text("Medical background is the current patient profile, not a historical snapshot.", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        CareFileSection("Current complaint") {
                            CareFileValue("Visit reason", careFile.visit_reason)
                            CareFileValue("Symptoms", careFile.current_symptoms)
                            CareFileValue("Duration", careFile.symptom_duration)
                            CareFileValue("Severity (out of 10)", careFile.symptom_severity?.toString())
                        }
                    }
                    item {
                        CareFileSection("Medical background") {
                            CareFileValue("Date of birth", careFile.date_of_birth)
                            CareFileValue("Gender", careFile.gender)
                            CareFileValue("Blood group", careFile.blood_group)
                            CareFileValue("Allergies", careFile.allergies)
                            CareFileValue("Chronic conditions", careFile.chronic_conditions)
                            CareFileValue("Current medication", careFile.current_medication)
                            CareFileValue("Surgeries", careFile.surgeries)
                        }
                    }
                    item {
                        CareFileSection("Consultation documentation") {
                            Text("What you save below is visible to the patient in My Care File.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = summary,
                                onValueChange = { if (it.length <= 5000) { summary = it; confirmation = null } },
                                label = { Text("Consultation summary") },
                                placeholder = { Text("Summary of the consultation and relevant findings") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                enabled = !saving
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = plan,
                                onValueChange = { if (it.length <= 5000) { plan = it; confirmation = null } },
                                label = { Text("Care plan / follow-up") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 7,
                                enabled = !saving
                            )
                            if (error != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(error!!, color = MaterialTheme.colorScheme.error)
                            }
                            if (confirmation != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(confirmation!!, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        saving = true
                                        error = null
                                        CareFileRepository.saveDoctorCareNote(appointmentId, summary, plan)
                                            .onSuccess { confirmation = "Consultation record saved securely." }
                                            .onFailure { error = it.message ?: "Could not save the consultation record." }
                                        saving = false
                                    }
                                },
                                enabled = !saving && summary.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (saving) "Saving…" else "Save patient-visible record")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareFileSection(title: String, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CareFileValue(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value?.takeIf { it.isNotBlank() } ?: "Not provided", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(9.dp))
}
