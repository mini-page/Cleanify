package com.cleanify.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.cleanify.R
import com.cleanify.ui.components.AppDialog
import com.cleanify.util.PermissionManager
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val gradientColor: Color
)

private val pages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_p1_title,
        descriptionRes = R.string.onboarding_p1_desc,
        icon = Icons.Default.AutoAwesome,
        gradientColor = Color(0xFF6366F1)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_p2_title,
        descriptionRes = R.string.onboarding_p2_desc,
        icon = Icons.Default.SwipeVertical,
        gradientColor = Color(0xFF06B6D4)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_p3_title,
        descriptionRes = R.string.onboarding_p3_desc,
        icon = Icons.Default.Description,
        gradientColor = Color(0xFF10B981)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_p4_title,
        descriptionRes = R.string.onboarding_p4_desc,
        icon = Icons.Default.CreateNewFolder,
        gradientColor = Color(0xFFF59E0B)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_p5_title,
        descriptionRes = R.string.onboarding_p5_desc,
        icon = Icons.Default.Storage,
        gradientColor = Color(0xFFEF4444)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_p6_title,
        descriptionRes = R.string.onboarding_p6_desc,
        icon = Icons.Default.Lock,
        gradientColor = Color(0xFF8B5CF6)
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var hasAllFilesAccess by remember { mutableStateOf(PermissionManager.hasAllFilesAccess()) }
    var showPermissionScreen by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val storagePermissionLauncher = remember {
        try {
            val activity = context as? ComponentActivity
            if (activity != null) {
                PermissionManager.registerAllFilesAccessLauncher(activity) { granted ->
                    hasAllFilesAccess = granted
                    if (!granted) showPermissionScreen = true
                }
            } else null
        } catch (e: Exception) {
            println("Error creating permission launcher: ${e.message}")
            null
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val newPermissionState = PermissionManager.hasAllFilesAccess()
                if (newPermissionState != hasAllFilesAccess) {
                    hasAllFilesAccess = newPermissionState
                    if (newPermissionState) showPermissionScreen = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 8.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!isLastPage) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pages.size - 1) } },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(modifier = Modifier.size(width = 68.dp, height = 48.dp))
            }
        }

        if (showPermissionScreen) {
            PermissionRequiredScreen(
                onGrant = {
                    storagePermissionLauncher?.let { launcher ->
                        PermissionManager.requestAllFilesAccess(context, launcher)
                    } ?: run {
                        try {
                            val intent = PermissionManager.createAllFilesAccessIntent(context)
                            if (intent != null) context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                },
                onClose = { showCloseDialog = true }
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = if (isLastPage) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            if (!isLastPage) {
                if (pagerState.currentPage > 0) {
                    FilledTonalButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier
                            .height(54.dp)
                            .weight(1f)
                            .padding(end = 6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.onboarding_previous),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = if (pagerState.currentPage > 0) {
                        Modifier
                            .height(54.dp)
                            .weight(1f)
                            .padding(start = 6.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                if (!hasAllFilesAccess) {
                    Button(
                        onClick = {
                            storagePermissionLauncher?.let { launcher ->
                                PermissionManager.requestAllFilesAccess(context, launcher)
                            } ?: run { showPermissionScreen = true }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.onboarding_grant_permission),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeOnboarding()
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.onboarding_get_started),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showCloseDialog) {
        AppDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text(stringResource(R.string.onboarding_close_dialog_title), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.onboarding_close_dialog_body), style = MaterialTheme.typography.bodyMedium) },
            buttons = {
                TextButton(onClick = {
                    showCloseDialog = false
                    showPermissionScreen = true
                }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    (context as? ComponentActivity)?.finish()
                }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(page.gradientColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(page.gradientColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = page.gradientColor
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(page.titleRes),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            lineHeight = 26.sp
        )
    }
}

@Composable
private fun PermissionRequiredScreen(
    onGrant: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.permission_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.permission_screen_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.permission_screen_instruction),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.open_settings))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.close_app))
            }
        }
    }
}
