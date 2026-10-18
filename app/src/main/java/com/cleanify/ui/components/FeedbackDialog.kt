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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanify.R
import java.net.URLEncoder
import java.util.Locale

enum class FeedbackChannel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    WHATSAPP("WhatsApp", "+91 9621272014", Icons.Default.Chat),
    EMAIL("Email", "Developer Aliases", Icons.Default.Email)
}

enum class FeedbackType(
    val titleRes: Int,
    val emailAlias: String,
    val tag: String,
    val icon: ImageVector
) {
    BUG_REPORT(
        R.string.feedback_type_bug,
        "raghavans5711+Cleanify_bugReport@gmail.com",
        "Bug Report",
        Icons.Default.BugReport
    ),
    FEATURE_REQUEST(
        R.string.feedback_type_feature,
        "raghavans5711+Cleanify_featureReq@gmail.com",
        "Feature Request",
        Icons.Default.Lightbulb
    ),
    SUGGESTION(
        R.string.feedback_type_suggestion,
        "raghavans5711+Cleanify_suggestion@gmail.com",
        "Suggestion",
        Icons.Default.EditNote
    ),
    GENERAL(
        R.string.feedback_type_general,
        "raghavans5711+Cleanify@gmail.com",
        "General Feedback",
        Icons.Default.Chat
    )
}

private const val DEVELOPER_WHATSAPP_NUMBER = "919621272014"

@Composable
fun FeedbackDialog(
    appVersion: String,
    initialType: FeedbackType = FeedbackType.BUG_REPORT,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedChannel by remember { mutableStateOf(FeedbackChannel.WHATSAPP) }
    var selectedType by remember { mutableStateOf(initialType) }
    var senderName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }

    val diagnosticsText = remember(appVersion) {
        """
        --- Diagnostic Information ---
        App: Cleanify v$appVersion
        OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})
        ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}
        Locale: ${Locale.getDefault().toLanguageTag()}
        """.trimIndent()
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
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
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
                                imageVector = if (selectedChannel == FeedbackChannel.WHATSAPP) Icons.Default.Chat else Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.feedback_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedChannel == FeedbackChannel.WHATSAPP)
                                "Send directly via WhatsApp (+91 9621272014)"
                            else
                                "Send to: ${selectedType.emailAlias}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Send Via (Channel Selector)
                    Text(
                        text = "Send Via",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeedbackChannel.entries.forEach { channel ->
                            val isSelected = selectedChannel == channel
                            val isWhatsApp = channel == FeedbackChannel.WHATSAPP
                            val activeColor = if (isWhatsApp) Color(0xFF25D366) else MaterialTheme.colorScheme.primary

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) activeColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedChannel = channel }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = channel.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = channel.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = channel.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category
                    Text(
                        text = stringResource(R.string.feedback_category_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Categories chips grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackType.entries.take(2).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(stringResource(type.titleRes), style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackType.entries.drop(2).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(stringResource(type.titleRes), style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Sender Name (Optional)
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Your Name / Contact (Optional)") },
                        placeholder = { Text("e.g. Alex or @handle") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Subject
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(stringResource(R.string.feedback_subject_label)) },
                        placeholder = { Text("Brief summary...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.feedback_description_label)) },
                        placeholder = { Text("Describe what happened, request a feature, or give suggestions...") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Diagnostics toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeDiagnostics = !includeDiagnostics }
                    ) {
                        Checkbox(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.feedback_device_info_included),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanSubject = if (subject.isNotBlank()) subject else "Cleanify Feedback"
                            val namePrefix = if (senderName.isNotBlank()) "From: $senderName\n" else ""

                            if (selectedChannel == FeedbackChannel.WHATSAPP) {
                                // WhatsApp Submission
                                val waMessage = buildString {
                                    append("✨ *[Cleanify • ${selectedType.tag}]*\n")
                                    if (senderName.isNotBlank()) {
                                        append("👤 *From:* $senderName\n")
                                    }
                                    append("📌 *Subject:* $cleanSubject\n\n")
                                    if (description.isNotBlank()) {
                                        append("💬 *Details:*\n$description\n\n")
                                    }
                                    if (includeDiagnostics) {
                                        append(diagnosticsText)
                                    }
                                }

                                try {
                                    val encoded = URLEncoder.encode(waMessage, "UTF-8")
                                    val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/$DEVELOPER_WHATSAPP_NUMBER?text=$encoded")
                                    }
                                    context.startActivity(waIntent)
                                    onDismiss()
                                } catch (e: Exception) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Cleanify Feedback", waMessage))
                                    Toast.makeText(context, "Copied to clipboard. Opening browser...", Toast.LENGTH_SHORT).show()
                                    try {
                                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$DEVELOPER_WHATSAPP_NUMBER"))
                                        context.startActivity(fallbackIntent)
                                    } catch (_: Exception) {}
                                    onDismiss()
                                }
                            } else {
                                // Email Submission
                                val emailSubject = "[Cleanify ${selectedType.tag}] $cleanSubject"
                                val emailBodyBuilder = StringBuilder()
                                if (senderName.isNotBlank()) {
                                    emailBodyBuilder.append(namePrefix).append("\n")
                                }
                                if (description.isNotBlank()) {
                                    emailBodyBuilder.append(description).append("\n\n")
                                }
                                if (includeDiagnostics) {
                                    emailBodyBuilder.append(diagnosticsText)
                                }

                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${selectedType.emailAlias}")
                                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                    putExtra(Intent.EXTRA_TEXT, emailBodyBuilder.toString())
                                }

                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Feedback via..."))
                                    onDismiss()
                                } catch (e: Exception) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Feedback", "To: ${selectedType.emailAlias}\n$emailSubject\n\n$emailBodyBuilder"))
                                    Toast.makeText(context, "${context.getString(R.string.feedback_no_email_client)} Copied to clipboard.", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            }
                        },
                        colors = if (selectedChannel == FeedbackChannel.WHATSAPP)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                        else
                            ButtonDefaults.buttonColors(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedChannel == FeedbackChannel.WHATSAPP) Icons.Default.Chat else Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedChannel == FeedbackChannel.WHATSAPP) "Send via WhatsApp" else "Send via Email")
                    }
                }
            }
        }
    }
}
