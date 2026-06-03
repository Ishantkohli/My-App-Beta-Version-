package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class IkVoiceService : Service(), TextToSpeech.OnInitListener {

    private lateinit var notificationManager: NotificationManager
    private var consecutiveErrors = 0
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isSpeechActive = false
    private var isListeningLoopRunning = false
    private var isRecognizerSessionActive = false
    private var restartJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initializeSpeechRecognizer()
        if (intent != null && intent.getBooleanExtra("force_listen", false)) {
            _isUserVoiceActive.value = true
            _voiceAssistantStatus.value = "IDLE"
            isListeningLoopRunning = true
            isRecognizerSessionActive = false
        }
        if (isListeningLoopRunning && isUserVoiceActive.value && isAppInForeground) {
            startListening()
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Persistent notification for Strategy B continuous listening
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IK Voice Assistant (Strategy B)")
            .setContentText("IK Speech Engine is listening for always-on voice commands.")
            .setSmallIcon(com.example.R.drawable.ic_overlay_status)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                try {
                    if (hasMicPermission) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        )
                    } else {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    }
                } catch (e: SecurityException) {
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Text To Speech
        tts = TextToSpeech(this, this)

        // Initialize Speech Recognizer safely
        initializeSpeechRecognizer()

        // Set static state
        isServiceRunningNow = true
        updateServiceState(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Speech Engine Active",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeSpeechRecognizer() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            _isVoiceServiceActive.value = false
            isListeningLoopRunning = false
            return
        }

        if (speechRecognizer != null) return

        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        consecutiveErrors = 0
                        isSpeechActive = true
                        updateStatusFlow("LISTENING")
                    }

                    override fun onBeginningOfSpeech() {
                        updateStatusFlow("SPEAKING")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDbLive.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        updateStatusFlow("PROCESSING")
                    }

                    override fun onError(error: Int) {
                        isRecognizerSessionActive = false
                        isSpeechActive = false
                        updateStatusFlow("IDLE")
                        consecutiveErrors++
                        if (consecutiveErrors >= 5) {
                            _isUserVoiceActive.value = false
                            isListeningLoopRunning = false
                            consecutiveErrors = 0
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "Voice control paused due to system limitations or unavailable voice engine.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            scheduleRestart()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        consecutiveErrors = 0
                        isRecognizerSessionActive = false
                        isSpeechActive = false
                        updateStatusFlow("IDLE")
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spokenText = matches[0]
                            handleVoiceCommand(spokenText)
                        }
                        scheduleRestart()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("en-IN", "hi-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }

                isListeningLoopRunning = true
                _isVoiceServiceActive.value = true
                updateStatusFlow("IDLE")
            } else {
                Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_LONG).show()
                stopSelf()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            _isVoiceServiceActive.value = false
        }
    }

    private fun startListening() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            _isVoiceServiceActive.value = false
            isListeningLoopRunning = false
            return
        }

        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }

        if (!isListeningLoopRunning || !isUserVoiceActive.value || speechRecognizer == null || !isAppInForeground) return
        if (isRecognizerSessionActive) return
        
        isRecognizerSessionActive = true
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Throwable) {
            e.printStackTrace()
            isRecognizerSessionActive = false
        }
    }

    private fun scheduleRestart() {
        Handler(Looper.getMainLooper()).post {
            if (!isListeningLoopRunning || !isUserVoiceActive.value || speechRecognizer == null || !isAppInForeground) return@post
            if (isRecognizerSessionActive) return@post
            
            restartJob?.cancel()
            restartJob = scope.launch {
                delay(1500) // Cooldown period to prevent rapid start-stop beep sound loop and keep it lag-free
                if (isListeningLoopRunning && isUserVoiceActive.value && !isRecognizerSessionActive && speechRecognizer != null && isAppInForeground) {
                    startListening()
                }
            }
        }
    }

    private fun handleVoiceCommand(spokenText: String) {
        Handler(Looper.getMainLooper()).post {
            _lastRecognizedText.value = spokenText
            
            val (message, action) = processCommandText(spokenText)
            
            if (action != null) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                action.invoke()
            }
        }
    }

    private fun processCommandText(rawText: String): Pair<String, (() -> Unit)?> {
        val text = rawText.lowercase().trim()
        
        // Match "ishu" trigger
        if (!text.contains("ishu") && !text.contains("ishoo") && !text.contains("eeshu") && !text.contains("issu") && !text.startsWith("hey ishu") && !text.contains("eesu") && !text.contains("issue")) {
            return Pair("No trigger", null) 
        }
        
        // Extract post-trigger parts
        var cmd = text
        val triggers = listOf("ishu", "ishoo", "eeshu", "issu", "hey ishu", "eesu", "issue")
        for (trig in triggers) {
            val idx = text.indexOf(trig)
            if (idx != -1) {
                cmd = text.substring(idx + trig.length).trim()
                break
            }
        }
        
        if (cmd.isEmpty()) {
            return Pair("Yes, Commander?", { speak("Yes, Commander? Online and waiting.") })
        }
        
        // Navigation: Dashboard / Bootloader
        if (cmd.contains("bootloader") || cmd.contains("dashboard") || cmd.contains("home") || cmd.contains("main") || cmd.contains("kholo") || cmd.contains("system")) {
            return Pair("Opening ISHU Bootloader Dashboard", {
                SystemViewModel.instance?.selectTab(ActiveTab.PERFORMANCE)
                bringAppToForeground()
                speak("Accessing active system node. Bootloader dashboard loaded.")
            })
        }
        
        // Clean RAM & Cache
        if (cmd.contains("clear ram") || cmd.contains("clean ram") || cmd.contains("free ram") || 
            cmd.contains("ram sweep") || cmd.contains("ram clear") || cmd.contains("ram clean") ||
            cmd.contains("ram saf") || cmd.contains("ram saaf") || cmd.contains("saf karo") ||
            cmd.contains("saaf karo") || cmd.contains("free up ram") || cmd.contains("clean cache") ||
            cmd.contains("ram clean kar") || cmd.contains("ram clean karo") || cmd.contains("cache clean") ||
            cmd.contains("cache saaf") || cmd.contains("cache clean kar")) {
            return Pair("Clearing system RAM and cached processes...", {
                SystemViewModel.instance?.triggerRamClean()
                speak("Engaging hyper-sweeper protocols. Cleaned RAM and cleared background cache successfully.")
            })
        }

        // Close Voice Assistant (IK Close)
        if (cmd.contains("close") || cmd.contains("stop listening") || cmd.contains("band") || cmd.contains("chup") || cmd.contains("offline") || cmd.contains("inactive")) {
            return Pair("Deactivating local voice listener...", {
                _isUserVoiceActive.value = false
                _voiceAssistantStatus.value = "CLOSED"
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                speak("Voice loops stopped. Deactivated continuous microphone listener.")
            })
        }

        // FPS Overlay
        if (cmd.contains("fps")) {
            val isEnable = cmd.contains("on") || cmd.contains("enable") || cmd.contains("show") || cmd.contains("chalu") || cmd.contains("start") || cmd.contains("laga")
            val isDisable = cmd.contains("off") || cmd.contains("disable") || cmd.contains("hide") || cmd.contains("band") || cmd.contains("stop") || cmd.contains("hata")
            
            if (isEnable) {
                return Pair("Enabling real-time FPS overlay", {
                    SystemViewModel.instance?.toggleFpsOverlay(true)
                    speak("Initializing real-time telemetry overlays. Core stats are now visible.")
                })
            } else if (isDisable) {
                return Pair("Disabling real-time FPS overlay", {
                    SystemViewModel.instance?.toggleFpsOverlay(false)
                    speak("Deactivating overlay matrices. Telemetry link is terminated.")
                })
            }
        }

        // RAM Defrag / Memory Sort
        if (cmd.contains("defrag") || cmd.contains("defragment") || cmd.contains("optimize ram") || cmd.contains("ram optimize")) {
            return Pair("Defragmenting memory pages...", {
                SystemViewModel.instance?.runMemoryDefrag()
                speak("Synchronizing RAM heap pages. Memory allocation aligned.")
            })
        }

        // Optimizations / Deep clean
        if (cmd.contains("deep clean") || cmd.contains("optimize cpu") || cmd.contains("optimize system") || cmd.contains("boost") || cmd.contains("deep cleaner")) {
            return Pair("Running deep system diagnostics and optimization...", {
                SystemViewModel.instance?.detectAndOptimizeState()
                speak("Executing deep memory analysis. Silicon performance is optimized.")
            })
        }

        // Apps tab
        if (cmd.contains("app") || cmd.contains("bloatware") || cmd.contains("workspace")) {
            return Pair("Opening App Workspace", {
                SystemViewModel.instance?.selectTab(ActiveTab.APPS)
                bringAppToForeground()
                speak("Entering package registry. Showing dynamic app lists.")
            })
        }

        // Terminal tab
        if (cmd.contains("terminal") || cmd.contains("console") || cmd.contains("coding") || cmd.contains("term")) {
            return Pair("Opening Admin Console Terminal", {
                SystemViewModel.instance?.selectTab(ActiveTab.TERMINAL)
                bringAppToForeground()
                speak("Command console online. Ready for manual shell code injections.")
            })
        }

        // Stealth / Sleep Mode
        if (cmd.contains("sleep") || cmd.contains("stealth") || cmd.contains("silent") || cmd.contains("deep sleep")) {
            return Pair("Transitioning to Stealth Sleep Mode", {
                SystemViewModel.instance?.selectTab(ActiveTab.SLEEP)
                bringAppToForeground()
                speak("Engaging core hibernation. Power levels descending to silent mode.")
            })
        }

        // Device Info / About
        if (cmd.contains("about") || cmd.contains("info") || cmd.contains("details") || cmd.contains("hardware")) {
            return Pair("Showing Silicon Hardware profiles", {
                SystemViewModel.instance?.selectTab(ActiveTab.ABOUT)
                bringAppToForeground()
                speak("Reading native system parameters. Board hardware profile loaded.")
            })
        }

        return Pair("Command Unknown: $cmd", {
            speak("Command unresolved, Commander. Say ishu clear ram, ishu open terminal, or ishu toggle fps.")
        })
    }

    private fun bringAppToForeground() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speak(text: String) {
        Handler(Looper.getMainLooper()).post {
            if (isTtsReady && tts != null && _isTtsFeedbackEnabled.value) {
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isRecognizerSessionActive = false
                isSpeechActive = true // Mark speech as active during TTS to suppress voice trigger listener
                updateStatusFlow("SPEAKING")
                
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "IK_TTS_CMD")
                }
                try {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "IK_TTS_CMD")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                it.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeechActive = true
                        updateStatusFlow("SPEAKING")
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeechActive = false
                        updateStatusFlow("IDLE")
                        scheduleRestart()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeechActive = false
                        updateStatusFlow("IDLE")
                        scheduleRestart()
                    }
                })

                val result = it.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                    it.setPitch(0.9f)
                    it.setSpeechRate(1.05f)
                    speak("IK voice control online.")
                }
            }
        }
    }

    private fun updateStatusFlow(state: String) {
        _voiceAssistantStatus.value = state
    }

    override fun onDestroy() {
        isServiceRunningNow = false
        updateServiceState(false)
        isListeningLoopRunning = false
        isRecognizerSessionActive = false
        restartJob?.cancel()
        
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        speechRecognizer = null

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tts = null

        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 9188
        private const val CHANNEL_ID = "IK_VOICE_SERVICE_CHANNEL"

        // Reactive states for UI bindings
        val _isVoiceServiceActive = MutableStateFlow(false)
        val isVoiceServiceActive: StateFlow<Boolean> = _isVoiceServiceActive.asStateFlow()

        val _isUserVoiceActive = MutableStateFlow(true)
        val isUserVoiceActive: StateFlow<Boolean> = _isUserVoiceActive.asStateFlow()

        val _voiceAssistantStatus = MutableStateFlow("OFFLINE")
        val voiceAssistantStatus: StateFlow<String> = _voiceAssistantStatus.asStateFlow()

        val _rmsDbLive = MutableStateFlow(0f)
        val rmsDbLive: StateFlow<Float> = _rmsDbLive.asStateFlow()

        val _lastRecognizedText = MutableStateFlow("")
        val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

        val _isTtsFeedbackEnabled = MutableStateFlow(true)
        val isTtsFeedbackEnabled: StateFlow<Boolean> = _isTtsFeedbackEnabled.asStateFlow()

        @Volatile
        var isServiceRunningNow = false

        @Volatile
        var isAppInForeground = false

        fun updateServiceState(active: Boolean) {
            _isVoiceServiceActive.value = active
            if (!active) {
                _voiceAssistantStatus.value = "OFFLINE"
            } else if (_voiceAssistantStatus.value == "OFFLINE") {
                _voiceAssistantStatus.value = "IDLE"
            }
        }

        fun startServiceSafely(context: Context, forceListen: Boolean = false) {
            val intent = Intent(context, IkVoiceService::class.java).apply {
                if (forceListen) {
                    putExtra("force_listen", true)
                }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
