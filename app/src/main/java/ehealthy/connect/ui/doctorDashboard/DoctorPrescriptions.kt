package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class PrescriptionPatient(
    val id: String,
    val name: String
)

data class RefillRequest(
    val id: String,
    val patientId: String,
    val patientName: String,
    val medication: String,
    val requestedAt: String? = null
)

data class MedicationCategory(
    val name: String,
    val emoji: String,
    val items: List<String>
)

/**
 * One medicine on a prescription, with everything the doctor fills in.
 * This is what ends up as a row in the patient's receipt table:
 * Medicine | Dosage | Frequency | Duration | Instructions
 *
 * NOTE: date issued, prescription number, doctor, specialization,
 * hospital and patient name are NOT here - the backend/Supabase supplies them.
 */
data class PrescribedMedicine(
    val medicine: String,
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val timesOfDay: List<String> = emptyList(),   // Morning, Afternoon, Evening, Night
    val notes: String = ""                         // e.g. "Take after meals"
) {
    /** Single string for the receipt's "Instructions" column, e.g. "Morning Afternoon Evening - Take after meals" */
    val instructions: String
        get() = listOf(
            timesOfDay.joinToString(" "),
            notes.trim()
        ).filter { it.isNotBlank() }.joinToString(" - ")

    val isComplete: Boolean
        get() = dosage.isNotBlank() && frequency.isNotBlank() && duration.isNotBlank()
}

private val frequencyOptions = listOf(
    "Once daily",
    "Twice daily",
    "Three times daily",
    "Four times daily",
    "Every 8 hours",
    "Every 12 hours",
    "As needed"
)

private val durationOptions = listOf(
    "3 days", "5 days", "7 days", "10 days", "14 days",
    "21 days", "30 days", "3 months", "Ongoing"
)

private val timeOfDayOptions = listOf("Morning", "Afternoon", "Evening", "Night")


val medicationCatalog = listOf(
    MedicationCategory(
        "Pain Relief & Anti-Inflammatory", "💊",
        listOf("Aspirin 100mg", "Ibuprofen 200mg", "Paracetamol 500mg", "Naproxen 250mg", "Diclofenac 50mg")
    ),
    MedicationCategory(
        "Antibiotics", "🦠",
        listOf("Amoxicillin 500mg", "Azithromycin 250mg", "Ciprofloxacin 500mg", "Doxycycline 100mg", "Metronidazole 400mg")
    ),
    MedicationCategory(
        "Cardiovascular", "❤️",
        listOf("Amlodipine 5mg", "Lisinopril 10mg", "Metoprolol 50mg", "Atorvastatin 20mg", "Warfarin 5mg")
    ),
    MedicationCategory(
        "Respiratory", "🫁",
        listOf("Albuterol 90mcg", "Montelukast 10mg", "Fluticasone 50mcg", "Prednisone 20mg")
    ),
    MedicationCategory(
        "Gastrointestinal", "🧪",
        listOf("Omeprazole 20mg", "Loperamide 2mg", "Metoclopramide 10mg", "Simethicone 125mg")
    ),
    MedicationCategory(
        "Mental Health", "🧠",
        listOf("Sertraline 50mg", "Escitalopram 10mg", "Alprazolam 0.5mg", "Lorazepam 1mg")
    ),
    MedicationCategory(
        "Diabetes", "🩸",
        listOf("Metformin 500mg", "Glipizide 5mg", "Insulin Glargine")
    ),
    MedicationCategory(
        "Vitamins & Supplements", "🌿",
        listOf("Vitamin D3", "Multivitamin", "Calcium 500mg", "Iron 65mg")
    ),
    MedicationCategory(
        "Other", "🧴",
        listOf("HCTZ 25mg", "Levothyroxine 50mcg", "Allopurinol 100mg", "Furosemide 40mg", "Diphenhydramine", "Cetirizine 10mg")
    )
)

/** Pulls a strength like "500mg" / "0.5mg" / "90mcg" out of a catalogue name to pre-fill the dosage. */
private fun guessDosage(medicine: String): String =
    Regex("""\d+(\.\d+)?\s?(mg|mcg|g|ml|%)""", RegexOption.IGNORE_CASE)
        .find(medicine)?.value ?: ""


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorPrescriptions(
    patients: List<PrescriptionPatient>,
    refillRequests: List<RefillRequest> = emptyList(),
    isLoadingPatients: Boolean,
    isLoadingRefills: Boolean = false,
    isSaving: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    // CHANGED: now sends full details per medicine instead of just names
    onSave: (patientId: String, medications: List<PrescribedMedicine>) -> Unit,
    onApproveRefill: (RefillRequest) -> Unit = {},
    onDeclineRefill: (RefillRequest) -> Unit = {}
) {
    val background = Color(0xFFF7F9FC)
    val navy = Color(0xFF171B27)
    val accent = Color(0xFF2F6FED)
    val muted = Color(0xFF6C7280)
    val success = Color(0xFF18A572)
    val successSoft = Color(0xFFE9FAF2)
    val warning = Color(0xFFD97706)
    val warningSoft = Color(0xFFFFF4DD)
    val danger = Color(0xFFDC2626)
    val dangerSoft = Color(0xFFFFEEEE)

    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMedications by remember { mutableStateOf(listOf<PrescribedMedicine>()) }
    var search by remember { mutableStateOf("") }

    val selectedPatientName = patients.find { it.id == selectedPatientId }?.name
    val query = search.trim().lowercase()

    val filteredCatalog = remember(query) {
        if (query.isEmpty()) medicationCatalog
        else medicationCatalog
            .map { c -> c.copy(items = c.items.filter { it.lowercase().contains(query) }) }
            .filter { it.items.isNotEmpty() }
    }

    fun toggleMedicine(name: String) {
        selectedMedications =
            if (selectedMedications.any { it.medicine == name })
                selectedMedications.filterNot { it.medicine == name }
            else
                selectedMedications + PrescribedMedicine(medicine = name, dosage = guessDosage(name))
    }

    fun updateMedicine(updated: PrescribedMedicine) {
        selectedMedications = selectedMedications.map {
            if (it.medicine == updated.medicine) updated else it
        }
    }

    val allComplete = selectedMedications.isNotEmpty() && selectedMedications.all { it.isComplete }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Prescriptions", color = navy, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                        Text("Clinical medication workspace", color = muted, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F7F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF119E95),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                PremiumPrescriptionHero(
                    refillCount = refillRequests.size,
                    selectedCount = selectedMedications.size
                )
            }

            // ---------------- REFILL REQUESTS ----------------
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(warningSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = warning, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Refill requests", color = navy, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text("Review requests submitted by your patients", color = muted, fontSize = 11.sp)
                    }
                    if (refillRequests.isNotEmpty()) {
                        Box(Modifier.background(warning, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(refillRequests.size.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (isLoadingRefills) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = accent)
                            Spacer(Modifier.width(12.dp))
                            Text("Loading refill requests…", color = muted, fontSize = 13.sp)
                        }
                    }
                }
            } else if (refillRequests.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).background(successSoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = success, modifier = Modifier.size(21.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text("No refill requests", color = navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("New requests from patients will appear here.", color = muted, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            } else {
                items(refillRequests, key = { it.id }) { request ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(46.dp).background(warningSoft, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Medication, null, tint = warning, modifier = Modifier.size(23.dp))
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(request.medication, color = navy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(request.patientName, color = muted, fontSize = 11.5.sp)
                                }
                                Box(
                                    Modifier.background(warningSoft, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text("REFILL", color = warning, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!request.requestedAt.isNullOrBlank()) {
                                Spacer(Modifier.height(9.dp))
                                Text("Requested ${formatRefillDate(request.requestedAt)}", color = muted, fontSize = 10.5.sp)
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onDeclineRefill(request) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, null, tint = danger, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Decline", color = danger, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onApproveRefill(request) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = success)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Approve", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- NEW PRESCRIPTION ----------------
            item {
                Spacer(Modifier.height(8.dp))
                Column {
                    Text("New prescription", color = navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select a patient, choose medication and fill in the details", color = muted, fontSize = 11.sp)
                }
            }

            // Patient picker
            item {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedPatientName
                            ?: if (isLoadingPatients) "Loading patients…" else "Select a patient",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLoadingPatients,
                        label = { Text("Patient") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select patient", tint = muted) },
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
                    Box(
                        Modifier.matchParentSize().clickable(enabled = !isLoadingPatients) {
                            patientDropdownExpanded = true
                        }
                    )
                    DropdownMenu(
                        expanded = patientDropdownExpanded,
                        onDismissRequest = { patientDropdownExpanded = false }
                    ) {
                        if (patients.isEmpty() && !isLoadingPatients) {
                            DropdownMenuItem(
                                text = { Text("No confirmed patients yet") },
                                onClick = { patientDropdownExpanded = false }
                            )
                        }
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient.name) },
                                onClick = {
                                    selectedPatientId = patient.id
                                    patientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Selected medicines header
            item {
                Text(
                    "SELECTED MEDICATIONS (${selectedMedications.size})",
                    color = navy, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
                if (selectedMedications.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("None selected - pick medicines from the list below.", color = muted, fontSize = 13.sp)
                }
            }

            // One details card per selected medicine
            items(selectedMedications, key = { it.medicine }) { med ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                med.medicine, color = navy, fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { toggleMedicine(med.medicine) }) {
                                Icon(Icons.Outlined.Close, "Remove ${med.medicine}", tint = danger)
                            }
                        }

                        OutlinedTextField(
                            value = med.dosage,
                            onValueChange = { updateMedicine(med.copy(dosage = it)) },
                            label = { Text("Dosage (e.g. 500mg, 2mg/5ml)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        PickerField(
                            label = "Frequency",
                            value = med.frequency,
                            options = frequencyOptions,
                            accent = accent,
                            muted = muted,
                            onSelect = { updateMedicine(med.copy(frequency = it)) }
                        )

                        PickerField(
                            label = "Duration",
                            value = med.duration,
                            options = durationOptions,
                            accent = accent,
                            muted = muted,
                            onSelect = { updateMedicine(med.copy(duration = it)) }
                        )

                        Text("Instructions - when to take", color = muted, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            timeOfDayOptions.forEach { time ->
                                val on = time in med.timesOfDay
                                FilterChip(
                                    selected = on,
                                    onClick = {
                                        // keep Morning→Night order regardless of tap order
                                        val next = if (on) med.timesOfDay - time else med.timesOfDay + time
                                        updateMedicine(
                                            med.copy(timesOfDay = timeOfDayOptions.filter { it in next })
                                        )
                                    },
                                    label = { Text(time) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = med.notes,
                            onValueChange = { updateMedicine(med.copy(notes = it)) },
                            label = { Text("Extra notes (optional, e.g. after meals)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!med.isComplete) {
                            Text("Dosage, frequency and duration are required.", color = warning, fontSize = 10.5.sp)
                        }
                    }
                }
            }

            if (saveError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = dangerSoft),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(saveError, color = danger, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // Confirm
            item {
                Button(
                    onClick = {
                        val patientId = selectedPatientId ?: return@Button
                        onSave(patientId, selectedMedications)
                    },
                    enabled = !isSaving && selectedPatientId != null && allComplete,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF119E95))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Issue prescription", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Medication search
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search medications…") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF119E95)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Catalogue
            filteredCatalog.forEach { category ->
                item {
                    Text(
                        "${category.emoji}  ${category.name}",
                        color = navy, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(category.items) { med ->
                    val checked = selectedMedications.any { it.medicine == med }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { toggleMedicine(med) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { toggleMedicine(med) },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(med, color = navy, fontSize = 14.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}


/** Read-only field that opens a dropdown of fixed options. */
@Composable
private fun PickerField(
    label: String,
    value: String,
    options: List<String>,
    accent: Color,
    muted: Color,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select $label", tint = muted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


private fun formatRefillDate(value: String?): String {
    if (value.isNullOrBlank()) return "recently"

    return try {
        val parts = value.substringBefore("T").split("-")
        if (parts.size != 3) {
            value
        } else {
            val months = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            "${months[parts[1].toInt() - 1]} ${parts[2].toInt()}, ${parts[0]}"
        }
    } catch (_: Exception) {
        value
    }
}


@Composable
private fun PremiumPrescriptionHero(
    refillCount: Int,
    selectedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0E8C86),
                            Color(0xFF1769AA)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.LocalPharmacy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Prescription workspace",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Create complete medication plans with dosage, frequency, duration and instructions.",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    PrescriptionHeroMetric(
                        value = selectedCount.toString(),
                        label = "Selected",
                        modifier = Modifier.weight(1f)
                    )
                    PrescriptionHeroMetric(
                        value = refillCount.toString(),
                        label = "Refills",
                        modifier = Modifier.weight(1f)
                    )
                    PrescriptionHeroMetric(
                        value = "Rx",
                        label = "Workspace",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrescriptionHeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 8.5.sp
        )
    }
}
