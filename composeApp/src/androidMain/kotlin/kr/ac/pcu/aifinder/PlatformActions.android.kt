package kr.ac.pcu.aifinder

import android.content.Context
import android.content.Intent

actual fun launchObjectDetectionCamera(context: Any?, onResult: (ItemRecord) -> Unit) {
    val ctx = context as? Context ?: return
    val intent = Intent(ctx, ObjectDetectionActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    ctx.startActivity(intent)
}
