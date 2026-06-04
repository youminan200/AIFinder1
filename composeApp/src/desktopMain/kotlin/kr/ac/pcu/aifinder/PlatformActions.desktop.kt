package kr.ac.pcu.aifinder

actual fun launchObjectDetectionCamera(context: Any?, onResult: (ItemRecord) -> Unit) {
    // Desktop dummy implementation:
    // Instantly simulate finding a dummy item '지갑' in Area 4 (현관)
    println("Desktop Camera Triggered - Simulating object detection")
    val dummyItem = ItemRecord(
        id = "desktop_dummy_${System.currentTimeMillis()}",
        name = "테스트 지갑",
        areaId = 4,
        areaName = "현관",
        timestamp = System.currentTimeMillis(),
        photoUri = null,
        boundingBox = null,
        isFavorite = true
    )
    onResult(dummyItem)
}
