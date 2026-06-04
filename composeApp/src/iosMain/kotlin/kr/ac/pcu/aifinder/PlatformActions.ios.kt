package kr.ac.pcu.aifinder

import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIApplication

actual fun launchObjectDetectionCamera(context: Any?, onResult: (ItemRecord) -> Unit) {
    val record = ItemRecord(
        id = getCurrentTimeMillis().toString(),
        name = "아이폰 열쇠 (iOS Simulation)",
        areaId = 4,
        areaName = "현관 구역",
        timestamp = getCurrentTimeMillis(),
        photoUri = null,
        boundingBox = null
    )
    
    // Add to storage
    val storage = ItemStorage(PlatformStorage(null))
    storage.addItem(record)
    
    // Show iOS Alert to give visual feedback in simulator
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (rootViewController != null) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = "객체 인식 카메라 시뮬레이션",
            message = "'아이폰 열쇠'가 검출되어 현관 구역에 자동으로 등록되었습니다. (실물 카메라 연동은 Mac 기기 필요)",
            preferredStyle = UIAlertControllerStyleAlert
        )
        val okAction = UIAlertAction.actionWithTitle(
            title = "확인",
            style = UIAlertActionStyleDefault,
            handler = { _ ->
                onResult(record)
            }
        )
        alert.addAction(okAction)
        rootViewController.presentViewController(alert, animated = true, completion = null)
    } else {
        onResult(record)
    }
}
