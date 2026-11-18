package com.kathir.buddy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject

/**
 * BUDDY AI (MIRA) 1.0
 * Tap-to-talk and optional BYO-key online chat; no background microphone service.
 */
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var transcript: TextView
    private lateinit var statusLine: TextView
    private lateinit var input: EditText
    private lateinit var conversationScroll: ScrollView
    private lateinit var languageButton: Button
    private var tts: TextToSpeech? = null
    private var speechReady = false
    private var tamilMode = false
    private var busy = false
    private val recentChat = ArrayDeque<Pair<String, String>>()

    private val background = Color.rgb(10, 24, 43)
    private val surface = Color.rgb(24, 45, 67)
    private val mint = Color.rgb(77, 220, 194)
    private val white = Color.rgb(242, 247, 251)
    private val pale = Color.rgb(172, 195, 213)
    private val speechRequest = 121
    private val micRequest = 122
    private val modelName = "gpt-4o-mini"
    private val keyAlias = "buddy_mira_openai_key_v1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        showInterface()
        tts = TextToSpeech(this, this)
        display("MIRA", "Welcome to BUDDY AI. I am MIRA, your HONOR tablet assistant. Tap Talk or type a command. வணக்கம்!")
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun button(title: String, accented: Boolean = false, onTap: () -> Unit): Button =
        Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            setTextColor(if (accented) background else white)
            background = rounded(if (accented) mint else surface, 14)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { onTap() }
        }

    private fun showInterface() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val heading = TextView(this).apply {
            text = "BUDDY AI  /  MIRA"
            textSize = 25f
            setTextColor(white)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        outer.addView(heading)
        val subtitle = TextView(this).apply {
            text = "HONOR Pad 10  •  Voice + AI  •  Tamil / English"
            textSize = 12f
            setTextColor(pale)
            setPadding(0, dp(4), 0, dp(10))
        }
        outer.addView(subtitle)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val talkButton = button("●  Talk", true) { requestMicrophone() }
        actions.addView(talkButton, LinearLayout.LayoutParams(0, dp(51), 1.3f))
        languageButton = button("English") { changeLanguage() }
        val languageParams = LinearLayout.LayoutParams(0, dp(51), 1f).apply { leftMargin = dp(7) }
        actions.addView(languageButton, languageParams)
        val apiButton = button("AI key") { showKeyDialog() }
        val apiParams = LinearLayout.LayoutParams(0, dp(51), 0.9f).apply { leftMargin = dp(7) }
        actions.addView(apiButton, apiParams)
        outer.addView(actions)

        statusLine = TextView(this).apply {
            text = "Ready • Tap Talk to speak. AI key optional."
            setTextColor(pale)
            textSize = 12f
            setPadding(0, dp(10), 0, dp(10))
        }
        outer.addView(statusLine)

        val quickScroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val quicks = listOf(
            "Time" to "time", "Battery" to "battery", "YouTube" to "open youtube",
            "WhatsApp" to "open whatsapp", "Wi-Fi" to "wifi settings",
            "Settings" to "open settings", "Help" to "help"
        )
        quicks.forEach { (title, cmd) ->
            val b = button(title) { process(cmd) }
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(43)).apply { rightMargin = dp(7) }
            quickRow.addView(b, p)
        }
        quickScroll.addView(quickRow)
        outer.addView(quickScroll)

        conversationScroll = ScrollView(this).apply {
            background = rounded(surface)
            isFillViewport = true
        }
        transcript = TextView(this).apply {
            textSize = 16f
            setTextColor(white)
            setPadding(dp(20), dp(14), dp(20), dp(18))
            setTextIsSelectable(true)
        }
        conversationScroll.addView(transcript)
        val scrollParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = dp(10)
            bottomMargin = dp(10)
        }
        outer.addView(conversationScroll, scrollParams)

        val entry = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Ask MIRA or enter a command..."
            setHintTextColor(Color.rgb(130, 153, 172))
            setTextColor(white)
            textSize = 15f
            background = rounded(surface, 14)
            setSingleLine(false)
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        entry.addView(input, LinearLayout.LayoutParams(0, dp(57), 1f))
        val sendButton = button("Send", true) { sendTyped() }
        entry.addView(sendButton, LinearLayout.LayoutParams(dp(82), dp(57)).apply { leftMargin = dp(8) })
        outer.addView(entry)
        val footer = TextView(this).apply {
            text = "Private local shortcuts • OpenAI key required for online AI • No background recording"
            setTextColor(pale)
            textSize = 10.5f
            setPadding(0, dp(9), 0, dp(2))
        }
        outer.addView(footer)
        setContentView(outer)
    }

    private fun changeLanguage() {
        tamilMode = !tamilMode
        languageButton.text = if (tamilMode) "தமிழ்" else "English"
        input.hint = if (tamilMode) "மீராவிடம் கேளுங்கள்..." else "Ask MIRA or enter a command..."
        tts?.language = Locale.forLanguageTag(if (tamilMode) "ta-IN" else "en-IN")
        statusLine.text = if (tamilMode) "தமிழ் தேர்வு செய்யப்பட்டது" else "English selected"
    }

    private fun sendTyped() {
        val words = input.text.toString().trim()
        if (words.isEmpty()) return
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(input.windowToken, 0)
        process(words)
    }

    private fun requestMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        } else beginListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) beginListening()
            else statusLine.text = "Microphone denied. Text commands still work."
        }
    }

    private fun beginListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (tamilMode) "ta-IN" else "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to MIRA / மீராவிடம் பேசுங்கள்")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            statusLine.text = "Listening through Android speech recognition..."
            @Suppress("DEPRECATION")
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            statusLine.text = "No Android speech service found; use typed input."
        }
    }

    @Deprecated("Speech recognition uses the platform activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        if (resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heard.isNullOrBlank()) process(heard)
        } else statusLine.text = "No speech received. Try again or type."
    }

    private fun process(raw: String) {
        if (busy) { Toast.makeText(this, "MIRA is completing a reply", Toast.LENGTH_SHORT).show(); return }
        val cleaned = raw.trim().replace(Regex("(?i)^(hey\\s+)?(buddy|mira)[,!.?]*\\s*"), "").trim()
        val request = if (cleaned.isBlank()) raw else cleaned
        display("YOU", raw)
        val q = request.lowercase(Locale.ROOT)
        val result = localAnswer(q, request)
        if (result != null) answer(result)
        else {
            val apiKey = readKey()
            if (!apiKey.isNullOrBlank()) askOnline(apiKey, request)
            else answer(if (tamilMode) "விரிவான AI பதில்களுக்கு AI key-ல் OpenAI API key சேர்க்கவும். உள்ளூர் கட்டளைகள் இப்போது வேலை செய்யும்."
            else "For open-ended AI answers, add your own OpenAI API key under AI key. Local commands work without a key.")
        }
    }

    private fun localAnswer(q: String, original: String): String? {
        fun has(vararg terms: String): Boolean = terms.any { q.contains(it) }
        if (q == "buddy" || q == "mira" || q.startsWith("hey buddy") || q.startsWith("hey mira") || has("hello", "hi mira", "hi buddy", "வணக்கம்"))
            return if (tamilMode || q.contains("வணக்கம்")) "வணக்கம்! சொல்லுங்க, என்ன உதவி வேண்டும்?" else "Hello! MIRA is ready. What can I do for you?"
        if (has("help", "commands", "உதவி")) return "Try: time, date, battery, open WhatsApp, open YouTube, Wi-Fi settings, Bluetooth settings, camera, search for ... or call a phone number. For general questions, configure AI key."
        if (has("time", "நேரம்", "மணி என்ன")) {
            val time = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
            return if (tamilMode || q.contains("நேரம்")) "இப்போது நேரம் $time" else "The time is $time"
        }
        if (has("date", "today's date", "தேதி", "இன்றைய நாள்")) {
            val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date())
            return if (tamilMode || q.contains("தேதி")) "இன்றைய தேதி $date" else "Today is $date"
        }
        if (has("battery", "பேட்டரி")) {
            val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            return if (level >= 0 && scale > 0) "Tablet battery level: ${level * 100 / scale}%" else "Battery level not available."
        }
        if (has("wi-fi", "wifi", "வைஃபை", "வைபை")) {
            launchIntent(Intent(Settings.ACTION_WIFI_SETTINGS))
            return "Opening Wi-Fi settings."
        }
        if (has("bluetooth", "ப்ளூடூத்")) {
            launchIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return "Opening Bluetooth settings."
        }
        if (has("settings", "அமைப்புகள்")) {
            launchIntent(Intent(Settings.ACTION_SETTINGS))
            return "Opening tablet settings."
        }
        if (has("camera", "கேமரா")) {
            launchIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            return "Opening camera, if available on your tablet."
        }
        if (has("whatsapp", "வாட்ஸ்அப்")) {
            launchApp("com.whatsapp", "https://www.whatsapp.com/")
            return "Opening WhatsApp."
        }
        if (has("youtube", "யூடியூப்")) {
            launchApp("com.google.android.youtube", "https://www.youtube.com/")
            return "Opening YouTube."
        }
        val call = Regex("(?i)(?:call|dial)\\s*([+0-9() -]{7,})").find(original)
        if (call != null) {
            val number = call.groupValues[1].replace(Regex("[^+0-9]"), "")
            if (number.length in 7..16) {
                launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                return "Opening the dialer with $number. Please confirm before calling."
            }
        }
        val search = Regex("(?i)^(?:search(?: for)?|google|look up)\\s+(.+)").find(original)
        if (search != null) {
            val term = search.groupValues[1]
            openUrl("https://www.google.com/search?q=${URLEncoder.encode(term, "UTF-8")}")
            return "Searching the web for $term."
        }
        return null
    }

    private fun launchApp(packageName: String, browserFallback: String) {
        val app = packageManager.getLaunchIntentForPackage(packageName)
        if (app != null) launchIntent(app) else openUrl(browserFallback)
    }

    private fun openUrl(url: String) = launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun launchIntent(intent: Intent) {
        try { startActivity(intent) }
        catch (_: Exception) { Toast.makeText(this, "No compatible application available", Toast.LENGTH_LONG).show() }
    }

    private fun display(from: String, message: String) {
        transcript.append("$from\n$message\n\n")
        conversationScroll.post { conversationScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun answer(message: String) {
        display("MIRA", message)
        statusLine.text = "Ready • Tamil / English"
        if (speechReady) {
            val voiceLanguage = if (tamilMode) Locale.forLanguageTag("ta-IN") else Locale.ENGLISH
            val supported = tts?.setLanguage(voiceLanguage)
            if (supported == TextToSpeech.LANG_MISSING_DATA || supported == TextToSpeech.LANG_NOT_SUPPORTED) {
                statusLine.text = "Selected spoken voice unavailable; install a compatible Android TTS voice."
            } else tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "mira-${System.currentTimeMillis()}")
        }
    }

    private fun askOnline(apiKey: String, prompt: String) {
        busy = true
        statusLine.text = "MIRA is asking online AI..."
        val historySnapshot = recentChat.toList().takeLast(8)
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("model", modelName)
                    val items = JSONArray()
                    items.put(JSONObject().put("role", "system").put("content", "You are MIRA, the user's practical personal assistant on an HONOR Android tablet. Answer in Tamil when the user uses Tamil, otherwise English. Be helpful, concise and truthful. Do not claim to have done actions outside this app."))
                    for ((role, content) in historySnapshot) items.put(JSONObject().put("role", role).put("content", content))
                    items.put(JSONObject().put("role", "user").put("content", prompt))
                    put("messages", items)
                    put("max_tokens", 450)
                }
                connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20000
                    readTimeout = 45000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Accept", "application/json")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (status !in 200..299) {
                    val error = try { JSONObject(raw).getJSONObject("error").optString("message") } catch (_: Exception) { "HTTP $status" }
                    throw IllegalStateException("AI service error: $error")
                }
                val response = JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content", "").trim()
                if (response.isBlank()) throw IllegalStateException("The AI service returned an empty answer.")
                runOnUiThread {
                    recentChat.addLast("user" to prompt)
                    recentChat.addLast("assistant" to response)
                    while (recentChat.size > 8) recentChat.removeFirst()
                    busy = false
                    answer(response)
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    busy = false
                    answer("Could not contact online AI: ${ex.message ?: "unknown error"}. Check your connection and API key.")
                }
            } finally { connection?.disconnect() }
        }.start()
    }

    private fun showKeyDialog() {
        val field = EditText(this).apply {
            hint = "Paste OpenAI API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val configured = !readKey().isNullOrBlank()
        AlertDialog.Builder(this)
            .setTitle("MIRA • Online AI settings")
            .setMessage("${if (configured) "An API key is saved securely on this tablet. " else "No API key is configured. "}OpenAI API calls are separately billed. Do not enter your ChatGPT password or post keys to GitHub.")
            .setView(field)
            .setPositiveButton("Save key") { _, _ ->
                val entered = field.text.toString().trim()
                if (entered.isBlank()) Toast.makeText(this, "Key was empty; unchanged", Toast.LENGTH_SHORT).show()
                else if (saveKey(entered)) Toast.makeText(this, "Encrypted API key saved", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "Could not save key", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Remove key") { _, _ ->
                getSharedPreferences("mira_local", MODE_PRIVATE).edit().remove("api_key").apply()
                recentChat.clear()
                Toast.makeText(this, "Saved API key removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }

    private fun saveKey(clear: String): Boolean = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.iv + cipher.doFinal(clear.toByteArray(Charsets.UTF_8))
        getSharedPreferences("mira_local", MODE_PRIVATE).edit()
            .putString("api_key", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()
    } catch (_: Exception) { false }

    private fun readKey(): String? {
        return try {
            val data = getSharedPreferences("mira_local", MODE_PRIVATE).getString("api_key", null)
                ?: return null
            val bytes = Base64.decode(data, Base64.DEFAULT)
            if (bytes.size <= 12) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    override fun onInit(status: Int) {
        speechReady = status == TextToSpeech.SUCCESS
        if (speechReady) tts?.setLanguage(Locale.ENGLISH)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
