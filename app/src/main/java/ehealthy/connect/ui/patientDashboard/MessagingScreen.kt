package ehealthy.connect.ui.patientDashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class MessageRow(
    val id: String? = null,

    @SerialName("doctor_id")
    val doctorId: String,

    @SerialName("patient_id")
    val patientId: String,

    val sender: String,

    val message: String,

    @SerialName("created_at")
    val createdAt: String? = null
)
@Serializable
private data class DoctorNameRow(
    val name: String,
    val surname: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    doctorId: String,
    onBack: () -> Unit
) {

    val patientId =
        SupabaseClientProvider.client
            .auth
            .currentSessionOrNull()
            ?.user
            ?.id

    var messages by remember {
        mutableStateOf<List<MessageRow>>(emptyList())
    }

    var text by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var doctorName by remember {
        mutableStateOf("Doctor")
    }

    LaunchedEffect(doctorId) {
        try {
            val doctor = SupabaseClientProvider.client
                .postgrest
                .from("doctors")
                .select {
                    filter {
                        eq("id", doctorId)
                    }
                }
                .decodeSingle<DoctorNameRow>()

            doctorName = "${doctor.name} ${doctor.surname}"

        } catch (e: Exception) {
            // Keep "Doctor" if the name cannot be loaded
        }
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    /*
     * Load the conversation between this patient
     * and this doctor.
     */
    suspend fun loadMessages() {

        if (patientId == null) {

            error = "Please log in to send messages."
            isLoading = false

            return
        }

        try {

            val result =
                SupabaseClientProvider.client
                    .postgrest
                    .from("messages")
                    .select {

                        filter {

                            eq(
                                "patient_id",
                                patientId
                            )

                            eq(
                                "doctor_id",
                                doctorId
                            )
                        }
                    }
                    .decodeList<MessageRow>()

            messages =
                result.sortedBy {
                    it.createdAt ?: ""
                }

            error = null

        } catch (e: Exception) {

            error =
                "Could not load messages: ${e.message}"

        } finally {

            isLoading = false
        }
    }

    /*
     * Initial load.
     */
    LaunchedEffect(
        patientId,
        doctorId
    ) {

        loadMessages()
    }

    /*
     * Automatically scroll to the
     * newest message.
     */
    LaunchedEffect(messages.size) {

        if (messages.isNotEmpty()) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    /*
     * Refresh the conversation every
     * five seconds.
     */
    LaunchedEffect(
        patientId,
        doctorId
    ) {

        while (true) {

            delay(5000)

            loadMessages()
        }
    }

    /*
     * Send a patient message.
     */
    fun sendMessage() {

        val currentPatientId =
            patientId ?: return

        val messageText =
            text.trim()

        if (messageText.isBlank()) {
            return
        }

        scope.launch {

            try {

                SupabaseClientProvider.client
                    .postgrest
                    .from("messages")
                    .insert(

                        buildJsonObject {

                            put(
                                "doctor_id",
                                doctorId
                            )

                            put(
                                "patient_id",
                                currentPatientId
                            )

                            put(
                                "sender",
                                "patient"
                            )

                            put(
                                "message",
                                messageText
                            )
                        }
                    )

                /*
                 * Clear input after successful send.
                 */
                text = ""

                /*
                 * Reload messages so the new
                 * message immediately appears.
                 */
                loadMessages()

            } catch (e: Exception) {

                error =
                    "Message could not be sent: ${e.message}"
            }
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                if (doctorName.isBlank())
                                    "Doctor"
                                else
                                    "Dr. $doctorName",

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                18.sp
                        )

                        Text(
                            text =
                                "Conversation",

                            fontSize =
                                11.sp,

                            color =
                                Color(0xFF64748B)
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                Color.White
                        )
            )
        },

        bottomBar = {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White
                        )
                        .imePadding()
                        .padding(10.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(

                    value = text,

                    onValueChange = {
                        text = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    placeholder = {
                        Text(
                            "Write a message…"
                        )
                    },

                    maxLines = 4,

                    keyboardOptions =
                        KeyboardOptions(
                            imeAction =
                                ImeAction.Default
                        ),

                    shape =
                        RoundedCornerShape(
                            22.dp
                        )
                )

                IconButton(

                    onClick = {
                        sendMessage()
                    },

                    enabled =
                        text.isNotBlank() &&
                                patientId != null
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Send,

                        contentDescription =
                            "Send",

                        tint =
                            Color(0xFF0D9488)
                    )
                }
            }
        }

    ) { padding ->

        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Color(0xFFF4F7FA)
                    )
        ) {

            when {

                isLoading -> {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.align(
                                Alignment.Center
                            ),

                        color =
                            Color(0xFF0D9488)
                    )
                }

                messages.isEmpty() -> {

                    Text(

                        text =
                            "Start a conversation with Dr. $doctorName.",

                        modifier =
                            Modifier
                                .align(
                                    Alignment.Center
                                )
                                .padding(24.dp),

                        color =
                            Color(0xFF64748B)
                    )
                }

                else -> {

                    LazyColumn(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),

                        state =
                            listState,

                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        items(

                            items =
                                messages,

                            key = { message ->

                                message.id
                                    ?: "${message.createdAt}-${message.message}"
                            }

                        ) { message ->

                            val isPatientMessage =
                                message.sender ==
                                        "patient"

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth(),

                                horizontalArrangement =
                                    if (isPatientMessage)
                                        Arrangement.End
                                    else
                                        Arrangement.Start
                            ) {

                                Text(

                                    text =
                                        message.message,

                                    modifier =
                                        Modifier
                                            .background(

                                                if (isPatientMessage)
                                                    Color(0xFFCCFBF1)
                                                else
                                                    Color.White,

                                                RoundedCornerShape(
                                                    16.dp
                                                )
                                            )
                                            .padding(
                                                horizontal = 14.dp,
                                                vertical = 10.dp
                                            ),

                                    color =
                                        Color(0xFF0F1F3D),

                                    fontSize =
                                        14.sp
                                )
                            }
                        }
                    }
                }
            }

            /*
             * Display errors at the top.
             */
            error?.let { errorMessage ->

                Text(

                    text =
                        errorMessage,

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .padding(12.dp),

                    color =
                        Color(0xFFDC2626),

                    fontSize =
                        12.sp
                )
            }
        }
    }
}

