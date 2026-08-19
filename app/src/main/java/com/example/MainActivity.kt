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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
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
        if (currentScreen == Screen.Onboarding) {
            OnboardingScreen(
                lang = lang,
                onComplete = { f, s, al, su ->
                    viewModel.completeOnboarding(f, s, al, su)
                }
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerContentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(280.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Stop! stay healthy",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        
                        if (habitState.fastingEnabled) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                label = { Text(Translation.getText("fasting", lang)) },
                                selected = currentScreen == Screen.Fasting,
                                onClick = { 
                                    viewModel.navigateTo(Screen.Fasting)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        if (habitState.smokingEnabled) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.SmokingRooms, contentDescription = null) },
                                label = { Text(Translation.getText("smoking", lang)) },
                                selected = currentScreen == Screen.Smoking,
                                onClick = { 
                                    viewModel.navigateTo(Screen.Smoking)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        if (habitState.alcoholEnabled) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.LocalBar, contentDescription = null) },
                                label = { Text(Translation.getText("alcohol", lang)) },
                                selected = currentScreen == Screen.Alcohol,
                                onClick = { 
                                    viewModel.navigateTo(Screen.Alcohol)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        if (habitState.sugarEnabled) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Cake, contentDescription = null) },
                                label = { Text(Translation.getText("sugar", lang)) },
                                selected = currentScreen == Screen.Sugar,
                                onClick = { 
                                    viewModel.navigateTo(Screen.Sugar)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        if (habitState.fastingEnabled) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                                label = { Text(Translation.getText("statistics", lang)) },
                                selected = currentScreen == Screen.Statistics,
                                onClick = { 
                                    viewModel.navigateTo(Screen.Statistics)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        CenterAlignedTopAppBar(
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(28.dp),
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Image(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                                            contentDescription = "App Logo",
                                            modifier = Modifier.padding(3.dp).clip(CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Stop! stay healthy", 
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            actions = {
                                if (currentScreen == Screen.Fasting) {
                                    IconButton(onClick = { viewModel.navigateTo(Screen.Statistics) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = "Statistics",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (currentScreen == Screen.Statistics) {
                                    IconButton(onClick = { viewModel.navigateTo(Screen.Fasting) }) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Fasting",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                            )
                        )
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
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
    val icon: ImageVector,
    val color: Color
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
            FastingStage(0.0, 2.0, Translation.getText("f_step_1", lang), "f_step_1_desc", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF43A047)),
            FastingStage(2.0, 5.0, Translation.getText("f_step_2", lang), "f_step_2_desc", Icons.AutoMirrored.Filled.TrendingDown, Color(0xFF00ACC1)),
            FastingStage(5.0, 8.0, Translation.getText("f_step_3", lang), "f_step_3_desc", Icons.Default.Balance, Color(0xFF1E88E5)),
            FastingStage(8.0, 10.0, Translation.getText("f_step_4", lang), "f_step_4_desc", Icons.Default.Timer, Color(0xFF3949AB)),
            FastingStage(10.0, 12.0, Translation.getText("f_step_5", lang), "f_step_5_desc", Icons.Default.Whatshot, Color(0xFFFBC02D)),
            FastingStage(12.0, 18.0, Translation.getText("f_step_6", lang), "f_step_6_desc", Icons.Default.Bolt, Color(0xFFFB8C00)),
            FastingStage(18.0, 24.0, Translation.getText("f_step_7", lang), "f_step_7_desc", Icons.Default.LocalFireDepartment, Color(0xFFE53935)),
            FastingStage(24.0, 48.0, Translation.getText("f_step_8", lang), "f_step_8_desc", Icons.Default.AutoFixHigh, Color(0xFF8E24AA)),
            FastingStage(48.0, 56.0, Translation.getText("f_step_9", lang), "f_step_9_desc", Icons.Default.KeyboardDoubleArrowUp, Color(0xFFD81B60)),
            FastingStage(56.0, 72.0, Translation.getText("f_step_10", lang), "f_step_10_desc", Icons.Default.South, Color(0xFF5E35B1)),
            FastingStage(72.0, 96.0, Translation.getText("f_step_11", lang), "f_step_11_desc", Icons.Default.HealthAndSafety, Color(0xFFD32F2F))
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

                // Timer Display (Optimized without circle ring)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (isActive) {
                        if (isFuture) {
                            val hoursToStart = msToStart / 3600000
                            val minsToStart = (msToStart % 3600000) / 60000
                            val secsToStart = (msToStart % 60000) / 1000
                            Text(
                                text = String.format("%02d:%02d:%02d", hoursToStart, minsToStart, secsToStart),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Translation.getText("future_start_countdown", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val totalSeconds = elapsedMs / 1000
                            val days = totalSeconds / 86400
                            val hours = (totalSeconds % 86400) / 3600
                            val mins = (totalSeconds % 3600) / 60
                            val secs = totalSeconds % 60

                            val timeText = if (days >= 1) {
                                val daySuffix = when (lang) {
                                    "it" -> "g"
                                    "es" -> "d"
                                    "fr" -> "j"
                                    else -> "d"
                                }
                                String.format("%d%s %02d:%02d:%02d", days, daySuffix, hours, mins, secs)
                            } else {
                                String.format("%02d:%02d:%02d", hours, mins, secs)
                            }

                            Text(
                                text = timeText,
                                fontSize = if (days >= 1) 34.sp else 40.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Translation.getText("time_elapsed", lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                if (isActive && !isFuture && currentStageIndex != -1) {
                    val currentStage = fastingStages[currentStageIndex]
                    val duration = currentStage.endHour - currentStage.startHour
                    val progressPercent = if (duration > 0) {
                        (((elapsedHours - currentStage.startHour) / duration).coerceIn(0.0, 1.0) * 100).toInt()
                    } else 100

                    val isLastStage = currentStageIndex == fastingStages.lastIndex
                    val nextStage = if (!isLastStage) fastingStages[currentStageIndex + 1] else null
                    val nextIcon = if (isLastStage) Icons.Default.StopCircle else nextStage!!.icon
                    val nextIconColor = if (isLastStage) Color(0xFFD32F2F) else nextStage!!.color

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, currentStage.color.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentStage.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Row with current icon -> progress bar -> next/stop icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(currentStage.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentStage.icon,
                                        contentDescription = currentStage.name,
                                        tint = currentStage.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { (progressPercent / 100f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = currentStage.color,
                                        trackColor = currentStage.color.copy(alpha = 0.25f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(nextIconColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = nextIcon,
                                        contentDescription = if (isLastStage) "Stop Limit" else nextStage?.name,
                                        tint = nextIconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentStage.startHour.toInt()}h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$progressPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isLastStage) "96h (Max)" else "${currentStage.endHour.toInt()}h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLastStage) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Danger / Safety Alert when in last stage (72h-96h)
                    if (isLastStage || elapsedHours >= 72.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Danger Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = Translation.getText("fasting_danger_title", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Translation.getText("fasting_danger_desc", lang),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translation.getText("edit", lang),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                showStopDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translation.getText("stop_fast", lang),
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
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

                    val rowModifier = if (isCurrent) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(stage.color.copy(alpha = 0.08f))
                            .border(1.5.dp, stage.color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    }

                    Row(
                        modifier = rowModifier.height(IntrinsicSize.Min),
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
                                        if (completed) stage.color
                                        else if (isCurrent) stage.color.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Default.Check else stage.icon,
                                    contentDescription = null,
                                    tint = if (completed) Color.White 
                                           else if (isCurrent) stage.color
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
                                            .background(stage.color)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (idx < fastingStages.lastIndex) (if (isCurrent) 8.dp else 20.dp) else (if (isCurrent) 4.dp else 0.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stage.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent || completed) FontWeight.Bold else FontWeight.Medium,
                                    color = if (completed) stage.color 
                                            else if (isCurrent) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = stage.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${(progressInStage * 100).toInt()}%",
                                            color = stage.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = Translation.getText(stage.descKey, lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progressInStage },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                                    color = stage.color,
                                    trackColor = stage.color.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                    if (isCurrent && idx < fastingStages.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
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
    val icon: ImageVector,
    val color: Color,
    val reliability: String
)

fun formatMilestoneTimeLabel(days: Double, lang: String): String {
    val daySuffix = when (lang) { "it" -> "g"; "es" -> "d"; "fr" -> "j"; else -> "d" }
    val hourSuffix = "h"
    val minSuffix = when (lang) { "it" -> "min"; "es" -> "min"; "fr" -> "min"; else -> "min" }
    val weekLabel = when (lang) { "it" -> "sett."; "es" -> "sem."; "fr" -> "sem."; else -> "wk" }
    val monthLabel = when (lang) { "it" -> "mesi"; "es" -> "meses"; "fr" -> "mois"; else -> "mo" }
    val singleMonthLabel = when (lang) { "it" -> "mese"; "es" -> "mes"; "fr" -> "mois"; else -> "mo" }
    val yearLabel = when (lang) { "it" -> "anni"; "es" -> "años"; "fr" -> "ans"; else -> "yrs" }
    val singleYearLabel = when (lang) { "it" -> "anno"; "es" -> "año"; "fr" -> "an"; else -> "yr" }

    return when {
        days < (1.0 / 24.0) -> "${(days * 24 * 60).toInt()} $minSuffix"
        days < 1.0 -> "${(days * 24).toInt()}$hourSuffix"
        days == 1.0 -> "24$hourSuffix"
        days < 7.0 -> "${days.toInt()}$daySuffix"
        days < 30.0 -> {
            val weeks = (days / 7.0).toInt()
            "$weeks $weekLabel"
        }
        days < 365.0 -> {
            val months = (days / 30.0).toInt()
            if (months == 1) "1 $singleMonthLabel" else "$months $monthLabel"
        }
        else -> {
            val years = (days / 365.0).toInt()
            if (years == 1) "1 $singleYearLabel" else "$years $yearLabel"
        }
    }
}

fun formatRemainingTime(daysRemaining: Double, lang: String): String {
    val hoursRemaining = daysRemaining * 24.0
    val daySuffix = when (lang) { "it" -> "g"; "es" -> "d"; "fr" -> "j"; else -> "d" }
    return when {
        hoursRemaining < 1.0 -> {
            val mins = (hoursRemaining * 60.0).toInt().coerceAtLeast(1)
            "$mins min"
        }
        hoursRemaining < 48.0 -> {
            val h = hoursRemaining.toInt()
            val m = ((hoursRemaining - h) * 60.0).toInt()
            if (m > 0) "${h}h ${m}m" else "${h}h"
        }
        else -> {
            val d = daysRemaining.toInt()
            val h = ((daysRemaining - d) * 24.0).toInt()
            if (h > 0) "$d$daySuffix ${h}h" else "$d$daySuffix"
        }
    }
}

@Composable
fun TimeItem(value: Long, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(60.dp)
    ) {
        Text(
            text = String.format("%02d", value),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 36.sp
        )
        Text(
            text = unit.uppercase(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
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
                Milestone(0.0139, Translation.getText("sm_1", lang), Translation.getText("sm_1_desc", lang), Icons.Default.Favorite, Color(0xFF43A047), "High"),
                Milestone(0.333, Translation.getText("sm_2", lang), Translation.getText("sm_2_desc", lang), Icons.Default.Air, Color(0xFF00ACC1), "High"),
                Milestone(0.5, Translation.getText("sm_3", lang), Translation.getText("sm_3_desc", lang), Icons.Default.Bloodtype, Color(0xFF1E88E5), "High"),
                Milestone(1.0, Translation.getText("sm_4", lang), Translation.getText("sm_4_desc", lang), Icons.Default.Healing, Color(0xFF3949AB), "Medium"),
                Milestone(2.0, Translation.getText("sm_5", lang), Translation.getText("sm_5_desc", lang), Icons.Default.SelfImprovement, Color(0xFFFBC02D), "Medium"),
                Milestone(3.0, Translation.getText("sm_6", lang), Translation.getText("sm_6_desc", lang), Icons.Default.Spa, Color(0xFFFB8C00), "High"),
                Milestone(7.0, Translation.getText("sm_7", lang), Translation.getText("sm_7_desc", lang), Icons.Default.Shield, Color(0xFFE53935), "High"),
                Milestone(14.0, Translation.getText("sm_8", lang), Translation.getText("sm_8_desc", lang), Icons.Default.DirectionsRun, Color(0xFF8E24AA), "High"),
                Milestone(30.0, Translation.getText("sm_9", lang), Translation.getText("sm_9_desc", lang), Icons.Default.LocalDrink, Color(0xFFD81B60), "Medium"),
                Milestone(90.0, Translation.getText("sm_10", lang), Translation.getText("sm_10_desc", lang), Icons.Default.FitnessCenter, Color(0xFF5E35B1), "High"),
                Milestone(180.0, Translation.getText("sm_11", lang), Translation.getText("sm_11_desc", lang), Icons.Default.HealthAndSafety, Color(0xFF00897B), "Medium"),
                Milestone(270.0, Translation.getText("sm_12", lang), Translation.getText("sm_12_desc", lang), Icons.Default.Psychology, Color(0xFF00ACC1), "Medium"),
                Milestone(365.0, Translation.getText("sm_13", lang), Translation.getText("sm_13_desc", lang), Icons.Default.WorkspacePremium, Color(0xFFF57C00), "High"),
                Milestone(1825.0, Translation.getText("sm_14", lang), Translation.getText("sm_14_desc", lang), Icons.Default.Celebration, Color(0xFF388E3C), "High"),
                Milestone(3650.0, Translation.getText("sm_15", lang), Translation.getText("sm_15_desc", lang), Icons.Default.Star, Color(0xFF1976D2), "High"),
                Milestone(5475.0, Translation.getText("sm_16", lang), Translation.getText("sm_16_desc", lang), Icons.Default.EmojiEvents, Color(0xFF7B1FA2), "High")
            )
        } else if (title.contains("Alcol") || title.contains("Alcohol")) {
            listOf(
                Milestone(0.0416, Translation.getText("al_1", lang), Translation.getText("al_1_desc", lang), Icons.Default.LocalDrink, Color(0xFF00ACC1), "High"),
                Milestone(0.25, Translation.getText("al_2", lang), Translation.getText("al_2_desc", lang), Icons.Default.WaterDrop, Color(0xFF1E88E5), "Medium"),
                Milestone(1.0, Translation.getText("al_3", lang), Translation.getText("al_3_desc", lang), Icons.Default.Healing, Color(0xFF43A047), "High"),
                Milestone(2.0, Translation.getText("al_4", lang), Translation.getText("al_4_desc", lang), Icons.Default.SelfImprovement, Color(0xFFFBC02D), "Medium"),
                Milestone(3.0, Translation.getText("al_5", lang), Translation.getText("al_5_desc", lang), Icons.Default.Bedtime, Color(0xFF8E24AA), "Medium"),
                Milestone(7.0, Translation.getText("al_6", lang), Translation.getText("al_6_desc", lang), Icons.Default.Spa, Color(0xFFFB8C00), "High"),
                Milestone(14.0, Translation.getText("al_7", lang), Translation.getText("al_7_desc", lang), Icons.Default.Shield, Color(0xFFE53935), "Medium"),
                Milestone(21.0, Translation.getText("al_8", lang), Translation.getText("al_8_desc", lang), Icons.Default.Favorite, Color(0xFFD81B60), "Medium"),
                Milestone(30.0, Translation.getText("al_9", lang), Translation.getText("al_9_desc", lang), Icons.Default.Face, Color(0xFF5E35B1), "High"),
                Milestone(90.0, Translation.getText("al_10", lang), Translation.getText("al_10_desc", lang), Icons.Default.HealthAndSafety, Color(0xFF00897B), "High"),
                Milestone(180.0, Translation.getText("al_11", lang), Translation.getText("al_11_desc", lang), Icons.Default.Psychology, Color(0xFF3949AB), "Medium"),
                Milestone(365.0, Translation.getText("al_12", lang), Translation.getText("al_12_desc", lang), Icons.Default.WorkspacePremium, Color(0xFFF57C00), "Medium"),
                Milestone(730.0, Translation.getText("al_13", lang), Translation.getText("al_13_desc", lang), Icons.Default.Celebration, Color(0xFF388E3C), "Medium"),
                Milestone(1825.0, Translation.getText("al_14", lang), Translation.getText("al_14_desc", lang), Icons.Default.EmojiEvents, Color(0xFF7B1FA2), "Medium")
            )
        } else {
            listOf(
                Milestone(0.0833, Translation.getText("su_1", lang), Translation.getText("su_1_desc", lang), Icons.Default.SelfImprovement, Color(0xFF00ACC1), "High"),
                Milestone(1.0, Translation.getText("su_2", lang), Translation.getText("su_2_desc", lang), Icons.Default.BatteryChargingFull, Color(0xFF1E88E5), "Medium"),
                Milestone(3.0, Translation.getText("su_3", lang), Translation.getText("su_3_desc", lang), Icons.Default.Shield, Color(0xFF43A047), "Low"),
                Milestone(7.0, Translation.getText("su_4", lang), Translation.getText("su_4_desc", lang), Icons.Default.Bolt, Color(0xFFFBC02D), "Medium"),
                Milestone(14.0, Translation.getText("su_5", lang), Translation.getText("su_5_desc", lang), Icons.Default.Restaurant, Color(0xFFFB8C00), "Medium"),
                Milestone(21.0, Translation.getText("su_6", lang), Translation.getText("su_6_desc", lang), Icons.Default.Spa, Color(0xFFE53935), "Low"),
                Milestone(30.0, Translation.getText("su_7", lang), Translation.getText("su_7_desc", lang), Icons.Default.Face, Color(0xFF8E24AA), "Low"),
                Milestone(60.0, Translation.getText("su_8", lang), Translation.getText("su_8_desc", lang), Icons.Default.Whatshot, Color(0xFFD81B60), "Medium"),
                Milestone(90.0, Translation.getText("su_9", lang), Translation.getText("su_9_desc", lang), Icons.Default.DirectionsRun, Color(0xFF5E35B1), "Medium"),
                Milestone(180.0, Translation.getText("su_10", lang), Translation.getText("su_10_desc", lang), Icons.Default.HealthAndSafety, Color(0xFF00897B), "Medium"),
                Milestone(365.0, Translation.getText("su_11", lang), Translation.getText("su_11_desc", lang), Icons.Default.WorkspacePremium, Color(0xFFF57C00), "Medium"),
                Milestone(730.0, Translation.getText("su_12", lang), Translation.getText("su_12_desc", lang), Icons.Default.Celebration, Color(0xFF388E3C), "Low"),
                Milestone(1095.0, Translation.getText("su_13", lang), Translation.getText("su_13_desc", lang), Icons.Default.EmojiEvents, Color(0xFF7B1FA2), "Medium")
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
    val elapsedDays = if (isFuture) -1.0 else elapsedMs.toDouble() / (24.0 * 3600000.0)

    val nextMilestoneIndex = if (isFuture) -1 else milestones.indexOfFirst { elapsedDays < it.days }

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
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

                // Time counters and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val d = if (isFuture) daysToStart else days
                    val h = if (isFuture) hoursToStart else hours
                    val m = if (isFuture) minutesToStart else minutes
                    val s = if (isFuture) secondsToStart else seconds

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeItem(value = d, unit = Translation.getText("days", lang))
                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
                        TimeItem(value = h, unit = "h")
                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
                        TimeItem(value = m, unit = "m")
                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
                        TimeItem(value = s, unit = "s")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Fixed side action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledIconButton(
                            onClick = {
                                showDateTimePicker(context, startTime) { newTime ->
                                    onStart(newTime)
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = Translation.getText("edit_start_time", lang),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = { showStopConfirmation = true },
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = Translation.getText("stop_journey", lang),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (isSmoking) {
                    Spacer(modifier = Modifier.height(20.dp))
                    var showEditCostDialog by remember { mutableStateOf(false) }
                    var costInput by remember { mutableStateOf(dailyCost.toString()) }

                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            costInput = dailyCost.toString()
                            showEditCostDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = Translation.getText("saved_money", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("€ %.2f", savedMoney),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showEditCostDialog) {
                        AlertDialog(
                            onDismissRequest = { showEditCostDialog = false },
                            title = {
                                Text(
                                    text = Translation.getText("daily_expense", lang),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = costInput,
                                        onValueChange = { costInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        costInput.toDoubleOrNull()?.let {
                                            onUpdateSmokingCost(it)
                                        }
                                        showEditCostDialog = false
                                    }
                                ) {
                                    Text(Translation.getText("save", lang))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditCostDialog = false }) {
                                    Text(Translation.getText("cancel", lang))
                                }
                            }
                        )
                    }
                }

                // Attractive active milestone progress box
                if (!isFuture) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (nextMilestoneIndex != -1) {
                        val targetMilestone = milestones[nextMilestoneIndex]
                        val prevMilestone = if (nextMilestoneIndex > 0) milestones[nextMilestoneIndex - 1] else null
                        val prevDays = prevMilestone?.days ?: 0.0
                        val stageDuration = targetMilestone.days - prevDays
                        val progressPercent = if (stageDuration > 0) {
                            (((elapsedDays - prevDays) / stageDuration).coerceIn(0.0, 1.0) * 100).toInt()
                        } else 100
                        val remainingDays = (targetMilestone.days - elapsedDays).coerceAtLeast(0.0)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, targetMilestone.color.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = targetMilestone.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = targetMilestone.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = formatMilestoneTimeLabel(targetMilestone.days, lang),
                                            color = targetMilestone.color,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "${Translation.getText("time_remaining", lang)}: ${formatRemainingTime(remainingDays, lang)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Row: start icon -> LinearProgressIndicator -> target icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background((prevMilestone?.color ?: icon.let { MaterialTheme.colorScheme.primary }).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = prevMilestone?.icon ?: icon,
                                            contentDescription = null,
                                            tint = prevMilestone?.color ?: MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        LinearProgressIndicator(
                                            progress = { progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = targetMilestone.color,
                                            trackColor = targetMilestone.color.copy(alpha = 0.25f)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(targetMilestone.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = targetMilestone.icon,
                                            contentDescription = null,
                                            tint = targetMilestone.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prevMilestone?.let { formatMilestoneTimeLabel(it.days, lang) } ?: "0",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$progressPercent%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatMilestoneTimeLabel(targetMilestone.days, lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        // All milestones achieved banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Translation.getText("all_milestones_achieved", lang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val format = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                val label = if (isFuture) {
                    Translation.getText("scheduled_start_on", lang)
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

        // Milestones list with visible step durations, evidence levels, and attractive progress
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

                milestones.forEachIndexed { idx, milestone ->
                    val completed = !isFuture && elapsedDays >= milestone.days
                    val isCurrent = !isFuture && idx == nextMilestoneIndex
                    val prevDays = if (idx == 0) 0.0 else milestones[idx - 1].days
                    val stageDuration = milestone.days - prevDays
                    val progressInStage = if (stageDuration > 0) {
                        ((elapsedDays - prevDays) / stageDuration).coerceIn(0.0, 1.0).toFloat()
                    } else 1f

                    val rowModifier = if (isCurrent) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(milestone.color.copy(alpha = 0.08f))
                            .border(1.5.dp, milestone.color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    }

                    Row(
                        modifier = rowModifier.height(IntrinsicSize.Min),
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
                                        if (completed) milestone.color
                                        else if (isCurrent) milestone.color.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Default.Check else milestone.icon,
                                    contentDescription = null,
                                    tint = if (completed) Color.White
                                           else if (isCurrent) milestone.color
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (idx < milestones.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(3.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    if (completed) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(milestone.color)
                                        )
                                    } else if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(progressInStage)
                                                .background(milestone.color)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (idx < milestones.lastIndex) (if (isCurrent) 8.dp else 18.dp) else (if (isCurrent) 4.dp else 0.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = milestone.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent || completed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (completed) milestone.color
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (completed || isCurrent) milestone.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = formatMilestoneTimeLabel(milestone.days, lang),
                                        color = if (completed || isCurrent) milestone.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = milestone.desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                if (isCurrent) {
                                    val remDays = (milestone.days - elapsedDays).coerceAtLeast(0.0)
                                    Text(
                                        text = "${Translation.getText("time_remaining", lang)}: ${formatRemainingTime(remDays, lang)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = milestone.color
                                    )
                                }
                            }

                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progressInStage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = milestone.color,
                                    trackColor = milestone.color.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    if (isCurrent && idx < milestones.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
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
                    Text(Translation.getText("avg_fast", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                    Text(Translation.getText("longest_fast", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                    text = Translation.getText("fasting_weight_correlation", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translation.getText("fasting_weight_correlation_desc", lang),
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
                        Text(Translation.getText("no_correlation_data", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
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
                    text = Translation.getText("fasting_history_logs", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (fastingSessions.isNotEmpty()) {
                    val sdf = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
                    val hrsSuffix = Translation.getText("hours", lang)
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
                                    text = String.format("%.1f %s", durationHrs, hrsSuffix),
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
                                        Icon(Icons.Default.Edit, contentDescription = Translation.getText("edit", lang), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                        Icon(Icons.Default.Delete, contentDescription = Translation.getText("delete", lang), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
                        text = Translation.getText("no_fasting_logs", lang),
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
                    text = Translation.getText("weight_history_logs", lang),
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
                                        Icon(Icons.Default.Edit, contentDescription = Translation.getText("edit", lang), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                        Icon(Icons.Default.Delete, contentDescription = Translation.getText("delete", lang), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
                        text = Translation.getText("no_weight_logs", lang),
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
            title = { Text(Translation.getText("edit_fasting_session", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Translation.getText("start_date_time", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

                    Text(Translation.getText("end_date_time", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

                    Text(Translation.getText("weight_optional_title", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        0 to Translation.getText("theme_system", lang),
                        1 to Translation.getText("theme_light", lang),
                        2 to Translation.getText("theme_dark", lang)
                    )
                    themes.forEach { pair ->
                        val selected = themeMode == pair.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onThemeModeSelected(pair.first) }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.second,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
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
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Brombolo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = Translation.getText("created_by", lang),
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
                        text = Translation.getText("evidence_legend_title", lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LegendItem(label = Translation.getText("rel_high", lang), color = Color(0xFF2E7D32), desc = Translation.getText("evidence_high_desc", lang))
                Spacer(modifier = Modifier.height(8.dp))
                LegendItem(label = Translation.getText("rel_medium", lang), color = Color(0xFFE65100), desc = Translation.getText("evidence_medium_desc", lang))
                Spacer(modifier = Modifier.height(8.dp))
                LegendItem(label = Translation.getText("rel_low", lang), color = Color(0xFF1565C0), desc = Translation.getText("evidence_low_desc", lang))
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
