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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kr.ac.pcu.aifinder.composeApp.databinding.ActivityObjectDetectionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ObjectDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObjectDetectionBinding
    private lateinit var itemStorage: ItemStorage
    private var photoUri: Uri? = null
    private var capturedBitmap: Bitmap? = null
    private var currentResults: List<ObjectOverlayView.DetectionResult> = emptyList()

    // Register camera launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let { uri ->
                    processCapturedPhoto(uri)
                }
            } else {
                Toast.makeText(this, "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
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
        photoUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )
        try {
            takePictureLauncher.launch(photoUri!!)
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

        // Load Bitmap
        capturedBitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            Toast.makeText(this, "이미지를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            finish()
            return
        }

        val bitmap = capturedBitmap ?: return

        // Run ML Kit Object Detection
        val objectDetectorOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(objectDetectorOptions)

        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isEmpty()) {
                    // No objects found, run full image labeling
                    runFullImageLabeling(bitmap)
                } else {
                    // Objects found, run Image Labeling on each cropped area
                    val labelerOptions = ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.4f)
                        .build()
                    val labeler = ImageLabeling.getClient(labelerOptions)

                    val tasks = detectedObjects.map { obj ->
                        val box = obj.boundingBox
                        val left = box.left.coerceIn(0, bitmap.width)
                        val top = box.top.coerceIn(0, bitmap.height)
                        val right = box.right.coerceIn(0, bitmap.width)
                        val bottom = box.bottom.coerceIn(0, bitmap.height)
                        val w = (right - left).coerceAtLeast(1)
                        val h = (bottom - top).coerceAtLeast(1)

                        val crop = Bitmap.createBitmap(bitmap, left, top, w, h)
                        val cropImage = InputImage.fromBitmap(crop, 0)

                        labeler.process(cropImage).continueWith { cropTask ->
                            val labels = cropTask.result ?: emptyList()
                            val label = translateLabel(labels.maxByOrNull { it.confidence }?.text ?: "물건")
                            val conf = labels.maxByOrNull { it.confidence }?.confidence ?: 0.5f
                            ObjectOverlayView.DetectionResult(box, label, conf)
                        }
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener { completedTasks ->
                        val results = completedTasks.mapNotNull {
                            if (it.isSuccessful) it.result as? ObjectOverlayView.DetectionResult else null
                        }
                        displayResults(bitmap, results)
                    }
                }
            }
            .addOnFailureListener {
                runFullImageLabeling(bitmap)
            }
    }

    private fun runFullImageLabeling(bitmap: Bitmap) {
        val labelerOptions = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.4f)
            .build()
        val labeler = ImageLabeling.getClient(labelerOptions)
        val image = InputImage.fromBitmap(bitmap, 0)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val label = translateLabel(labels.maxByOrNull { it.confidence }?.text ?: "물건")
                val conf = labels.maxByOrNull { it.confidence }?.confidence ?: 0.5f
                val fullRect = Rect(0, 0, bitmap.width, bitmap.height)
                val result = ObjectOverlayView.DetectionResult(fullRect, label, conf)
                displayResults(bitmap, listOf(result))
            }
            .addOnFailureListener {
                val fullRect = Rect(0, 0, bitmap.width, bitmap.height)
                displayResults(bitmap, listOf(ObjectOverlayView.DetectionResult(fullRect, "물건", 0.5f)))
            }
    }

    private fun translateLabel(englishLabel: String): String {
        return when (englishLabel.lowercase(Locale.ROOT)) {
            "cup", "coffee cup", "mug" -> "컵"
            "glass", "drink" -> "유리컵"
            "bottle", "water bottle" -> "물병"
            "book", "novel" -> "책"
            "notebook", "laptop" -> "노트북"
            "cell phone", "mobile phone", "telephone" -> "휴대폰"
            "keys", "key" -> "열쇠"
            "wallet", "purse" -> "지갑"
            "backpack", "bag" -> "가방"
            "umbrella" -> "우산"
            "glasses", "sunglasses" -> "안경"
            "watch", "clock" -> "시계"
            "pen", "pencil" -> "필기구"
            "clothing", "jeans", "shirt", "coat", "jacket" -> "의류"
            "shoe", "footwear", "sneaker" -> "신발"
            "card" -> "카드"
            "mouse", "computer mouse" -> "마우스"
            "headphones", "earphones" -> "이어폰"
            "pillow" -> "베개"
            "blanket" -> "이불"
            else -> englishLabel
        }
    }

    private fun displayResults(bitmap: Bitmap, results: List<ObjectOverlayView.DetectionResult>) {
        binding.progressBar.visibility = View.GONE
        currentResults = results
        binding.objectOverlayView.setData(bitmap, results)
        if (results.isNotEmpty()) {
            binding.nameEditText.setText(results[0].label)
            binding.nameEditText.selectAll()
        }

        binding.objectOverlayView.setOnObjectSelectedListener { result ->
            binding.nameEditText.setText(result.label)
        }
    }

    private fun registerItem() {
        val itemName = binding.nameEditText.text?.toString()?.trim().orEmpty()
        if (itemName.isEmpty()) {
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

        val selectedIdx = binding.objectOverlayView.selectedIndex
        var boundingBoxStr: String? = null
        if (selectedIdx in currentResults.indices) {
            val rect = currentResults[selectedIdx].boundingBox
            boundingBoxStr = "${rect.left},${rect.top},${rect.right},${rect.bottom}"
        }

        val timestamp = System.currentTimeMillis()
        val record = ItemRecord(
            id = UUID.randomUUID().toString(),
            name = itemName,
            areaId = areaId,
            areaName = selectedArea.name,
            timestamp = timestamp,
            photoUri = photoUri?.toString(),
            boundingBox = boundingBoxStr,
            isFavorite = false
        )

        itemStorage.addItem(record)

        Toast.makeText(this, "${itemName}이(가) ${selectedArea.name}에 등록되었습니다.", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}
