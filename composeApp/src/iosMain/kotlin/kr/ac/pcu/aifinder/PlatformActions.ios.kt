package kr.ac.pcu.aifinder

actual fun launchObjectDetectionCamera(context: Any?, onResult: (ItemRecord) -> Unit) {
    // iOS dummy implementation to allow building without errors on Mac, returning mock items for test.
    val record = ItemRecord(
        id = getCurrentTimeMillis().toString(),
        name = "아이폰 열쇠 (iOS Simulation)",
        areaId = 4,
        areaName = "현관 구역",
        timestamp = getCurrentTimeMillis(),
        photoUri = null,
        boundingBox = null
    )
    onResult(record)
}
