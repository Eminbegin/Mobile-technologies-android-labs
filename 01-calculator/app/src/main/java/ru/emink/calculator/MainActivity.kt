package ru.emink.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CalculatorColorScheme = lightColorScheme(
    primary = Color(0xFF7A1838),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7BEC9),
    onPrimaryContainer = Color(0xFF3A0716),
    secondary = Color(0xFF9D3B5C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0CBD6),
    onSecondaryContainer = Color(0xFF3D0B1B),
    tertiary = Color(0xFF65122D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD991A7),
    onTertiaryContainer = Color(0xFF31030F),
    background = Color(0xFFFFF7F9),
    onBackground = Color(0xFF2A0B14),
    surface = Color(0xFFFFF7F9),
    onSurface = Color(0xFF2A0B14),
    surfaceVariant = Color(0xFFEADCE1),
    onSurfaceVariant = Color(0xFF4C2834),
    outline = Color(0xFF80606A),
)

private val CalculatorStateSaver = listSaver<CalculatorState, String>(
    save = { state ->
        listOf(
            state.display,
            state.accumulator?.toString().orEmpty(),
            state.pendingOperation?.name.orEmpty(),
            state.startNewEntry.toString(),
        )
    },
    restore = { saved ->
        CalculatorState(
            display = saved[0],
            accumulator = saved[1].toDoubleOrNull(),
            pendingOperation = saved[2].takeIf(String::isNotEmpty)?.let(Operation::valueOf),
            startNewEntry = saved[3].toBoolean(),
        )
    },
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                CalculatorApp()
            }
        }
    }
}

@Composable
private fun CalculatorApp() {
    var state by rememberSaveable(stateSaver = CalculatorStateSaver) {
        mutableStateOf(CalculatorState())
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DisplayPanel(
                    state = state,
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                )
                Keypad(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    onDigit = { state = state.inputDigit(it) },
                    onDecimal = { state = state.inputDecimal() },
                    onOperation = { state = state.selectOperation(it) },
                    onEquals = { state = state.calculateResult() },
                    onClear = { state = state.clear() },
                    onClearEntry = { state = state.clearEntry() },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DisplayPanel(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f),
                )
                Keypad(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.1f),
                    onDigit = { state = state.inputDigit(it) },
                    onDecimal = { state = state.inputDecimal() },
                    onOperation = { state = state.selectOperation(it) },
                    onEquals = { state = state.calculateResult() },
                    onClear = { state = state.clear() },
                    onClearEntry = { state = state.clearEntry() },
                )
            }
        }
    }
}

@Composable
private fun DisplayPanel(state: CalculatorState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val displayDescription = stringResource(R.string.display_description)
    val operationLabel = when (state.pendingOperation) {
        Operation.ADD -> stringResource(R.string.button_add)
        Operation.SUBTRACT -> stringResource(R.string.button_subtract)
        Operation.MULTIPLY -> stringResource(R.string.button_multiply)
        Operation.DIVIDE -> stringResource(R.string.button_divide)
        null -> null
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = operationLabel?.let {
                    stringResource(R.string.pending_operation, it)
                }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.display,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result")
                    .semantics { contentDescription = displayDescription },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            TextButton(
                onClick = { copyResult(context, state.display) },
            ) {
                Text(stringResource(R.string.copy_result))
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onOperation: (Operation) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onClearEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyRow {
            CalculatorButton(R.string.button_clear, onClear, ButtonKind.ACTION)
            CalculatorButton(R.string.button_clear_entry, onClearEntry, ButtonKind.ACTION)
            CalculatorButton(R.string.button_divide, { onOperation(Operation.DIVIDE) }, ButtonKind.OPERATION)
            CalculatorButton(R.string.button_multiply, { onOperation(Operation.MULTIPLY) }, ButtonKind.OPERATION)
        }
        KeyRow {
            CalculatorButton(R.string.digit_7, { onDigit(7) })
            CalculatorButton(R.string.digit_8, { onDigit(8) })
            CalculatorButton(R.string.digit_9, { onDigit(9) })
            CalculatorButton(R.string.button_subtract, { onOperation(Operation.SUBTRACT) }, ButtonKind.OPERATION)
        }
        KeyRow {
            CalculatorButton(R.string.digit_4, { onDigit(4) })
            CalculatorButton(R.string.digit_5, { onDigit(5) })
            CalculatorButton(R.string.digit_6, { onDigit(6) })
            CalculatorButton(R.string.button_add, { onOperation(Operation.ADD) }, ButtonKind.OPERATION)
        }
        KeyRow {
            CalculatorButton(R.string.digit_1, { onDigit(1) })
            CalculatorButton(R.string.digit_2, { onDigit(2) })
            CalculatorButton(R.string.digit_3, { onDigit(3) })
            CalculatorButton(R.string.button_equals, onEquals, ButtonKind.EQUALS)
        }
        KeyRow {
            CalculatorButton(R.string.digit_0, { onDigit(0) }, weight = 3f)
            CalculatorButton(R.string.button_decimal, onDecimal)
        }
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private enum class ButtonKind {
    NUMBER,
    OPERATION,
    ACTION,
    EQUALS,
}

@Composable
private fun RowScope.CalculatorButton(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    kind: ButtonKind = ButtonKind.NUMBER,
    weight: Float = 1f,
) {
    val colors = when (kind) {
        ButtonKind.NUMBER -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        ButtonKind.OPERATION -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ButtonKind.ACTION -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        ButtonKind.EQUALS -> ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxHeight()
            .weight(weight)
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = colors,
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun copyResult(context: Context, result: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val label = context.getString(R.string.copy_label)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, result))
    Toast.makeText(context, R.string.result_copied, Toast.LENGTH_SHORT).show()
}

@Composable
private fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalculatorColorScheme,
        content = content,
    )
}
