package ehealthy.connect.ui.doctorDashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DoctorTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        "Home",
        Icons.Filled.Home,
        Icons.Outlined.Home
    ),

    APPOINTMENTS(
        "Appointments",
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth
    ),

    PATIENTS(
        "Patients",
        Icons.Filled.People,
        Icons.Outlined.People
    ),

    PROFILE(
        "Profile",
        Icons.Filled.Person,
        Icons.Outlined.Person
    )
}

// Premium doctor navigation palette.
// Kept local to this file so it cannot clash with the dashboard colors.
private val DoctorNavBackground = Color(0xFFFFFFFF)
private val DoctorNavTeal = Color(0xFF119E95)
private val DoctorNavTealSoft = Color(0xFFE8F7F5)
private val DoctorNavMuted = Color(0xFF818895)
private val DoctorNavInk = Color(0xFF171B27)

/**
 * Premium doctor bottom navigation.
 *
 * Functionality remains exactly the same as the original:
 * HOME / APPOINTMENTS / PATIENTS / PROFILE.
 *
 * The styling is intentionally stronger than the patient navigation:
 * - larger active icon container
 * - stronger selected state
 * - compact typography
 * - softer inactive state
 * - zero heavy shadow
 */
@Composable
fun DoctorBottomNavBar(
    selectedTab: DoctorTab,
    onTabSelected: (DoctorTab) -> Unit
) {
    NavigationBar(
        containerColor = DoctorNavBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.height(76.dp)
    ) {
        DoctorTab.entries.forEach { tab ->

            val selected =
                tab == selectedTab

            val iconColor by animateColorAsState(
                targetValue =
                    if (selected) {
                        DoctorNavTeal
                    } else {
                        DoctorNavMuted
                    },
                label = "doctorNavIconColor"
            )

            val textColor by animateColorAsState(
                targetValue =
                    if (selected) {
                        DoctorNavInk
                    } else {
                        DoctorNavMuted
                    },
                label = "doctorNavTextColor"
            )

            val iconSize by animateDpAsState(
                targetValue =
                    if (selected) {
                        23.dp
                    } else {
                        21.dp
                    },
                label = "doctorNavIconSize"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    onTabSelected(tab)
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                            .background(
                                if (selected) {
                                    DoctorNavTealSoft
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(
                                horizontal =
                                    if (selected) 13.dp else 8.dp,
                                vertical =
                                    if (selected) 8.dp else 7.dp
                            )
                    ) {
                        Icon(
                            imageVector =
                                if (selected) {
                                    tab.selectedIcon
                                } else {
                                    tab.unselectedIcon
                                },
                            contentDescription =
                                tab.label,
                            tint = iconColor,
                            modifier =
                                Modifier.size(iconSize)
                        )
                    }
                },
                label = {
                    Row {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        DoctorNavTeal
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(5.dp)
                            )
                        }

                        Text(
                            text = tab.label,
                            color = textColor,
                            fontSize = 9.5.sp,
                            fontWeight =
                                if (selected) {
                                    FontWeight.ExtraBold
                                } else {
                                    FontWeight.Medium
                                },
                            maxLines = 1
                        )
                    }
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor =
                            DoctorNavTeal,
                        selectedTextColor =
                            DoctorNavInk,
                        unselectedIconColor =
                            DoctorNavMuted,
                        unselectedTextColor =
                            DoctorNavMuted,
                        indicatorColor =
                            Color.Transparent
                    )
            )
        }
    }
}
