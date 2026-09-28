package com.sage.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sage.app.SageApplication
import com.sage.app.ui.chat.ChatScreen
import com.sage.app.ui.chat.ChatViewModel
import com.sage.app.ui.onboarding.OnboardingScreen
import com.sage.app.ui.onboarding.OnboardingViewModel
import com.sage.app.ui.publicapi.PublicApiScreen
import com.sage.app.ui.settings.SettingsScreen
import com.sage.app.ui.settings.SettingsViewModel

object SageDestinations {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val PUBLIC_APIS = "public_apis"
}

@Composable
fun SageNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val app = SageApplication.instance

    // If secure storage failed on this device, show a fatal security refusal screen
    val secureError = app.secureStorageError
    if (secureError != null || app.securityPreferences == null) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Security Requirement Not Met",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Secure storage unavailable on this device.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SAGE requires hardware-backed EncryptedSharedPreferences (AES-256 GCM) to protect your API keys. Storing keys without encryption is strictly refused.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val settingsRepo = app.settingsRepository
    val chatRepo = app.chatRepository

    val startDestination = if (settingsRepo.hasValidGeminiKey()) {
        SageDestinations.CHAT
    } else {
        SageDestinations.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SageDestinations.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(settingsRepo)
            )
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onOnboardingComplete = {
                    navController.navigate(SageDestinations.CHAT) {
                        popUpTo(SageDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(SageDestinations.CHAT) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    chatRepository = chatRepo,
                    settingsRepository = settingsRepo
                )
            )
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(SageDestinations.SETTINGS) },
                onNavigateToPublicApis = { navController.navigate(SageDestinations.PUBLIC_APIS) }
            )
        }

        composable(SageDestinations.PUBLIC_APIS) {
            PublicApiScreen(onBack = { navController.popBackStack() })
        }

        composable(SageDestinations.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(settingsRepo, chatRepo)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToOnboarding = {
                    navController.navigate(SageDestinations.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
