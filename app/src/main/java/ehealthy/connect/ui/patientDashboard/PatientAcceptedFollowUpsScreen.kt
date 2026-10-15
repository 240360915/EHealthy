package ehealthy.connect.ui.patientDashboard

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
private data class AcceptedFollowUpPin(
    val request_id: String,
    val request_status: String,
    val request_reason: String,
    val practice_name: String? = null,
    val practice_address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val invoice_id: String? = null,
    val invoice_status: String? = null
)

/** This is not live tracking. Only a patient who owns an accepted
 * request can retrieve the doctor’s saved practice pin via the RPC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAcceptedFollowUpsScreen(
    onBack: () -> Unit,
    onOpenInvoices: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = MaterialTheme.colorScheme
    var visits by remember { mutableStateOf<List<AcceptedFollowUpPin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            visits = SupabaseClientProvider.client.postgrest
                .rpc("patient_get_my_accepted_followup_locations")
                .decodeList<AcceptedFollowUpPin>()
        } catch (e: Exception) {
            error = e.message ?: "Could not load accepted follow-ups."
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Accepted follow-ups", fontWeight = FontWeight.Bold)
                        Text("Practice maps & invoice arrangements",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh++ }, enabled = !loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh follow-ups")
                    }
                }
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(Modifier.size(49.dp).clip(RoundedCornerShape(15.dp))
                            .background(colors.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Map, contentDescription = null,
                                tint = colors.primary, modifier = Modifier.size(27.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Visit your doctor's practice",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer)
                            Text("Explore the saved clinic location and directions. A doctor accepting your request does not confirm a date or fee.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onPrimaryContainer)
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            } else {
                error?.let { problem ->
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                        ) {
                            Column(Modifier.padding(17.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Unable to load follow-ups", fontWeight = FontWeight.Bold,
                                    color = colors.onErrorContainer)
                                Text(problem, color = colors.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { refresh++ }) { Text("Try again") }
                            }
                        }
                    }
                }
                if (visits.isEmpty() && error == null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null,
                                    tint = colors.primary, modifier = Modifier.size(35.dp))
                                Text("No accepted follow-ups yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("Once your doctor accepts an in-person request, its practice location will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                items(visits, key = { it.request_id }) { visit ->
                    AcceptedFollowUpCard(visit = visit, onOpenInvoices = onOpenInvoices)
                }
            }
        }
    }
}

@Composable
private fun AcceptedFollowUpCard(
    visit: AcceptedFollowUpPin,
    onOpenInvoices: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lat = visit.latitude
    val lng = visit.longitude
    val mapAvailable = lat != null && lng != null
    val statusText = when (visit.request_status.lowercase()) {
        "accepted" -> "Accepted • Awaiting invoice"
        "invoiced" -> "Invoice sent • Your decision needed"
        "patient_accepted" -> "Visit arrangement accepted"
        else -> visit.request_status.replace('_', ' ')
    }
    val hasInvoice = !visit.invoice_id.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp))
                    .background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                        tint = colors.primary)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("In-person follow-up", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text(statusText, color = colors.primary,
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(color = colors.outlineVariant)
            Text("REQUEST REASON", style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant)
            Text(visit.request_reason, style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface)

            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null,
                        tint = colors.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(visit.practice_name?.takeIf(String::isNotBlank) ?: "Doctor's practice",
                            fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text(visit.practice_address?.takeIf(String::isNotBlank)
                            ?: "The doctor has not saved a practice address.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (mapAvailable) {
                Text("PRACTICE MAP", style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                    AcceptedPracticeMapPreview(lat!!, lng!!)
                }
                OutlinedButton(
                    onClick = {
                        val destination = String.format(Locale.US,
                            "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f",lat,lng)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destination)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View map & directions")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null,
                        tint = colors.error)
                    Spacer(Modifier.width(9.dp))
                    Text("Map pin unavailable. Ask the doctor to save their practice location.",
                        style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
            }

            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null,
                        tint = colors.onSecondaryContainer)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (hasInvoice) "Visit invoice available" else "Awaiting visit invoice",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSecondaryContainer)
                        Text(if (hasInvoice)
                            "Review the proposed visit and its payment details."
                            else "The visit date, time and fee are not confirmed yet.",
                            color = colors.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (hasInvoice) {
                Button(
                    onClick = onOpenInvoices, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Open visit invoice")
                }
            }
        }
    }
}

@Composable
private fun AcceptedPracticeMapPreview(lat: Double, lng: Double) {
    val delta = 0.008
    val url = String.format(Locale.US,
        "https://www.openstreetmap.org/export/embed.html?bbox=%.6f,%.6f,%.6f,%.6f&layer=mapnik&marker=%.6f,%.6f",
        lng-delta,lat-delta,lng+delta,lat+delta,lat,lng)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.rgb(237,243,246))
                loadUrl(url)
            }
        },
        update = { if (it.url != url) it.loadUrl(url) },
        modifier = Modifier.fillMaxWidth().height(210.dp)
    )
}
