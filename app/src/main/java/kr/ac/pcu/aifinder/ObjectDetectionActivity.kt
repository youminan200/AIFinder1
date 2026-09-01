package kr.ac.pcu.aifinder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kr.ac.pcu.aifinder.BuildConfig
import kr.ac.pcu.aifinder.databinding.ActivityObjectDetectionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ObjectDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObjectDetectionBinding
    private lateinit var itemStorage: ItemStorage
    private var photoUri: Uri? = null
    private var currentCaptureUri: Uri? = null
    private val additionalPhotoUris = mutableListOf<Uri>()
    private var capturedBitmap: Bitmap? = null

    // Register camera launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentCaptureUri?.let { uri ->
                    if (photoUri == null) {
                        photoUri = uri
                        processCapturedPhoto(uri)
                    } else {
                        additionalPhotoUris.add(uri)
                        addExtraPhotoToUI(uri)
                    }
                }
            } else {
                if (photoUri == null) {
                    Toast.makeText(this, "첫 사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "추가 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun addExtraPhotoToUI(uri: Uri) {
        val imageView = android.widget.ImageView(this).apply {
            val size = (80 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(size, android.widget.LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
        }
        binding.extraPhotosContainer.addView(imageView)
    }

    // Register permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityObjectDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        itemStorage = ItemStorage(PlatformStorage(this))

        setupRoomMapView()

        binding.registerButton.setOnClickListener { registerItem() }
        binding.addPhotoButton.setOnClickListener { launchCamera() }

        // Start flow by checking camera permission
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = createPhotoFile()
        currentCaptureUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )
        try {
            takePictureLauncher.launch(currentCaptureUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "카메라 앱 실행에 실패했습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createPhotoFile(): File {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: cacheDir
        val photoDir = File(baseDir, "camera_photos").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(photoDir, "item_$timestamp.jpg")
    }

    private fun setupRoomMapView() {
        val areas = itemStorage.getRoomAreas()
        val items = itemStorage.getItems()
        val counts = items.groupBy { it.areaId }.mapValues { it.value.size }
        binding.roomMapView.setAreas(areas, counts)
        binding.roomMapView.setOnAreaClickListener { area ->
            binding.roomMapView.selectedAreaId = area.id
        }
    }

    private fun processCapturedPhoto(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Correct orientation and load Bitmap
        capturedBitmap = try {
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }.copy(Bitmap.Config.ARGB_8888, true)
            
            // Handle EXIF rotation
            val rotation = getRotationFromExif(uri)
            if (rotation != 0) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이미지를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            finish()
            return
        }

        binding.photoImageView.setImageBitmap(capturedBitmap)
        binding.nameEditText.setText("AI가 물건을 분석 중입니다...")

        val bitmap = capturedBitmap ?: return

        lifecycleScope.launch {
            try {
                val models = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-1.5-flash-latest", "gemini-flash-latest")
                val areas = itemStorage.getRoomAreas()
                val areasText = areas.joinToString(", ") { "${it.id}(${it.name})" }
                
                val prompt = """
                    이미지 분석 전문가로서 사진 속의 물건들을 파악하고 보관 위치를 추천해 주세요.
                    
                    반드시 아래의 JSON 형식으로만 답변하십시오. 다른 텍스트나 마크다운은 절대 포함하지 마십시오.
                    
                    {
                      "items": "파악된 물건들의 이름을 쉼표로 구분한 문자열 (예: '안경, 케이스')",
                      "areaId": "추천 구역의 ID 숫자 하나 (예: 3)"
                    }
                    
                    [사용 가능한 구역 목록]
                    $areasText
                    
                    [주의사항]
                    1. 가장 눈에 띄는 1~3개의 물건에 집중하세요.
                    2. 반드시 위에서 제공한 구역 목록 중 하나를 선택하세요.
                """.trimIndent()

                var detectedText = ""
                var lastError: Exception? = null
                for (modelName in models) {
                    try {
                        val generativeModel = GenerativeModel(
                            modelName = modelName,
                            apiKey = BuildConfig.GEMINI_API_KEY
                        )
                        val response = generativeModel.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        )
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrEmpty()) {
                            detectedText = textResult
                            android.util.Log.d("ObjectDetection", "Successfully detected using model: $modelName")
                            break
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ObjectDetection", "Model $modelName failed: ${e.message}")
                        lastError = e
                    }
                }

                if (detectedText.isEmpty() && lastError != null) {
                    throw lastError
                }

                binding.progressBar.visibility = View.GONE
                android.util.Log.d("ObjectDetection", "Gemini raw response:\n$detectedText")
                
                // Parse JSON response
                try {
                    val cleanJson = detectedText.substringAfter("{").substringBeforeLast("}")
                    val itemsMatch = Regex("\"items\"\\s*:\\s*\"([^\"]+)\"").find(detectedText)
                    val areaMatch = Regex("\"areaId\"\\s*:\\s*(\\d+)").find(detectedText)
                    
                    val items = itemsMatch?.groupValues?.get(1) ?: "인식된 물건"
                    val areaId = areaMatch?.groupValues?.get(1)?.toIntOrNull()
                    
                    binding.nameEditText.setText(items)
                    binding.nameEditText.selectAll()
                    
                    if (areaId != null) {
                        binding.roomMapView.selectedAreaId = areaId
                    }
                } catch (e: Exception) {
                    // Fallback to old parsing if JSON fails
                    val lines = detectedText.lines().map { it.replace(Regex("[*#\\\\-]"), "").trim() }
                    val items = lines.firstOrNull { !it.contains("구역") } ?: "물건"
                    binding.nameEditText.setText(items)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.nameEditText.setText("")
                android.util.Log.e("GeminiError", "Gemini API failed", e)
                val isNetError = e is java.net.UnknownHostException || 
                                 e is java.net.ConnectException || 
                                 e is java.io.IOException || 
                                 (e.message?.contains("Unable to resolve host", ignoreCase = true) == true) ||
                                 (e.message?.contains("network", ignoreCase = true) == true)
                val friendlyMessage = if (isNetError) {
                    "네트워크 연결이 없어서 AI 사물인식을 사용할 수 없습니다. 물건 이름을 수동으로 입력해 주세요!"
                } else {
                    "Gemini API 오류: ${e.message}"
                }
                Toast.makeText(this@ObjectDetectionActivity, friendlyMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getRotationFromExif(uri: Uri): Int {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return 0
            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            inputStream.close()
            when (exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun registerItem() {
        val inputText = binding.nameEditText.text?.toString()?.trim().orEmpty()
        if (inputText.isEmpty() || inputText == "인식된 물건이 없습니다." || inputText.startsWith("AI가")) {
            Toast.makeText(this, "물건 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val areaId = binding.roomMapView.selectedAreaId
        if (areaId == -1) {
            Toast.makeText(this, "보관할 위치 구역을 지도에서 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val areas = itemStorage.getRoomAreas()
        val selectedArea = areas.firstOrNull { it.id == areaId } ?: return

        val itemNames = inputText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (itemNames.isEmpty()) {
            Toast.makeText(this, "유효한 물건 이름이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        var successCount = 0

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.registerButton.isEnabled = false

            for (itemName in itemNames) {
                val record = ItemRecord(
                    id = UUID.randomUUID().toString(),
                    name = itemName,
                    areaId = areaId,
                    areaName = selectedArea.name,
                    timestamp = timestamp,
                    photoUri = photoUri?.toString(),
                    boundingBox = null,
                    isFavorite = false
                )

                itemStorage.addItem(record)
                successCount++
            }
            
            // Add to remote sync
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                try {
                    itemStorage.syncItemsRemote()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.progressBar.visibility = View.GONE
            binding.registerButton.isEnabled = true

            Toast.makeText(this@ObjectDetectionActivity, "${itemNames.size}개의 물건이 등록되었습니다! (서버 동기화 성공: $successCount)", Toast.LENGTH_LONG).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
