package ehealthy.connect.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.CancellationException


private val consultationBackground =
    Color(0xFFF4F7FA)

private val consultationText =
    Color(0xFF0F1F3D)

private val consultationMuted =
    Color(0xFF64748B)

private val consultationTeal =
    Color(0xFF0D9488)

private val consultationRed =
    Color(0xFFDC2626)


/*
 * Public Stream API key.
 *
 * NEVER put STREAM_API_SECRET here.
 */
private const val CONSULTATION_STREAM_API_KEY =
    "67jj8rzevh48"


private sealed class ConsultationCallState {

    data object Loading :
        ConsultationCallState()


    data class Error(
        val message: String
    ) :
        ConsultationCallState()


    data object Waiting :
        ConsultationCallState()


    data object InCall :
        ConsultationCallState()
}


@Composable
fun ConsultationCallScreen(
    requestId: String,
    displayName: String,
    isDoctorView: Boolean,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current


    var screenState by remember {

        mutableStateOf<
                ConsultationCallState
                >(
            ConsultationCallState.Loading
        )
    }


    var call by remember {

        mutableStateOf<Call?>(
            null
        )
    }


    var retryKey by remember {

        mutableIntStateOf(
            0
        )
    }


    /*
     * ---------------------------------------------
     * CONNECT TO SECURE CONSULTATION
     * ---------------------------------------------
     */
    LaunchedEffect(
        requestId,
        retryKey
    ) {

        screenState =
            ConsultationCallState.Loading


        try {

            val result =
                ConsultationStreamRepository
                    .getCredentials(
                        requestId
                    )


            val credentials =
                result.getOrElse {
                        error ->

                    screenState =
                        ConsultationCallState.Error(
                            consultationCallMessage(
                                error
                            )
                        )

                    return@LaunchedEffect
                }


            /*
             * Remove any old Stream client.
             */
            try {

                StreamVideo
                    .removeClient()

            } catch (
                _: Exception
            ) {

                // No previous client.
            }


            /*
             * Stream user identity comes from
             * the secure Edge Function.
             */
            val user =
                User(
                    id =
                        credentials.userId,

                    name =
                        displayName,

                    role =
                        "user"
                )


            val client =
                StreamVideoBuilder(

                    context =
                        context
                            .applicationContext,

                    apiKey =
                        CONSULTATION_STREAM_API_KEY,

                    geo =
                        GEO.GlobalEdgeNetwork,

                    user =
                        user,

                    token =
                        credentials.token
                )
                    .build()


            /*
             * IMPORTANT:
             *
             * We use the server-returned:
             *
             * callType
             * callId
             *
             * NOT requestId directly.
             */
            val streamCall =
                client.call(

                    type =
                        credentials.callType,

                    id =
                        credentials.callId
                )


            /*
             * IMPORTANT:
             *
             * create = false
             *
             * The Stream room must already have
             * been created by our secure server.
             */
            val joinResult =
                streamCall.join(
                    create =
                        false
                )


            if (
                joinResult.isFailure
            ) {

                /*
                 * Stream's error type is NOT Throwable.
                 *
                 * So we convert it to text instead of
                 * passing it to Log.e as a Throwable.
                 */
                val cause =
                    joinResult
                        .errorOrNull()


                android.util.Log.e(
                    "ConsultationCall",
                    "Stream consultation join failed: $cause"
                )


                screenState =
                    ConsultationCallState.Error(

                        cause
                            ?.message
                            ?: "Could not join the consultation."
                    )


                return@LaunchedEffect
            }


            call =
                streamCall


            screenState =
                ConsultationCallState.Waiting


        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            e: Exception
        ) {

            screenState =
                ConsultationCallState.Error(
                    consultationCallMessage(
                        e
                    )
                )
        }
    }


    /*
     * ---------------------------------------------
     * WAIT FOR OTHER PERSON
     * ---------------------------------------------
     */
    val currentCall =
        call


    if (
        currentCall != null &&
        screenState is
                ConsultationCallState.Waiting
    ) {

        val remoteParticipants by
        currentCall
            .state
            .remoteParticipants
            .collectAsState()


        LaunchedEffect(
            remoteParticipants.size
        ) {

            if (
                remoteParticipants
                    .isNotEmpty()
            ) {

                screenState =
                    ConsultationCallState.InCall
            }
        }
    }


    /*
     * ---------------------------------------------
     * CLEANUP
     * ---------------------------------------------
     */
    DisposableEffect(
        Unit
    ) {

        onDispose {

            try {

                call
                    ?.leave()

            } catch (
                _: Exception
            ) {
            }


            try {

                StreamVideo
                    .removeClient()

            } catch (
                _: Exception
            ) {
            }
        }
    }


    VideoTheme {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        consultationBackground
                    )
        ) {

            when (
                val state =
                    screenState
            ) {

                ConsultationCallState.Loading -> {

                    ConsultationLoading()
                }


                is ConsultationCallState.Error -> {

                    ConsultationCallError(
                        message =
                            state.message,

                        onRetry = {
                            retryKey++
                        },

                        onBack =
                            onBack
                    )
                }


                ConsultationCallState.Waiting -> {

                    ConsultationWaiting(
                        isDoctorView =
                            isDoctorView,
                        onBack =
                            onBack
                    )
                }


                ConsultationCallState.InCall -> {

                    currentCall
                        ?.let {
                                activeCall ->

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                            ) {

                                ConsultationRecordingBanner()


                                CallContent(

                                    modifier =
                                        Modifier
                                            .fillMaxSize(),

                                    call =
                                        activeCall,

                                    onBackPressed = {

                                        try {

                                            activeCall
                                                .leave()

                                        } catch (
                                            _: Exception
                                        ) {
                                        }

                                        onBack()
                                    },

                                    onCallAction = {
                                            action ->

                                        if (
                                            action is
                                                    io.getstream.video.android.core.call.state.LeaveCall
                                        ) {

                                            try {

                                                activeCall
                                                    .leave()

                                            } catch (
                                                _: Exception
                                            ) {
                                            }

                                            onBack()
                                        }
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
private fun ConsultationLoading() {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                color =
                    consultationTeal
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    "Preparing secure consultation…",

                color =
                    consultationMuted,

                fontSize =
                    14.sp
            )
        }
    }
}


@Composable
private fun ConsultationWaiting(
    isDoctorView: Boolean,
    onBack: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    28.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.VideoCall,

                contentDescription =
                    null,

                tint =
                    consultationTeal,

                modifier =
                    Modifier.height(
                        52.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Text(
                text =
                    if (
                        isDoctorView
                    ) {

                        "Waiting for the patient"

                    } else {

                        "Doctor accepted your request"
                    },

                color =
                    consultationText,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    if (
                        isDoctorView
                    ) {

                        "Your secure consultation room is ready. " +
                                "The call will start automatically " +
                                "when the patient joins."

                    } else {

                        "Your secure consultation room is ready. " +
                                "The call will start automatically " +
                                "when the doctor joins."
                    },

                color =
                    consultationMuted,

                fontSize =
                    13.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            CircularProgressIndicator(
                color =
                    consultationTeal
            )


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )


            OutlinedButton(
                onClick = {

                    onBack()
                }
            ) {

                Text(
                    "Leave waiting room"
                )
            }
        }
    }
}


@Composable
private fun ConsultationCallError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    28.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Info,

                contentDescription =
                    null,

                tint =
                    consultationRed,

                modifier =
                    Modifier.height(
                        44.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    "Could not join consultation",

                color =
                    consultationText,

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    message,

                color =
                    consultationMuted,

                fontSize =
                    13.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Button(
                onClick =
                    onRetry
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Refresh,

                    contentDescription =
                        null
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )


                Text(
                    "Try again"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            OutlinedButton(
                onClick =
                    onBack
            ) {

                Text(
                    "Go back"
                )
            }
        }
    }
}


@Composable
private fun ConsultationRecordingBanner() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color(
                        0xFF7C2D12
                    )
                )
                .statusBarsPadding()
                .padding(
                    horizontal =
                        16.dp,

                    vertical =
                        8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Outlined.Info,

            contentDescription =
                null,

            tint =
                Color.White,

            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )


        Text(
            text =
                "Recording this consultation is not allowed " +
                        "unless both parties verbally agree.",

            color =
                Color.White,

            fontSize =
                11.5.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}


private fun consultationCallMessage(
    error: Throwable
): String {

    val message =
        error.message
            .orEmpty()


    return when {

        message.contains(
            "403"
        ) ->

            "You are not authorized to join this consultation."


        message.contains(
            "not a participant",
            ignoreCase =
                true
        ) ->

            "You are not authorized to join this consultation."


        message.contains(
            "409"
        ) ->

            "The video room is still being prepared. Try again shortly."


        message.contains(
            "still being prepared",
            ignoreCase =
                true
        ) ->

            "The video room is still being prepared. Try again."


        message.contains(
            "401"
        ) ->

            "Your session expired. Please sign in again."


        else ->

            message.ifBlank {

                "The secure video consultation could not be started."
            }
    }
}