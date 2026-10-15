package ehealthy.connect.ui.doctorDashboard

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

@Serializable
private data class EditablePracticeLocation(
    val location_name: String? = null,
    val location_address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** The same stable clinic map pin and doctor_save_practice_location RPC as the web portal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPracticeLocationScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val saved = SupabaseClientProvider.client.postgrest
                .rpc("doctor_get_my_editable_practice_location")
                .decodeList<EditablePracticeLocation>().firstOrNull()
            name = saved?.location_name.orEmpty()
            address = saved?.location_address.orEmpty()
            latitude = saved?.latitude
            longitude = saved?.longitude
        } catch (e: Exception) {
            message = e.message ?: "Could not load your saved practice location."
        } finally { loading = false }
    }

    Scaffold(topBar = { TopAppBar(
        title = { Text("Practice location") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
    ) }) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set your fixed practice map pin. After accepting a follow-up, the patient can preview this location. The same pin is copied into new invoices; it is not live tracking.")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Practice or location name") }, singleLine = true
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Street, suburb and city") }
            )
            if (loading) CircularProgressIndicator() else {
                Text("Tap the map to place or move the exact practice pin.")
                PracticePinPicker(
                    latitude = latitude,
                    longitude = longitude,
                    onPick = { lat, lng ->
                        latitude = lat
                        longitude = lng
                        message = null
                    }
                )
                Text(
                    if (latitude == null || longitude == null) "No pin selected yet."
                    else "Pin: ${"%.6f".format(Locale.US, latitude)}°, ${"%.6f".format(Locale.US, longitude)}°"
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && name.isNotBlank() && address.trim().length >= 3 &&
                        latitude != null && longitude != null,
                    onClick = {
                        saving = true
                        scope.launch {
                            try {
                                SupabaseClientProvider.client.postgrest.rpc(
                                    "doctor_save_practice_location",
                                    parameters = buildJsonObject {
                                        put("p_location_name", name.trim())
                                        put("p_address", address.trim())
                                        put("p_latitude", requireNotNull(latitude))
                                        put("p_longitude", requireNotNull(longitude))
                                    }
                                )
                                message = "Practice location saved. New invoices will carry this map pin."
                            } catch (e: Exception) {
                                message = e.message ?: "Could not save your practice location."
                            } finally { saving = false }
                        }
                    }
                ) { Text(if (saving) "Saving…" else "Save practice location") }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to dashboard")
                }
            }
        }
    }
}

@Composable
private fun PracticePinPicker(
    latitude: Double?,
    longitude: Double?,
    onPick: (Double, Double) -> Unit
) {
    // Default map center is NOT a stored doctor pin.
    val startLat = latitude ?: -25.47
    val startLng = longitude ?: 30.98
    val initialMarker = if (latitude != null && longitude != null)
        "placePin($latitude,$longitude,false);" else ""
    val html = remember(startLat, startLng) {
        """
        <!DOCTYPE html><html><head><meta name="viewport"
        content="width=device-width,initial-scale=1">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
        <style>html,body,#map{height:100%;width:100%;margin:0}</style></head>
        <body><div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          const map=L.map('map').setView([$startLat,$startLng],13);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            {attribution:'© OpenStreetMap contributors',maxZoom:19}).addTo(map);
          let marker;
          function placePin(lat,lng,notify) {
            if(marker) marker.setLatLng([lat,lng]);
            else marker=L.marker([lat,lng]).addTo(map);
            if(notify) location.href='ehealthy-pin://pick?lat='+lat+'&lng='+lng;
          }
          $initialMarker
          map.on('click',function(e){placePin(e.latlng.lat,e.latlng.lng,true)});
        </script></body></html>
        """.trimIndent()
    }
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri: Uri = request?.url ?: return false
                    if (uri.scheme != "ehealthy-pin") return false
                    val lat = uri.getQueryParameter("lat")?.toDoubleOrNull()
                    val lng = uri.getQueryParameter("lng")?.toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        onPick(lat, lng)
                    }
                    return true
                }
            }
            loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null)
        }
    }, modifier = Modifier.fillMaxWidth().height(300.dp))
}
