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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanify.R
import java.util.Locale

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

@Composable
fun FeedbackDialog(
    appVersion: String,
    initialType: FeedbackType = FeedbackType.BUG_REPORT,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }

    val diagnosticsText = remember(appVersion) {
        """
        --- Diagnostic Information ---
        App Version: Cleanify v$appVersion
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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.feedback_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "To: ${selectedType.emailAlias}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                label = { Text(stringResource(type.titleRes)) },
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
                                label = { Text(stringResource(type.titleRes)) },
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

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(stringResource(R.string.feedback_subject_label)) },
                        placeholder = { Text("Brief summary...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.feedback_description_label)) },
                        placeholder = { Text("Provide details, steps to reproduce, or feature ideas...") },
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

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
                            val emailSubject = "[Cleanify ${selectedType.tag}] ${if (subject.isNotBlank()) subject else "App Feedback"}"
                            val emailBodyBuilder = StringBuilder()
                            if (description.isNotBlank()) {
                                emailBodyBuilder.append(description)
                                emailBodyBuilder.append("\n\n")
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
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feedback_send_button))
                    }
                }
            }
        }
    }
}
