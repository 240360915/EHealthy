package ehealthy.connect.ui.patientDashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.PatientNotification
import ehealthy.connect.data.patient.PatientRepository
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientNotificationsScreen(
    onBack: () -> Unit,
    onOpenInvoice: (invoiceId: String) -> Unit = {}
) {

    val scope =
        rememberCoroutineScope()

    var notifications by remember {
        mutableStateOf<List<PatientNotification>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }


    suspend fun loadNotifications() {

        isLoading = true
        errorMessage = null

        PatientRepository
            .getMyNotifications()
            .onSuccess {

                notifications = it

            }
            .onFailure {

                errorMessage =
                    it.message
                        ?: "Could not load notifications."
            }

        isLoading = false
    }


    LaunchedEffect(refreshKey) {

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
                                Icons.Outlined.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(
                            text =
                                "Notifications",

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        if (unreadCount > 0) {

                            Text(
                                text =
                                    "$unreadCount unread",

                                fontSize =
                                    10.5.sp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }
                },

                actions = {

                    if (unreadCount > 0) {

                        TextButton(

                            onClick = {

                                scope.launch {

                                    PatientRepository
                                        .markAllNotificationsAsRead()
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
                                text =
                                    "Mark all read",

                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }


                    IconButton(

                        onClick = {
                            refreshKey++
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Refresh,

                            contentDescription =
                                "Refresh"
                        )
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

    ) { paddingValues ->


        when {

            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            ),

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
                            .padding(
                                paddingValues
                            )
                            .padding(
                                24.dp
                            ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.NotificationsActive,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                46.dp
                            ),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .error
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    Text(
                        text =
                            "Could not load notifications",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            17.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            errorMessage
                                ?: "Something went wrong.",

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    Button(

                        onClick = {
                            refreshKey++
                        }
                    ) {

                        Text(
                            text =
                                "Try again"
                        )
                    }
                }
            }


            notifications.isEmpty() -> {

                EmptyNotificationsState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                )
            }


            else -> {

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            ),

                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    items(
                        items =
                            notifications,

                        key = {
                            it.id
                        }
                    ) { notification ->


                        PatientNotificationCard(

                            notification =
                                notification,

                            onClick = {

                                if (!notification.is_read) {

                                    scope.launch {

                                        PatientRepository
                                            .markNotificationAsRead(
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
                                                                is_read =
                                                                    true
                                                            )

                                                        } else {

                                                            it
                                                        }
                                                    }
                                            }
                                    }
                                }


                                if (
                                    notification.related_entity_type
                                        .equals(
                                            "physical_invoice",
                                            ignoreCase = true
                                        ) &&
                                    !notification.related_entity_id
                                        .isNullOrBlank()
                                ) {

                                    onOpenInvoice(
                                        notification.related_entity_id
                                    )
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
private fun PatientNotificationCard(
    notification: PatientNotification,
    onClick: () -> Unit
) {

    val unread =
        !notification.is_read


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =

                    if (unread) {

                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha = 0.35f
                            )

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment.Top
        ) {


            Box(
                modifier =
                    Modifier
                        .size(
                            46.dp
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(
                                    alpha = 0.10f
                                ),

                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.NotificationsActive,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(
                            22.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
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
                            Modifier.weight(
                                1f
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,

                        fontSize =
                            14.sp,

                        fontWeight =

                            if (unread) {

                                FontWeight.ExtraBold

                            } else {

                                FontWeight.Bold
                            }
                    )


                    if (unread) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        8.dp
                                    )
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .primary,

                                        CircleShape
                                    )
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                Text(
                    text =
                        notification.message,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    fontSize =
                        12.sp,

                    lineHeight =
                        17.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Text(
                    text =
                        notification.created_at
                            .take(
                                16
                            )
                            .replace(
                                "T",
                                " "
                            ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                            .copy(
                                alpha = 0.70f
                            ),

                    fontSize =
                        10.sp
                )
            }
        }
    }
}


@Composable
private fun EmptyNotificationsState(
    modifier: Modifier =
        Modifier
) {

    Column(

        modifier =
            modifier.padding(
                28.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {


        Box(
            modifier =
                Modifier
                    .size(
                        90.dp
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(
                                alpha = 0.08f
                            ),

                        CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.NotificationsActive,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(
                        40.dp
                    ),

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        Text(
            text =
                "No notifications yet",

            fontWeight =
                FontWeight.ExtraBold,

            fontSize =
                19.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(
            text =
                "Appointment updates and other important information will appear here.",

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            fontSize =
                12.5.sp
        )
    }
}

