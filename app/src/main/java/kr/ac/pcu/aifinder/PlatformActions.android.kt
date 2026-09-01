package kr.ac.pcu.aifinder

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import com.google.ai.client.generativeai.GenerativeModel
import kr.ac.pcu.aifinder.BuildConfig

private var cameraCallback: ((ItemRecord) -> Unit)? = null
private var voiceCallback: ((String) -> Unit)? = null
private var galleryCallback: ((String) -> Unit)? = null

private const val REQUEST_CAMERA_DETECTION = 1001
private const val REQUEST_VOICE_RECOGNITION = 1003
private const val REQUEST_GALLERY_PICK = 1004

fun launchObjectDetectionCamera(context: Any?, onResult: (ItemRecord) -> Unit) {
    val activity = context as? Activity ?: return
    cameraCallback = onResult
    val intent = Intent(activity, ObjectDetectionActivity::class.java)
    activity.startActivityForResult(intent, REQUEST_CAMERA_DETECTION)
}

fun handlePlatformActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
        REQUEST_CAMERA_DETECTION -> {
            if (resultCode == Activity.RESULT_OK) {
                // Trigger refresh by passing a dummy record as App.kt doesn't use the result value
                cameraCallback?.invoke(ItemRecord("", "", 0, "", 0, null, null))
            }
            cameraCallback = null
        }
        REQUEST_VOICE_RECOGNITION -> {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    voiceCallback?.invoke(results[0])
                }
            }
            voiceCallback = null
        }
        REQUEST_GALLERY_PICK -> {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                if (uri != null) {
                    galleryCallback?.invoke(uri.toString())
                }
            }
            galleryCallback = null
        }
    }
}

fun scheduleOutingAlarm(context: Any?, hour: Int, minute: Int) {
    val ctx = context as? Context ?: return
    val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, OutingAlarmReceiver::class.java).apply {
        action = OutingAlarmReceiver.ACTION_OUTING_ALARM
    }
    val pendingIntent = PendingIntent.getBroadcast(
        ctx, 100, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val now = Calendar.getInstance()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val nowHour = now.get(Calendar.HOUR_OF_DAY)
    val nowMinute = now.get(Calendar.MINUTE)
    if (hour < nowHour || (hour == nowHour && minute < nowMinute)) {
        calendar.add(Calendar.DATE, 1)
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        Toast.makeText(
            ctx,
            "외출 전 알림이 ${String.format("%02d:%02d", hour, minute)}에 예약되었습니다.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: SecurityException) {
        Toast.makeText(ctx, "알람 권한 설정이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
}

fun cancelOutingAlarm(context: Any?) {
    val ctx = context as? Context ?: return
    val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, OutingAlarmReceiver::class.java).apply {
        action = OutingAlarmReceiver.ACTION_OUTING_ALARM
    }
    val pendingIntent = PendingIntent.getBroadcast(
        ctx, 100, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    Toast.makeText(ctx, "외출 알림을 취소했습니다.", Toast.LENGTH_SHORT).show()
}

fun checkAndRequestNotificationPermission(context: Any?) {
    val activity = context as? Activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002
            )
        }
    }
}

fun showTimePickerDialog(
    context: Any?,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val ctx = context as? Context ?: return
    TimePickerDialog(ctx, { _, hour, minute ->
        onTimeSelected(hour, minute)
    }, initialHour, initialMinute, false).show()
}

suspend fun askGeminiAssistant(
    context: Any?,
    items: List<ItemRecord>,
    areas: List<RoomArea>,
    question: String
): String {
    val models = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-1.5-flash-latest", "gemini-flash-latest")
    var lastError: Exception? = null
    
    val itemsText = items.joinToString("\n") { 
        "- 이름: ${it.name}, 위치: ${it.areaName} (ID: ${it.areaId}), 등록일시: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREAN).format(java.util.Date(it.timestamp))}"
    }
    val areasText = areas.joinToString(", ") { "${it.id}(${it.name})" }
    
    val prompt = """
        사용자는 보관 중인 물건들의 위치를 찾기 위해 질문하고 있습니다.
        
        [보관된 물건 목록]
        $itemsText
        
        [방/구역 목록]
        $areasText
        
        [사용자 질문]
        $question
        
        [답변 지침]
        1. 사용자의 질문에 맞춰 물건의 위치(구역 이름)와 필요한 경우 등록 일시를 친근하고 명확한 한국어 존댓말로 답변해 주십시오.
        2. 물건 목록에 없는 물건을 물어볼 경우, 목록에 존재하지 않는다고 정중히 설명하고 방 구역 목록을 알려주며 다른 물건을 물어보도록 유도해 주십시오.
        3. 답변은 3줄 이내로 간결하고 핵심만 작성해 주십시오.
    """.trimIndent()

    for (modelName in models) {
        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = BuildConfig.GEMINI_API_KEY
            )
            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim()
            if (!text.isNullOrEmpty()) {
                android.util.Log.d("GeminiAssistant", "Successfully answered using model: $modelName")
                return text
            }
        } catch (e: Exception) {
            android.util.Log.w("GeminiAssistant", "Model $modelName failed: ${e.message}")
            lastError = e
        }
    }
    val isNetError = lastError is java.net.UnknownHostException || 
                     lastError is java.net.ConnectException || 
                     lastError is java.io.IOException || 
                     (lastError?.message?.contains("Unable to resolve host", ignoreCase = true) == true) ||
                     (lastError?.message?.contains("network", ignoreCase = true) == true)
    return if (isNetError) {
        "네트워크 연결이 되어 있지 않아 AI 비서 기능을 사용할 수 없습니다. 모바일 데이터나 와이파이 연결 상태를 확인해 주세요."
    } else {
        "AI 비서 통신 오류: ${lastError?.message ?: "응답을 받지 못했습니다."}"
    }
}

fun startVoiceRecognition(context: Any?, onResult: (String) -> Unit) {
    val activity = context as? Activity ?: return
    voiceCallback = onResult
    val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "찾으시는 물건을 말씀해주세요.")
    }
    try {
        activity.startActivityForResult(intent, REQUEST_VOICE_RECOGNITION)
    } catch (e: Exception) {
        Toast.makeText(activity, "음성 인식을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

fun speakVoiceOutput(context: Any?, text: String) {
    val ctx = context as? Context ?: return
    val tts = android.speech.tts.TextToSpeech(ctx) { status ->
        // Simplified TTS implementation - would need proper lifecycle in real app
    }
}

fun pickImageFromGallery(context: Any?, onImageSelected: (String) -> Unit) {
    val activity = context as? Activity ?: return
    galleryCallback = onImageSelected
    val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    activity.startActivityForResult(intent, REQUEST_GALLERY_PICK)
}
