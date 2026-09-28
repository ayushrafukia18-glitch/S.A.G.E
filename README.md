# SAGE — Personal AI Assistant (Milestone 1)

SAGE is a private, responsive personal AI assistant built with modern Android standards: Kotlin, Jetpack Compose, Material 3, Room persistence, and EncryptedSharedPreferences.

Milestone 1 delivers the core conversational foundation: minimal onboarding with live API key testing, persistent Room-backed chat history, robust Gemini question-answering with contextual history, and an in-app settings screen.

---

## Features Implemented in Milestone 1

1. **Minimal Onboarding**:
   - Single clean setup screen asking for:
     - User Name
     - Gemini API Key (**Required**)
     - Grok / xAI API Key (*Optional*)
   - Per-key "Test Key" button with real-time verification against the Gemini `models` endpoint and xAI `models` endpoint.
   - Secure storage via `EncryptedSharedPreferences` (AES-256 GCM).
   - App is strictly blocked from the main chat until at least a verified/valid Gemini API key is provided. ("Skip for now" is strictly forbidden).

2. **Chat Screen (Main UI)**:
   - Modern Material 3 conversational UI.
   - Message bubbles: User messages aligned right in primary tone, Sage messages aligned left in surface variant tone.
   - One-tap quick copy action on Sage responses.
   - Full persistence using **Room DB** (`MessageEntity`, `MessageDao`, `AppDatabase`). Messages survive app restarts and process recreation.
   - Animated loading bubble indicator ("Sage is thinking…") while awaiting model responses.
   - Quick starter suggestions (e.g. *"What is the capital of Norway?"*) for first launches.

3. **Gemini Question-Answering**:
   - Direct REST connection to Google's generative language endpoint using the latest stable flash model (`gemini-2.5-flash`).
   - Sends recent chat history context for coherent dialogue.
   - Comprehensive error handling:
     - **HTTP 401 / Invalid Key**: Displays *"Invalid Gemini key — update it in Settings"*
     - **HTTP 429**: Displays *"Quota exceeded — add another key or wait"*
     - **Network Failure / Airplane Mode**: Displays *"No internet connection. Please check your network and try again."*
   - Strictly plain Q&A (no tool calling, calls, or SMS in M1).

4. **Settings Screen**:
   - View and edit stored profile name and API keys.
   - "Test Key" buttons for on-demand key validation.
   - "Clear Chat History" with confirmation dialog to purge Room DB messages.
   - "Wipe All Keys" with confirmation dialog to purge secure preferences. Automatically resets back to Onboarding if the Gemini key is removed.

---

## Folder Structure (Section 10 Spec Compliant)

```
app/src/main/java/com/example/
├── SageApplication.kt              # App container initializing Room & services
├── MainActivity.kt                 # Activity host with edge-to-edge Compose
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Room Database definition
│   │   ├── MessageDao.kt           # Room DAO queries & inserts
│   │   ├── MessageEntity.kt        # Room Message entity
│   │   └── SecurityPreferences.kt  # EncryptedSharedPreferences wrapper
│   ├── remote/
│   │   ├── ApiResult.kt            # Sealed result class with status classifications
│   │   ├── GeminiApiService.kt     # Gemini listModels & generateContent
│   │   └── GrokApiService.kt       # xAI models key verification
│   └── repository/
│       ├── ChatRepository.kt       # Chat coordination, Room persistence & Gemini API
│       └── SettingsRepository.kt   # Key testing, profile storage & wiping
└── ui/
    ├── components/
    │   ├── KeyStatusBadge.kt       # Verification status pill
    │   └── MessageBubble.kt        # User and Sage chat bubbles & loading state
    ├── navigation/
    │   └── SageNavHost.kt          # Type-safe Jetpack Navigation routing
    ├── onboarding/
    │   ├── OnboardingScreen.kt     # Initial credential & setup UI
    │   └── OnboardingViewModel.kt  # Key test & validation logic
    ├── chat/
    │   ├── ChatScreen.kt           # Conversation UI & input bar
    │   └── ChatViewModel.kt        # Message dispatch & state flow
    ├── settings/
    │   ├── SettingsScreen.kt       # Key inspection, editing, wipe & clear
    │   └── SettingsViewModel.kt    # Settings update handlers
    └── theme/
        ├── Color.kt                # SAGE bespoke palette
        ├── Theme.kt                # M3 Light and Dark schemes
        └── Type.kt                 # Typography definitions
```

---

## Setup & Running Instructions

### 1. Requirements
- Android Studio Ladybug (2024.2+) or Meerkat
- JDK 17 or JDK 21
- Android SDK 36 (Minimum SDK: 26)

### 2. Getting API Keys
- **Gemini API Key (Required)**: Generate a free key from [Google AI Studio](https://aistudio.google.com/app/apikey).
- **xAI / Grok API Key (Optional)**: Generate a key from the [xAI Console](https://console.x.ai/).

### 3. Build & Run
1. Open the project in Android Studio.
2. Allow Gradle to sync dependencies.
3. Run the app on an Android device or emulator running API 26 or higher.
4. On first launch:
   - Type your name (e.g. "Alex").
   - Paste your Gemini API key.
   - Tap **Test Key** to verify connectivity with Gemini servers.
   - Tap **Continue to SAGE** to enter the chat.

---

## Manual Files & Configuration
- **No hardcoded API keys exist in the codebase**. All keys are entered securely by the user at runtime and stored in Android `EncryptedSharedPreferences`.
- If compiling via Gradle command line locally, ensure standard `local.properties` exists with `sdk.dir=/path/to/Android/sdk`.

## Public APIs + Needle integration
This build includes the Public APIs directory client, Supabase catalogue migration, controlled API registry design, and real Needle 3 Android native bridge. See `docs/PUBLIC_API_INTEGRATION.md` and `docs/AI_STUDIO_NEXT_STEP_PROMPT.md`.
