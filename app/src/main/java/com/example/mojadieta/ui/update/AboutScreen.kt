package com.example.mojadieta.ui.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mojadieta.R
import com.example.mojadieta.domain.update.AppRelease
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: UpdateViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("O aplikacji") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppHeaderCard(version = state.currentVersion)

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Aktualizacje", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    UpdateStatusContent(
                        status = state.status,
                        onCheck = viewModel::checkNow,
                        onDownload = viewModel::download,
                        onCancel = viewModel::cancelDownload,
                        onInstall = { viewModel.install(context) }
                    )
                    if (state.needsInstallPermission) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Zezwól aplikacji Moja Dieta na instalowanie aplikacji w ustawieniach, " +
                                "które właśnie się otworzyły, a potem wróć tutaj.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Powiadamiaj o nowych wersjach", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Sprawdzanie w tle co 12 godzin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled
                        )
                    }
                }
            }

            DataSourcesCard()

            Text(
                "Aktualizacje są pobierane z publicznego repozytorium aplikacji na GitHubie. " +
                    "Wszystkie Twoje dane (dziennik, przepisy, notatki) zostają na telefonie i nie są usuwane podczas aktualizacji.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppHeaderCard(version: String) {
    Card(shape = MaterialTheme.shapes.large) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1F9D74), Color(0xFF0A5C42))))
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(150.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Moja Dieta", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "by Tala",
                    color = Color(0xFFCDF7E5),
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Wersja $version",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusContent(
    status: UpdateStatus,
    onCheck: () -> Unit,
    onDownload: (AppRelease) -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    when (status) {
        UpdateStatus.Idle -> {
            StatusLine(icon = null, text = "Sprawdź, czy jest dostępna nowsza wersja aplikacji.")
            Spacer(Modifier.height(12.dp))
            CheckButton(onCheck)
        }

        UpdateStatus.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text("Sprawdzanie aktualizacji…", style = MaterialTheme.typography.bodyLarge)
        }

        UpdateStatus.UpToDate -> {
            StatusLine(
                icon = { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                text = "Masz najnowszą wersję aplikacji."
            )
            Spacer(Modifier.height(12.dp))
            CheckButton(onCheck)
        }

        is UpdateStatus.Available -> {
            StatusLine(
                icon = { Icon(Icons.Outlined.CloudDownload, null, tint = MaterialTheme.colorScheme.primary) },
                text = "Dostępna jest nowa wersja ${status.release.versionName}" +
                    (if (status.release.apkSizeBytes > 0) " (${formatSize(status.release.apkSizeBytes)})" else "")
            )
            ReleaseNotes(status.release)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onDownload(status.release) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pobierz i zainstaluj")
            }
        }

        is UpdateStatus.Downloading -> {
            Text(
                "Pobieranie wersji ${status.release.versionName}… ${(status.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("Anuluj") }
        }

        is UpdateStatus.ReadyToInstall -> {
            StatusLine(
                icon = { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                text = "Wersja ${status.release.versionName} jest pobrana i gotowa do instalacji."
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.InstallMobile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zainstaluj teraz")
            }
        }

        is UpdateStatus.Error -> {
            StatusLine(
                icon = { Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                text = status.message
            )
            Spacer(Modifier.height(12.dp))
            val release = status.release
            if (release != null) {
                Button(onClick = { onDownload(release) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Spróbuj ponownie")
                }
            } else {
                CheckButton(onCheck)
            }
        }
    }
}

@Composable
private fun StatusLine(icon: (@Composable () -> Unit)?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CheckButton(onCheck: () -> Unit) {
    OutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Sprawdź aktualizacje")
    }
}

@Composable
private fun ReleaseNotes(release: AppRelease) {
    if (release.notes.isBlank()) return
    Spacer(Modifier.height(10.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Co nowego:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(release.notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Okno pokazywane po uruchomieniu aplikacji, gdy na GitHubie jest nowsza wersja. */
@Composable
fun UpdatePromptDialog(release: AppRelease, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
        title = { Text("Dostępna nowa wersja", textAlign = TextAlign.Center) },
        text = {
            Column {
                Text("Wersja ${release.versionName} aplikacji Moja Dieta jest gotowa do pobrania.")
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Co nowego:", style = MaterialTheme.typography.labelLarge)
                    Text(release.notes, style = MaterialTheme.typography.bodyMedium, maxLines = 8)
                }
            }
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text("Aktualizuj") } },
        dismissButton = { TextButton(onClick = onLater) { Text("Później") } }
    )
}

private fun formatSize(bytes: Long): String =
    String.format(Locale.forLanguageTag("pl-PL"), "%.1f MB", bytes / 1_048_576.0)

/** Źródła danych i wymagane przypisanie autorstwa (Open Food Facts, licencja ODbL). */
@Composable
private fun DataSourcesCard() {
    val uriHandler = LocalUriHandler.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Źródła danych", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Open Food Facts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Dane produktów ze skanera kodów kreskowych i wyszukiwania w internecie pochodzą z otwartej, " +
                            "tworzonej przez społeczność bazy Open Food Facts, udostępnianej na licencji " +
                            "Open Database License (ODbL). Wartości mogą zawierać błędy – w razie wątpliwości sprawdź etykietę.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = { uriHandler.openUri("https://world.openfoodfacts.org") }) {
                            Text("openfoodfacts.org")
                        }
                        TextButton(onClick = { uriHandler.openUri("https://opendatacommons.org/licenses/odbl/1-0/") }) {
                            Text("Licencja ODbL")
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Wbudowana baza produktów zawiera orientacyjne wartości odżywcze na 100 g dla popularnych produktów. " +
                        "Kalkulator celu korzysta ze wzoru Mifflina-St Jeora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Biblioteki i usługi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Aplikacja korzysta z bibliotek open source na licencji Apache License 2.0: " +
                            OPEN_SOURCE_LIBRARIES.joinToString(", ") + ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Skaner kodów kreskowych i rozpoznawanie tekstu działają dzięki Google ML Kit " +
                            "(przetwarzanie na telefonie, zgodnie z warunkami ML Kit).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") }) {
                            Text("Apache 2.0")
                        }
                        TextButton(onClick = { uriHandler.openUri("https://developers.google.com/ml-kit/terms") }) {
                            Text("Warunki ML Kit")
                        }
                    }
                }
            }
        }
    }
}

private val OPEN_SOURCE_LIBRARIES = listOf(
    "Kotlin i Kotlin Coroutines (JetBrains)",
    "Android Jetpack: Compose, Material 3, Material Icons, Navigation, Lifecycle, Room, WorkManager, " +
        "CameraX, Glance, Health Connect, Core, Activity, ExifInterface (Google / AOSP)",
    "Coil (Coil Contributors)"
)
