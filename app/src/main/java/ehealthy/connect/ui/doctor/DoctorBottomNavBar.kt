package ehealthy.connect.ui.doctor

import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    APPOINTMENTS("Appointments", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    PATIENTS("Patients", Icons.Filled.People, Icons.Outlined.People),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

/** Same styling as PatientBottomNavBar — kept as a separate composable
 *  since the doctor side has its own tab set, not because the look differs. */
@Composable
fun DoctorBottomNavBar(
    selectedTab: DoctorTab,
    onTabSelected: (DoctorTab) -> Unit
) {
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF94A3B8)

    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.shadow(elevation = 6.dp).height(72.dp)
    ) {
        DoctorTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    unselectedIconColor = muted,
                    unselectedTextColor = muted,
                    indicatorColor = accent.copy(alpha = 0.12f)
                )
            )
        }
    }
}