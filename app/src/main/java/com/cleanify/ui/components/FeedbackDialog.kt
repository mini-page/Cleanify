/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.ui.components

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cleanify.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URLEncoder
import java.io.File
import java.util.Locale

enum class FeedbackType(
    val title: String,
    val emailAlias: String,
    val tag: String,
    val icon: ImageVector,
    val sampleSubjects: List<String>
) {
    GENERAL(
        "General Feedback",
        "raghavans5711+Cleanify@gmail.com",
        "General Feedback",
        Icons.Default.Chat,
        listOf(
            "Thoughts on overall experience",
            "Love the clean minimalist UI",
            "Performance on my device",
            "Question regarding storage safety",
            "General appreciation & idea"
        )
    ),
    BUG_REPORT(
        "Bug Report",
        "raghavans5711+Cleanify_bugReport@gmail.com",
        "Bug Report",
        Icons.Default.BugReport,
        listOf(
            "App crashed during swipe sorting",
            "Media preview shows black screen",
            "Empty cleaner stopped abruptly",
            "Storage calculation mismatch",
            "Lag when scrolling through folders"
        )
    ),
    UI_THEME(
        "UI / Theme Issue",
        "raghavans5711+Cleanify_uiTheme@gmail.com",
        "UI / Theme Issue",
        Icons.Default.Palette,
        listOf(
            "Dropdown styling in settings",
            "Search bar height glitches",
            "Accent color not applying properly",
            "Icon contrast in dark mode",
            "Text overflow or misalignment"
        )
    ),
    PERFORMANCE(
        "Performance / Lag",
        "raghavans5711+Cleanify_perf@gmail.com",
        "Performance / Lag",
        Icons.Default.Speed,
        listOf(
            "Stutter when scrolling media",
            "Slow storage scanning time",
            "High battery usage observed",
            "Slow thumbnail image loading",
            "Freeze when deleting large folders"
        )
    ),
    FEATURE_REQUEST(
        "Feature Request",
        "raghavans5711+Cleanify_featureReq@gmail.com",
        "Feature Request",
        Icons.Default.Lightbulb,
        listOf(
            "Add cloud backup & sync option",
            "Custom color themes creator",
            "Batch file rename support",
            "Photo tagging & favorite ratings",
            "Scheduled background auto-cleaner"
        )
    ),
    SUGGESTION(
        "Suggestion",
        "raghavans5711+Cleanify_suggestion@gmail.com",
        "Suggestion",
        Icons.Default.EditNote,
        listOf(
            "Faster thumbnail pre-caching",
            "Custom swipe action gestures",
            "AMOLED pure black theme option",
            "Audio equalizer support in player",
            "More vibrant accent color shades"
        )
    )
}

private const val DEVELOPER_WHATSAPP_NUMBER = "919621272014"
private const val DEVELOPER_TELEGRAM_HANDLE = "ug_5711"

private val RANDOM_NAMES = listOf(
    "Alex", "Rose", "Martine", "Guth", "Jordan", "Taylor", "Sam", "Chris", "Morgan", "Casey"
)

@Composable
fun FeedbackDialog(
    appVersion: String,
    initialType: FeedbackType = FeedbackType.GENERAL,
    onDismiss: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var senderName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var attachedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showSendOptionsModal by remember { mutableStateOf(false) }

    // Rotating name index
    var nameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1200)
            nameIndex = (nameIndex + 1) % RANDOM_NAMES.size
        }
    }

    // Rotating subject index for the currently selected feedback type
    var subjectIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedType) {
        subjectIndex = 0
        while (isActive) {
            delay(2000)
            subjectIndex = (subjectIndex + 1) % selectedType.sampleSubjects.size
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            attachedImages = (attachedImages + uris).distinct().take(5)
        }
    }

    val diagnosticsText = remember(appVersion) {
        getEnhancedDeviceDiagnostics(context, appVersion)
    }

    val formatMessageBody: (channel: String) -> String = { channel ->
        buildString {
            appendLine("📋 *Cleanify User Feedback*")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("🏷️ *Category:* ${selectedType.tag}")
            if (senderName.isNotBlank()) {
                appendLine("👤 *Sender:* $senderName")
            }
            if (subject.isNotBlank()) {
                appendLine("📌 *Subject:* $subject")
            }
            appendLine()
            appendLine("💬 *Message:*")
            appendLine(description.ifBlank { "_(No description entered)_" })
            if (includeDiagnostics) {
                appendLine()
                appendLine("📱 *Device Diagnostics:*")
                appendLine("━━━━━━━━━━━━━━━━━━━")
                appendLine(diagnosticsText)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 700.dp)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = selectedType.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Help us improve Cleanify",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            onDismiss()
                            onNavigateToSettings?.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Selection - 2 Aligned Scrollable Rows (General Feedback first)
                    Column {
                        Text(
                            text = "Feedback Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        val row1 = listOf(FeedbackType.GENERAL, FeedbackType.BUG_REPORT, FeedbackType.UI_THEME)
                        val row2 = listOf(FeedbackType.PERFORMANCE, FeedbackType.FEATURE_REQUEST, FeedbackType.SUGGESTION)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row1.forEach { type ->
                                    val isSelected = type == selectedType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = type },
                                        label = { Text(type.title, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = {
                                            Icon(
                                                type.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row2.forEach { type ->
                                    val isSelected = type == selectedType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = type },
                                        label = { Text(type.title, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = {
                                            Icon(
                                                type.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Optional Sender Name Field with rotating placeholder
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Name (Optional)") },
                        placeholder = {
                            Text(
                                "e.g. ${RANDOM_NAMES[nameIndex]}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Subject Field with rotating contextual placeholders
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (Optional)") },
                        placeholder = {
                            Text(
                                selectedType.sampleSubjects.getOrElse(subjectIndex) { "Feedback topic" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Subject, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Detailed Description Field with bottom-right attachment button
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("Tell us what happened, your thoughts, or ideas...") },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        // Compact Attachment Pill Button positioned neatly at bottom-right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (attachedImages.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${attachedImages.size} image(s) attached",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (attachedImages.isEmpty()) "Attach Screenshot" else "+ Add More",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (attachedImages.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                attachedImages.forEach { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(20.dp)
                                                .clickable {
                                                    attachedImages = attachedImages.filter { it != uri }
                                                }
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Visible Diagnostics Privacy Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { includeDiagnostics = !includeDiagnostics }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Device Diagnostics",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "App version, Android OS, memory & display metrics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = includeDiagnostics,
                                onCheckedChange = { includeDiagnostics = it }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (description.isBlank() && subject.isBlank()) {
                                Toast.makeText(context, "Please enter a subject or description", Toast.LENGTH_SHORT).show()
                            } else {
                                showSendOptionsModal = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Feedback")
                    }
                }
            }
        }
    }

    // Send Options Modal (Presented only after clicking Send - keeps contact info private until submit)
    if (showSendOptionsModal) {
        SendOptionsDialog(
            onDismiss = { showSendOptionsModal = false },
            onSelectWhatsApp = {
                val preparedUris = prepareFeedbackAttachments(context, attachedImages)
                val fullText = formatMessageBody("WhatsApp")
                if (preparedUris.isNotEmpty()) {
                    val intent = if (preparedUris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, preparedUris.first())
                            putExtra(Intent.EXTRA_TEXT, fullText)
                            putExtra("jid", "$DEVELOPER_WHATSAPP_NUMBER@s.whatsapp.net")
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(preparedUris))
                            putExtra(Intent.EXTRA_TEXT, fullText)
                            putExtra("jid", "$DEVELOPER_WHATSAPP_NUMBER@s.whatsapp.net")
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    try {
                        context.startActivity(intent)
                        onDismiss()
                    } catch (e: Exception) {
                        try {
                            intent.setPackage("com.whatsapp.w4b")
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e2: Exception) {
                            val encoded = URLEncoder.encode(fullText, "UTF-8")
                            val url = "https://wa.me/$DEVELOPER_WHATSAPP_NUMBER?text=$encoded"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                onDismiss()
                            } catch (e3: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Cleanify Feedback", fullText))
                                Toast.makeText(context, "Feedback copied to clipboard! Open WhatsApp to send.", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        }
                    }
                } else {
                    val encoded = URLEncoder.encode(fullText, "UTF-8")
                    val url = "https://wa.me/$DEVELOPER_WHATSAPP_NUMBER?text=$encoded"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                        onDismiss()
                    } catch (e: Exception) {
                        try {
                            intent.setPackage("com.whatsapp.w4b")
                            context.startActivity(intent)
                            onDismiss()
                        } catch (ex: Exception) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                onDismiss()
                            } catch (e3: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Cleanify Feedback", fullText))
                                Toast.makeText(context, "Feedback copied to clipboard! Open WhatsApp to send.", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        }
                    }
                }
            },
            onSelectTelegram = {
                val preparedUris = prepareFeedbackAttachments(context, attachedImages)
                val fullText = formatMessageBody("Telegram")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Cleanify Feedback", fullText))

                if (preparedUris.isNotEmpty()) {
                    val intent = if (preparedUris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, preparedUris.first())
                            putExtra(Intent.EXTRA_TEXT, fullText)
                            setPackage("org.telegram.messenger")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(preparedUris))
                            putExtra(Intent.EXTRA_TEXT, fullText)
                            setPackage("org.telegram.messenger")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    try {
                        context.startActivity(intent)
                        Toast.makeText(context, "Feedback & images prepared for Telegram! Message copied to clipboard.", Toast.LENGTH_LONG).show()
                        onDismiss()
                    } catch (e: Exception) {
                        try {
                            intent.setPackage("org.thunderdog.challegram")
                            context.startActivity(intent)
                            Toast.makeText(context, "Feedback & images prepared for Telegram! Message copied to clipboard.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } catch (e2: Exception) {
                            val tgUrl = "https://t.me/$DEVELOPER_TELEGRAM_HANDLE"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl)))
                                onDismiss()
                            } catch (e3: Exception) {
                                onDismiss()
                            }
                        }
                    }
                } else {
                    val tgUrl = "https://t.me/$DEVELOPER_TELEGRAM_HANDLE"
                    try {
                        val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl)).apply {
                            setPackage("org.telegram.messenger")
                        }
                        context.startActivity(tgIntent)
                        Toast.makeText(context, "Feedback copied to clipboard! Paste it into @$DEVELOPER_TELEGRAM_HANDLE", Toast.LENGTH_LONG).show()
                        onDismiss()
                    } catch (e: Exception) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl)))
                            Toast.makeText(context, "Feedback copied to clipboard! Paste it into @$DEVELOPER_TELEGRAM_HANDLE", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } catch (ex: Exception) {
                            onDismiss()
                        }
                    }
                }
            },
            onSelectEmail = {
                val preparedUris = prepareFeedbackAttachments(context, attachedImages)
                val fullText = formatMessageBody("Email")
                val emailSubject = "[Cleanify ${selectedType.tag}] ${subject.ifBlank { "App Feedback" }}"
                val intent = if (preparedUris.size > 1) {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(selectedType.emailAlias))
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        putExtra(Intent.EXTRA_TEXT, fullText)
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(preparedUris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        type = if (preparedUris.isNotEmpty()) "*/*" else "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(selectedType.emailAlias))
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        putExtra(Intent.EXTRA_TEXT, fullText)
                        if (preparedUris.isNotEmpty()) {
                            putExtra(Intent.EXTRA_STREAM, preparedUris.first())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "Send Email via..."))
                    onDismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

private fun prepareFeedbackAttachments(context: Context, uris: List<Uri>): List<Uri> {
    if (uris.isEmpty()) return emptyList()
    val cacheDir = File(context.cacheDir, "feedback_attachments").apply {
        deleteRecursively()
        mkdirs()
    }
    val contentUris = mutableListOf<Uri>()
    uris.forEachIndexed { index, uri ->
        try {
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
            val tempFile = File(cacheDir, "feedback_attach_${System.currentTimeMillis()}_$index.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )
                contentUris.add(contentUri)
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedbackDialog", "Failed to cache attachment $uri", e)
        }
    }
    return contentUris
}

@Composable
private fun SendOptionsDialog(
    onDismiss: () -> Unit,
    onSelectWhatsApp: () -> Unit,
    onSelectTelegram: () -> Unit,
    onSelectEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Send via...", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose your preferred channel to deliver feedback directly to the developer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // WhatsApp Option
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectWhatsApp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF25D366).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WhatsApp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Fast direct chat & instant support", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Telegram Option
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectTelegram)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0088CC).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF0088CC), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Telegram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Direct message to @ug_5711", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Email Option
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectEmail)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Categorized developer inbox alias", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getEnhancedDeviceDiagnostics(context: Context, appVersion: String): String {
    return buildString {
        appendLine("--- Device & Environment Diagnostics ---")
        appendLine("Cleanify Version: v$appVersion")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build Fingerprint: ${Build.FINGERPRINT.take(60)}...")
        appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        appendLine("Locale: ${Locale.getDefault().toLanguageTag()}")

        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)
            val availRamGb = memInfo.availMem / (1024 * 1024 * 1024.0)
            appendLine(String.format(Locale.US, "RAM: %.1f GB free / %.1f GB total", availRamGb, totalRamGb))
        } catch (_: Exception) {}

        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val freeGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024.0)
            val totalGb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024.0)
            appendLine(String.format(Locale.US, "Internal Storage: %.1f GB free / %.1f GB total", freeGb, totalGb))
        } catch (_: Exception) {}

        val dm = context.resources.displayMetrics
        appendLine("Display: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi} dpi)")
        append("----------------------------------------")
    }
}
