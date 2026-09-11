package ehealthy.connect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

private val titleOptions = listOf("Mr", "Mrs", "Ms", "Dr", "Prof")
private val languageOptions = listOf(
    "English",
    "Afrikaans",
    "isiZulu",
    "isiXhosa",
    "Sepedi",
    "Setswana",
    "Sesotho",
    "Xitsonga",
    "siSwati",
    "Tshivenda",
    "isiNdebele"
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfile(
    onContinue: (title: String, idNumber: String, dateOfBirth: String, gender: String, language: String) -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)

    var title by remember { mutableStateOf(titleOptions[0]) }
    var titleExpanded by remember { mutableStateOf(false) }

    var idNumber by remember { mutableStateOf("") }
    val parsedId =
        remember(idNumber) { if (idNumber.length == 13) parseSaIdNumber(idNumber) else null }
    val idError = idNumber.length == 13 && parsedId == null

    var language by remember { mutableStateOf(languageOptions[0]) }
    var languageExpanded by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 28.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Complete your profile",
                color = darkText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A few required details before you can book with a doctor.",
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ------------------------------------------------
            // TITLE DROPDOWN
            // ------------------------------------------------

            ExposedDropdownMenuBox(
                expanded = titleExpanded,
                onExpandedChange = { titleExpanded = it }
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Title") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = titleExpanded) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = titleExpanded,
                    onDismissRequest = { titleExpanded = false }
                ) {
                    titleOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { title = option; titleExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ------------------------------------------------
            // ID NUMBER
            // ------------------------------------------------

            OutlinedTextField(
                value = idNumber,
                onValueChange = { new ->
                    if (new.length <= 13 && new.all { it.isDigit() }) idNumber = new
                },
                label = { Text("SA ID Number") },
                singleLine = true,
                isError = idError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                idError -> Text(
                    text = "That ID number doesn't look valid — please double-check it.",
                    color = Color(0xFFD64545),
                    fontSize = 13.sp
                )

                parsedId != null -> Text(
                    text = "Date of birth: ${
                        parsedId.dateOfBirth.format(
                            DateTimeFormatter.ofPattern(
                                "d MMMM yyyy"
                            )
                        )
                    }  •  Gender: ${parsedId.gender}",
                    color = green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                else -> Text(
                    text = "We'll automatically detect your date of birth and gender from this.",
                    color = greyText,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ------------------------------------------------
            // LANGUAGE DROPDOWN
            // ------------------------------------------------

            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it }
            ) {
                OutlinedTextField(
                    value = language,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false }
                ) {
                    languageOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { language = option; languageExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    parsedId?.let {
                        onContinue(
                            title,
                            idNumber,
                            it.dateOfBirth.toString(),   // "yyyy-MM-dd" — matches Postgres `date` type
                            it.gender,
                            language
                        )
                    }
                },
                enabled = parsedId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                Text(
                    "Continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
