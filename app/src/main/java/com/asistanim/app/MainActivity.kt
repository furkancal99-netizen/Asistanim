package com.asistanim.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.ai.ai
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

import java.text.SimpleDateFormat
import java.util.*

data class Message(val id: Long, val text: String, val user: Boolean)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    FirebaseApp.initializeApp(this)

    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
    )
FirebaseAppCheck.getInstance().getAppCheckToken(false)
    .addOnSuccessListener {
        android.widget.Toast.makeText(
            this, "App Check token alındı",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    .addOnFailureListener { error ->
    val mesaj = android.util.Log.getStackTraceString(error)

    android.app.AlertDialog.Builder(this)
        .setTitle("App Check Hatası")
        .setMessage(mesaj)
        .setPositiveButton("Kapat", null)
        .setNeutralButton("Kopyala") { _, _ ->
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("App Check", mesaj)
            )
        }
        .show()

    Log.e("APP_CHECK_TEST", mesaj)
}

    }
override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale("tr", "TR")
    }
}

private fun say(s: String) {
    tts?.speak(s, TextToSpeech.QUEUE_FLUSH, null, null)
}

override fun onDestroy() {
    tts?.shutdown()
    super.onDestroy()
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantApp(speak: (String) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("assistant_notes", 0) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(listOf(Message(0, "Merhaba! Ben Asistanım. Notlarını kaydedebilir, saati söyleyebilir ve desteklenen uygulamaları açabilirim. Genel yapay zekâ bağlantısı sonraki aşamada eklenecek.", false))) }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) listen(ctx) { input = it } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF67DDF5), background = Color(0xFF0B1020), surface = Color(0xFF17233A))) {
        Scaffold(containerColor = Color(0xFF0B1020), topBar = {
            TopAppBar(title = { Column { Text("Asistanım", color = Color.White, fontWeight = FontWeight.Bold); Text("Kişisel yapay zekâ asistanın", color = Color.LightGray, fontSize = 12.sp) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111B30)))
        }, bottomBar = {
            Row(Modifier.fillMaxWidth().background(Color(0xFF111B30)).imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Mesajını yaz…") }, maxLines = 3)
                IconButton(onClick = { if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen(ctx) { input = it } else mic.launch(Manifest.permission.RECORD_AUDIO) }) { Icon(Icons.Default.Mic, "Konuş") }
                IconButton(enabled = input.isNotBlank() && !busy, onClick = {
                    val q = input.trim(); input = ""; items = items + Message(System.nanoTime(), q, true); busy = true
                    scope.launch {
                        Log.d("ASISTAN_TEST", "Gemini isteği başladı: $q")
val a = onlineAnswer(ctx, q, prefs)
Log.d("ASISTAN_TEST", "Gemini cevabı geldi: $a")
                        items = items + Message(System.nanoTime(), a, false); busy = false; speak(a)
                    }
                }) { Icon(Icons.Default.Send, "Gönder") }
            }
        }) { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (m.user) Color(0xFF24456A) else Color(0xFF192941), shape = RoundedCornerShape(16.dp)) {
                            Text(m.text, Modifier.widthIn(max = 310.dp).padding(13.dp), color = Color.White, lineHeight = 21.sp)
                        }
                    }
                }
                if (busy) item { Text("Yanıt hazırlanıyor…", color = Color.LightGray) }
            }
        }
    }
}

private fun listen(ctx: android.content.Context, result: (String) -> Unit) {
    if (!SpeechRecognizer.isRecognitionAvailable(ctx)) return
    val sr = SpeechRecognizer.createSpeechRecognizer(ctx)
    sr.setRecognitionListener(object : RecognitionListener {
        override fun onResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(result); sr.destroy() }
        override fun onError(e: Int) { sr.destroy() }
        override fun onReadyForSpeech(p: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(r: Float) {}
        override fun onBufferReceived(b: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(b: Bundle?) {}
        override fun onEvent(t: Int, b: Bundle?) {}
    })
    sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"))
}

private fun localAnswer(ctx: android.content.Context, q: String, prefs: android.content.SharedPreferences): String {
    val s = q.lowercase(Locale("tr", "TR"))
    if (s.startsWith("not al:") || s.startsWith("hatırla:")) {
        val note = q.substringAfter(":").trim()
        prefs.edit().putString("n_${System.currentTimeMillis()}", note).apply()
        return "Notunu bu telefona kaydettim."
    }
    if (s.contains("notlarım") || s.contains("notlarimi")) {
        val notes = prefs.all.filterKeys { it.startsWith("n_") }.values.filterIsInstance<String>()
        return if (notes.isEmpty()) "Henüz kayıtlı notun yok." else notes.joinToString(separator = "\n", prefix = "Notların:\n• ", postfix = "\n")
    }
    if (s.contains("saat kaç")) return "Saat: " + SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(Date())
    val apps = listOf("youtube" to "com.google.android.youtube", "whatsapp" to "com.whatsapp", "chrome" to "com.android.chrome", "ayarlar" to "com.android.settings")
    val target = apps.firstOrNull { s.contains(it.first) && (s.contains("aç") || s.contains("başlat")) }
    if (target != null) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(target.second)
        if (launch != null) { ctx.startActivity(launch); return "${target.first} açılıyor." }
        return "${target.first} uygulaması bulunamadı."
    }
    return "Bu özellik şu anda çevrimdışı temel modda. “Not al: …”, “Notlarımı göster”, “Saat kaç?” veya “YouTube aç” gibi komutları deneyebilirsin. Bulut yapay zekâ bağlantısını sonraki aşamada güvenli bir sunucu üzerinden ekleyeceğiz."
}


private suspend fun groqAnswer(q: String): String =
    withContext(Dispatchers.IO) {
        val connection = URL("https://asistanim-groq.furkancal99.workers.dev")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
val appCheckToken = com.google.android.gms.tasks.Tasks.await(
    FirebaseAppCheck.getInstance().getAppCheckToken(false)
).token
connection.setRequestProperty("X-Firebase-AppCheck", appCheckToken)

            val body = JSONObject().put("message", q).toString()
            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val result = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(result)

            if (code !in 200..299) {
                throw Exception(json.optString("error", "Groq bağlantı hatası"))
            }

            json.optString("answer", "Groq boş yanıt verdi.")
        } finally {
            connection.disconnect()
        }
    }

private suspend fun onlineAnswer(
    ctx: android.content.Context,
    q: String,
    prefs: android.content.SharedPreferences
): String {
    val local = localAnswer(ctx, q, prefs)

    if (!local.startsWith("Bu özellik şu anda çevrimdışı")) {
        return local
    }

    return try {
        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")

        val response = model.generateContent(q)
        response.text ?: "Yapay zekâ boş yanıt verdi."

    } catch (geminiError: Exception) {
        try {
            "Gemini kullanılamıyor. Groq yanıtı:\n${groqAnswer(q)}"
        } catch (groqError: Exception) {
            "Gemini hatası: ${geminiError.message}\nGroq hatası: ${groqError.message}"
        }
    }
}
}
