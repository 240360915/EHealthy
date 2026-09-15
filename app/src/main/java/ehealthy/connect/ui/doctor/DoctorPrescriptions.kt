package ehealthy.connect.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ArrowDropDown


/** One patient this doctor can prescribe to — currently scoped to patients
 *  with a confirmed appointment, matching the web version's behaviour. */
data class PrescriptionPatient(val id: String, val name: String)

data class MedicationCategory(val name: String, val emoji: String, val items: List<String>)

// Kept exactly as your teammate had it.
val medicationCatalog = listOf(
    MedicationCategory("Pain Relief & Anti-Inflammatory", "💊", listOf("Aspirin 100mg", "Ibuprofen 200mg", "Paracetamol 500mg", "Naproxen 250mg", "Diclofenac 50mg")),
    MedicationCategory("Antibiotics", "🦠", listOf("Amoxicillin 500mg", "Azithromycin 250mg", "Ciprofloxacin 500mg", "Doxycycline 100mg", "Metronidazole 400mg")),
    MedicationCategory("Cardiovascular", "❤️", listOf("Amlodipine 5mg", "Lisinopril 10mg", "Metoprolol 50mg", "Atorvastatin 20mg", "Warfarin 5mg")),
    MedicationCategory("Respiratory", "🫁", listOf("Albuterol 90mcg", "Montelukast 10mg", "Fluticasone 50mcg", "Prednisone 20mg")),
    MedicationCategory("Gastrointestinal", "🧪", listOf("Omeprazole 20mg", "Loperamide 2mg", "Metoclopramide 10mg", "Simethicone 125mg")),
    MedicationCategory("Mental Health", "🧠", listOf("Sertraline 50mg", "Escitalopram 10mg", "Alprazolam 0.5mg", "Lorazepam 1mg")),
    MedicationCategory("Diabetes", "🩸", listOf("Metformin 500mg", "Glipizide 5mg", "Insulin Glargine")),
    MedicationCategory("Vitamins & Supplements", "🌿", listOf("Vitamin D3", "Multivitamin", "Calcium 500mg", "Iron 65mg")),
    MedicationCategory("Other", "🧴", listOf("HCTZ 25mg", "Levothyroxine 50mcg", "Allopurinol 100mg", "Furosemide 40mg", "Diphenhydramine", "Cetirizine 10mg"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPrescriptions(
    patients: List<PrescriptionPatient>,
    isLoadingPatients: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    onSave: (patientId: String, medications: List<String>) -> Unit
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0F1F3D)
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF64748B)

    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMedications by remember { mutableStateOf(listOf<String>()) }
    var search by remember { mutableStateOf("") }

    val selectedPatientName = patients.find { it.id == selectedPatientId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prescriptions", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .padding(20.dp)
        ) {
            // ------------------------------------------------
            // PATIENT PICKER
            // ------------------------------------------------

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPatientName
                        ?: if (isLoadingPatients) "Loading patients…" else "Select a patient",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isLoadingPatients,
                    label = { Text("Patient") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select patient", tint = muted)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        disabledBorderColor = Color(0xFFE2E5EC),
                        disabledTextColor = muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                // Invisible overlay intercepts the tap before the field can
                // grab focus and raise the keyboard — this is what makes it
                // behave as choose-only, matching the web dashboard's PATIENT
                // select rather than looking like a searchable text field.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = !isLoadingPatients) { patientDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = patientDropdownExpanded,
                    onDismissRequest = { patientDropdownExpanded = false }
                ) {
                    if (patients.isEmpty() && !isLoadingPatients) {
                        DropdownMenuItem(text = { Text("No confirmed patients yet") }, onClick = { patientDropdownExpanded = false })
                    }
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name) },
                            onClick = { selectedPatientId = patient.id; patientDropdownExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ------------------------------------------------
            // SELECTED MEDICATIONS SUMMARY
            // ------------------------------------------------

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("SELECTED MEDICATIONS", color = navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (selectedMedications.isEmpty()) {
                        Text("None selected", color = muted, fontSize = 13.sp)
                    } else {
                        selectedMedications.forEach { Text("• $it", color = navy, fontSize = 13.sp) }
                    }
                }
            }

            if (saveError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(saveError, color = Color(0xFFEF4444), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.material3.Button(
                onClick = {
                    val patientId = selectedPatientId ?: return@Button
                    onSave(patientId, selectedMedications)
                },
                enabled = !isSaving && selectedPatientId != null && selectedMedications.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = navy)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("✓ Confirm Prescription", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------------
            // MEDICATION SEARCH + CHECKLIST
            // ------------------------------------------------

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search medications…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val query = search.trim().lowercase()
            val filteredCatalog = remember(query) {
                if (query.isEmpty()) {
                    medicationCatalog
                } else {
                    medicationCatalog
                        .map { it.copy(items = it.items.filter { m -> m.lowercase().contains(query) }) }
                        .filter { it.items.isNotEmpty() }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredCatalog.forEach { category ->
                    item {
                        Text(
                            "${category.emoji}  ${category.name}",
                            color = navy, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)
                        )
                    }
                    items(category.items) { med ->
                        val checked = selectedMedications.contains(med)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMedications = if (checked) selectedMedications - med else selectedMedications + med
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedMedications = if (checked) selectedMedications - med else selectedMedications + med
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = accent)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(med, color = navy, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}