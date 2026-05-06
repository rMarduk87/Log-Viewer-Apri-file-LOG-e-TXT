package rpt.tool.logviewer_aprifilelogetxt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import rpt.tool.logviewer_aprifilelogetxt.ui.log.LogViewModel
import rpt.tool.logviewer_aprifilelogetxt.ui.theme.LogViewerAppTheme
import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType
import java.util.regex.Pattern

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

    LaunchedEffect(Unit) {
        vm.init(context)
        uri?.let { vm.loadFile(context, it) }
    }

    when {
        vm.showSplash -> SplashScreen()
        vm.showOnboarding -> OnboardingScreen(onFinish = { vm.completeOnboarding(context) })
        else -> LogViewerApp(vm)
    }
}

@Composable
fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.log_viewer_pro),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.welcome_text), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.desc_text))
        Spacer(Modifier.height(24.dp))

        Button(onClick = onFinish) {
            Text(stringResource(R.string.start_btn))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerApp(vm: LogViewModel) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let { vm.loadFile(context, it) } }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.log_viewer_pro)) })
                SecondaryTabRow(selectedTabIndex = vm.currentTab) {
                    Tab(selected = vm.currentTab == 0, onClick = { vm.setTab(0) }) {
                        Box(Modifier.padding(16.dp)) { Text(stringResource(R.string.all_tab)) }
                    }
                    Tab(selected = vm.currentTab == 1, onClick = { vm.setTab(1) }) {
                        Box(Modifier.padding(16.dp)) { Text(stringResource(R.string.bookmarks_tab)) }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { p ->

        Column(
            Modifier
                .padding(p)
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.open_btn))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val text = vm.getFilteredText()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_btn)))
                }) {
                    Text(stringResource(R.string.share_btn))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.errors_label, vm.errorCount),
                    color = Color.Red
                )
            }

            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.search(it) },
                label = {
                    Text(
                        if (vm.isRegexMode) stringResource(R.string.regex_hint)
                        else stringResource(R.string.search_hint)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        vm.isRegexMode = !vm.isRegexMode
                        vm.search(vm.searchQuery)
                    }) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = if (vm.isRegexMode) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                itemsIndexed(vm.displayedLines) { i, line ->
                    if (i > vm.displayedLines.size - 10) vm.loadNextPage()

                    LogLineItem(
                        line,
                        vm.searchQuery,
                        vm.isRegexMode,
                        vm.isBookmarked(line.id)
                    ) { vm.toggleBookmark(context, line.id) }
                }
            }
        }
    }
}

@Composable
fun LogLineItem(
    line: LogLine,
    query: String,
    regex: Boolean,
    bookmarked: Boolean,
    onBookmark: () -> Unit
) {
    val color = when (line.type) {
        LogType.ERROR -> Color.Red
        LogType.WARNING -> Color.Yellow
        LogType.INFO -> Color.Green
        else -> Color.White
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            buildAnnotatedString {
                val t = line.text
                if (query.isEmpty()) append(t)
                else try {
                    val r = if (regex) Pattern.compile(query)
                    else Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
                    val m = r.matcher(t)
                    var last = 0
                    while (m.find()) {
                        append(t.substring(last, m.start()))
                        withStyle(SpanStyle(background = Color(0xFF2962FF))) {
                            append(t.substring(m.start(), m.end()))
                        }
                        last = m.end()
                    }
                    append(t.substring(last))
                } catch (_: Exception) {
                    append(t)
                }
            },
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onBookmark) {
            Icon(
                Icons.Default.Star,
                null,
                tint = if (bookmarked) Color.Yellow else Color.Gray
            )
        }
    }
}
