package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DoctorNotification(
    val id: String,
    val doctor_id: String,
    val notification_type: String,
    val title: String,
    val message: String,
    val related_entity_type: String? = null,
    val related_entity_id: String? = null,
    val is_read: Boolean = false,
    val created_at: String
)

@Serializable
private data class DoctorNotificationDoctorId(
    val id: String
)

object DoctorNotificationRepository {

    private val supabase = SupabaseClientProvider.client

    private suspend fun requireDoctorId(): String {
        supabase.auth.awaitInitialization()

        val userId =
            supabase.auth.currentUserOrNull()?.id
                ?: error("Doctor is not signed in.")

        return supabase
            .from("doctors")
            .select(columns = Columns.list("id")) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<DoctorNotificationDoctorId>()
            ?.id
            ?: error("Doctor profile could not be found.")
    }

    suspend fun getNotifications(): Result<List<DoctorNotification>> =
        runCatching {
            val doctorId = requireDoctorId()

            supabase
                .from("doctor_notifications")
                .select(
                    columns = Columns.list(
                        "id",
                        "doctor_id",
                        "notification_type",
                        "title",
                        "message",
                        "related_entity_type",
                        "related_entity_id",
                        "is_read",
                        "created_at"
                    )
                ) {
                    filter {
                        eq("doctor_id", doctorId)
                    }
                }
                .decodeList<DoctorNotification>()
                .sortedByDescending {
                    it.created_at
                }
        }

    suspend fun getUnreadCount(): Result<Int> =
        getNotifications()
            .map { notifications ->
                notifications.count { !it.is_read }
            }

    suspend fun markAsRead(
        notificationId: String
    ): Result<Unit> =
        runCatching {
            val doctorId = requireDoctorId()

            supabase
                .from("doctor_notifications")
                .update(
                    {
                        set("is_read", true)
                        set(
                            "read_at",
                            java.time.Instant.now().toString()
                        )
                    }
                ) {
                    filter {
                        eq("id", notificationId)
                        eq("doctor_id", doctorId)
                    }
                }
        }

    suspend fun markAllAsRead(): Result<Unit> =
        runCatching {
            val doctorId = requireDoctorId()

            supabase
                .from("doctor_notifications")
                .update(
                    {
                        set("is_read", true)
                        set(
                            "read_at",
                            java.time.Instant.now().toString()
                        )
                    }
                ) {
                    filter {
                        eq("doctor_id", doctorId)
                        eq("is_read", false)
                    }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorNotificationsScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var notifications by remember {
        mutableStateOf<List<DoctorNotification>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun loadNotifications(
        refreshing: Boolean = false
    ) {
        if (refreshing) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        errorMessage = null

        DoctorNotificationRepository
            .getNotifications()
            .onSuccess {
                notifications = it
            }
            .onFailure {
                errorMessage =
                    it.message
                        ?: "Could not load doctor notifications."
            }

        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val unreadCount =
        notifications.count {
            !it.is_read
        }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text =
                                if (unreadCount == 1) {
                                    "1 unread update"
                                } else {
                                    "$unreadCount unread updates"
                                },
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    DoctorNotificationRepository
                                        .markAllAsRead()
                                        .onSuccess {
                                            notifications =
                                                notifications.map {
                                                    it.copy(
                                                        is_read = true
                                                    )
                                                }
                                        }
                                }
                            }
                        ) {
                            Text(
                                text = "Read all",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                loadNotifications(
                                    refreshing = true
                                )
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector =
                                    Icons.Outlined.Refresh,
                                contentDescription =
                                    "Refresh notifications"
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        }
    ) { paddingValues ->

        when {
            isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    Text(
                        text =
                            errorMessage.orEmpty(),
                        color =
                            MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    TextButton(
                        onClick = {
                            scope.launch {
                                loadNotifications()
                            }
                        }
                    ) {
                        Text("Try again")
                    }
                }
            }

            notifications.isEmpty() -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(28.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer,
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.Notifications,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    Text(
                        text = "No notifications yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text =
                            "New appointments, completion codes and physical follow-up requests will appear here.",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->

                        DoctorNotificationCard(
                            notification = notification,
                            onClick = {
                                if (!notification.is_read) {
                                    scope.launch {
                                        DoctorNotificationRepository
                                            .markAsRead(
                                                notification.id
                                            )
                                            .onSuccess {
                                                notifications =
                                                    notifications.map {
                                                        if (
                                                            it.id ==
                                                            notification.id
                                                        ) {
                                                            it.copy(
                                                                is_read = true
                                                            )
                                                        } else {
                                                            it
                                                        }
                                                    }
                                            }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorNotificationCard(
    notification: DoctorNotification,
    onClick: () -> Unit
) {
    val accent =
        if (notification.is_read) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (notification.is_read) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(alpha = 0.38f)
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Row(
            modifier =
                Modifier.padding(15.dp),
            verticalAlignment =
                Alignment.Top
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            accent.copy(alpha = 0.10f),
                            RoundedCornerShape(13.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        doctorNotificationIcon(
                            notification.notification_type
                        ),
                    contentDescription =
                        null,
                    tint =
                        accent,
                    modifier =
                        Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(11.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            notification.title,
                        modifier =
                            Modifier.weight(1f),
                        fontWeight =
                            if (notification.is_read) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.ExtraBold
                            },
                        fontSize = 13.sp
                    )

                    if (!notification.is_read) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        notification.message,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                Text(
                    text =
                        formatDoctorNotificationTime(
                            notification.created_at
                        ),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

private fun doctorNotificationIcon(
    type: String
): ImageVector =
    when (type.lowercase()) {
        "appointment_created" ->
            Icons.Outlined.CalendarMonth

        "completion_code_ready" ->
            Icons.Outlined.Verified

        "physical_visit_request" ->
            Icons.Outlined.LocationOn

        else ->
            Icons.Outlined.Notifications
    }

private fun formatDoctorNotificationTime(
    raw: String
): String {
    return runCatching {
        val instant =
            java.time.Instant.parse(raw)

        val local =
            java.time.ZonedDateTime
                .ofInstant(
                    instant,
                    java.time.ZoneId.systemDefault()
                )

        local.format(
            java.time.format.DateTimeFormatter
                .ofPattern(
                    "dd MMM yyyy • HH:mm"
                )
        )
    }
        .getOrDefault(raw)
}
