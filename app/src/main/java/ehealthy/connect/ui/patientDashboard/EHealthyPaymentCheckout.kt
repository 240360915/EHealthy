
package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

private val PayNavy = Color(0xFF102445)
private val PayBlue = Color(0xFF2563EB)
private val PayGreen = Color(0xFF059669)
private val PayMuted = Color(0xFF64748B)

@Composable
fun EHealthyPaymentCheckout(
    amountLabel: String,
    description: String,
    doctorName: String?,
    isProcessing: Boolean,
    onCancel: () -> Unit,
    onResult: (String) -> Unit
) {
    var holder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val digits = cardNumber.filter(Char::isDigit)

    fun validateAndPay() {
        val exp = expiry.filter(Char::isDigit)
        val month = exp.take(2).toIntOrNull()
        val year = exp.drop(2).toIntOrNull()

        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR) % 100
        val currentMonth = today.get(Calendar.MONTH) + 1

        val validExpiry =
            exp.length == 4 &&
                    month != null &&
                    year != null &&
                    month in 1..12 &&
                    (
                            year > currentYear ||
                                    (year == currentYear && month >= currentMonth)
                            )


        error = when {

            holder.trim().length < 2 ->
                "Please enter the cardholder name."

            digits.length != 16 ->
                "Card number must contain 16 digits."

            !validExpiry ->
                "Please enter a valid expiry date."

            cvv.length != 3 ->
                "Security code must contain 3 digits."

            else -> null
        }


        if (error == null) {

            // Demo only: simulate a successful payment.
            // No card details are transmitted or saved.
            onResult("success")
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onCancel()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 12.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 690.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = PayBlue
                        )

                        Spacer(Modifier.width(9.dp))

                        Text(
                            "EHEALTHY PAY",
                            color = PayNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(
                        enabled = !isProcessing,
                        onClick = onCancel
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close payment",
                            tint = PayMuted
                        )
                    }
                }

                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "SANDBOX • DEMONSTRATION PAYMENT",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = PayBlue
                    )
                }

                Text(
                    "Complete your payment",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PayNavy
                )

                Text(
                    "Review your consultation and enter your test card details.",
                    color = PayMuted,
                    fontSize = 12.sp
                )

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "PAYMENT SUMMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PayMuted
                        )

                        Text(
                            description,
                            fontWeight = FontWeight.SemiBold,
                            color = PayNavy
                        )

                        doctorName?.takeIf {
                            it.isNotBlank()
                        }?.let {
                            Text(
                                it,
                                fontSize = 12.sp,
                                color = PayMuted
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
                                fontWeight = FontWeight.Medium,
                                color = PayNavy
                            )

                            Text(
                                amountLabel,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = PayNavy
                            )
                        }
                    }
                }

                // Premium-style demo card preview.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF102445),
                                    Color(0xFF254D91),
                                    Color(0xFF3278CA)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "EHEALTHY",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )

                            Text(
                                "TEST CARD",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            if (digits.length >= 4) {
                                "••••  ••••  ••••  ${
                                    digits.takeLast(4)
                                }"
                            } else {
                                "••••  ••••  ••••  ••••"
                            },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                holder.ifBlank { "CARDHOLDER" }
                                    .uppercase()
                                    .take(23),
                                fontSize = 11.sp,
                                color = Color.White
                            )

                            Text(
                                text = if (expiry.isBlank()) {
                                    "MM/YY"
                                } else if (expiry.length > 2) {
                                    "${expiry.take(2)}/${expiry.drop(2)}"
                                } else {
                                    expiry
                                },
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Text(
                    "Card details",
                    color = PayNavy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = holder,
                    onValueChange = {
                        holder = it.take(40)
                        error = null
                    },
                    label = { Text("Name on card") },
                    placeholder = { Text("TEST PATIENT") },
                    singleLine = true,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )


                OutlinedTextField(
                    value = cardNumber,

                    onValueChange = { input ->
                        cardNumber = input
                            .filter { it.isDigit() }
                            .take(16)

                        error = null
                    },

                    label = {
                        Text("Card number")
                    },

                    placeholder = {
                        Text("0000 0000 0000 0000")
                    },

                    visualTransformation = CardNumberFormatting,

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),

                    singleLine = true,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedTextField(
                        value = expiry,

                        onValueChange = { input ->
                            expiry = input
                                .filter { it.isDigit() }
                                .take(4)

                            error = null
                        },

                        label = {
                            Text("Expiry")
                        },

                        placeholder = {
                            Text("MM/YY")
                        },

                        visualTransformation = CardExpiryFormatting,

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),

                        singleLine = true,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )


                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            cvv = it.filter(Char::isDigit)
                                .take(3)
                            error = null
                        },
                        label = { Text("Security code") },
                        placeholder = { Text("CVV") },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.NumberPassword
                        ),
                        singleLine = true,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }


                Button(
                    onClick = ::validateAndPay,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PayBlue
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Processing payment...")
                    } else {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Pay $amountLabel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PayGreen
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        "Test mode • No real charge",
                        color = PayMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


private object CardNumberFormatting : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {

        val digits = text.text
        val formatted = digits.chunked(4).joinToString(" ")

        val mapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                val position = offset.coerceIn(0, digits.length)

                val spaces = if (position == 0) {
                    0
                } else {
                    (position - 1) / 4
                }

                return (position + spaces)
                    .coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val position = offset.coerceIn(0, formatted.length)

                return (position - position / 5)
                    .coerceIn(0, digits.length)
            }
        }

        return TransformedText(
            AnnotatedString(formatted),
            mapping
        )
    }
}

private object CardExpiryFormatting : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {

        val digits = text.text

        val formatted = if (digits.length > 2) {
            digits.take(2) + "/" + digits.drop(2)
        } else {
            digits
        }

        val mapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                val position = offset.coerceIn(0, digits.length)
                return (
                        position + if (position > 2) 1 else 0
                        ).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val position = offset.coerceIn(0, formatted.length)
                return (
                        position - if (position > 2) 1 else 0
                        ).coerceIn(0, digits.length)
            }
        }

        return TransformedText(
            AnnotatedString(formatted),
            mapping
        )
    }
}

