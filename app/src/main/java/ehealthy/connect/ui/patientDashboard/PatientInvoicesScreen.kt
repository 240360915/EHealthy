package ehealthy.connect.ui.patientDashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.data.patient.PatientDoctorSummary
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.patient.PhysicalVisitInvoice
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private enum class InvoiceFilter(val label: String) {
    ALL("All"),
    AWAITING("Awaiting"),
    ACCEPTED("Accepted"),
    DECLINED("Declined")
}

private val InvoiceSuccess = Color(0xFF15803D)
private val InvoiceWarning = Color(0xFFF59E0B)
private val InvoiceDanger = Color(0xFFB91C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientInvoicesScreen(
    initialInvoiceId: String? = null,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Export the invoice currently selected by the patient.
    var pendingPdfInvoice by remember { mutableStateOf<PhysicalVisitInvoice?>(null) }
    var pendingPdfDoctor by remember { mutableStateOf<PatientDoctorSummary?>(null) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val invoice = pendingPdfInvoice
        if (uri != null && invoice != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    InvoicePdfExporter.writeInvoicePdf(
                        context = context,
                        invoice = invoice,
                        doctor = pendingPdfDoctor,
                        output = stream
                    )
                } ?: error("Could not open the selected PDF file.")
            }.onSuccess {
                Toast.makeText(context, "Invoice PDF saved", Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Could not save invoice PDF.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        pendingPdfInvoice = null
        pendingPdfDoctor = null
    }


    var invoices by remember { mutableStateOf<List<PhysicalVisitInvoice>>(emptyList()) }
    var doctors by remember { mutableStateOf<Map<String, PatientDoctorSummary>>(emptyMap()) }
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var decisionInvoice by remember { mutableStateOf<PhysicalVisitInvoice?>(null) }
    var decisionType by remember { mutableStateOf<String?>(null) }
    var decisionInProgressId by remember { mutableStateOf<String?>(null) }

    suspend fun loadInvoices() {
        isLoading = true
        errorMessage = null

        PatientRepository.getMyPhysicalVisitInvoices()
            .onSuccess { result ->
                invoices = result

                val loaded = mutableMapOf<String, PatientDoctorSummary>()
                result.map { it.doctor_id }.distinct().forEach { doctorId ->
                    PatientRepository.getDoctor(doctorId).getOrNull()?.let {
                        loaded[doctorId] = it
                    }
                }
                doctors = loaded
            }
            .onFailure {
                errorMessage = it.message ?: "Could not load your invoices."
            }

        isLoading = false
    }

    LaunchedEffect(Unit) { loadInvoices() }

    val visibleInvoices = invoices
        .filter { invoice ->
            when (selectedFilter) {
                InvoiceFilter.ALL -> true
                InvoiceFilter.AWAITING -> invoice.status.equals("sent", true)
                InvoiceFilter.ACCEPTED -> invoice.status.equals("accepted", true)
                InvoiceFilter.DECLINED -> invoice.status.equals("declined", true)
            }
        }
        .sortedWith(
            compareByDescending<PhysicalVisitInvoice> { it.id == initialInvoiceId }
                .thenByDescending { it.issued_at }
        )

    decisionInvoice?.let { invoice ->
        InvoiceDecisionDialog(
            invoice = invoice,
            decision = decisionType ?: "accepted",
            isSubmitting = decisionInProgressId == invoice.id,
            onDismiss = {
                if (decisionInProgressId == null) {
                    decisionInvoice = null
                    decisionType = null
                }
            },
            onConfirm = {
                val decision = decisionType ?: return@InvoiceDecisionDialog
                scope.launch {
                    decisionInProgressId = invoice.id
                    PatientRepository.respondToPhysicalVisitInvoice(
                        invoiceId = invoice.id,
                        decision = decision
                    )
                        .onSuccess {
                            decisionInvoice = null
                            decisionType = null
                            loadInvoices()
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Could not update the invoice."
                        }
                    decisionInProgressId = null
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Column {
                        Text("My invoices", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            "Physical visits, maps and PDF invoices",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { loadInvoices() } }) {
                        Icon(Icons.Outlined.Refresh, "Refresh invoices")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                InvoiceFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 10.5.sp) }
                    )
                }
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(errorMessage ?: "Could not load invoices.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { loadInvoices() } }) {
                            Text("Try again")
                        }
                    }
                }

                visibleInvoices.isEmpty() -> EmptyInvoices()

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleInvoices, key = { it.id }) { invoice ->
                            InvoiceCard(
                                invoice = invoice,
                                doctor = doctors[invoice.doctor_id],
                                highlighted = invoice.id == initialInvoiceId,
                                busy = decisionInProgressId == invoice.id,
                                onAccept = {
                                    decisionInvoice = invoice
                                    decisionType = "accepted"
                                },
                                onDecline = {
                                    decisionInvoice = invoice
                                    decisionType = "declined"
                                },
                                onDownloadPdf = {
                                    pendingPdfInvoice = invoice
                                    pendingPdfDoctor = doctors[invoice.doctor_id]
                                    createPdfLauncher.launch(
                                        "EHealthy_Invoice_${invoice.id.take(8)}.pdf"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(
    invoice: PhysicalVisitInvoice,
    doctor: PatientDoctorSummary?,
    highlighted: Boolean,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (highlighted) 2.dp else 1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        doctor?.fullName ?: "Doctor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        invoice.service_description,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                InvoiceStatusChip(invoice.status)
            }

            Text(
                formatInvoiceAmount(invoice),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )

            InvoiceDetailRow("Date", invoice.proposed_date)
            InvoiceDetailRow("Time", formatInvoiceTime(invoice.proposed_time))
            InvoiceDetailRow(
                "Location",
                invoice.location_name?.takeIf { it.isNotBlank() } ?: "Doctor practice"
            )

            Text(
                invoice.location,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                ) {
                    Text(notes, modifier = Modifier.padding(11.dp), fontSize = 10.5.sp)
                }
            }

            val latitude = invoice.location_latitude
            val longitude = invoice.location_longitude

            if (latitude != null && longitude != null) {
                InvoiceMapPreview(latitude, longitude)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openInvoiceMap(context, invoice) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.LocationOn, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Open map")
                    }

                    Button(
                        onClick = { openInvoiceDirections(context, invoice) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Directions, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Directions")
                    }
                }
            }

            // Available for every issued status, including accepted/declined.
            // This remains an invoice copy, not a paid receipt.
            OutlinedButton(
                onClick = onDownloadPdf,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !busy
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download invoice PDF", fontWeight = FontWeight.SemiBold)
            }

            if (invoice.status.equals("sent", true)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = onAccept,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceMapPreview(
    latitude: Double,
    longitude: Double
) {
    val delta = 0.006
    val url =
        "https://www.openstreetmap.org/export/embed.html" +
                "?bbox=${longitude - delta},${latitude - delta}," +
                "${longitude + delta},${latitude + delta}" +
                "&layer=mapnik&marker=$latitude,$longitude"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl(url)
            }
        },
        update = {
            if (it.url != url) it.loadUrl(url)
        },
        modifier = Modifier.fillMaxWidth().height(190.dp)
    )
}

@Composable
private fun InvoiceStatusChip(status: String) {
    val normalized = status.lowercase()

    val background = when (normalized) {
        "accepted" -> InvoiceSuccess.copy(alpha = 0.12f)
        "declined", "cancelled" -> InvoiceDanger.copy(alpha = 0.12f)
        else -> InvoiceWarning.copy(alpha = 0.14f)
    }

    val foreground = when (normalized) {
        "accepted" -> InvoiceSuccess
        "declined", "cancelled" -> InvoiceDanger
        else -> InvoiceWarning
    }

    Surface(color = background, shape = RoundedCornerShape(50.dp)) {
        Text(
            text = if (normalized == "sent") "Awaiting response"
            else normalized.replace("_", " ").replaceFirstChar { it.uppercase() },
            color = foreground,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InvoiceDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.width(70.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp
        )
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
    }
}

@Composable
private fun InvoiceDecisionDialog(
    invoice: PhysicalVisitInvoice,
    decision: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val accepting = decision == "accepted"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (accepting) "Accept physical visit?" else "Decline invoice?") },
        text = {
            Text(
                if (accepting) {
                    "You are accepting ${formatInvoiceAmount(invoice)} for " +
                            "${invoice.proposed_date} at ${formatInvoiceTime(invoice.proposed_time)}. " +
                            "The physical visit arrangement will be confirmed."
                } else {
                    "This physical visit arrangement will be declined."
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (accepting) "Accept" else "Decline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Back")
            }
        }
    )
}

@Composable
private fun EmptyInvoices() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Payments,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text("No invoices here", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Physical visit invoices will appear here when a doctor sends one.",
                modifier = Modifier.padding(horizontal = 32.dp),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatInvoiceAmount(invoice: PhysicalVisitInvoice): String {
    val amount = invoice.amount_minor / 100.0
    return if (invoice.currency.equals("ZAR", true)) {
        "R %.2f".format(amount)
    } else {
        "${invoice.currency} %.2f".format(amount)
    }
}

private fun formatInvoiceTime(raw: String): String {
    val parts = raw.take(5).split(":")
    if (parts.size < 2) return raw

    val hour = parts[0].toIntOrNull() ?: return raw
    val minute = parts[1]
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    return "$displayHour:$minute $period"
}

private fun openInvoiceMap(
    context: Context,
    invoice: PhysicalVisitInvoice
) {
    val latitude = invoice.location_latitude ?: return
    val longitude = invoice.location_longitude ?: return

    val label = invoice.location_name?.takeIf { it.isNotBlank() } ?: invoice.location
    val uri = Uri.parse(
        "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})"
    )

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }.onFailure {
        val fallback = Uri.parse(
            "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, fallback))
    }
}

private fun openInvoiceDirections(
    context: Context,
    invoice: PhysicalVisitInvoice
) {
    val latitude = invoice.location_latitude ?: return
    val longitude = invoice.location_longitude ?: return

    val destination = URLEncoder.encode(
        "$latitude,$longitude",
        StandardCharsets.UTF_8.toString()
    )

    val url = "https://www.google.com/maps/dir/?api=1&destination=$destination"

    context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
    )
}
