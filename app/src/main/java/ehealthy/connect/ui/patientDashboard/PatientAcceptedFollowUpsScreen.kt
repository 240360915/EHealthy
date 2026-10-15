package ehealthy.connect.ui.patientDashboard

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

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

/** The patient only sees the pin after their own doctor accepted the request.
 * Supabase checks patient ownership, not client-supplied doctor IDs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAcceptedFollowUpsScreen(
    onBack: () -> Unit,
    onOpenInvoices: () -> Unit
) {
    BackHandler(onBack = onBack)
    var rows by remember { mutableStateOf<List<AcceptedFollowUpPin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(refresh) {
        loading = true
        try {
            rows = SupabaseClientProvider.client.postgrest
                .rpc("patient_get_my_accepted_followup_locations")
                .decodeList<AcceptedFollowUpPin>()
            error = null
        } catch (e: Exception) { error = e.message ?: "Could not load follow-up locations." }
        finally { loading = false }
    }
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(
        title = { Text("Accepted follow-ups") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
        actions = { TextButton(onClick = { refresh++ }) { Text("Refresh") } }
    ) }) { insets ->
        if (loading) Box(Modifier.fillMaxSize().padding(insets)) { CircularProgressIndicator() }
        else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Your accepted in-person requests. The map pin shows the doctor's saved practice, not live tracking. The proposed date, time and fee are only confirmed once you accept an invoice.")
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (rows.isEmpty() && error == null) {
                item { Text("No accepted follow-up requests yet.") }
            }
            items(rows, key = { it.request_id }) { request ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            when(request.request_status) {
                                "accepted" -> "Doctor accepted • invoice not sent yet"
                                "invoiced" -> "Invoice sent • awaiting your response"
                                "patient_accepted" -> "You accepted the invoice arrangement"
                                else -> request.request_status
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(request.request_reason, style = MaterialTheme.typography.bodyMedium)
                        Text(request.practice_name?.takeIf(String::isNotBlank) ?: "Doctor's practice",
                            style = MaterialTheme.typography.titleMedium)
                        Text(request.practice_address?.takeIf(String::isNotBlank)
                            ?: "The doctor has not saved a practice address yet.")
                        val lat = request.latitude
                        val lng = request.longitude
                        if (lat != null && lng != null) {
                            AcceptedPracticeMapPreview(lat, lng)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val url = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                                    context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
                                }
                            ) { Text("View map and directions") }
                        } else {
                            Text("Map pin unavailable. Ask the doctor to save their practice location.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (!request.invoice_id.isNullOrBlank()) {
                            Button(onClick = onOpenInvoices, modifier = Modifier.fillMaxWidth()) {
                                Text("Review visit invoice")
                            }
                        } else {
                            Text("The doctor will send a separate invoice for your approval.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcceptedPracticeMapPreview(lat: Double, lng: Double) {
    val delta = 0.008
    val url = "https://www.openstreetmap.org/export/embed.html" +
        "?bbox=${lng-delta},${lat-delta},${lng+delta},${lat+delta}" +
        "&layer=mapnik&marker=$lat,$lng"
    AndroidView(
        factory = { ctx -> WebView(ctx).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(url)
        }},
        update = { if (it.url != url) it.loadUrl(url) },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}
