package com.dailerx.app

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.UUID

private const val PREFS = "dailer_x_local"
private const val DEFAULT_BACKEND = "http://10.0.2.2:8000"
private val Gold = DailerPalette.Gold
private val Ivory = DailerPalette.Ivory
private val Charcoal = DailerPalette.Charcoal

private data class SavedNumber(
    val name: String,
    val number: String,
    val note: String = "",
    val tags: List<String> = emptyList()
)

private data class SavedLink(
    val title: String,
    val url: String,
    val note: String = "",
    val folder: String = "General"
)

private data class BusinessResult(
    val name: String,
    val number: String,
    val category: String,
    val source: String,
    val verified: Boolean
)

private data class MessageTemplate(val title: String, val body: String)

private val MessageTemplates = listOf(
    MessageTemplate("Quick introduction", "Hello, this is [your name]. I am reaching out regarding your call."),
    MessageTemplate("Call me back", "Hi, I missed your call. Please let me know when it is convenient to speak."),
    MessageTemplate("Delivery follow-up", "Hello, I am contacting you about a delivery. Please share a convenient time to connect."),
    MessageTemplate("Appointment confirmation", "Hello, I am confirming our appointment. Please let me know if anything changes.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        DailerXNotifications.createChannels(this)
        setContent { DailerXApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailerXApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val installationToken = remember { prefs.getString("install_token", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("install_token", it).apply() } }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    val numbersType = object : TypeToken<MutableList<SavedNumber>>() {}.type
    val linksType = object : TypeToken<MutableList<SavedLink>>() {}.type
    var saved by remember {
        mutableStateOf((runCatching {
            gson.fromJson<MutableList<SavedNumber>>(prefs.getString("saved", "[]"), numbersType)
        }.getOrNull() ?: mutableListOf()).toList())
    }
    var links by remember {
        mutableStateOf((runCatching {
            gson.fromJson<MutableList<SavedLink>>(prefs.getString("links", "[]"), linksType)
        }.getOrNull() ?: mutableListOf()).toList())
    }
    val templatesType = object : TypeToken<MutableList<MessageTemplate>>() {}.type
    var messageTemplates by remember {
        mutableStateOf((runCatching {
            gson.fromJson<MutableList<MessageTemplate>>(prefs.getString("templates", null), templatesType)
        }.getOrNull() ?: MessageTemplates.toMutableList()).toList())
    }
    var history by remember {
        mutableStateOf((prefs.getString("history", "") ?: "").split("\n").filter { it.isNotBlank() }.distinct().take(25))
    }
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "System") ?: "System") }
    var textSize by remember { mutableStateOf(prefs.getString("text_size", "Default") ?: "Default") }
    var backgroundStyle by remember { mutableStateOf(prefs.getString("background_style", "Ivory paper") ?: "Ivory paper") }
    var customBackground by remember { mutableStateOf(prefs.getString("background_image", "") ?: "") }
    var backendUrl by remember { mutableStateOf(prefs.getString("backend", DEFAULT_BACKEND) ?: DEFAULT_BACKEND) }
    var query by remember { mutableStateOf("") }
    var activePage by remember { mutableStateOf("Search") }
    var searchBusy by remember { mutableStateOf(false) }
    var searchNotice by remember { mutableStateOf("") }
    var remoteResults by remember { mutableStateOf(emptyList<BusinessResult>()) }
    var backendStatus by remember { mutableStateOf("Not checked") }
    var allowHistory by remember { mutableStateOf(prefs.getBoolean("keep_history", true)) }
    var autoBlockSpam by remember { mutableStateOf(prefs.getBoolean("auto_block_spam", false)) }
    var blockedNumbers by remember { mutableStateOf(prefs.getString("blocked_numbers", "") ?: "") }
    var allowedNumbers by remember { mutableStateOf(prefs.getString("allowed_numbers", "") ?: "") }
    var phoneRegion by remember { mutableStateOf(prefs.getString("phone_region", java.util.Locale.getDefault().country.ifBlank { "US" }) ?: "US") }
    var ringtoneStatus by remember { mutableStateOf("System ringtone unchanged") }
    var notificationsStatus by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "Notifications enabled or permission not required" else "Notification permission not granted")
    }
    var pendingRingtone by remember { mutableStateOf<Uri?>(null) }
    var saveDialog by remember { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }
    var templateDialog by remember { mutableStateOf(false) }
    var clearAllDialog by remember { mutableStateOf(false) }
    var editNumber by remember { mutableStateOf<SavedNumber?>(null) }
    var saveName by remember { mutableStateOf("") }
    var saveNumber by remember { mutableStateOf("") }
    var saveNote by remember { mutableStateOf("") }
    var saveTags by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var linkNote by remember { mutableStateOf("") }
    var editLink by remember { mutableStateOf<SavedLink?>(null) }
    var editTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
    var templateTitle by remember { mutableStateOf("") }
    var templateBody by remember { mutableStateOf("") }
    var backgroundNotice by remember { mutableStateOf("") }
    var spamNumber by remember { mutableStateOf("") }
    var spamCategory by remember { mutableStateOf("suspected_fraud") }
    var spamDetails by remember { mutableStateOf("") }
    var spamNotice by remember { mutableStateOf("") }
    var spamBusy by remember { mutableStateOf(false) }
    var messageTarget by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(messageTemplates.firstOrNull() ?: MessageTemplates.first()) }
    var dataConsent by remember { mutableStateOf(prefs.getBoolean("community_consent", false)) }
    var callRoleStatus by remember { mutableStateOf(readCallScreeningRole(context)) }

    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val target = File(context.filesDir, "dailerx-custom-background.jpg")
                if (customBackground.isNotBlank() && customBackground != target.absolutePath) runCatching { File(customBackground).delete() }
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not open the selected image." }.use { stream -> target.outputStream().use { output -> stream.copyTo(output) } }
                }
                customBackground = target.absolutePath
                prefs.edit().putString("background_image", customBackground).apply()
                backgroundNotice = "Custom background saved on this device."
            }.onFailure { backgroundNotice = "Unable to save that image: ${it.message ?: "unknown error"}" }
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        callRoleStatus = readCallScreeningRole(context)
    }
    val writeSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val picked = pendingRingtone
        if (Settings.System.canWrite(context) && picked != null) {
            runCatching {
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, picked)
                ringtoneStatus = "Device call ringtone updated"
            }.onFailure { ringtoneStatus = "Could not change ringtone: ${it.message ?: "permission denied"}" }
        } else if (picked != null) {
            ringtoneStatus = "Allow Dailer X to modify system settings, then try again"
        }
        pendingRingtone = null
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (picked != null) {
                if (Settings.System.canWrite(context)) {
                    runCatching {
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, picked)
                        ringtoneStatus = "Device call ringtone updated"
                    }.onFailure { ringtoneStatus = "Could not change ringtone: ${it.message ?: "permission denied"}" }
                } else {
                    pendingRingtone = picked
                    writeSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
            } else ringtoneStatus = "No ringtone selected"
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsStatus = if (granted) "Notifications enabled" else "Notifications denied; caller alerts may be hidden"
    }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkUi = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }
    val hostActivity = context as? Activity
    SideEffect {
        hostActivity?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkUi
                isAppearanceLightNavigationBars = !darkUi
            }
        }
    }
    val background = when (backgroundStyle) {
        "Charcoal" -> DailerPalette.Charcoal
        "Onyx" -> DailerPalette.Onyx
        "Gold haze" -> if (darkUi) Color(0xFF28241A) else Color(0xFFEDE2C7)
        else -> if (darkUi) DailerPalette.Onyx else DailerPalette.Ivory
    }
    val colors = dailerColorScheme(darkUi, background)
    val customBitmap = remember(customBackground) {
        runCatching { if (customBackground.isNotBlank()) BitmapFactory.decodeFile(customBackground)?.asImageBitmap() else null }.getOrNull()
    }

    fun persistNumbers() = prefs.edit().putString("saved", gson.toJson(saved)).apply()
    fun persistLinks() = prefs.edit().putString("links", gson.toJson(links)).apply()
    fun persistTemplates() = prefs.edit().putString("templates", gson.toJson(messageTemplates)).apply()
    fun setPage(page: String) {
        activePage = page
        scope.launch { drawerState.close() }
    }

    fun saveSearch(term: String) {
        if (!allowHistory || term.isBlank()) return
        history = (listOf(term.trim()) + history).distinct().take(25)
        prefs.edit().putString("history", history.joinToString("\n")).apply()
    }

    fun performSearch(term: String = query) {
        if (term.isBlank()) {
            searchNotice = "Enter a name, business, or phone number to search."
            remoteResults = emptyList()
            return
        }
        query = term.trim()
        saveSearch(query)
        searchBusy = true
        searchNotice = "Searching the configured public business directory…"
        remoteResults = emptyList()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val request = Request.Builder().url("${backendUrl.trimEnd('/')}/v1/search?q=$encoded").get().build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Server returned HTTP ${response.code}")
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val arr = json.optJSONArray("results")
                        val matches = if (arr == null) emptyList() else (0 until arr.length()).map { i ->
                            val item = arr.getJSONObject(i)
                            BusinessResult(
                                name = item.optString("name", "Unknown business"),
                                number = item.optString("number", ""),
                                category = item.optString("category", "Business"),
                                source = item.optString("source", "Unspecified source"),
                                verified = item.optBoolean("verified", false)
                            )
                        }
                        matches to if (matches.isEmpty()) "No public business matches found. Search your private saved list below." else "${matches.size} public listing(s). A listing is not proof of caller identity."
                    }
                }.getOrElse { emptyList<BusinessResult>() to "Lookup unavailable: ${it.message ?: "check backend URL and connection"}. Local search still works." }
            }
            remoteResults = result.first
            searchNotice = result.second
            searchBusy = false
        }
    }

    fun clearSelectedBackground() {
        if (customBackground.isNotBlank()) runCatching { File(customBackground).delete() }
        customBackground = ""
        prefs.edit().remove("background_image").apply()
    }

    fun useSampleBackground() {
        runCatching {
            val target = File(context.filesDir, "dailerx-charcoal-gold-background.png")
            if (customBackground.isNotBlank() && customBackground != target.absolutePath) runCatching { File(customBackground).delete() }
            context.assets.open("branding/charcoal-gold-background.png").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            customBackground = target.absolutePath
            prefs.edit().putString("background_image", customBackground).apply()
            backgroundNotice = "Dailer X sample wallpaper applied."
        }.onFailure { backgroundNotice = "Unable to apply sample wallpaper: ${it.message ?: "unknown error"}" }
    }

    fun checkBackend() {
        backendStatus = "Checking…"
        scope.launch {
            backendStatus = withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder().url("${backendUrl.trimEnd('/')}/health").get().build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) "Server returned HTTP ${response.code}" else "Connected"
                    }
                }.getOrElse { "Unavailable: ${it.message ?: "connection failed"}" }
            }
        }
    }

    fun startScreeningSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = context.getSystemService(RoleManager::class.java)
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) callRoleStatus = "Dailer X is the call-screening app"
                else roleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            } else callRoleStatus = "Call-screening role is not available on this device"
        } else {
            callRoleStatus = "The Android call-screening role request requires Android 10 or later"
        }
    }

    fun launchDeviceRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose device call ringtone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        ringtonePickerLauncher.launch(intent)
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else notificationsStatus = "Notifications enabled or permission not required"
    }

    val baseDensity = LocalDensity.current
    val fontScaleFactor = when (textSize) {
        "Compact" -> 0.92f
        "Large" -> 1.15f
        else -> 1.0f
    }
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScaleFactor)) {
    MaterialTheme(colorScheme = colors) {
        Box(Modifier.fillMaxSize().background(background)) {
            if (customBitmap != null) {
                Image(customBitmap, contentDescription = "Custom background", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(if (darkUi) Charcoal.copy(alpha = 0.70f) else Ivory.copy(alpha = 0.66f)))
            } else {
                val brush = when (backgroundStyle) {
                    "Gold haze" -> Brush.linearGradient(listOf(background, if (darkUi) DailerPalette.Charcoal else DailerPalette.Paper, background))
                    "Charcoal" -> Brush.verticalGradient(listOf(DailerPalette.Charcoal, DailerPalette.Onyx))
                    else -> Brush.verticalGradient(listOf(background, background))
                }
                Box(Modifier.fillMaxSize().background(brush))
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Charcoal.copy(alpha = 0.58f),
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = if (darkUi) DailerPalette.Charcoal else DailerPalette.Paper,
                        drawerContentColor = colors.onSurface,
                        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
                        modifier = Modifier.fillMaxHeight().widthIn(max = 320.dp)
                    ) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BrandMark(48)
                                Column {
                                    Text("Dailer X", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Private by design", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(22.dp))
                            Text("WORKSPACE", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, letterSpacing = 1.8.sp)
                            Spacer(Modifier.height(8.dp))
                            val navItems = listOf(
                                Triple("Search", Icons.Default.Search, "Look up numbers"),
                                Triple("Saved", Icons.Default.BookmarkBorder, "Private list"),
                                Triple("Links", Icons.Default.Link, "Saved web links"),
                                Triple("Messages", Icons.Default.Sms, "Templates and shortcuts"),
                                Triple("Activity", Icons.Default.History, "Search history"),
                                Triple("Spam", Icons.Default.ReportProblem, "Submit a report")
                            )
                            navItems.forEach { (label, icon, helper) -> DrawerNavRow(label, helper, icon, activePage == label) { setPage(label) } }
                            Spacer(Modifier.height(16.dp))
                            Text("APP & LEGAL", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, letterSpacing = 1.8.sp)
                            Spacer(Modifier.height(8.dp))
                            DrawerNavRow("Settings", "Appearance and controls", Icons.Default.Settings, activePage == "Settings") { setPage("Settings") }
                            DrawerNavRow("Privacy policy", "Data use and controls", Icons.Default.Policy, activePage == "Privacy") { setPage("Privacy") }
                            DrawerNavRow("Terms & conditions", "Rules and limitations", Icons.Default.Description, activePage == "Terms") { setPage("Terms") }
                            Spacer(Modifier.weight(1f))
                            HorizontalDivider(color = colors.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                            Text("Dailer X · Open source MVP", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                DailerIconAction(Icons.Default.Menu, "Open sidebar", onClick = { scope.launch { drawerState.open() } }, size = 43.dp)
                                BrandMark(39)
                                Column(Modifier.weight(1f)) {
                                    Text("Dailer X", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                                    Text(pageSubtitle(activePage), fontSize = 11.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                DailerIconAction(Icons.Default.Settings, "Settings", onClick = { activePage = "Settings" }, size = 43.dp)
                            }
                            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.75f))
                        }
                    },
                    bottomBar = {
                        CustomDock(activePage = activePage, onSelect = { activePage = it })
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        when (activePage) {
                            "Search" -> SearchPage(
                                query = query,
                                onQuery = { query = it },
                                onSearch = { performSearch() },
                                busy = searchBusy,
                                notice = searchNotice,
                                remoteResults = remoteResults,
                                saved = saved,
                                history = history,
                                onHistorySelect = { query = it; performSearch(it) },
                                onCall = { number -> launchDial(context, number) },
                                onSms = { number -> launchSms(context, number) },
                                onSaveRemote = { result -> saveName = result.name; saveNumber = result.number; saveNote = "Source: ${result.source}"; saveTags = "Business"; editNumber = null; saveDialog = true },
                                onReport = { number -> spamNumber = number; activePage = "Spam" }
                            )
                            "Saved" -> SavedPage(
                                saved = saved,
                                onAdd = { saveName = ""; saveNumber = ""; saveNote = ""; saveTags = ""; editNumber = null; saveDialog = true },
                                onEdit = { item -> editNumber = item; saveName = item.name; saveNumber = item.number; saveNote = item.note; saveTags = item.tags.orEmpty().joinToString(", "); saveDialog = true },
                                onCall = { launchDial(context, it) },
                                onSms = { launchSms(context, it) },
                                onDelete = { item -> saved = saved.filterNot { it.number == item.number }; persistNumbers() },
                                onCopy = { copyText(context, it) }
                            )
                            "Links" -> LinksPage(
                                links = links,
                                onAdd = { editLink = null; linkTitle = ""; linkUrl = ""; linkNote = ""; linkDialog = true },
                                onOpen = { openUrl(context, it) },
                                onCopy = { copyText(context, it) },
                                onDelete = { link -> links = links.filterNot { it.url == link.url }; persistLinks() },
                                onEditNote = { link -> editLink = link; linkTitle = link.title; linkUrl = link.url; linkNote = link.note; linkDialog = true }
                            )
                            "Messages" -> MessagesPage(
                                saved = saved,
                                target = messageTarget,
                                onTarget = { messageTarget = it },
                                templates = messageTemplates,
                                selected = selectedTemplate,
                                onSelectTemplate = { selectedTemplate = it },
                                onAddTemplate = { editTemplate = null; templateTitle = ""; templateBody = ""; templateDialog = true },
                                onEditTemplate = { item -> editTemplate = item; templateTitle = item.title; templateBody = item.body; templateDialog = true },
                                onDeleteTemplate = { item ->
                                    messageTemplates = messageTemplates.filterNot { it.title == item.title }
                                    persistTemplates()
                                    if (selectedTemplate.title == item.title) selectedTemplate = messageTemplates.firstOrNull() ?: MessageTemplate("Choose a template", "")
                                },
                                onCompose = { number, body -> launchSms(context, number, body) },
                                onCopy = { copyText(context, it) }
                            )
                            "Activity" -> ActivityPage(history = history, onSelect = { query = it; activePage = "Search"; performSearch(it) }, onClear = { history = emptyList(); prefs.edit().remove("history").apply() })
                            "Spam" -> SpamPage(
                                number = spamNumber,
                                onNumber = { spamNumber = it },
                                category = spamCategory,
                                onCategory = { spamCategory = it },
                                details = spamDetails,
                                onDetails = { spamDetails = it },
                                notice = spamNotice,
                                busy = spamBusy,
                                onSubmit = {
                                    if (spamNumber.isBlank()) spamNotice = "Enter the number you want to report."
                                    else {
                                        spamBusy = true; spamNotice = "Submitting report…"
                                        scope.launch {
                                            spamNotice = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val reportNumber = PhoneNumberUtils.formatNumberToE164(spamNumber.trim(), phoneRegion) ?: spamNumber.trim()
                                                    val body = JSONObject().put("number", reportNumber).put("category", spamCategory).put("details", spamDetails.trim()).toString()
                                                    val request = Request.Builder().url("${backendUrl.trimEnd('/')}/v1/reports/spam").header("X-Install-Token", installationToken).post(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
                                                    client.newCall(request).execute().use { response ->
                                                        if (!response.isSuccessful) error("Server returned HTTP ${response.code}")
                                                        DailerXNotifications.postStatus(context, "Spam report submitted", "Your report was received for $reportNumber.")
                                                        "Report submitted. Reports are signals, not verified proof of wrongdoing."
                                                    }
                                                }.getOrElse { "Report not sent: ${it.message ?: "backend unavailable"}." }
                                            }
                                            spamBusy = false
                                        }
                                    }
                                }
                            )
                            "Settings" -> SettingsPage(
                                themeMode = themeMode,
                                onThemeMode = { themeMode = it; prefs.edit().putString("theme_mode", it).apply() },
                                textSize = textSize,
                                onTextSize = { textSize = it; prefs.edit().putString("text_size", it).apply() },
                                backgroundStyle = backgroundStyle,
                                onBackgroundStyle = { backgroundStyle = it; prefs.edit().putString("background_style", it).apply(); clearSelectedBackground() },
                                onPickBackground = { pickBackground.launch("image/*") },
                                onSampleBackground = { useSampleBackground() },
                                onClearBackground = { clearSelectedBackground(); backgroundNotice = "Using the selected built-in background." },
                                backgroundNotice = backgroundNotice,
                                backendUrl = backendUrl,
                                onBackendUrl = { backendUrl = it },
                                onSaveBackend = { prefs.edit().putString("backend", backendUrl.trim().trimEnd('/')).apply(); backendStatus = "Saved" },
                                backendStatus = backendStatus,
                                onCheckBackend = { checkBackend() },
                                keepHistory = allowHistory,
                                onKeepHistory = { allowHistory = it; prefs.edit().putBoolean("keep_history", it).apply(); if (!it) { history = emptyList(); prefs.edit().remove("history").apply() } },
                                autoBlockSpam = autoBlockSpam,
                                onAutoBlockSpam = { autoBlockSpam = it; prefs.edit().putBoolean("auto_block_spam", it).apply() },
                                blockedNumbers = blockedNumbers,
                                onBlockedNumbers = { blockedNumbers = it; prefs.edit().putString("blocked_numbers", it).apply() },
                                allowedNumbers = allowedNumbers,
                                onAllowedNumbers = { allowedNumbers = it; prefs.edit().putString("allowed_numbers", it).apply() },
                                phoneRegion = phoneRegion,
                                onPhoneRegion = { phoneRegion = it.uppercase(java.util.Locale.ROOT).take(2); prefs.edit().putString("phone_region", phoneRegion).apply() },
                                callRoleStatus = callRoleStatus,
                                onChangeRingtone = { launchDeviceRingtonePicker() },
                                ringtoneStatus = ringtoneStatus,
                                onEnableNotifications = { requestNotifications() },
                                notificationsStatus = notificationsStatus,
                                onOpenPhoneApp = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL)) } },
                                onSetupCallScreening = { startScreeningSetup() },
                                onClearHistory = { history = emptyList(); prefs.edit().remove("history").apply() },
                                onClearAll = { clearAllDialog = true },
                                onPrivacy = { activePage = "Privacy" },
                                onTerms = { activePage = "Terms" },
                                dataConsent = dataConsent,
                                onDataConsent = { dataConsent = it; prefs.edit().putBoolean("community_consent", it).apply() }
                            )
                            "Privacy" -> LegalPage(
                                title = "Privacy policy",
                                subtitle = "What Dailer X stores, sends, and leaves under your control.",
                                sections = privacySections,
                                onBack = { activePage = "Settings" }
                            )
                            "Terms" -> LegalPage(
                                title = "Terms & conditions",
                                subtitle = "Terms for this experimental, community-oriented MVP.",
                                sections = termsSections,
                                onBack = { activePage = "Settings" }
                            )
                            else -> SearchPage(query, { query = it }, { performSearch() }, searchBusy, searchNotice, remoteResults, saved, history, { query = it; performSearch(it) }, { launchDial(context, it) }, { launchSms(context, it) }, { result -> saveName = result.name; saveNumber = result.number; saveNote = "Source: ${result.source}"; saveTags = "Business"; editNumber = null; saveDialog = true }, { spamNumber = it; activePage = "Spam" })
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Dailer X · Caller data can be inaccurate or spoofed.", fontSize = 11.sp, color = colors.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        if (saveDialog) {
            DailerDialog(
                title = if (editNumber == null) "Save number" else "Edit saved number",
                subtitle = "Saved names and notes stay in this app's local storage.",
                onDismiss = { saveDialog = false },
                actions = {
                    DailerAction("Cancel", onClick = { saveDialog = false })
                    DailerAction(if (editNumber == null) "Save number" else "Save changes", icon = Icons.Default.Check, onClick = {
                        if (saveName.isNotBlank() && saveNumber.isNotBlank()) {
                            val item = SavedNumber(saveName.trim(), saveNumber.trim(), saveNote.trim(), saveTags.split(",").map { it.trim() }.filter { it.isNotBlank() })
                            saved = if (editNumber == null) (saved.filterNot { it.number == item.number } + item) else (saved.filterNot { it.number == editNumber?.number || it.number == item.number } + item)
                            persistNumbers()
                            saveDialog = false
                        }
                    }, enabled = saveName.isNotBlank() && saveNumber.isNotBlank())
                }
            ) {
                OutlinedTextField(saveName, { saveName = it }, label = { Text("Name or label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(saveNumber, { saveNumber = it }, label = { Text("Phone number") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(saveNote, { saveNote = it }, label = { Text("Private note (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(saveTags, { saveTags = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        if (linkDialog) {
            DailerDialog(
                title = if (editLink == null) "Save a link" else "Edit saved link",
                subtitle = "Only save URLs you trust. Review a link before opening it.",
                onDismiss = { linkDialog = false; editLink = null },
                actions = {
                    DailerAction("Cancel", onClick = { linkDialog = false; editLink = null })
                    DailerAction(if (editLink == null) "Save link" else "Save changes", icon = Icons.Default.Check, onClick = {
                        val normalized = normalizeWebUrl(linkUrl)
                        if (linkTitle.isNotBlank() && normalized != null) {
                            val updated = SavedLink(linkTitle.trim(), normalized, linkNote.trim())
                            links = (links.filterNot { it.url == editLink?.url || it.url == normalized } + updated).distinctBy { it.url }
                            persistLinks()
                            linkDialog = false
                            editLink = null
                        }
                    }, enabled = linkTitle.isNotBlank() && normalizeWebUrl(linkUrl) != null)
                }
            ) {
                OutlinedTextField(linkTitle, { linkTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(linkUrl, { linkUrl = it }, label = { Text("URL (https://…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(linkNote, { linkNote = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }

        if (templateDialog) {
            DailerDialog(
                title = if (editTemplate == null) "Create message template" else "Edit message template",
                subtitle = "Templates are stored locally. Review draft text before sending it.",
                onDismiss = { templateDialog = false; editTemplate = null },
                actions = {
                    DailerAction("Cancel", onClick = { templateDialog = false; editTemplate = null })
                    DailerAction("Save template", icon = Icons.Default.Check, enabled = templateTitle.isNotBlank() && templateBody.isNotBlank(), onClick = {
                        if (templateTitle.isNotBlank() && templateBody.isNotBlank()) {
                            val updated = MessageTemplate(templateTitle.trim(), templateBody.trim())
                            messageTemplates = (messageTemplates.filterNot { it.title == editTemplate?.title || it.title.equals(updated.title, ignoreCase = true) } + updated)
                            persistTemplates()
                            selectedTemplate = updated
                            templateDialog = false
                            editTemplate = null
                        }
                    })
                }
            ) {
                OutlinedTextField(templateTitle, { templateTitle = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(templateBody, { templateBody = it }, label = { Text("Message text") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7)
            }
        }

        if (clearAllDialog) {
            DailerDialog(
                title = "Delete local app data?",
                subtitle = "This deletes saved numbers, notes, saved links, message templates, search history, and local preferences from this installation. It does not delete server-side reports already submitted.",
                onDismiss = { clearAllDialog = false },
                actions = {
                    DailerAction("Keep my data", onClick = { clearAllDialog = false })
                    DailerAction("Delete local data", icon = Icons.Default.DeleteOutline, onClick = {
                        saved = emptyList(); links = emptyList(); history = emptyList(); remoteResults = emptyList()
                        messageTemplates = MessageTemplates
                        selectedTemplate = MessageTemplates.first()
                        prefs.edit().clear().apply()
                        themeMode = "System"; textSize = "Default"; backgroundStyle = "Ivory paper"; customBackground = ""; backendUrl = DEFAULT_BACKEND; allowHistory = true; autoBlockSpam = false; blockedNumbers = ""; allowedNumbers = ""; phoneRegion = java.util.Locale.getDefault().country.ifBlank { "US" }; dataConsent = false
                        runCatching {
                            File(context.filesDir, "dailerx-custom-background.jpg").delete()
                            File(context.filesDir, "dailerx-charcoal-gold-background.png").delete()
                        }
                        clearAllDialog = false
                    })
                }
            ) { Icon(Icons.Default.WarningAmber, null, tint = Gold, modifier = Modifier.size(28.dp)); Text("This action cannot be undone.", color = colors.onSurfaceVariant) }
        }
    }
    }
}

@Composable
private fun SearchPage(
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    busy: Boolean,
    notice: String,
    remoteResults: List<BusinessResult>,
    saved: List<SavedNumber>,
    history: List<String>,
    onHistorySelect: (String) -> Unit,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onSaveRemote: (BusinessResult) -> Unit,
    onReport: (String) -> Unit
) {
    SectionEyebrow("NUMBER INTELLIGENCE")
    Text("Find who called.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
    Text("Search your saved list or the public business listings on your configured server.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    DailerPanel {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Name, business, or number") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(10.dp))
        DailerAction(if (busy) "Searching…" else "Search directory", icon = Icons.Default.Search, onClick = onSearch, enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (query.isNotBlank()) {
        SectionHeading("Your saved matches", "${saved.count { it.name.contains(query, true) || it.number.contains(query, true) || it.note.contains(query, true) || it.tags.orEmpty().any { tag -> tag.contains(query, true) } }} found")
        val local = saved.filter { it.name.contains(query, true) || it.number.contains(query, true) || it.note.contains(query, true) || it.tags.orEmpty().any { tag -> tag.contains(query, true) } }
        if (local.isEmpty()) MutedEmpty("No local matches", "Save a number to build your private list.", Icons.Default.BookmarkBorder)
        local.forEach { NumberCard(item = it, onCall = { onCall(it.number) }, onSms = { onSms(it.number) }) }
    }

    if (remoteResults.isNotEmpty()) {
        SectionHeading("Public business results", "${remoteResults.size} result(s)")
        remoteResults.forEach { result -> BusinessCard(result, onCall = { onCall(result.number) }, onSms = { onSms(result.number) }, onSave = { onSaveRemote(result) }, onReport = { onReport(result.number) }) }
    }

    if (history.isNotEmpty()) {
        SectionHeading("Recent searches", "${history.size} saved locally")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            history.take(8).forEach { term -> DailerAction(term, icon = Icons.Default.History, compact = true, onClick = { onHistorySelect(term) }) }
        }
    }
    DailerPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Shield, null, tint = Gold, modifier = Modifier.size(25.dp))
            Column(Modifier.weight(1f)) {
                Text("Identity is never guaranteed", fontWeight = FontWeight.SemiBold)
                Text("Caller IDs may be spoofed, reused, or incorrectly reported. Check the source before trusting a match.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SavedPage(
    saved: List<SavedNumber>,
    onAdd: () -> Unit,
    onEdit: (SavedNumber) -> Unit,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onDelete: (SavedNumber) -> Unit,
    onCopy: (String) -> Unit
) {
    SectionEyebrow("YOUR PRIVATE DIRECTORY")
    SectionHeading("Saved numbers", "${saved.size} saved")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DailerAction("Add number", icon = Icons.Default.Add, onClick = onAdd, modifier = Modifier.weight(1f))
        DailerAction("Copy list", icon = Icons.Default.ContentCopy, onClick = { onCopy(saved.joinToString("\n") { "${it.name}\t${it.number}\t${it.note}\t${it.tags.orEmpty().joinToString(",")}" }) }, enabled = saved.isNotEmpty(), modifier = Modifier.weight(1f))
    }
    if (saved.isEmpty()) MutedEmpty("Your directory starts here", "Save names, numbers, notes, and private tags. Nothing is uploaded automatically.", Icons.Default.Bookmarks)
    saved.sortedBy { it.name.lowercase() }.forEach { item ->
        NumberCard(
            item = item,
            onCall = { onCall(item.number) },
            onSms = { onSms(item.number) },
            onCopy = { onCopy(item.number) },
            onEdit = { onEdit(item) },
            onDelete = { onDelete(item) }
        )
    }
}

@Composable
private fun LinksPage(
    links: List<SavedLink>,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (SavedLink) -> Unit,
    onEditNote: (SavedLink) -> Unit
) {
    SectionEyebrow("PERSONAL LINK LIBRARY")
    SectionHeading("Saved links", "${links.size} stored")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DailerAction("Save a link", icon = Icons.Default.Add, onClick = onAdd, modifier = Modifier.weight(1f))
        DailerAction("Copy all URLs", icon = Icons.Default.ContentCopy, onClick = { onCopy(links.joinToString("\n") { it.url }) }, enabled = links.isNotEmpty(), modifier = Modifier.weight(1f))
    }
    if (links.isEmpty()) MutedEmpty("Keep useful links together", "Save business pages, references, maps, and resources using the bordered controls.", Icons.Default.Link)
    links.forEach { link ->
        DailerPanel {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Link, null, tint = Gold, modifier = Modifier.padding(top = 3.dp).size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(link.title, fontWeight = FontWeight.SemiBold)
                    Text(link.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (link.note.isNotBlank()) Text(link.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        DailerAction("Open", icon = Icons.Default.OpenInNew, compact = true, onClick = { onOpen(link.url) })
                        DailerAction("Copy", icon = Icons.Default.ContentCopy, compact = true, onClick = { onCopy(link.url) })
                        DailerAction("Edit", icon = Icons.Default.Edit, compact = true, onClick = { onEditNote(link) })
                        DailerIconAction(Icons.Default.DeleteOutline, "Delete link", onClick = { onDelete(link) }, size = 38.dp)
                    }
                }
            }
        }
    }
    Text("Links open in another app only after you tap Open. Check the domain before visiting.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MessagesPage(
    saved: List<SavedNumber>,
    target: String,
    onTarget: (String) -> Unit,
    templates: List<MessageTemplate>,
    selected: MessageTemplate,
    onSelectTemplate: (MessageTemplate) -> Unit,
    onAddTemplate: () -> Unit,
    onEditTemplate: (MessageTemplate) -> Unit,
    onDeleteTemplate: (MessageTemplate) -> Unit,
    onCompose: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    SectionEyebrow("MESSAGING SHORTCUTS")
    Text("Say it quicker.", fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    Text("Dailer X opens the messaging app you choose. It does not read private messages from other apps.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    DailerPanel {
        Text("Send to", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(target, onTarget, modifier = Modifier.fillMaxWidth(), label = { Text("Phone number") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        if (saved.isNotEmpty()) {
            Text("Choose a saved number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                saved.take(8).forEach { item -> DailerAction("${item.name} · ${item.number}", icon = Icons.Default.Person, compact = true, modifier = Modifier.fillMaxWidth(), onClick = { onTarget(item.number) }) }
            }
        }
    }
    SectionHeading("Saved message templates", "${templates.size} saved")
    DailerAction("Create message template", icon = Icons.Default.Add, onClick = onAddTemplate, modifier = Modifier.fillMaxWidth())
    if (templates.isEmpty()) MutedEmpty("No message templates", "Create a reusable message that you can copy or open in your SMS app.", Icons.Default.Sms)
    templates.forEach { template ->
        val chosen = selected.title == template.title
        DailerPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(template.title, fontWeight = FontWeight.SemiBold)
                    Text(template.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DailerIconAction(if (chosen) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (chosen) "Selected template" else "Select template", onClick = { onSelectTemplate(template) }, selected = chosen, size = 38.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                DailerAction("Edit", icon = Icons.Default.Edit, compact = true, onClick = { onEditTemplate(template) })
                DailerAction("Delete", icon = Icons.Default.DeleteOutline, compact = true, onClick = { onDeleteTemplate(template) })
            }
            if (chosen) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DailerAction("Copy text", icon = Icons.Default.ContentCopy, onClick = { onCopy(template.body) }, modifier = Modifier.weight(1f))
                    DailerAction("Open SMS", icon = Icons.Default.Send, onClick = { if (target.isNotBlank()) onCompose(target, template.body) }, enabled = target.isNotBlank(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActivityPage(history: List<String>, onSelect: (String) -> Unit, onClear: () -> Unit) {
    SectionEyebrow("LOCAL ACTIVITY")
    SectionHeading("Search history", "${history.size} item(s)")
    Text("History stays on this device unless you configure external sync in a future version.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    DailerAction("Clear history", icon = Icons.Default.DeleteOutline, onClick = onClear, enabled = history.isNotEmpty())
    if (history.isEmpty()) MutedEmpty("Nothing to show", "Searches appear here while history is enabled in Settings.", Icons.Default.History)
    history.forEach { entry ->
        DailerPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.History, null, tint = Gold)
                Text(entry, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                DailerIconAction(Icons.Default.Search, "Search again", onClick = { onSelect(entry) }, size = 38.dp)
            }
        }
    }
}

@Composable
private fun SpamPage(
    number: String,
    onNumber: (String) -> Unit,
    category: String,
    onCategory: (String) -> Unit,
    details: String,
    onDetails: (String) -> Unit,
    notice: String,
    busy: Boolean,
    onSubmit: () -> Unit
) {
    SectionEyebrow("COMMUNITY SIGNALS")
    Text("Report a suspicious number.", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
    Text("Reports are unverified and can be wrong. Do not include sensitive personal information.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    DailerPanel {
        OutlinedTextField(number, onNumber, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Report category", fontWeight = FontWeight.SemiBold)
        listOf("telemarketing" to "Telemarketing", "suspected_fraud" to "Suspected fraud", "nuisance" to "Nuisance", "other" to "Other").forEach { (value, label) ->
            DailerAction(label, icon = if (category == value) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, selected = category == value, onClick = { onCategory(value) }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(details, onDetails, label = { Text("Details (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
        DailerAction(if (busy) "Submitting…" else "Submit report", icon = Icons.Default.Send, onClick = onSubmit, enabled = !busy && number.isNotBlank(), modifier = Modifier.fillMaxWidth())
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsPage(
    themeMode: String,
    onThemeMode: (String) -> Unit,
    textSize: String,
    onTextSize: (String) -> Unit,
    backgroundStyle: String,
    onBackgroundStyle: (String) -> Unit,
    onPickBackground: () -> Unit,
    onSampleBackground: () -> Unit,
    onClearBackground: () -> Unit,
    backgroundNotice: String,
    backendUrl: String,
    onBackendUrl: (String) -> Unit,
    onSaveBackend: () -> Unit,
    backendStatus: String,
    onCheckBackend: () -> Unit,
    keepHistory: Boolean,
    onKeepHistory: (Boolean) -> Unit,
    autoBlockSpam: Boolean,
    onAutoBlockSpam: (Boolean) -> Unit,
    blockedNumbers: String,
    onBlockedNumbers: (String) -> Unit,
    allowedNumbers: String,
    onAllowedNumbers: (String) -> Unit,
    phoneRegion: String,
    onPhoneRegion: (String) -> Unit,
    callRoleStatus: String,
    onChangeRingtone: () -> Unit,
    ringtoneStatus: String,
    onEnableNotifications: () -> Unit,
    notificationsStatus: String,
    onOpenPhoneApp: () -> Unit,
    onSetupCallScreening: () -> Unit,
    onClearHistory: () -> Unit,
    onClearAll: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    dataConsent: Boolean,
    onDataConsent: (Boolean) -> Unit
) {
    SectionEyebrow("CONTROL ROOM")
    Text("Settings", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
    Text("Make Dailer X feel like yours. Controls are saved locally on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)

    SettingsSection("Appearance", Icons.Default.Palette, "Theme, background, and readability") {
        Text("Theme mode", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("System", "Light", "Dark").forEach { mode -> DailerAction(mode, selected = themeMode == mode, onClick = { onThemeMode(mode) }, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Text size", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Compact", "Default", "Large").forEach { size -> DailerAction(size, selected = textSize == size, onClick = { onTextSize(size) }, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Built-in backgrounds", fontWeight = FontWeight.SemiBold)
        listOf("Ivory paper", "Gold haze", "Charcoal", "Onyx").forEach { style ->
            DailerAction(style, icon = backgroundStyleIcon(style), selected = backgroundStyle == style, onClick = { onBackgroundStyle(style) }, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            DailerAction("Choose image", icon = Icons.Default.Image, compact = true, onClick = onPickBackground, modifier = Modifier.weight(1f))
            DailerAction("Sample", icon = Icons.Default.Wallpaper, compact = true, onClick = onSampleBackground, modifier = Modifier.weight(1f))
            DailerAction("Reset", icon = Icons.Default.RestartAlt, compact = true, onClick = onClearBackground, modifier = Modifier.weight(1f))
        }
        if (backgroundNotice.isNotBlank()) Text(backgroundNotice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Use a photo or image as the background. Dailer X stores a copy inside app-private storage and overlays it for text contrast.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SettingsSection("Backend connection", Icons.Default.Cloud, "Search service and API address") {
        OutlinedTextField(backendUrl, onBackendUrl, label = { Text("API base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DailerAction("Save URL", icon = Icons.Default.Save, onClick = onSaveBackend, modifier = Modifier.weight(1f))
            DailerAction("Test connection", icon = Icons.Default.NetworkCheck, onClick = onCheckBackend, modifier = Modifier.weight(1f))
        }
        Text("Status: $backendStatus", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        Text("Emulator default: http://10.0.2.2:8000. Use HTTPS for production; local cleartext HTTP is enabled only for development.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SettingsSection("Caller ID & spam blocking", Icons.Default.PhoneInTalk, "Caller identification, allowlist, and block rules") {
        Text("Role status: $callRoleStatus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DailerAction("Enable call identification and blocking", icon = Icons.Default.Security, onClick = onSetupCallScreening, modifier = Modifier.fillMaxWidth())
        SettingsToggle("Automatically block numbers with 5+ reports", "Off by default. When enabled, incoming numbers with five or more distinct reports in the last 30 days are rejected. Allowlisted numbers always pass; backend failures fail open.", autoBlockSpam, onAutoBlockSpam)
        OutlinedTextField(blockedNumbers, onBlockedNumbers, modifier = Modifier.fillMaxWidth(), label = { Text("Manual blocklist") }, minLines = 2, maxLines = 4, placeholder = { Text("One number per line or comma-separated") })
        OutlinedTextField(allowedNumbers, onAllowedNumbers, modifier = Modifier.fillMaxWidth(), label = { Text("Always-allow list") }, minLines = 2, maxLines = 4, placeholder = { Text("Trusted numbers; these override spam rules") })
        OutlinedTextField(phoneRegion, onPhoneRegion, modifier = Modifier.fillMaxWidth(), label = { Text("Default phone region (ISO code, e.g. IN or US)") }, singleLine = true)
        Text("Use E.164 numbers when possible. The region helps normalize national-format incoming numbers before lookup. Manual block and allow rules are stored locally. The Android call-screening role is required for call blocking. Reports can be wrong; review rules before enabling automatic blocking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SettingsSection("Ringtone & notifications", Icons.Default.NotificationsActive, "Device ringtone and caller alert badges") {
        DailerAction("Change device call ringtone", icon = Icons.Default.MusicNote, onClick = onChangeRingtone, modifier = Modifier.fillMaxWidth())
        Text(ringtoneStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DailerAction("Enable caller notifications", icon = Icons.Default.Notifications, onClick = onEnableNotifications, modifier = Modifier.fillMaxWidth())
        Text(notificationsStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Notification channels enable launcher badges where the device launcher supports them. Android/OEM settings can override badges and alert behavior. Changing the call ringtone changes the device-wide default and requires explicit system permission.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SettingsSection("Call recording compatibility", Icons.Default.Mic, "Use only recording features provided by your phone") {
        Text("Standard third-party Android apps cannot reliably capture cellular call audio across devices. Dailer X does not bypass Android/OEM restrictions or record calls silently. If your phone's built-in dialler provides a lawful call-recording feature, use its in-call controls and follow local consent laws.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DailerAction("Open phone app", icon = Icons.Default.Phone, onClick = onOpenPhoneApp, modifier = Modifier.fillMaxWidth())
    }

    SettingsSection("Privacy controls", Icons.Default.Shield, "History, optional contributions, and data removal") {
        SettingsToggle("Keep search history", "Stores recent queries locally for quick reuse.", keepHistory, onKeepHistory)
        SettingsToggle("Allow voluntary community contribution", "This is a consent setting only. The current MVP does not upload your contacts or automatically submit saved names.", dataConsent, onDataConsent)
        DailerAction("Clear search history", icon = Icons.Default.DeleteOutline, onClick = onClearHistory, modifier = Modifier.fillMaxWidth())
        DailerAction("Delete all local app data", icon = Icons.Default.DeleteForever, onClick = onClearAll, modifier = Modifier.fillMaxWidth())
        DailerAction("Privacy policy", icon = Icons.Default.Policy, onClick = onPrivacy, modifier = Modifier.fillMaxWidth())
        DailerAction("Terms & conditions", icon = Icons.Default.Description, onClick = onTerms, modifier = Modifier.fillMaxWidth())
    }

    SettingsSection("About Dailer X", Icons.Default.Info, "Open-source project details") {
        Text("Dailer X is an independent, community-oriented Android MVP. It is not affiliated with or endorsed by Truecaller.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version 0.3.0 · Apache-2.0", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LegalPage(title: String, subtitle: String, sections: List<Pair<String, String>>, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DailerIconAction(Icons.Default.ArrowBack, "Back to settings", onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    DailerPanel {
        Text("Effective date: 11 October 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sections.forEach { (heading, body) ->
            Text(heading, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text("This document is a starter policy for the current source build, not legal advice. The project maintainer must review and adapt it before public release.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    DailerPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(23.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
private fun SettingsToggle(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Charcoal, checkedTrackColor = Gold, checkedBorderColor = Gold))
    }
}

@Composable
private fun DailerDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), content = actions)
            }
        }
    }
}

@Composable
private fun NumberCard(
    item: SavedNumber,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    DailerPanel {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Gold.copy(alpha = 0.8f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text(item.name.take(1).uppercase(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Gold)
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(item.number, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val tags = item.tags.orEmpty()
                if (tags.isNotEmpty()) Text(tags.joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            DailerAction("Call", icon = Icons.Default.Call, compact = true, onClick = onCall)
            DailerAction("Message", icon = Icons.Default.Sms, compact = true, onClick = onSms)
            if (onCopy != null) DailerIconAction(Icons.Default.ContentCopy, "Copy number", onClick = onCopy, size = 38.dp)
            if (onEdit != null) DailerIconAction(Icons.Default.Edit, "Edit", onClick = onEdit, size = 38.dp)
            if (onDelete != null) DailerIconAction(Icons.Default.DeleteOutline, "Delete", onClick = onDelete, size = 38.dp)
        }
    }
}

@Composable
private fun BusinessCard(result: BusinessResult, onCall: () -> Unit, onSms: () -> Unit, onSave: () -> Unit, onReport: () -> Unit) {
    DailerPanel {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Gold, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Storefront, null, tint = Gold, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(result.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(result.number, fontSize = 13.sp)
                Text(result.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Source: ${result.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (result.verified) "Source marked verified; still confirm independently." else "Unverified listing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            DailerAction("Call", icon = Icons.Default.Call, compact = true, onClick = onCall)
            DailerAction("Message", icon = Icons.Default.Sms, compact = true, onClick = onSms)
            DailerAction("Save", icon = Icons.Default.BookmarkBorder, compact = true, onClick = onSave)
            DailerAction("Report", icon = Icons.Default.ReportProblem, compact = true, onClick = onReport)
        }
    }
}

@Composable
private fun DailerPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.91f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun DailerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (selected) Gold else MaterialTheme.colorScheme.outline
    val textColor = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.heightIn(min = if (compact) 36.dp else 42.dp).clip(shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        contentColor = textColor,
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor.copy(alpha = if (enabled) 0.96f else 0.35f))
    ) {
        Row(Modifier.padding(horizontal = if (compact) 10.dp else 13.dp, vertical = if (compact) 7.dp else 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 15.dp else 17.dp), tint = if (selected) Gold else textColor)
                Spacer(Modifier.width(7.dp))
            }
            Text(label, fontSize = if (compact) 12.sp else 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DailerIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 42.dp
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier.size(size).clip(shape).clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) Gold else MaterialTheme.colorScheme.outline),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp), tint = if (selected) Gold else MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun CustomDock(activePage: String, onSelect: (String) -> Unit) {
    val allTabs = listOf(
        Triple("Search", Icons.Default.Search, "Search"),
        Triple("Saved", Icons.Default.BookmarkBorder, "Saved"),
        Triple("Links", Icons.Default.Link, "Links"),
        Triple("Messages", Icons.Default.Sms, "Messages"),
        Triple("Settings", Icons.Default.Settings, "Settings")
    )
    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 9.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // In split-screen/floating windows, reduce dock items; every page remains in the sidebar.
            val tabs = when {
                maxWidth < 280.dp -> allTabs.filter { it.first in setOf("Search", "Saved", "Settings") }
                maxWidth < 360.dp -> allTabs.filter { it.first in setOf("Search", "Saved", "Links", "Settings") }
                else -> allTabs
            }
            Row(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEach { (page, icon, label) ->
                    val selected = activePage == page
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).border(if (selected) 1.2.dp else 1.dp, if (selected) Gold else Color.Transparent, RoundedCornerShape(9.dp)).clickable(role = Role.Tab, onClick = { onSelect(page) }).padding(vertical = 7.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp), tint = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerNavRow(label: String, helper: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(if (selected) 1.3.dp else 1.dp, if (selected) Gold else Color.Transparent, RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Icon(icon, null, tint = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 14.sp)
            Text(helper, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.ChevronRight, null, tint = Gold, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BrandMark(size: Int) {
    Surface(
        modifier = Modifier.size(size.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, Gold)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PhoneInTalk, contentDescription = "Dailer X mark", tint = Gold, modifier = Modifier.size((size * 0.52f).dp))
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(text, color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionHeading(title: String, count: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.3).sp)
        if (!count.isNullOrBlank()) Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun MutedEmpty(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DailerPanel {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun pageSubtitle(page: String): String = when (page) {
    "Search" -> "Caller intelligence, on your terms"
    "Saved" -> "Your private number library"
    "Links" -> "Useful links, kept together"
    "Messages" -> "Templates and communication shortcuts"
    "Activity" -> "Recent local searches"
    "Spam" -> "Community reporting"
    "Settings" -> "Personalize and protect your app"
    "Privacy" -> "Data, transparency, and control"
    "Terms" -> "Usage and limitations"
    else -> "Caller intelligence, on your terms"
}

private fun backgroundStyleIcon(style: String) = when (style) {
    "Gold haze" -> Icons.Default.AutoAwesome
    "Charcoal" -> Icons.Default.DarkMode
    "Onyx" -> Icons.Default.Contrast
    else -> Icons.Default.WbSunny
}

private fun readCallScreeningRole(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Requires Android 10 or later to request the role"
    val manager = context.getSystemService(RoleManager::class.java) ?: return "Role manager unavailable"
    return if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) "Dailer X is the call-screening app" else "Not enabled"
}

private fun launchDial(context: Context, number: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))) }
}

private fun launchSms(context: Context, number: String, body: String? = null) {
    runCatching {
        val uri = if (body.isNullOrBlank()) Uri.parse("smsto:${Uri.encode(number)}") else Uri.parse("smsto:${Uri.encode(number)}?body=${Uri.encode(body)}")
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
    }
}

private fun openUrl(context: Context, rawUrl: String) {
    val url = normalizeWebUrl(rawUrl) ?: return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun normalizeWebUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null
    val candidate = if (trimmed.startsWith("https://", true) || trimmed.startsWith("http://", true)) trimmed else "https://$trimmed"
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    if (uri.scheme !in listOf("https", "http") || uri.host.isNullOrBlank() || uri.userInfo != null) return null
    return uri.toString()
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Dailer X", text))
}

private val privacySections = listOf(
    "1. Who operates this app" to "Dailer X is an open-source project. The person or organization distributing a build is responsible for operating it and maintaining a contact address for privacy requests. Add the operator's legal name and contact details before public release.",
    "2. Information stored on this device" to "The current MVP stores saved numbers, labels, notes, tags, saved links, reusable message templates, theme preferences, background selection, backend URL, and optionally recent searches in app-private storage. This version does not encrypt these preference records at the application layer. Do not store highly sensitive information in notes.",
    "3. Information sent to a backend" to "When you run a search, the query is sent to the backend URL configured in Settings. When you submit a spam report, the phone number, category, and optional details are sent to that server. The server operator controls that service's logs, retention, and backups. Only connect to servers you trust; use HTTPS outside local development.",
    "4. Contacts and messages" to "This source build does not request permission to upload your address book and does not read SMS, WhatsApp, Telegram, or other app messages. Message actions open an installed SMS app with a recipient and optional draft text. You choose whether to send the message.",
    "5. Optional call screening" to "If you choose the Android call-screening role and enable automatic silencing, an incoming number may be sent to your configured backend for a spam-report count. A number is silenced only when the backend responds with at least five reports. If the service cannot reach the backend, it allows the call through. Disable the option or remove the role to stop this behavior.",
    "6. Community submissions and accuracy" to "The voluntary contribution switch in this MVP records your preference only; it does not automatically upload saved contacts or personal labels. Directory entries may be stale, mistaken, spoofed, or disputed. A listing or spam report is not proof of identity or wrongdoing.",
    "7. Retention and deletion" to "You can clear local search history or delete local app data in Settings. That does not remove reports already submitted to a server. Server-side data deletion, correction, and dispute requests must be implemented and handled by the server operator.",
    "8. Security" to "Android app-private storage restricts ordinary access by other apps, but this MVP does not provide app-layer encryption, cloud backup, or a production account system. Protect your device, avoid untrusted backends, and do not use development HTTP over untrusted networks.",
    "9. Children and sensitive data" to "This project is not designed to collect children's data. Do not submit medical, financial, authentication, or other sensitive information in notes or spam report details.",
    "10. Policy changes and contact" to "The project operator should publish a versioned policy and contact channel before release. This template does not replace jurisdiction-specific legal advice. Update this document when network behavior, data sources, retention, or permissions change."
)

private val termsSections = listOf(
    "1. Experimental software" to "Dailer X is provided as an open-source, experimental MVP. Features may be incomplete, change without notice, or be unavailable on particular Android versions and devices.",
    "2. No identity guarantee" to "Caller names, business listings, number metadata, and community reports can be incorrect, malicious, outdated, or affected by caller-ID spoofing or number reassignment. Verify important requests independently. Do not rely on this app for emergency response, financial authorization, or security decisions.",
    "3. Acceptable use" to "Do not use this software to harass, stalk, dox, unlawfully profile people, circumvent access controls, scrape data contrary to its terms, or publish personal information without a lawful basis. Use only data you are authorized to process and redistribute.",
    "4. Data and third-party services" to "You are responsible for the backend you configure, data sources you import, licenses and attribution, security controls, and retention practices. Third-party apps opened for calls, messaging, or URLs operate under their own terms and privacy policies.",
    "5. Reports and disputes" to "Spam reports are unverified user-generated signals, not determinations of guilt. Do not submit false or retaliatory reports. A production deployment should provide moderation, correction, appeal, and abuse-prevention processes.",
    "6. Permissions and call screening" to "Some optional features require Android system roles or user actions. Call-screening access is opt-in. You can remove the role in Android settings. Dailer X does not guarantee that every call can be identified or screened.",
    "7. Open-source license" to "Project source code is offered under Apache License 2.0 unless a file or dependency states otherwise. Dependencies, icons, and data sources may have separate licenses; review each applicable license. The license does not grant rights to third-party phone-number data.",
    "8. No warranty" to "To the extent permitted by law, the software is provided as-is, without warranties of merchantability, fitness for a particular purpose, availability, accuracy, or non-infringement. You are responsible for assessing whether it is suitable for your use.",
    "9. Limitation of liability" to "To the extent permitted by applicable law, project contributors are not liable for indirect, incidental, special, consequential, or data-loss damages arising from use of this software. These terms do not exclude rights or liabilities that cannot legally be excluded.",
    "10. Changes and contact" to "The maintainer should provide a contact address and versioned terms before public distribution. Continued use after updates is subject to applicable law and any required consent process. This template is not legal advice."
)
