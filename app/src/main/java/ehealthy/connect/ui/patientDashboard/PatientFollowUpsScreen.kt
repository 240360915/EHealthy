package ehealthy.connect.ui.patientDashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.patient.PhysicalVisitRequest

/**
 * A short overview of every in-person follow-up the signed-in patient requested.
 * The existing accepted-follow-ups screen owns maps, locations and invoice details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFollowUpsScreen(
    onBack: () -> Unit,
    onViewAccepted: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = MaterialTheme.colorScheme
    var requests by remember { mutableStateOf<List<PhysicalVisitRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        PatientRepository.getMyPhysicalVisitRequests()
            .onSuccess { requests = it }
            .onFailure { error = it.message ?: "Unable to load your follow-up requests." }
        loading = false
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("My follow-ups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }, enabled = !loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh follow-ups")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Your in-person follow-up requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (loading) "Loading your follow-ups..."
                        else if (error != null) "See your request history and accepted visits."
                        else if (requests.isEmpty()) "You have not requested an in-person follow-up yet."
                        else if (requests.size == 1) "1 follow-up request"
                        else requests.size.toString() + " follow-up requests",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer
                    )
                    Button(
                        onClick = onViewAccepted,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View Accepted Follow Ups")
                    }
                }
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Couldn't load follow-ups", fontWeight = FontWeight.Bold,
                                color = colors.onErrorContainer)
                            Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                color = colors.onErrorContainer)
                            TextButton(onClick = { refreshKey++ }) { Text("Try again") }
                        }
                    }
                }
                requests.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Text(
                            "Your follow-ups will appear here after you request an in-person visit.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(requests, key = { it.id }) { request ->
                            FollowUpSummaryCard(request)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpSummaryCard(request: PhysicalVisitRequest) {
    val colors = MaterialTheme.colorScheme
    val status = request.status.lowercase()
    val statusText = when (status) {
        "pending" -> "Awaiting doctor"
        "accepted" -> "Doctor accepted"
        "invoiced" -> "Invoice sent"
        "patient_accepted" -> "Visit accepted"
        "patient_declined" -> "Invoice declined"
        "declined", "rejected" -> "Declined"
        "cancelled", "canceled" -> "Cancelled"
        else -> request.status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    val isAccepted = status in setOf("accepted", "invoiced", "patient_accepted")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = colors.primary
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "In-person follow-up",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
            Text(
                request.reason.ifBlank { "Follow-up visit request" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Requested " + request.requested_at.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAccepted) colors.primaryContainer else colors.surfaceVariant
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAccepted) colors.onPrimaryContainer else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
