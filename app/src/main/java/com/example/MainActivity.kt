package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
            val viewModel: HealthViewModel = viewModel()
            val themeMode = viewModel.themeMode
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: HealthViewModel) {
    val context = LocalContext.current
    
    val habitState by viewModel.habitState.collectAsStateWithLifecycle()
    val fastingSessions by viewModel.fastingSessions.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()
    
    val currentScreen = viewModel.currentScreen
    val lang = Translation.getLanguageCode(habitState.language)

    // Statistics Screen Visibility Constraint:
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        ),
                        center = Offset(0f, 0f),
                        radius = 2500f
                    )
                )
        ) {
            if (currentScreen == Screen.Onboarding) {
                OnboardingScreen(
                    lang = lang,
                    onComplete = { f, s, al, su ->
                        viewModel.completeOnboarding(f, s, al, su)
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NavigationSidebar(
                        currentScreen = currentScreen,
                        habitState = habitState,
                        lang = lang,
                        onNavigate = { viewModel.navigateTo(it) }
                    )

                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

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
                                onStop = { end, weightVal -> viewModel.stopFasting(end, weightVal) },
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
                                themeMode = viewModel.themeMode,
                                onToggleFasting = { viewModel.toggleFastingEnabled(it) },
                                onToggleSmoking = { viewModel.toggleSmokingEnabled(it) },
                                onToggleAlcohol = { viewModel.toggleAlcoholEnabled(it) },
                                onToggleSugar = { viewModel.toggleSugarEnabled(it) },
                                onLanguageSelected = { viewModel.setLanguage(it) },
                                onThemeModeSelected = { viewModel.updateThemeMode(it) },
                                onResetApp = { viewModel.resetApplication() }
                            )
                            else -> {}
                        }
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
        modifier = Modifier.fillMaxHeight(),
        containerColor = Color.Transparent,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shadowElevation = 4.dp
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.padding(8.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (habitState.fastingEnabled) {
                    SidebarItem(
                        icon = Icons.Default.Timer,
                        label = Translation.getText("fasting", lang),
                        selected = currentScreen == Screen.Fasting,
                        onClick = { onNavigate(Screen.Fasting) }
                    )
                }

                if (habitState.smokingEnabled) {
                    SidebarItem(
                        icon = Icons.Default.SmokingRooms,
                        label = Translation.getText("smoking", lang),
                        selected = currentScreen == Screen.Smoking,
                        onClick = { onNavigate(Screen.Smoking) }
                    )
                }

                if (habitState.alcoholEnabled) {
                    SidebarItem(
                        icon = Icons.Default.LocalBar,
                        label = Translation.getText("alcohol", lang),
                        selected = currentScreen == Screen.Alcohol,
                        onClick = { onNavigate(Screen.Alcohol) }
                    )
                }

                if (habitState.sugarEnabled) {
                    SidebarItem(
                        icon = Icons.Default.Cake,
                        label = Translation.getText("sugar", lang),
                        selected = currentScreen == Screen.Sugar,
                        onClick = { onNavigate(Screen.Sugar) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    modifier = Modifier.width(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (habitState.fastingEnabled) {
                    SidebarItem(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = Translation.getText("statistics", lang),
                        selected = currentScreen == Screen.Statistics,
                        onClick = { onNavigate(Screen.Statistics) }
                    )
                }

                SidebarItem(
                    icon = Icons.Default.Settings,
                    label = Translation.getText("settings", lang),
                    selected = currentScreen == Screen.Settings,
                    onClick = { onNavigate(Screen.Settings) }
                )
            }
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
    val iconSize by animateDpAsState(targetValue = if (selected) 28.dp else 24.dp)

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
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
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue")
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
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                        tint = MaterialTheme.colorScheme.primary,
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
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

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

data class FastingStage(
    val startHour: Double,
    val endHour: Double,
    val name: String,
    val descKey: String,
    val icon: ImageVector
)

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

    val fastingStages = remember(lang) {
        listOf(
            FastingStage(0.0, 2.0, Translation.getText("f_step_1", lang), "f_step_1_desc", Icons.AutoMirrored.Filled.TrendingUp),
            FastingStage(2.0, 5.0, Translation.getText("f_step_2", lang), "f_step_2_desc", Icons.AutoMirrored.Filled.TrendingDown),
            FastingStage(5.0, 8.0, Translation.getText("f_step_3", lang), "f_step_3_desc", Icons.Default.Balance),
            FastingStage(8.0, 10.0, Translation.getText("f_step_4", lang), "f_step_4_desc", Icons.Default.Timer),
            FastingStage(10.0, 12.0, Translation.getText("f_step_5", lang), "f_step_5_desc", Icons.Default.Whatshot),
            FastingStage(12.0, 18.0, Translation.getText("f_step_6", lang), "f_step_6_desc", Icons.Default.Bolt),
            FastingStage(18.0, 24.0, Translation.getText("f_step_7", lang), "f_step_7_desc", Icons.Default.LocalFireDepartment),
            FastingStage(24.0, 48.0, Translation.getText("f_step_8", lang), "f_step_8_desc", Icons.Default.AutoFixHigh),
            FastingStage(48.0, 56.0, Translation.getText("f_step_9", lang), "f_step_9_desc", Icons.Default.KeyboardDoubleArrowUp),
            FastingStage(56.0, 72.0, Translation.getText("f_step_10", lang), "f_step_10_desc", Icons.Default.South),
            FastingStage(72.0, 999.0, Translation.getText("f_step_11", lang), "f_step_11_desc", Icons.Default.HealthAndSafety)
        )
    }

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

                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sweepAngle = if (isActive && !isFuture) {
                        ((elapsedHours % 24.0) / 24.0 * 360f).toFloat()
                    } else 0f

                    val backgroundRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    val activeRingColor = MaterialTheme.colorScheme.primary

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
                                    color = MaterialTheme.colorScheme.primary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = Translation.getText("milestones", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                fastingStages.forEachIndexed { idx, stage ->
                    val completed = !isFuture && isActive && elapsedHours >= stage.startHour
                    val isCurrent = !isFuture && isActive && idx == currentStageIndex
                    
                    val progressInStage = if (isCurrent) {
                        val duration = stage.endHour - stage.startHour
                        val elapsedInStage = elapsedHours - stage.startHour
                        (elapsedInStage / duration).coerceIn(0.0, 1.0).toFloat()
                    } else if (completed) 1f else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completed) MaterialTheme.colorScheme.primary
                                        else if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = stage.icon,
                                    contentDescription = null,
                                    tint = if (completed) Color.White 
                                           else if (isCurrent) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            if (idx < fastingStages.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(3.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(progressInStage)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (idx < fastingStages.lastIndex) 20.dp else 0.dp)
                        ) {
                            Text(
                                text = stage.name,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent || completed) FontWeight.Bold else FontWeight.Medium,
                                color = if (completed) MaterialTheme.colorScheme.primary 
                                        else if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Translation.getText(stage.descKey, lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progressInStage },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        var editedEndTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var weightInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(text = Translation.getText("stop_fast", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = Translation.getText("edit_end_time", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val format = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
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
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = format.format(Date(editedEndTime)), fontSize = 14.sp)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                    }

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
                            Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        suffix = { Text("kg") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

data class Milestone(
    val days: Double,
    val title: String,
    val desc: String,
    val reliability: String
)

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

    val milestones = remember(isSmoking, lang, title) {
        if (isSmoking) {
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
    }

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
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
            
            Button(
                onClick = { onStart(System.currentTimeMillis()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translation.getText("start_counter", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translation.getText("edit_start_time", lang), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
        return
    }

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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                val format = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                val label = if (isFuture) {
                    when (lang) {
                        "it" -> "Inizio previsto il"
                        "es" -> "Inicio programado el"
                        "fr" -> "Début previsto le"
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
                                    if (completed) MaterialTheme.colorScheme.primary
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
                                    color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
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

    var isWeeklyView by remember { mutableStateOf(true) }
    val periodDays = if (isWeeklyView) 7 else 30

    val validSessions = remember(fastingSessions) {
        fastingSessions.filter { it.endTimestamp > it.startTimestamp && it.startTimestamp <= System.currentTimeMillis() }
    }

    val now = System.currentTimeMillis()
    val dailyFastingHours = remember(validSessions, isWeeklyView) {
        val hours = FloatArray(periodDays)
        val cal = Calendar.getInstance()
        for (i in 0 until periodDays) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -(periodDays - 1 - i))
            val targetDay = cal.get(Calendar.DAY_OF_YEAR)
            val targetYear = cal.get(Calendar.YEAR)
            val dayFasts = validSessions.filter { session ->
                val sessionCal = Calendar.getInstance().apply { timeInMillis = session.endTimestamp }
                sessionCal.get(Calendar.DAY_OF_YEAR) == targetDay && sessionCal.get(Calendar.YEAR) == targetYear
            }
            hours[i] = dayFasts.sumOf { (it.endTimestamp - it.startTimestamp) / 3600000.0 }.toFloat()
        }
        hours
    }

    val dailyWeights = remember(weightEntries, isWeeklyView) {
        val weights = arrayOfNulls<Double>(periodDays)
        val cal = Calendar.getInstance()
        for (i in 0 until periodDays) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -(periodDays - 1 - i))
            val targetDay = cal.get(Calendar.DAY_OF_YEAR)
            val targetYear = cal.get(Calendar.YEAR)
            val dayWeights = weightEntries.filter { entry ->
                val wCal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                wCal.get(Calendar.DAY_OF_YEAR) == targetDay && wCal.get(Calendar.YEAR) == targetYear
            }
            if (dayWeights.isNotEmpty()) {
                weights[i] = dayWeights.sumOf { it.weight } / dayWeights.size
            }
        }
        weights
    }

    val chartLabels = remember(isWeeklyView) {
        val labels = Array(periodDays) { "" }
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        for (i in 0 until periodDays) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -(periodDays - 1 - i))
            labels[i] = sdf.format(Date(cal.timeInMillis))
        }
        labels
    }

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

                            for (i in 1..3) {
                                val gridY = h * i / 4
                                drawLine(
                                    color = outlineColor,
                                    start = Offset(0f, gridY),
                                    end = Offset(w, gridY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val format = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
        Text(
            text = "Fasting History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isWeeklyView) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isWeeklyView = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "7 Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isWeeklyView) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isWeeklyView = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "30 Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val maxFastingHours = (dailyFastingHours.maxOrNull() ?: 0f).coerceAtLeast(16f)

                if (validSessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        val barColor = MaterialTheme.colorScheme.primary
                        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val barSpacingRatio = if (isWeeklyView) 0.3f else 0.15f
                            val totalBarWidths = w / periodDays
                            val barWidth = totalBarWidths * (1 - barSpacingRatio)
                            val spacing = totalBarWidths * barSpacingRatio

                            for (i in 0 until periodDays) {
                                val barHeight = (dailyFastingHours[i] / maxFastingHours) * h
                                val x = i * totalBarWidths + spacing / 2
                                val y = h - barHeight

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }

                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(chartLabels.first(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (periodDays > 7) {
                            Text(chartLabels[periodDays / 2], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(chartLabels.last(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                if (validSessions.isNotEmpty() || weightEntries.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        val barColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        val lineColor = MaterialTheme.colorScheme.error
                        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val maxFast = (dailyFastingHours.maxOrNull() ?: 0f).coerceAtLeast(16f)
                            val nonNullWeights = dailyWeights.filterNotNull()
                            val minW = (nonNullWeights.minOrNull() ?: 50.0) - 2.0
                            val maxW = (nonNullWeights.maxOrNull() ?: 100.0) + 2.0
                            val wRange = (maxW - minW).coerceAtLeast(1.0)

                            val stepX = w / periodDays

                            val barWidthRatio = 0.6f
                            for (i in 0 until periodDays) {
                                val fastHrs = dailyFastingHours[i]
                                val barH = (fastHrs / maxFast) * h
                                val barW = stepX * barWidthRatio
                                val x = i * stepX + (stepX - barW) / 2

                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(x, (h - barH).toFloat()),
                                    size = androidx.compose.ui.geometry.Size(barW, barH.toFloat())
                                )
                            }

                            val lineCoords = mutableListOf<Offset>()
                            for (i in 0 until periodDays) {
                                val weight = dailyWeights[i]
                                if (weight != null) {
                                    val x = i * stepX + stepX / 2
                                    val y = h - (((weight - minW) / wRange) * h).toFloat()
                                    lineCoords.add(Offset(x, y))
                                }
                            }

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

                            lineCoords.forEach { offset ->
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = offset
                                )
                            }
                            
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(chartLabels.first(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (periodDays > 7) {
                            Text(chartLabels[periodDays / 2], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(chartLabels.last(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data available for correlation analysis.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Fasting History Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (fastingSessions.isNotEmpty()) {
                    val sdf = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
                    fastingSessions.sortedByDescending { it.endTimestamp }.forEachIndexed { idx, session ->
                        val durationHrs = (session.endTimestamp - session.startTimestamp) / 3600000.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format("%.1f hours", durationHrs),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sdf.format(Date(session.startTimestamp))} - ${sdf.format(Date(session.endTimestamp))}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (session.weight != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MonitorWeight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${session.weight} kg",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = {
                                        editingSession = session
                                        editSessionStart = session.startTimestamp
                                        editSessionEnd = session.endTimestamp
                                        editSessionWeight = session.weight?.toString() ?: ""
                                        showEditSessionDialog = true
                                    },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { onDeleteFastingSession(session) },
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (idx < fastingSessions.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    Text(
                        text = "No fasting history logs recorded.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weight History Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (weightEntries.isNotEmpty()) {
                    val format = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    weightEntries.sortedByDescending { it.timestamp }.forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.weight} kg",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = format.format(Date(entry.timestamp)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = {
                                        editingEntry = entry
                                        weightInput = entry.weight.toString()
                                        customDate = entry.timestamp
                                    },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { onDeleteWeight(entry) },
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (idx < weightEntries.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    Text(
                        text = "No weight logs recorded.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }

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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

    if (showEditSessionDialog && editingSession != null) {
        val session = editingSession!!
        val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showEditSessionDialog = false },
            title = { Text("Edit Fasting Session", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    themeMode: Int,
    onToggleFasting: (Boolean) -> Unit,
    onToggleSmoking: (Boolean) -> Unit,
    onToggleAlcohol: (Boolean) -> Unit,
    onToggleSugar: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeModeSelected: (Int) -> Unit,
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

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("fasting", lang), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = habitState.fastingEnabled,
                        onCheckedChange = { onToggleFasting(it) },
                        thumbContent = {
                            if (habitState.fastingEnabled) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("smoking", lang), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = habitState.smokingEnabled,
                        onCheckedChange = { onToggleSmoking(it) },
                        thumbContent = {
                            if (habitState.smokingEnabled) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("alcohol", lang), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = habitState.alcoholEnabled,
                        onCheckedChange = { onToggleAlcohol(it) },
                        thumbContent = {
                            if (habitState.alcoholEnabled) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translation.getText("sugar", lang), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = habitState.sugarEnabled,
                        onCheckedChange = { onToggleSugar(it) },
                        thumbContent = {
                            if (habitState.sugarEnabled) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                                tint = MaterialTheme.colorScheme.primary
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = Translation.getText("settings_theme", lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    val themes = listOf(
                        0 to Translation.getText("theme_system", lang),
                        1 to Translation.getText("theme_light", lang),
                        2 to Translation.getText("theme_dark", lang)
                    )
                    themes.forEachIndexed { index, pair ->
                        val selected = themeMode == pair.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onThemeModeSelected(pair.first) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.second,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < themes.size - 1) Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Legenda Livelli Evidenza",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LegendItem(label = Translation.getText("rel_high", lang), color = Color(0xFF2E7D32), desc = "Basato su solide prove cliniche e scientifiche.")
                Spacer(modifier = Modifier.height(8.dp))
                LegendItem(label = Translation.getText("rel_medium", lang), color = Color(0xFFE65100), desc = "Supportato da studi ma con variabili individuali.")
                Spacer(modifier = Modifier.height(8.dp))
                LegendItem(label = Translation.getText("rel_low", lang), color = Color(0xFF1565C0), desc = "Prove emergenti o basate su osservazioni preliminari.")
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

@Composable
fun LegendItem(label: String, color: Color, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
