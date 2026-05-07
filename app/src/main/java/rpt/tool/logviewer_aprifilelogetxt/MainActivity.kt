package rpt.tool.logviewer_aprifilelogetxt

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import rpt.tool.logviewer_aprifilelogetxt.ui.log.LogViewModel
import rpt.tool.logviewer_aprifilelogetxt.ui.theme.LogViewerAppTheme
import rpt.tool.logviewer_aprifilelogetxt.utils.Prefs
import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType
import java.util.regex.Pattern
import androidx.core.net.toUri

data class AppColors(
    val bg: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val info: Color,
    val debug: Color,
    val warn: Color,
    val error: Color,
    val highlight: Color = Color(0xFF0055FF)
)

val DarkColors = AppColors(
    bg = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    textPrimary = Color.LightGray,
    textSecondary = Color.Gray,
    info = Color(0xFF69F0AE),
    debug = Color(0xFF448AFF),
    warn = Color(0xFFFFD740),
    error = Color(0xFFFF5252)
)

val LightColors = AppColors(
    bg = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color.DarkGray,
    textSecondary = Color.Gray,
    info = Color(0xFF00C853),
    debug = Color(0xFF2962FF),
    warn = Color(0xFFFF8F00),
    error = Color(0xFFD50000)
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri: Uri? = intent?.data ?: when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }

        setContent {
            LogViewerAppTheme {
                val vm: LogViewModel = viewModel()
                LogViewerRoot(vm, uri)
            }
        }
    }
}

@Composable
fun LogViewerRoot(vm: LogViewModel, uri: Uri?) {
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val fileToLoad = uri ?: Prefs.getLastUri(context)
            fileToLoad?.let { vm.loadFile(context, it) }
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (vm.themeMode) {
        1 -> false
        2 -> true 
        else -> isSystemDark
    }
    val colors = if (isDarkTheme) DarkColors else LightColors

    LaunchedEffect(Unit) {
        vm.init(context)
        val fileToLoad = uri ?: Prefs.getLastUri(context)
        
        val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Request All Files Access on Android 11+
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            fileToLoad?.let { vm.loadFile(context, it) }
        }
    }

    Surface(color = colors.bg, modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalAppColors provides colors) {
            when {
                vm.showSplash -> SplashScreen()
                vm.showOnboarding -> OnboardingScreen(onFinish = { vm.completeOnboarding(context) })
                else -> LogViewerApp(vm, isDarkTheme)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val colors = LocalAppColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, 
                tint = colors.debug, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.log_viewer_pro),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val colors = LocalAppColors.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val icon = when (page) {
                    0 -> Icons.Default.FindInPage
                    1 -> Icons.Default.Star
                    else -> Icons.Default.Share
                }
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, 
                        tint = colors.debug, modifier = Modifier.size(64.dp))
                }

                Spacer(Modifier.height(32.dp))

                val titleRes = when (page) {
                    0 -> R.string.welcome_title
                    1 -> R.string.onboarding_title_2
                    else -> R.string.onboarding_title_3
                }
                val descRes = when (page) {
                    0 -> R.string.onboarding_desc_1
                    1 -> R.string.onboarding_desc_2
                    else -> R.string.onboarding_desc_3
                }

                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) colors.debug 
                    else colors.textSecondary.copy(alpha = 0.3f)
                    val width = if (pagerState.currentPage == iteration) 24.dp else 10.dp
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(10.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.debug),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = stringResource(if (pagerState.currentPage < 2) R.string.next_btn else R.string.start_btn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerApp(vm: LogViewModel, isDarkTheme: Boolean) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let { vm.loadFile(context, it) } }

    Scaffold(
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(colors.bg)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.log_viewer_pro), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { /* Menu Action */ }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_content_description))
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.toggleSearchActive() }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_content_description))
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options_content_description))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            Text(stringResource(R.string.export_label), fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(16.dp, 8.dp))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_all), color = colors.textSecondary) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_fav), color = colors.textSecondary) },
                                onClick = { showMenu = false }
                            )

                            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))

                            Text(stringResource(R.string.share_label), fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(16.dp, 8.dp))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_all), color = colors.textSecondary) },
                                onClick = {
                                    showMenu = false
                                    shareText(context, vm.getFilteredText(false))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_fav), color = colors.textSecondary) },
                                onClick = {
                                    showMenu = false
                                    shareText(context, vm.getFilteredText(true))
                                }
                            )

                            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))

                            Text(stringResource(R.string.appearance_label), fontWeight = FontWeight.Bold, color = colors.textPrimary,
                                modifier = Modifier.padding(16.dp, 8.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(if (isDarkTheme) R.string.theme_light else R.string.theme_dark),
                                        color = colors.textSecondary
                                    )
                                },
                                onClick = {
                                    vm.toggleTheme(context, isDarkTheme)
                                    showMenu = false
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.textPrimary,
                        actionIconContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textPrimary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = vm.fileName, color = colors.textPrimary, fontSize = 16.sp,
                            fontWeight = FontWeight.Medium)
                        Text(text = vm.fileDetails, color = colors.textSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, 
                        tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }

                TabRow(
                    selectedTabIndex = vm.currentTab,
                    containerColor = colors.bg,
                    contentColor = colors.debug,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(
                                tabPositions[vm.currentTab]),
                            color = colors.debug
                        )
                    }
                ) {
                    Tab(
                        selected = vm.currentTab == 0,
                        onClick = { vm.setTab(0) },
                        text = { Text(stringResource(R.string.all_tab), fontWeight = FontWeight.SemiBold, color = 
                            if(vm.currentTab == 0) colors.debug else colors.textSecondary) }
                    )
                    Tab(
                        selected = vm.currentTab == 1,
                        onClick = { vm.setTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null,
                                    Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.bookmarks_tab), fontWeight = FontWeight.SemiBold, color =
                                    if(vm.currentTab == 1) colors.debug else colors.textSecondary)
                            }
                        }
                    )
                }

                if (vm.isSearchActive) {
                    OutlinedTextField(
                        value = vm.searchQuery,
                        onValueChange = { vm.search(it) },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.debug,
                            unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.5f),
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        trailingIcon = {
                            Row(Modifier.padding(end = 8.dp)) {
                                Text(
                                    stringResource(R.string.regex_label),
                                    fontWeight = FontWeight.Bold,
                                    color = if (vm.isRegexMode) colors.debug else 
                                        colors.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            vm.isRegexMode = !vm.isRegexMode
                                            vm.search(vm.searchQuery)
                                        }
                                        .background(if (vm.isRegexMode) 
                                            colors.debug.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(8.dp)
                                )
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch(arrayOf("*/*")) },
                containerColor = colors.debug,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { p ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .background(colors.bg)
        ) {
            itemsIndexed(vm.displayedLines) { i, line ->
                if (i > vm.displayedLines.size - 10) vm.loadNextPage()

                LogLineItem(
                    line = line,
                    query = vm.searchQuery,
                    regex = vm.isRegexMode,
                    bookmarked = vm.isBookmarked(line.id),
                    isBookmarkTab = vm.currentTab == 1
                ) { vm.toggleBookmark(context, line.id) }

                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f))
            }
        }
    }
}

fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
}

@Composable
fun LogLineItem(
    line: LogLine,
    query: String,
    regex: Boolean,
    bookmarked: Boolean,
    isBookmarkTab: Boolean,
    onBookmark: () -> Unit
) {
    val colors = LocalAppColors.current

    val levelColor = when (line.type) {
        LogType.ERROR -> colors.error
        LogType.WARNING -> colors.warn
        LogType.INFO -> colors.info
        else -> colors.debug
    }

    val annotatedString = buildAnnotatedString {
        val t = line.text

        if (query.isNotEmpty()) {
            try {
                val r = if (regex) Pattern.compile(query)
                else Pattern.compile(Pattern.quote(query), 
                    Pattern.CASE_INSENSITIVE)
                val m = r.matcher(t)
                var last = 0
                while (m.find()) {
                    append(t.substring(last, m.start()))
                    withStyle(SpanStyle(background = colors.highlight, color = Color.White)) {
                        append(t.substring(m.start(), m.end()))
                    }
                    last = m.end()
                }
                append(t.substring(last))
            } catch (_: Exception) { append(t) }
        } else {
            append(t)
        }

        val logTypes = listOf("INFO", "ERROR", "WARN", "DEBUG", "WARNING")
        val currentText = this.toAnnotatedString().text
        logTypes.forEach { typeLabel ->
            var startIndex = currentText.indexOf(typeLabel)
            while (startIndex != -1) {
                val end = startIndex + typeLabel.length
                val colorToUse = when(typeLabel) {
                    "INFO" -> colors.info
                    "ERROR" -> colors.error
                    "WARN", "WARNING" -> colors.warn
                    else -> colors.debug
                }
                addStyle(style = SpanStyle(color = colorToUse, fontWeight = FontWeight.Bold), 
                    start = startIndex, end = end)
                startIndex = currentText.indexOf(typeLabel, end)
            }
        }
    }

    Surface(
        onClick = onBookmark,
        color = if (bookmarked) colors.warn.copy(alpha = 0.05f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (bookmarked) colors.warn else 
                        levelColor.copy(alpha = 0.5f))
            )

            if (isBookmarkTab) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = annotatedString,
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = if (bookmarked) colors.warn else 
                            colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = annotatedString,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp)
                    )

                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = if (bookmarked) colors.warn else 
                            colors.textSecondary.copy(alpha = 0.1f),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}