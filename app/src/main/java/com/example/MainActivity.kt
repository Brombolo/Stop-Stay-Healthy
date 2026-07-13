package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.translation.Translation
import com.example.ui.viewmodel.HealthViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val viewModel: HealthViewModel = viewModel()
    
    val habitState by viewModel.habitState.collectAsStateWithLifecycle()
    val fastingSessions by viewModel.fastingSessions.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()
    
    val currentScreen = viewModel.currentScreen
    val lang = Translation.getLanguageCode(habitState.language)

    // Statistics Screen Visibility Constraint:
    // "nascondere la sezione delle statistiche se il modulo del digiuno è disabilitato."
    if (currentScreen == Screen.Statistics && !habitState.fastingEnabled) {
        LaunchedEffect(Unit) {
            viewModel.navigateTo(Screen.Settings)
        }
    }

    // Ticker to refresh timers automatically in the UI every second
    var ticker by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ticker = System.currentTimeMillis()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentScreen == Screen.Onboarding) {
            OnboardingScreen(
                lang = lang,
                onComplete = { f, s, al, su ->
                    viewModel.completeOnboarding(f, s, al, su)
                }
            )
        } else {
            // Main structure with Sidebar (NavigationRail) and Main Content area
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Sidebar Menu (Sempre Visibile as requested)
                NavigationSidebar(
                    currentScreen = currentScreen,
                    habitState = habitState,
                    lang = lang,
                    onNavigate = { viewModel.navigateTo(it) }
                )

                // Divider line
                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Main Content Screen Box with animated transitions
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    when (currentScreen) {
                        Screen.Fasting -> FastingScreen(
                            lang = lang,
                            startTime = viewModel.fastingStartTime,
                            ticker = ticker,
                            onStart = { viewModel.startFasting(it) },
                            onStop = { end, weight -> viewModel.stopFasting(end, weight) },
                            onEditTime = { viewModel.updateFastingStartTime(it) }
                        )
                        Screen.Smoking -> BadHabitScreen(
                            title = Translation.getText("smoking", lang),
                            lang = lang,
                            startTime = viewModel.smokingStartTime,
                            ticker = ticker,
                            icon = Icons.Default.SmokingRooms,
                            isSmoking = true,
                            dailyCost = habitState.smokingDailyCost,
                            onStart = { viewModel.startSmoking(it) },
                            onStop = { viewModel.stopSmoking() },
                            onUpdateSmokingCost = { viewModel.updateSmokingCost(it) }
                        )
                        Screen.Alcohol -> BadHabitScreen(
                            title = Translation.getText("alcohol", lang),
                            lang = lang,
                            startTime = viewModel.alcoholStartTime,
                            ticker = ticker,
                            icon = Icons.Default.LocalBar,
                            isSmoking = false,
                            onStart = { viewModel.startAlcohol(it) },
                            onStop = { viewModel.stopAlcohol() }
                        )
                        Screen.Sugar -> BadHabitScreen(
                            title = Translation.getText("sugar", lang),
                            lang = lang,
                            startTime = viewModel.sugarStartTime,
                            ticker = ticker,
                            icon = Icons.Default.Cake,
                            isSmoking = false,
                            onStart = { viewModel.startSugar(it) },
                            onStop = { viewModel.stopSugar() }
                        )
                        Screen.Statistics -> StatisticsScreen(
                            lang = lang,
                            fastingSessions = fastingSessions,
                            weightEntries = weightEntries,
                            onAddWeight = { w, d -> viewModel.addWeightEntry(w, d) },
                            onEditWeight = { viewModel.editWeightEntry(it) },
                            onDeleteWeight = { viewModel.deleteWeightEntry(it) },
                            onEditFastingSession = { viewModel.updateFastingSession(it) },
                            onDeleteFastingSession = { viewModel.deleteFastingSession(it) }
                        )
                        Screen.Settings -> SettingsScreen(
                            lang = lang,
                            habitState = habitState,
                            onToggleFasting = { viewModel.toggleFastingEnabled(it) },
                            onToggleSmoking = { viewModel.toggleSmokingEnabled(it) },
                            onToggleAlcohol = { viewModel.toggleAlcoholEnabled(it) },
                            onToggleSugar = { viewModel.toggleSugarEnabled(it) },
                            onLanguageSelected = { viewModel.setLanguage(it) },
                            onResetApp = { viewModel.resetApplication() }
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationSidebar(
    currentScreen: Screen,
    habitState: HabitState,
    lang: String,
    onNavigate: (Screen) -> Unit
) {
    NavigationRail(
        modifier = Modifier.width(76.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        header = {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_app_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fasting Icon (if active/enabled)
            if (habitState.fastingEnabled) {
                SidebarItem(
                    icon = Icons.Default.Timer,
                    label = Translation.getText("fasting", lang),
                    selected = currentScreen == Screen.Fasting,
                    onClick = { onNavigate(Screen.Fasting) }
                )
            }

            // Smoking Icon (if active/enabled)
            if (habitState.smokingEnabled) {
                SidebarItem(
                    icon = Icons.Default.SmokingRooms,
                    label = Translation.getText("smoking", lang),
                    selected = currentScreen == Screen.Smoking,
                    onClick = { onNavigate(Screen.Smoking) }
                )
            }

            // Alcohol Icon (if active/enabled)
            if (habitState.alcoholEnabled) {
                SidebarItem(
                    icon = Icons.Default.LocalBar,
                    label = Translation.getText("alcohol", lang),
                    selected = currentScreen == Screen.Alcohol,
                    onClick = { onNavigate(Screen.Alcohol) }
                )
            }

            // Sugar Icon (if active/enabled)
            if (habitState.sugarEnabled) {
                SidebarItem(
                    icon = Icons.Default.Cake,
                    label = Translation.getText("sugar", lang),
                    selected = currentScreen == Screen.Sugar,
                    onClick = { onNavigate(Screen.Sugar) }
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.width(36.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Statistics Icon (only if fasting is active/enabled)
            if (habitState.fastingEnabled) {
                SidebarItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = Translation.getText("statistics", lang),
                    selected = currentScreen == Screen.Statistics,
                    onClick = { onNavigate(Screen.Statistics) }
                )
            }

            // Settings Icon (always active)
            SidebarItem(
                icon = Icons.Default.Settings,
                label = Translation.getText("settings", lang),
                selected = currentScreen == Screen.Settings,
                onClick = { onNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 8.sp,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    lang: String,
    onComplete: (fasting: Boolean, smoking: Boolean, alcohol: Boolean, sugar: Boolean) -> Unit
) {
    var fastingSelected by remember { mutableStateOf(true) }
    var smokingSelected by remember { mutableStateOf(false) }
    var alcoholSelected by remember { mutableStateOf(false) }
    var sugarSelected by remember { mutableStateOf(false) }

    // At least one option must be selected
    val isValid = fastingSelected || smokingSelected || alcoholSelected || sugarSelected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_app_icon),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = Translation.getText("onboarding_title", lang),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Translation.getText("onboarding_subtitle", lang),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Options List
        OnboardingOptionCard(
            title = Translation.getText("fasting", lang),
            description = Translation.getText("onboarding_desc_fasting", lang),
            icon = Icons.Default.Timer,
            selected = fastingSelected,
            onSelectedChange = { fastingSelected = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingOptionCard(
            title = Translation.getText("smoking", lang),
            description = Translation.getText("onboarding_desc_smoking", lang),
            icon = Icons.Default.SmokingRooms,
            selected = smokingSelected,
            onSelectedChange = { smokingSelected = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingOptionCard(
            title = Translation.getText("alcohol", lang),
            description = Translation.getText("onboarding_desc_alcohol", lang),
            icon = Icons.Default.LocalBar,
            selected = alcoholSelected,
            onSelectedChange = { alcoholSelected = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingOptionCard(
            title = Translation.getText("sugar", lang),
            description = Translation.getText("onboarding_desc_sugar", lang),
            icon = Icons.Default.Cake,
            selected = sugarSelected,
            onSelectedChange = { sugarSelected = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (isValid) {
                    onComplete(fastingSelected, smokingSelected, alcoholSelected, sugarSelected)
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = Translation.getText("continue", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = "Continue")
        }
    }
}

@Composable
fun OnboardingOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onSelectedChange(!selected) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onSelectedChange(it) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

// Helpers for DateTime Picking
fun showDateTimePicker(
    context: Context,
    initialTimestamp: Long,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = if (initialTimestamp > 0) initialTimestamp else System.currentTimeMillis()
    }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            
            android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    onDateTimeSelected(calendar.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun FastingScreen(
    lang: String,
    startTime: Long,
    ticker: Long,
    onStart: (Long) -> Unit,
    onStop: (Long, Double?) -> Unit,
    onEditTime: (Long) -> Unit
) {
    val context = LocalContext.current
    var showStopDialog by remember { mutableStateOf(false) }

    val isActive = startTime != -1L
    val isFuture = isActive && startTime > ticker
    val elapsedMs = if (isActive && !isFuture) ticker - startTime else 0L
    val elapsedHours = elapsedMs / 3600000.0
    val msToStart = if (isFuture) startTime - ticker else 0L

    // List of fasting stages as described by user
    val fastingStages = listOf(
        FastingStage(0.0, 2.0, Translation.getText("f_step_1", lang), "f_step_1_desc"),
        FastingStage(2.0, 5.0, Translation.getText("f_step_2", lang), "f_step_2_desc"),
        FastingStage(5.0, 8.0, Translation.getText("f_step_3", lang), "f_step_3_desc"),
        FastingStage(8.0, 10.0, Translation.getText("f_step_4", lang), "f_step_4_desc"),
        FastingStage(10.0, 12.0, Translation.getText("f_step_5", lang), "f_step_5_desc"),
        FastingStage(12.0, 18.0, Translation.getText("f_step_6", lang), "f_step_6_desc"),
        FastingStage(18.0, 24.0, Translation.getText("f_step_7", lang), "f_step_7_desc"),
        FastingStage(24.0, 48.0, Translation.getText("f_step_8", lang), "f_step_8_desc"),
        FastingStage(48.0, 56.0, Translation.getText("f_step_9", lang), "f_step_9_desc"),
        FastingStage(56.0, 72.0, Translation.getText("f_step_10", lang), "f_step_10_desc"),
        FastingStage(72.0, 999.0, Translation.getText("f_step_11", lang), "f_step_11_desc")
    )

    // Current active fasting stage index
    val currentStageIndex = if (isActive && !isFuture) {
        fastingStages.indexOfLast { elapsedHours >= it.startHour }.coerceAtLeast(0)
    } else -1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card of active/inactive state
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isActive) {
                        if (isFuture) Translation.getText("future_start_message", lang)
                        else Translation.getText("fasting_active", lang)
                    } else Translation.getText("fasting_inactive", lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFuture) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFuture) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful Circular Timer Graphic (Canvas)
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sweepAngle = if (isActive && !isFuture) {
                        // Max circle is 24 hours
                        ((elapsedHours % 24.0) / 24.0 * 360f).toFloat()
                    } else 0f

                    val backgroundRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    val activeRingColor = MaterialTheme.colorScheme.secondary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = backgroundRingColor,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = activeRingColor,
                            startAngle = -90f,
                            sweepAngle = if (isActive && !isFuture) sweepAngle else 0f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isActive) {
                            if (isFuture) {
                                val hoursToStart = msToStart / 3600000
                                val minsToStart = (msToStart % 3600000) / 60000
                                val secsToStart = (msToStart % 60000) / 1000
                                Text(
                                    text = String.format("%02d:%02d:%02d", hoursToStart, minsToStart, secsToStart),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translation.getText("future_start_countdown", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            } else {
                                val hours = elapsedMs / 3600000
                                val mins = (elapsedMs % 3600000) / 60000
                                val secs = (elapsedMs % 60000) / 1000
                                Text(
                                    text = String.format("%02d:%02d:%02d", hours, mins, secs),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentStageIndex != -1) fastingStages[currentStageIndex].name else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isActive) {
                        Button(
                            onClick = {
                                showDateTimePicker(context, startTime) { newTime ->
                                    onEditTime(newTime)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translation.getText("edit", lang),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                showStopDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translation.getText("stop_fast", lang),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                showDateTimePicker(context, System.currentTimeMillis()) { start ->
                                    onStart(start)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translation.getText("start_fast", lang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fasting Goals/Timeline Checklist (Vivi come un Goal as requested)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translation.getText("milestones", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                fastingStages.forEachIndexed { idx, stage ->
                    val completed = !isFuture && isActive && elapsedHours >= stage.startHour
                    val isCurrent = !isFuture && isActive && idx == currentStageIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (completed) MaterialTheme.colorScheme.secondary
                                    else if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = (idx + 1).toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stage.name,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Translation.getText(stage.descKey, lang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (idx < fastingStages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 40.dp, top = 2.dp, bottom = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    // Stop Fast Dialog with customizable Date/Time and prominent weight input
    if (showStopDialog) {
        var editedEndTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var weightInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(text = Translation.getText("stop_fast", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Date Time picker container
                    Text(
                        text = Translation.getText("edit_end_time", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDateTimePicker(context, editedEndTime) { newTime ->
                                    editedEndTime = newTime
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = format.format(Date(editedEndTime)), fontSize = 14.sp)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Weight input textfield (Highly Prominent!)
                    Text(
                        text = Translation.getText("weight_optional", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        placeholder = { Text("e.g. 74.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        suffix = { Text("kg") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedWeight = weightInput.toDoubleOrNull()
                        onStop(editedEndTime, parsedWeight)
                        showStopDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Translation.getText("confirm", lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

data class FastingStage(
    val startHour: Double,
    val endHour: Double,
    val name: String,
    val descKey: String
)

@Composable
fun BadHabitScreen(
    title: String,
    lang: String,
    startTime: Long,
    ticker: Long,
    icon: ImageVector,
    isSmoking: Boolean,
    dailyCost: Double = 0.0,
    onStart: (Long) -> Unit,
    onStop: () -> Unit,
    onUpdateSmokingCost: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    var showStopConfirmation by remember { mutableStateOf(false) }

    // Milestones definitions from PDF
    val milestones = if (isSmoking) {
        listOf(
            Milestone(0.0139, Translation.getText("sm_1", lang), Translation.getText("sm_1_desc", lang), "High"),
            Milestone(0.333, Translation.getText("sm_2", lang), Translation.getText("sm_2_desc", lang), "High"),
            Milestone(0.5, Translation.getText("sm_3", lang), Translation.getText("sm_3_desc", lang), "High"),
            Milestone(1.0, Translation.getText("sm_4", lang), Translation.getText("sm_4_desc", lang), "Medium"),
            Milestone(2.0, Translation.getText("sm_5", lang), Translation.getText("sm_5_desc", lang), "Medium"),
            Milestone(3.0, Translation.getText("sm_6", lang), Translation.getText("sm_6_desc", lang), "High"),
            Milestone(7.0, Translation.getText("sm_7", lang), Translation.getText("sm_7_desc", lang), "High"),
            Milestone(14.0, Translation.getText("sm_8", lang), Translation.getText("sm_8_desc", lang), "High"),
            Milestone(30.0, Translation.getText("sm_9", lang), Translation.getText("sm_9_desc", lang), "Medium"),
            Milestone(90.0, Translation.getText("sm_10", lang), Translation.getText("sm_10_desc", lang), "High"),
            Milestone(180.0, Translation.getText("sm_11", lang), Translation.getText("sm_11_desc", lang), "Medium"),
            Milestone(270.0, Translation.getText("sm_12", lang), Translation.getText("sm_12_desc", lang), "Medium"),
            Milestone(365.0, Translation.getText("sm_13", lang), Translation.getText("sm_13_desc", lang), "High"),
            Milestone(1825.0, Translation.getText("sm_14", lang), Translation.getText("sm_14_desc", lang), "High"),
            Milestone(3650.0, Translation.getText("sm_15", lang), Translation.getText("sm_15_desc", lang), "High"),
            Milestone(5475.0, Translation.getText("sm_16", lang), Translation.getText("sm_16_desc", lang), "High")
        )
    } else if (title.contains("Alcol") || title.contains("Alcohol")) {
        listOf(
            Milestone(0.0416, Translation.getText("al_1", lang), Translation.getText("al_1_desc", lang), "High"),
            Milestone(0.25, Translation.getText("al_2", lang), Translation.getText("al_2_desc", lang), "Medium"),
            Milestone(1.0, Translation.getText("al_3", lang), Translation.getText("al_3_desc", lang), "High"),
            Milestone(2.0, Translation.getText("al_4", lang), Translation.getText("al_4_desc", lang), "Medium"),
            Milestone(3.0, Translation.getText("al_5", lang), Translation.getText("al_5_desc", lang), "Medium"),
            Milestone(7.0, Translation.getText("al_6", lang), Translation.getText("al_6_desc", lang), "High"),
            Milestone(14.0, Translation.getText("al_7", lang), Translation.getText("al_7_desc", lang), "Medium"),
            Milestone(21.0, Translation.getText("al_8", lang), Translation.getText("al_8_desc", lang), "Medium"),
            Milestone(30.0, Translation.getText("al_9", lang), Translation.getText("al_9_desc", lang), "High"),
            Milestone(90.0, Translation.getText("al_10", lang), Translation.getText("al_10_desc", lang), "High"),
            Milestone(180.0, Translation.getText("al_11", lang), Translation.getText("al_11_desc", lang), "Medium"),
            Milestone(365.0, Translation.getText("al_12", lang), Translation.getText("al_12_desc", lang), "Medium"),
            Milestone(730.0, Translation.getText("al_13", lang), Translation.getText("al_13_desc", lang), "Medium"),
            Milestone(1825.0, Translation.getText("al_14", lang), Translation.getText("al_14_desc", lang), "Medium")
        )
    } else {
        // Sugar milestones
        listOf(
            Milestone(0.0833, Translation.getText("su_1", lang), Translation.getText("su_1_desc", lang), "High"),
            Milestone(1.0, Translation.getText("su_2", lang), Translation.getText("su_2_desc", lang), "Medium"),
            Milestone(3.0, Translation.getText("su_3", lang), Translation.getText("su_3_desc", lang), "Low"),
            Milestone(7.0, Translation.getText("su_4", lang), Translation.getText("su_4_desc", lang), "Medium"),
            Milestone(14.0, Translation.getText("su_5", lang), Translation.getText("su_5_desc", lang), "Medium"),
            Milestone(21.0, Translation.getText("su_6", lang), Translation.getText("su_6_desc", lang), "Low"),
            Milestone(30.0, Translation.getText("su_7", lang), Translation.getText("su_7_desc", lang), "Low"),
            Milestone(60.0, Translation.getText("su_8", lang), Translation.getText("su_8_desc", lang), "Medium"),
            Milestone(90.0, Translation.getText("su_9", lang), Translation.getText("su_9_desc", lang), "Medium"),
            Milestone(180.0, Translation.getText("su_10", lang), Translation.getText("su_10_desc", lang), "Medium"),
            Milestone(365.0, Translation.getText("su_11", lang), Translation.getText("su_11_desc", lang), "Medium"),
            Milestone(730.0, Translation.getText("su_12", lang), Translation.getText("su_12_desc", lang), "Low"),
            Milestone(1095.0, Translation.getText("su_13", lang), Translation.getText("su_13_desc", lang), "Medium")
        )
    }

    // 1. INACTIVE STATE
    if (startTime == -1L) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Translation.getText("bad_habit_desc_inactive", lang),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            // Start Immediately
            Button(
                onClick = { onStart(System.currentTimeMillis()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translation.getText("start_counter", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Start from custom past date
            OutlinedButton(
                onClick = {
                    showDateTimePicker(context, System.currentTimeMillis()) { selectedTime ->
                        onStart(selectedTime)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translation.getText("edit_start_time", lang), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
        return
    }

    // 2. ACTIVE STATE
    val isFuture = startTime > ticker
    val elapsedMs = if (isFuture) 0L else ticker - startTime
    val days = elapsedMs / (24 * 3600000)
    val hours = (elapsedMs % (24 * 3600000)) / 3600000
    val minutes = (elapsedMs % 3600000) / 60000
    val seconds = (elapsedMs % 60000) / 1000

    val msToStart = if (isFuture) startTime - ticker else 0L
    val daysToStart = msToStart / (24 * 3600000)
    val hoursToStart = (msToStart % (24 * 3600000)) / 3600000
    val minutesToStart = (msToStart % 3600000) / 60000
    val secondsToStart = (msToStart % 60000) / 1000

    val totalSavedDays = elapsedMs.toDouble() / (24.0 * 3600000.0)
    val savedMoney = if (isSmoking) totalSavedDays * dailyCost else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main counter card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isFuture) Translation.getText("future_start_message", lang) else title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFuture) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFuture) Translation.getText("time_to_start", lang) else Translation.getText("time_elapsed", lang),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Spacious Timer numbers
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val d = if (isFuture) daysToStart else days
                    val h = if (isFuture) hoursToStart else hours
                    val m = if (isFuture) minutesToStart else minutes
                    val s = if (isFuture) secondsToStart else seconds

                    TimeItem(value = d, unit = Translation.getText("days", lang))
                    Spacer(modifier = Modifier.width(12.dp))
                    TimeItem(value = h, unit = "h")
                    Spacer(modifier = Modifier.width(12.dp))
                    TimeItem(value = m, unit = "m")
                    Spacer(modifier = Modifier.width(12.dp))
                    TimeItem(value = s, unit = "s")
                }

                if (isSmoking) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Translation.getText("saved_money", lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("€ %.2f", savedMoney),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Editable Tobacco Cost
                            var costInput by remember { mutableStateOf(dailyCost.toString()) }
                            var isEditingCost by remember { mutableStateOf(false) }

                            if (isEditingCost) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedTextField(
                                        value = costInput,
                                        onValueChange = { costInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            costInput.toDoubleOrNull()?.let {
                                                onUpdateSmokingCost(it)
                                            }
                                            isEditingCost = false
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save")
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isEditingCost = true }
                                ) {
                                    Text(
                                        text = "${Translation.getText("daily_expense", lang)}: € $dailyCost",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit cost",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Edit Start Date/Time
                    Button(
                        onClick = {
                            showDateTimePicker(context, startTime) { newTime ->
                                onStart(newTime)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translation.getText("edit", lang), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
                    }

                    // Stop tracking completely (set to -1)
                    Button(
                        onClick = { showStopConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translation.getText("stop_journey", lang), color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val label = if (isFuture) {
                    when (lang) {
                        "it" -> "Inizio previsto il"
                        "es" -> "Inicio programado el"
                        "fr" -> "Début prévu le"
                        else -> "Scheduled to start on"
                    }
                } else {
                    Translation.getText("stopped_since", lang)
                }
                Text(
                    text = "$label: ${format.format(Date(startTime))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Health Milestones Timeline (with reliability level badges)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translation.getText("milestones", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                val elapsedDays = if (isFuture) -1.0 else elapsedMs.toDouble() / (24 * 3600000)

                milestones.forEachIndexed { idx, milestone ->
                    val completed = !isFuture && elapsedDays >= milestone.days

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (completed) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (completed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = milestone.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Evidence reliability Badge
                                val badgeColor = when (milestone.reliability) {
                                    "High" -> Color(0xFF2E7D32)
                                    "Medium" -> Color(0xFFE65100)
                                    else -> Color(0xFF1565C0)
                                }
                                val badgeLabel = when (milestone.reliability) {
                                    "High" -> Translation.getText("rel_high", lang)
                                    "Medium" -> Translation.getText("rel_medium", lang)
                                    else -> Translation.getText("rel_low", lang)
                                }
                                Text(
                                    text = badgeLabel,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = milestone.desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (idx < milestones.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 40.dp, top = 2.dp, bottom = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text(Translation.getText("reset_confirm_title", lang), fontWeight = FontWeight.Bold) },
            text = { Text(Translation.getText("reset_confirm_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        onStop()
                        showStopConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Translation.getText("confirm", lang), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

@Composable
fun TimeItem(value: Long, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class Milestone(
    val days: Double,
    val title: String,
    val desc: String,
    val reliability: String
)

@Composable
fun StatisticsScreen(
    lang: String,
    fastingSessions: List<FastingSession>,
    weightEntries: List<WeightEntry>,
    onAddWeight: (Double, Long) -> Unit,
    onEditWeight: (WeightEntry) -> Unit,
    onDeleteWeight: (WeightEntry) -> Unit,
    onEditFastingSession: (FastingSession) -> Unit,
    onDeleteFastingSession: (FastingSession) -> Unit
) {
    val context = LocalContext.current
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var customDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var editingEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var editingSession by remember { mutableStateOf<FastingSession?>(null) }
    var showEditSessionDialog by remember { mutableStateOf(false) }
    var editSessionStart by remember { mutableStateOf(0L) }
    var editSessionEnd by remember { mutableStateOf(0L) }
    var editSessionWeight by remember { mutableStateOf("") }

    // Fasting chart period selection (Weekly vs Monthly)
    var isWeeklyView by remember { mutableStateOf(true) }

    // Filter out future or invalid sessions from stats calculation as per debug requirement 1/2
    val validSessions = remember(fastingSessions) {
        fastingSessions.filter { it.endTimestamp > it.startTimestamp && it.startTimestamp <= System.currentTimeMillis() }
    }

    // Fasting summaries
    val totalFasts = validSessions.size
    val averageDurationHours = if (totalFasts > 0) {
        validSessions.map { (it.endTimestamp - it.startTimestamp) / 3600000.0 }.average()
    } else 0.0

    val longestFastHours = if (totalFasts > 0) {
        validSessions.map { (it.endTimestamp - it.startTimestamp) / 3600000.0 }.maxOrNull() ?: 0.0
    } else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Translation.getText("statistics", lang),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Summary Cards Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Average Fast", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(String.format("%.1f hrs", averageDurationHours), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Longest Fast", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(String.format("%.1f hrs", longestFastHours), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight Trend Section with Beautiful Line Chart Drawing (Canvas)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translation.getText("weight_chart", lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Add Weight Entry
                    IconButton(
                        onClick = {
                            weightInput = ""
                            customDate = System.currentTimeMillis()
                            showAddWeightDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Weight",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Drawn Weight Chart
                if (weightEntries.isNotEmpty()) {
                    val sortedWeights = weightEntries.sortedBy { it.timestamp }
                    val weights = sortedWeights.map { it.weight }
                    val minW = (weights.minOrNull() ?: 0.0) - 1.0
                    val maxW = (weights.maxOrNull() ?: 100.0) + 1.0
                    val weightRange = if (maxW - minW > 0) maxW - minW else 1.0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        val textColor = MaterialTheme.colorScheme.onSurface

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pointsCount = sortedWeights.size
                            val w = size.width
                            val h = size.height

                            val stepX = if (pointsCount > 1) w / (pointsCount - 1) else w

                            val coordinates = sortedWeights.mapIndexed { idx, entry ->
                                val x = idx * stepX
                                val y = h - (((entry.weight - minW) / weightRange) * h).toFloat()
                                Offset(x, y)
                            }

                            // Draw horizontal reference lines (grid)
                            for (i in 1..3) {
                                val gridY = h * i / 4
                                drawLine(
                                    color = outlineColor,
                                    start = Offset(0f, gridY),
                                    end = Offset(w, gridY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Draw the gradient filled path below the weight trend line
                            if (coordinates.size > 1) {
                                val fillPath = Path().apply {
                                    moveTo(coordinates.first().x, coordinates.first().y)
                                    for (i in 1 until coordinates.size) {
                                        lineTo(coordinates[i].x, coordinates[i].y)
                                    }
                                    lineTo(coordinates.last().x, h)
                                    lineTo(coordinates.first().x, h)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            secondaryColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )

                                // Draw line connecting points
                                val linePath = Path().apply {
                                    moveTo(coordinates.first().x, coordinates.first().y)
                                    for (i in 1 until coordinates.size) {
                                        lineTo(coordinates[i].x, coordinates[i].y)
                                    }
                                }
                                drawPath(
                                    path = linePath,
                                    color = secondaryColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Draw points and values
                            coordinates.forEachIndexed { idx, offset ->
                                drawCircle(
                                    color = secondaryColor,
                                    radius = 6.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }

                    // Display weight values on the horizontal timeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val format = SimpleDateFormat("dd/MM", Locale.getDefault())
                        Text(format.format(Date(sortedWeights.first().timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (sortedWeights.size > 2) {
                            Text(format.format(Date(sortedWeights[sortedWeights.size / 2].timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (sortedWeights.size > 1) {
                            Text(format.format(Date(sortedWeights.last().timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Translation.getText("no_weight_data", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fasting Hours Chart (Weekly / Monthly Bar Chart)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fasting Hours History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Period Toggle Buttons
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "7 Days",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isWeeklyView) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                .clickable { isWeeklyView = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "30 Days",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isWeeklyView) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                .clickable { isWeeklyView = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Build daily fast statistics
                val now = System.currentTimeMillis()
                val periodDays = if (isWeeklyView) 7 else 30
                val dailyFastingHours = FloatArray(periodDays)
                val labels = Array(periodDays) { "" }

                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                val cal = Calendar.getInstance()

                for (i in 0 until periodDays) {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, - (periodDays - 1 - i))
                    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
                    val targetYear = cal.get(Calendar.YEAR)

                    labels[i] = sdf.format(Date(cal.timeInMillis))

                    // Sum fasting duration completed on this target day
                    val dayFasts = validSessions.filter { session ->
                        val sessionCal = Calendar.getInstance().apply { timeInMillis = session.endTimestamp }
                        sessionCal.get(Calendar.DAY_OF_YEAR) == targetDay && sessionCal.get(Calendar.YEAR) == targetYear
                    }
                    dailyFastingHours[i] = dayFasts.sumOf { (it.endTimestamp - it.startTimestamp) / 3600000.0 }.toFloat()
                }

                val maxFastingHours = (dailyFastingHours.maxOrNull() ?: 0f).coerceAtLeast(16f)

                if (validSessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barColor = MaterialTheme.colorScheme.secondary
                        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val barSpacingRatio = 0.3f
                            val totalBarWidths = w / periodDays
                            val barWidth = totalBarWidths * (1 - barSpacingRatio)
                            val spacing = totalBarWidths * barSpacingRatio

                            // Draw vertical bars
                            for (i in 0 until periodDays) {
                                val barHeight = (dailyFastingHours[i] / maxFastingHours) * h
                                val x = i * totalBarWidths + spacing / 2
                                val y = h - barHeight

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }

                            // Horizontal reference line
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Bottom labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(labels.first(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (periodDays > 7) {
                            Text(labels[15], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(labels.last(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Translation.getText("no_history", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fasting & Weight Correlation Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fasting & Weight Correlation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Observe how fasting hours (bars) correlate with your body weight (line).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                val correlationSessions = validSessions.filter { it.weight != null }.sortedBy { it.endTimestamp }

                if (correlationSessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        val lineColor = MaterialTheme.colorScheme.error
                        val count = correlationSessions.size

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val stepX = if (count > 1) w / (count - 1) else w
                            
                            val maxFast = correlationSessions.maxOf { (it.endTimestamp - it.startTimestamp) / 3600000.0 }.coerceAtLeast(12.0)
                            val weights = correlationSessions.mapNotNull { it.weight }
                            val minW = weights.minOrNull() ?: 50.0
                            val maxW = weights.maxOrNull() ?: 100.0
                            val wRange = if (maxW - minW > 0) maxW - minW else 1.0

                            // Draw bars (fasting hours) and points (weights)
                            val lineCoords = mutableListOf<Offset>()

                            correlationSessions.forEachIndexed { idx, s ->
                                val fastHrs = (s.endTimestamp - s.startTimestamp) / 3600000.0
                                val barH = (fastHrs / maxFast) * h
                                val barW = (w / count) * 0.4f
                                val x = idx * stepX

                                // Draw fasting bar
                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(x - barW / 2, (h - barH).toFloat()),
                                    size = androidx.compose.ui.geometry.Size(barW, barH.toFloat())
                                )

                                // Save weight line point
                                val weightY = h - (((s.weight!! - minW) / wRange) * h).toFloat()
                                lineCoords.add(Offset(x, weightY))
                            }

                            // Connect weight line
                            if (lineCoords.size > 1) {
                                val path = Path().apply {
                                    moveTo(lineCoords.first().x, lineCoords.first().y)
                                    for (i in 1 until lineCoords.size) {
                                        lineTo(lineCoords[i].x, lineCoords[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Draw dots on weight line
                            lineCoords.forEach { offset ->
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }

                    // Display date range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val format = SimpleDateFormat("dd/MM", Locale.getDefault())
                        Text(format.format(Date(correlationSessions.first().endTimestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (correlationSessions.size > 1) {
                            Text(format.format(Date(correlationSessions.last().endTimestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add fasting entries with weight to unlock correlation analysis.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fasting History Logs (With edit and remove)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fasting History Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (fastingSessions.isNotEmpty()) {
                    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                    fastingSessions.sortedByDescending { it.endTimestamp }.forEachIndexed { idx, session ->
                        val durationHrs = (session.endTimestamp - session.startTimestamp) / 3600000.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format("%.1f hours", durationHrs),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Start: ${sdf.format(Date(session.startTimestamp))} | End: ${sdf.format(Date(session.endTimestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (session.weight != null) {
                                    Text(
                                        text = "Weight: ${session.weight} kg",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingSession = session
                                        editSessionStart = session.startTimestamp
                                        editSessionEnd = session.endTimestamp
                                        editSessionWeight = session.weight?.toString() ?: ""
                                        showEditSessionDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteFastingSession(session) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (idx < fastingSessions.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                } else {
                    Text(
                        text = "No fasting history logs recorded.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight logs list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weight History Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (weightEntries.isNotEmpty()) {
                    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    weightEntries.sortedByDescending { it.timestamp }.forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.weight} kg",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = format.format(Date(entry.timestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingEntry = entry
                                        weightInput = entry.weight.toString()
                                        customDate = entry.timestamp
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteWeight(entry) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (idx < weightEntries.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                } else {
                    Text(
                        text = "No weight logs recorded.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }

    // Add Weight Dialog
    if (showAddWeightDialog) {
        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            title = { Text(Translation.getText("add_weight_title", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(Translation.getText("weight", lang) + " (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDateTimePicker(context, customDate) {
                                    customDate = it
                                }
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = dateFormat.format(Date(customDate)), fontSize = 13.sp)
                        Icon(Icons.Default.CalendarToday, contentDescription = "Choose Date")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedWeight = weightInput.toDoubleOrNull()
                        if (parsedWeight != null) {
                            onAddWeight(parsedWeight, customDate)
                        }
                        showAddWeightDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(Translation.getText("save", lang), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightDialog = false }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    // Edit Weight Dialog
    if (editingEntry != null) {
        val entry = editingEntry!!
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text(Translation.getText("edit_weight_title", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(Translation.getText("weight", lang) + " (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDateTimePicker(context, customDate) {
                                    customDate = it
                                }
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = dateFormat.format(Date(customDate)), fontSize = 13.sp)
                        Icon(Icons.Default.CalendarToday, contentDescription = "Choose Date")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedWeight = weightInput.toDoubleOrNull()
                        if (parsedWeight != null) {
                            onEditWeight(entry.copy(weight = parsedWeight, timestamp = customDate))
                        }
                        editingEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(Translation.getText("save", lang), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    // Edit Fasting History Session Dialog (Modifica data/ora inizio o fine e peso)
    if (showEditSessionDialog && editingSession != null) {
        val session = editingSession!!
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showEditSessionDialog = false },
            title = { Text("Edit Fasting Session", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Start Date/Time picker
                    Text("Start Date & Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDateTimePicker(context, editSessionStart) { editSessionStart = it }
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = sdf.format(Date(editSessionStart)), fontSize = 13.sp)
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    // End Date/Time picker
                    Text("End Date & Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDateTimePicker(context, editSessionEnd) { editSessionEnd = it }
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = sdf.format(Date(editSessionEnd)), fontSize = 13.sp)
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    // Optional Weight associated
                    Text("Weight (kg, Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = editSessionWeight,
                        onValueChange = { editSessionWeight = it },
                        placeholder = { Text("e.g. 74.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedWeight = editSessionWeight.toDoubleOrNull()
                        onEditFastingSession(
                            session.copy(
                                startTimestamp = editSessionStart,
                                endTimestamp = editSessionEnd,
                                weight = parsedWeight
                            )
                        )
                        showEditSessionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(Translation.getText("save", lang), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSessionDialog = false }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    lang: String,
    habitState: HabitState,
    onToggleFasting: (Boolean) -> Unit,
    onToggleSmoking: (Boolean) -> Unit,
    onToggleAlcohol: (Boolean) -> Unit,
    onToggleSugar: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onResetApp: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    val currentLangLabel = when (habitState.language) {
        "en" -> "English"
        "it" -> "Italiano"
        "es" -> "Español"
        "fr" -> "Français"
        else -> Translation.getText("language_system", lang)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Translation.getText("settings", lang),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Modules management card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translation.getText("settings_modules", lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Fasting switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("fasting", lang), fontSize = 14.sp)
                    Switch(
                        checked = habitState.fastingEnabled,
                        onCheckedChange = { onToggleFasting(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                // Smoking switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("smoking", lang), fontSize = 14.sp)
                    Switch(
                        checked = habitState.smokingEnabled,
                        onCheckedChange = { onToggleSmoking(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                // Alcohol switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("alcohol", lang), fontSize = 14.sp)
                    Switch(
                        checked = habitState.alcoholEnabled,
                        onCheckedChange = { onToggleAlcohol(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                // Sugar switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("sugar", lang), fontSize = 14.sp)
                    Switch(
                        checked = habitState.sugarEnabled,
                        onCheckedChange = { onToggleSugar(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language settings card with Dropdown Menu (Mappa a un menu a tendina as debug req 1!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translation.getText("settings_language", lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown trigger button
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { languageDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = currentLangLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select language",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val languagesList = listOf(
                            "system" to Translation.getText("language_system", lang),
                            "en" to Translation.getText("language_en", lang),
                            "it" to Translation.getText("language_it", lang),
                            "es" to Translation.getText("language_es", lang),
                            "fr" to Translation.getText("language_fr", lang)
                        )

                        languagesList.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(text = label, fontWeight = if (habitState.language == code) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onLanguageSelected(code)
                                    languageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Reset & Creator credits card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Brombolo Creator credit
                Text(
                    text = "Brombolo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "creato da Brombolo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = Translation.getText("reset_app", lang), color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(Translation.getText("reset_confirm_title", lang), fontWeight = FontWeight.Bold) },
            text = { Text(Translation.getText("reset_confirm_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetApp()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Translation.getText("confirm", lang), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(Translation.getText("cancel", lang), color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
