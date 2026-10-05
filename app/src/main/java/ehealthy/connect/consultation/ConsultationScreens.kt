package ehealthy.connect.consultation

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

private val AvailabilityGreen =
    Color(0xFF15803D)

private val AvailabilityAmber =
    Color(0xFFF59E0B)

private val AvailabilityRed =
    Color(0xFFB91C1C)

private val AvailabilityHeroStart =
    Color(0xFF073B5C)

private val AvailabilityHeroEnd =
    Color(0xFF087F8C)

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun DoctorAvailabilityScreen(
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current

    val scope =
        rememberCoroutineScope()

    var presence by remember {
        mutableStateOf<DoctorPresence?>(
            null
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isUpdating by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    var notificationsEnabled by remember {
        mutableStateOf(
            NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()
        )
    }

    var fullScreenAllowed by remember {
        mutableStateOf(
            canUseFullScreenIntent(
                context
            )
        )
    }

    val serviceStatus by
    DoctorAvailabilityService
        .status
        .collectAsState()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            notificationsEnabled =
                NotificationManagerCompat
                    .from(context)
                    .areNotificationsEnabled()

            if (granted) {

                successMessage =
                    "Notifications are enabled. You can now go available."

                errorMessage =
                    null

            } else {

                errorMessage =
                    "Notifications are required so consultation invitations can reach this phone."
            }
        }

    suspend fun refreshPresence() {

        try {

            presence =
                withTimeout(
                    8_000
                ) {

                    ConsultationRepository
                        .presence()
                }

            notificationsEnabled =
                NotificationManagerCompat
                    .from(context)
                    .areNotificationsEnabled()

            fullScreenAllowed =
                canUseFullScreenIntent(
                    context
                )

            errorMessage =
                null

        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (e: Exception) {

            android.util.Log.e(
                "DoctorAvailability",
                "Failed to load doctor availability",
                e
            )

            errorMessage =
                e.message
                    ?: "We couldn't confirm your availability with the server."
        }

        isLoading =
            false
    }

    fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >= 33
        ) {

            permissionLauncher.launch(
                Manifest.permission
                    .POST_NOTIFICATIONS
            )

        } else {

            val intent =
                Intent(
                    Settings
                        .ACTION_APP_NOTIFICATION_SETTINGS
                )
                    .putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )

            context.startActivity(
                intent
            )
        }
    }

    fun enableAvailability() {

        if (isUpdating) {
            return
        }

        if (
            !NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()
        ) {

            requestNotificationPermission()
            return
        }

        isUpdating = true
        errorMessage = null
        successMessage = null

        scope.launch {

            var currentStep =
                "starting availability"

            try {

                /*
                 * STEP 1
                 * Register Firebase/device token.
                 */
                currentStep =
                    "registering this phone"

                ConsultationDevices
                    .register()


                /*
                 * STEP 2
                 * Tell Supabase that doctor is available.
                 */
                currentStep =
                    "enabling availability on Supabase"

                val enabledPresence =
                    withTimeout(
                        10_000
                    ) {

                        ConsultationRepository
                            .availability(
                                true
                            )
                    }

                presence =
                    enabledPresence


                /*
                 * STEP 3
                 * Start Android foreground heartbeat service.
                 */
                currentStep =
                    "starting the availability service"

                try {

                    ContextCompat
                        .startForegroundService(
                            context,
                            Intent(
                                context,
                                DoctorAvailabilityService::class.java
                            )
                        )

                } catch (e: Exception) {

                    runCatching {

                        withTimeout(
                            5_000
                        ) {

                            ConsultationRepository
                                .availability(
                                    false
                                )
                        }
                    }

                    throw IllegalStateException(
                        "Android could not start the availability service: " +
                                (e.message ?: e.javaClass.simpleName),
                        e
                    )
                }


                /*
                 * STEP 4
                 * Send immediate heartbeat.
                 */
                currentStep =
                    "sending the first heartbeat"

                presence =
                    withTimeout(
                        8_000.milliseconds
                    ) {

                        ConsultationRepository
                            .heartbeat()
                    }


                successMessage =
                    "You are now available for on-demand consultations."

                errorMessage =
                    null

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                android.util.Log.e(
                    "DoctorAvailability",
                    "Availability failed at: $currentStep",
                    e
                )

                errorMessage =
                    "Failed while $currentStep:\n\n" +
                            (
                                    e.message
                                        ?: e.javaClass.simpleName
                                    )
            }

            isUpdating =
                false
        }
    }

    fun disableAvailability() {

        if (
            isUpdating
        ) {
            return
        }

        isUpdating =
            true

        errorMessage =
            null

        successMessage =
            null

        scope.launch {

            try {

                presence =
                    withTimeout(
                        8_000
                    ) {

                        ConsultationRepository
                            .availability(
                                false
                            )
                    }

                successMessage =
                    "You are no longer receiving new on-demand consultation requests."

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                _: Exception
            ) {

                /*
                 * Stop the heartbeat even if the explicit
                 * server call failed.
                 *
                 * The backend freshness timeout will then
                 * make the doctor ineligible automatically.
                 */
                errorMessage =
                    "The server could not confirm the change. Heartbeats have been stopped, so your availability will expire automatically."
            }

            context.stopService(
                Intent(
                    context,
                    DoctorAvailabilityService::class.java
                )
            )

            isUpdating =
                false
        }
    }

    /*
     * Refresh from the server whenever this screen is visible.
     *
     * Do not trust the foreground service text by itself.
     */
    LaunchedEffect(
        lifecycleOwner
    ) {

        lifecycleOwner
            .lifecycle
            .repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                while (
                    isActive
                ) {

                    refreshPresence()

                    delay(
                        15_000
                    )
                }
            }
    }

    val serverAvailable =
        presence
            ?.is_available ==
                true

    val heartbeatFresh =
        isHeartbeatFresh(
            presence
                ?.last_heartbeat
        )

    val readyForRequests =
        serverAvailable &&
                heartbeatFresh

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(
                            text =
                                "Consultation availability",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Manage on-demand requests",
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                actions = {

                    IconButton(
                        enabled =
                            !isLoading &&
                                    !isUpdating,
                        onClick = {

                            scope.launch {

                                isLoading =
                                    true

                                refreshPresence()
                            }
                        }
                    ) {

                        if (
                            isLoading
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    ),
                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.Refresh,
                                contentDescription =
                                    "Refresh"
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
            )
        }

    ) { innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal =
                            16.dp,
                        vertical =
                            14.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                )
        ) {

            AvailabilityHero(
                available =
                    readyForRequests,
                serverAvailable =
                    serverAvailable,
                loading =
                    isLoading
            )

            AvailabilitySwitchCard(
                serverAvailable =
                    serverAvailable,
                updating =
                    isUpdating,
                loading =
                    isLoading,
                onAvailabilityChange = {
                        enabled ->

                    if (
                        enabled
                    ) {

                        enableAvailability()

                    } else {

                        disableAvailability()
                    }
                }
            )

            AvailabilityStatusCard(
                presence =
                    presence,
                heartbeatFresh =
                    heartbeatFresh,
                serviceStatus =
                    serviceStatus
            )

            PhoneReadinessCard(
                notificationsEnabled =
                    notificationsEnabled,
                fullScreenAllowed =
                    fullScreenAllowed,
                onNotificationSettings =
                    ::requestNotificationPermission,
                onFullScreenSettings = {

                    if (
                        Build.VERSION.SDK_INT >= 34
                    ) {

                        context.startActivity(
                            Intent(
                                Settings
                                    .ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse(
                                    "package:${context.packageName}"
                                )
                            )
                        )
                    }
                }
            )

            HowAvailabilityWorksCard()

            AnimatedContent(
                targetState =
                    Pair(
                        errorMessage,
                        successMessage
                    ),
                label =
                    "availability-message"
            ) { state ->

                val error =
                    state.first

                val success =
                    state.second

                when {

                    error != null -> {

                        AvailabilityMessageCard(
                            text =
                                error,
                            isError =
                                true
                        )
                    }

                    success != null -> {

                        AvailabilityMessageCard(
                            text =
                                success,
                            isError =
                                false
                        )
                    }

                    else -> {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    1.dp
                                )
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )
        }
    }
}

@Composable
private fun AvailabilityHero(
    available: Boolean,
    serverAvailable: Boolean,
    loading: Boolean
) {

    val title =
        when {

            loading ->
                "Checking availability..."

            available ->
                "You're available"

            serverAvailable ->
                "Availability needs attention"

            else ->
                "You're unavailable"
        }

    val subtitle =
        when {

            loading ->
                "Confirming your status with the server."

            available ->
                "You can receive eligible private and broadcast consultation requests."

            serverAvailable ->
                "The server still has availability enabled, but the most recent heartbeat is stale."

            else ->
                "Patients will not be matched to you for new on-demand consultations."
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.linearGradient(
                            listOf(
                                AvailabilityHeroStart,
                                AvailabilityHeroEnd
                            )
                        ),
                    shape =
                        RoundedCornerShape(
                            24.dp
                        )
                )
                .padding(
                    21.dp
                )
    ) {

        Column {

            Surface(
                shape =
                    CircleShape,
                color =
                    Color.White
                        .copy(
                            alpha =
                                0.16f
                        )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier
                            .padding(
                                12.dp
                            )
                            .size(
                                29.dp
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )

            Text(
                text =
                    title,
                color =
                    Color.White,
                fontSize =
                    23.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    subtitle,
                color =
                    Color.White
                        .copy(
                            alpha =
                                0.88f
                        ),
                fontSize =
                    12.5.sp,
                lineHeight =
                    18.sp
            )
        }
    }
}

@Composable
private fun AvailabilitySwitchCard(
    serverAvailable: Boolean,
    updating: Boolean,
    loading: Boolean,
    onAvailabilityChange: (
        Boolean
    ) -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,
                color =
                    if (
                        serverAvailable
                    ) {

                        AvailabilityGreen
                            .copy(
                                alpha =
                                    0.12f
                            )

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription =
                        null,
                    tint =
                        if (
                            serverAvailable
                        ) {
                            AvailabilityGreen
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        },
                    modifier =
                        Modifier.padding(
                            11.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        if (
                            serverAvailable
                        ) {
                            "Available"
                        } else {
                            "Unavailable"
                        },
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        if (
                            serverAvailable
                        ) {
                            "Accept new on-demand requests"
                        } else {
                            "Do not send me new requests"
                        },
                    fontSize =
                        11.5.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            if (
                updating
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            26.dp
                        ),
                    strokeWidth =
                        2.5.dp
                )

            } else {

                Switch(
                    checked =
                        serverAvailable,
                    enabled =
                        !loading,
                    onCheckedChange =
                        onAvailabilityChange
                )
            }
        }
    }
}

@Composable
private fun AvailabilityStatusCard(
    presence: DoctorPresence?,
    heartbeatFresh: Boolean,
    serviceStatus: String
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    "Live status",
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            AvailabilityStatusRow(
                icon =
                    Icons.Outlined.CloudDone,
                title =
                    "Server availability",
                value =
                    if (
                        presence
                            ?.is_available ==
                        true
                    ) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },
                good =
                    presence
                        ?.is_available ==
                            true
            )

            AvailabilityStatusRow(
                icon =
                    Icons.Outlined.Schedule,
                title =
                    "Heartbeat",
                value =
                    heartbeatDescription(
                        presence
                            ?.last_heartbeat
                    ),
                good =
                    heartbeatFresh
            )

            AvailabilityStatusRow(
                icon =
                    Icons.Outlined.PhoneAndroid,
                title =
                    "Phone service",
                value =
                    serviceStatus,
                good =
                    serviceStatus.startsWith(
                        "Available",
                        ignoreCase =
                            true
                    )
            )

            presence
                ?.service_scope
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?.let { scope ->

                    AvailabilityStatusRow(
                        icon =
                            Icons.Outlined.VideoCall,
                        title =
                            "Services",
                        value =
                            scope.joinToString(
                                ", "
                            ) {
                                it.replaceFirstChar {
                                        char ->
                                    char.uppercase()
                                }
                            },
                        good =
                            true
                    )
                }
        }
    }
}

@Composable
private fun AvailabilityStatusRow(
    icon: ImageVector,
    title: String,
    value: String,
    good: Boolean
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        7.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Surface(
            shape =
                CircleShape,
            color =
                if (
                    good
                ) {

                    AvailabilityGreen
                        .copy(
                            alpha =
                                0.1f
                        )

                } else {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                }
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    if (
                        good
                    ) {
                        AvailabilityGreen
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                modifier =
                    Modifier
                        .padding(
                            8.dp
                        )
                        .size(
                            18.dp
                        )
            )
        }

        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,
                fontSize =
                    11.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    value,
                fontSize =
                    12.5.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }

        if (
            good
        ) {

            Icon(
                imageVector =
                    Icons.Filled.CheckCircle,
                contentDescription =
                    null,
                tint =
                    AvailabilityGreen,
                modifier =
                    Modifier.size(
                        18.dp
                    )
            )
        }
    }
}

@Composable
private fun PhoneReadinessCard(
    notificationsEnabled: Boolean,
    fullScreenAllowed: Boolean,
    onNotificationSettings: () -> Unit,
    onFullScreenSettings: () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    "Phone readiness",
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            RequirementRow(
                icon =
                    if (
                        notificationsEnabled
                    ) {
                        Icons.Outlined.Notifications
                    } else {
                        Icons.Outlined.NotificationsOff
                    },
                title =
                    "Consultation notifications",
                description =
                    if (
                        notificationsEnabled
                    ) {
                        "Notifications are enabled."
                    } else {
                        "Allow notifications so incoming consultation requests can reach this phone."
                    },
                ready =
                    notificationsEnabled,
                buttonText =
                    if (
                        notificationsEnabled
                    ) {
                        null
                    } else {
                        "Enable"
                    },
                onClick =
                    onNotificationSettings
            )

            if (
                Build.VERSION.SDK_INT >= 34
            ) {

                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical =
                                10.dp
                        )
                )

                RequirementRow(
                    icon =
                        Icons.Outlined.PhoneAndroid,
                    title =
                        "Incoming screen",
                    description =
                        if (
                            fullScreenAllowed
                        ) {
                            "Full-screen incoming consultation alerts are allowed."
                        } else {
                            "Android may show invitations only as notifications while the phone is locked."
                        },
                    ready =
                        fullScreenAllowed,
                    buttonText =
                        if (
                            fullScreenAllowed
                        ) {
                            null
                        } else {
                            "Settings"
                        },
                    onClick =
                        onFullScreenSettings
                )
            }
        }
    }
}

@Composable
private fun RequirementRow(
    icon: ImageVector,
    title: String,
    description: String,
    ready: Boolean,
    buttonText: String?,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Surface(
            shape =
                CircleShape,
            color =
                if (
                    ready
                ) {

                    AvailabilityGreen
                        .copy(
                            alpha =
                                0.1f
                        )

                } else {

                    AvailabilityAmber
                        .copy(
                            alpha =
                                0.12f
                        )
                }
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    if (
                        ready
                    ) {
                        AvailabilityGreen
                    } else {
                        AvailabilityAmber
                    },
                modifier =
                    Modifier.padding(
                        9.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.width(
                    11.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    12.5.sp
            )

            Text(
                text =
                    description,
                fontSize =
                    10.5.sp,
                lineHeight =
                    14.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        if (
            buttonText != null
        ) {

            TextButton(
                onClick =
                    onClick
            ) {

                Text(
                    buttonText
                )
            }
        }
    }
}

@Composable
private fun HowAvailabilityWorksCard() {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .secondaryContainer
                .copy(
                    alpha =
                        0.5f
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Security,
                    contentDescription =
                        null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .secondary
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    text =
                        "How availability works",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    "Going Available does not permanently mark you online. Your phone sends regular heartbeats. If the app, phone or network stops checking in, the backend stops treating you as eligible after the freshness window.",
                fontSize =
                    11.5.sp,
                lineHeight =
                    16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Lock,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            17.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )

                Text(
                    text =
                        "A broadcast invitation can reach several eligible doctors, but the database permits only one valid doctor to claim it.",
                    fontSize =
                        10.5.sp,
                    lineHeight =
                        15.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        9.dp
                    )
            )

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            17.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )

                Text(
                    text =
                        "On-demand consultation availability is separate from the doctor's scheduled appointment time slots.",
                    fontSize =
                        10.5.sp,
                    lineHeight =
                        15.sp
                )
            }
        }
    }
}

@Composable
private fun AvailabilityMessageCard(
    text: String,
    isError: Boolean
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                14.dp
            ),
        color =
            if (
                isError
            ) {

                MaterialTheme
                    .colorScheme
                    .errorContainer

            } else {

                AvailabilityGreen
                    .copy(
                        alpha =
                            0.1f
                    )
            }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    13.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    if (
                        isError
                    ) {
                        Icons.Outlined.Info
                    } else {
                        Icons.Filled.CheckCircle
                    },
                contentDescription =
                    null,
                tint =
                    if (
                        isError
                    ) {
                        MaterialTheme
                            .colorScheme
                            .error
                    } else {
                        AvailabilityGreen
                    }
            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    text,
                fontSize =
                    11.5.sp,
                color =
                    if (
                        isError
                    ) {
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    }
            )
        }
    }
}

private fun isHeartbeatFresh(
    timestamp: String?
): Boolean {

    if (
        timestamp.isNullOrBlank()
    ) {
        return false
    }

    return try {

        val heartbeat =
            Instant.parse(
                timestamp
            )

        val age =
            Duration.between(
                heartbeat,
                Instant.now()
            )

        !age.isNegative &&
                age.seconds <= 120

    } catch (
        _: Exception
    ) {

        false
    }
}

private fun heartbeatDescription(
    timestamp: String?
): String {

    if (
        timestamp.isNullOrBlank()
    ) {
        return "No heartbeat yet"
    }

    return try {

        val heartbeat =
            Instant.parse(
                timestamp
            )

        val seconds =
            Duration.between(
                heartbeat,
                Instant.now()
            )
                .seconds
                .coerceAtLeast(
                    0
                )

        when {

            seconds < 10 ->
                "Just now"

            seconds < 60 ->
                "$seconds seconds ago"

            seconds < 120 ->
                "${seconds / 60} minute ago"

            else ->
                "${seconds / 60} minutes ago"
        }

    } catch (
        _: Exception
    ) {

        "Unknown"
    }
}

private fun canUseFullScreenIntent(
    context: android.content.Context
): Boolean {

    return if (
        Build.VERSION.SDK_INT >= 34
    ) {

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        manager
            .canUseFullScreenIntent()

    } else {

        true
    }
}