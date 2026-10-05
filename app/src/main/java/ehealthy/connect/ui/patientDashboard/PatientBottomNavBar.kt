package ehealthy.connect.ui.patientDashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


enum class PatientTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val accentColor: Color
) {

    HOME(
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        accentColor = PatientColors.Primary
    ),

    FIND_DOCTORS(
        label = "Doctors",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        accentColor = PatientColors.DoctorAccent
    ),

    APPOINTMENTS(
        label = "Visits",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        accentColor = PatientColors.AppointmentAccent
    ),

    PROFILE(
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        accentColor = PatientColors.RecordsAccent
    )
}


@Composable
fun PatientBottomNavBar(
    selectedTab: PatientTab,
    onTabSelected: (PatientTab) -> Unit
) {

    NavigationBar(
        containerColor =
            MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {

        PatientTab.entries.forEach { tab ->

            val selected =
                selectedTab == tab


            val iconScale by
            animateFloatAsState(
                targetValue =
                    if (selected) {
                        1.12f
                    } else {
                        1f
                    },
                animationSpec =
                    spring(
                        dampingRatio =
                            Spring.DampingRatioMediumBouncy,
                        stiffness =
                            Spring.StiffnessMedium
                    ),
                label = "BottomNavIconScale"
            )


            val iconContainerWidth by
            animateDpAsState(
                targetValue =
                    if (selected) {
                        44.dp
                    } else {
                        38.dp
                    },
                animationSpec =
                    spring(
                        dampingRatio =
                            Spring.DampingRatioNoBouncy,
                        stiffness =
                            Spring.StiffnessMedium
                    ),
                label = "BottomNavContainerWidth"
            )


            val iconContainerColor by
            animateColorAsState(
                targetValue =
                    if (selected) {
                        tab.accentColor.copy(
                            alpha = 0.13f
                        )
                    } else {
                        Color.Transparent
                    },
                label =
                    "BottomNavContainerColor"
            )


            val iconColor by
            animateColorAsState(
                targetValue =
                    if (selected) {
                        tab.accentColor
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                            .copy(
                                alpha = 0.70f
                            )
                    },
                label =
                    "BottomNavIconColor"
            )


            NavigationBarItem(
                selected = selected,

                onClick = {
                    if (!selected) {
                        onTabSelected(tab)
                    }
                },

                icon = {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    width =
                                        iconContainerWidth,
                                    height =
                                        34.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        13.dp
                                    )
                                )
                                .background(
                                    iconContainerColor
                                ),
                        contentAlignment =
                            androidx.compose.ui.Alignment.Center
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
                            tint =
                                iconColor,
                            modifier =
                                Modifier
                                    .size(
                                        22.dp
                                    )
                                    .scale(
                                        iconScale
                                    )
                        )
                    }
                },

                label = {

                    Text(
                        text =
                            tab.label,
                        fontSize =
                            10.5.sp,
                        fontWeight =
                            if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                    )
                },

                alwaysShowLabel =
                    true,

                colors =
                    NavigationBarItemDefaults
                        .colors(
                            selectedIconColor =
                                tab.accentColor,

                            selectedTextColor =
                                tab.accentColor,

                            unselectedIconColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            unselectedTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            /*
                             * We created our own
                             * animated indicator.
                             */
                            indicatorColor =
                                Color.Transparent
                        )
            )
        }
    }
}