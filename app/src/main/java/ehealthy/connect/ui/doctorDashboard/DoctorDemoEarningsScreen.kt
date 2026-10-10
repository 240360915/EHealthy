package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.DemoEarning
import ehealthy.connect.data.DemoEarningsRepository
import ehealthy.connect.data.DemoReadyConsultation
import kotlinx.coroutines.launch
import java.util.Locale

/** Virtual accounting only. NEVER requests bank or card details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDemoEarningsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var earnings by remember { mutableStateOf<List<DemoEarning>>(emptyList()) }
    var ready by remember { mutableStateOf<List<DemoReadyConsultation>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var releasingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<DemoReadyConsultation?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        busy = true
        error = null
        val currentEarnings = DemoEarningsRepository.getMyEarnings()
        val currentReady = DemoEarningsRepository.getReadyConsultations()
        currentEarnings.onSuccess { earnings = it }.onFailure {
            error = "Could not load virtual earnings. Check the Supabase migration and try again."
        }
        currentReady.onSuccess { ready = it }.onFailure {
            error = "Could not load consultations ready for completion."
        }
        busy = false
    }

    val gross = earnings.sumOf { it.total_minor.toLong() }
    val doctorTotal = earnings.sumOf { it.doctor_minor.toLong() }
    val platformTotal = earnings.sumOf { it.platform_minor.toLong() }

    confirmation?.let { visit ->
        AlertDialog(
            onDismissRequest = { if (releasingId == null) confirmation = null },
            title = { Text("Complete consultation?") },
            text = { Text("Mark the scheduled consultation with ${visit.patient_name ?: "this patient"} as completed and record the virtual earnings split? No real money will move.") },
            confirmButton = {
                Button(enabled = releasingId == null, onClick = {
                    releasingId = visit.appointment_id
                    scope.launch {
                        DemoEarningsRepository.completeOnlineAppointment(visit.appointment_id)
                            .onSuccess {
                                info = "Consultation completed. Virtual earnings recorded successfully."
                                confirmation = null
                                refreshKey++
                            }
                            .onFailure {
                                error = "Could not complete this consultation. Verify that it is confirmed and both participants joined the call."
                                confirmation = null
                            }
                        releasingId = null
                    }
                }) { Text(if (releasingId == null) "Complete (demo)" else "Saving…") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }, enabled = releasingId == null) { Text("Not now") }
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Demo earnings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = { TextButton(onClick = { refreshKey++ }) { Text("Refresh") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("SCHOOL PROJECT • VIRTUAL MONEY ONLY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Consultation revenue is split after a verified scheduled online consultation. No bank transfers occur.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (busy) item { CircularProgressIndicator() }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            info?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row { Icon(Icons.Outlined.Payments, contentDescription = null); Spacer(Modifier.padding(horizontal = 5.dp)); Text("Virtual earnings summary", fontWeight = FontWeight.Bold) }
                        MoneyRow("Total demo consultations", money(gross))
                        MoneyRow("Your virtual earnings", money(doctorTotal), emphasized = true)
                        MoneyRow("EHealthy virtual commission", money(platformTotal))
                        HorizontalDivider()
                        Text("${earnings.size} completed paid demo appointment(s). Commission is recorded per consultation.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Text("Ready to complete (${ready.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Only confirmed online appointments where doctor and patient both joined the call appear here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!busy && ready.isEmpty()) item {
                Text("No DemoPay consultations are ready for completion yet.", fontSize = 13.sp)
            }
            items(ready, key = { it.appointment_id }) { visit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(visit.patient_name ?: "Patient", fontWeight = FontWeight.Bold)
                        Text("${visit.appointment_date} • ${visit.appointment_time} • ${money(visit.total_minor.toLong())}", fontSize = 12.sp)
                        Button(enabled = releasingId == null, onClick = { confirmation = visit }) {
                            Text("Complete consultation")
                        }
                    }
                }
            }
            item { Text("Released virtual earnings", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (!busy && earnings.isEmpty()) item {
                Text("No virtual earnings released yet. Complete a scheduled DemoPay consultation to create the first entry.", fontSize = 13.sp)
            }
            items(earnings, key = { it.appointment_id }) { earning ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(earning.appointment_date ?: "Consultation", fontWeight = FontWeight.SemiBold)
                        Text("Payment: ${earning.payment_reference}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyRow("Total", money(earning.total_minor.toLong()))
                        MoneyRow("Doctor (${100 - earning.commission_bps / 100.0}% before rounding)", money(earning.doctor_minor.toLong()), emphasized = true)
                        MoneyRow("EHealthy (${earning.commission_bps / 100.0}%)", money(earning.platform_minor.toLong()))
                    }
                }
            }
            item { Spacer(Modifier.padding(12.dp)) }
        }
    }
}

@Composable
private fun MoneyRow(label: String, amount: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun money(minor: Long): String = String.format(Locale.forLanguageTag("en-ZA"), "R %,.2f", minor / 100.0)
