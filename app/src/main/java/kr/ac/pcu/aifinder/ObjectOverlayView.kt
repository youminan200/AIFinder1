package kr.ac.pcu.aifinder

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class ObjectOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var imageBitmap: Bitmap? = null
    private var detectedObjects: List<DetectionResult> = emptyList()
    var selectedIndex: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    private var onObjectSelectedListener: ((DetectionResult) -> Unit)? = null

    // Scale mapping values
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Paints
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E62563EB") // Blue overlay
    }

    data class DetectionResult(
        val boundingBox: Rect,
        val label: String,
        val confidence: Float
    )

    fun setData(bitmap: Bitmap, objects: List<DetectionResult>) {
        this.imageBitmap = bitmap
        this.detectedObjects = objects
        this.selectedIndex = if (objects.isNotEmpty()) 0 else -1
        invalidate()
    }

    fun setOnObjectSelectedListener(listener: (DetectionResult) -> Unit) {
        this.onObjectSelectedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = imageBitmap ?: return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW == 0f || viewH == 0f) return

        // Compute scaling to fit the bitmap in the view (center fit)
        val scaleW = viewW / bitmap.width
        val scaleH = viewH / bitmap.height
        scaleFactor = min(scaleW, scaleH)

        val imageDrawW = bitmap.width * scaleFactor
        val imageDrawH = bitmap.height * scaleFactor
        offsetX = (viewW - imageDrawW) / 2f
        offsetY = (viewH - imageDrawH) / 2f

        // Draw the image scaled
        val destRect = RectF(offsetX, offsetY, offsetX + imageDrawW, offsetY + imageDrawH)
        canvas.drawBitmap(bitmap, null, destRect, null)

        // Draw bounding boxes
        detectedObjects.forEachIndexed { idx, obj ->
            val rect = obj.boundingBox
            val isSelected = idx == selectedIndex

            // Map rect to view coordinates
            val left = rect.left * scaleFactor + offsetX
            val top = rect.top * scaleFactor + offsetY
            val right = rect.right * scaleFactor + offsetX
            val bottom = rect.bottom * scaleFactor + offsetY
            val mappedRect = RectF(left, top, right, bottom)

            if (isSelected) {
                boxPaint.color = Color.parseColor("#2563EB") // Primary blue
                boxPaint.strokeWidth = dpToPx(4f)
                fillPaint.color = Color.parseColor("#222563EB") // Alpha blue
                textBgPaint.color = Color.parseColor("#2563EB")
            } else {
                boxPaint.color = Color.parseColor("#0F766E") // Secondary teal
                boxPaint.strokeWidth = dpToPx(2.5f)
                fillPaint.color = Color.parseColor("#110F766E") // Alpha teal
                textBgPaint.color = Color.parseColor("#0F766E")
            }

            // Draw bounding box
            canvas.drawRoundRect(mappedRect, dpToPx(4f), dpToPx(4f), fillPaint)
            canvas.drawRoundRect(mappedRect, dpToPx(4f), dpToPx(4f), boxPaint)

            // Draw Label text tag
            val labelText = "${obj.label} (${(obj.confidence * 100).toInt()}% )"
            textPaint.textSize = dpToPx(12f)
            val textWidth = textPaint.measureText(labelText)
            val tagHeight = dpToPx(20f)
            val tagRect = RectF(left, top - tagHeight, left + textWidth + dpToPx(12f), top)
            
            // Adjust label bounds if drawing outside top edge
            if (tagRect.top < offsetY) {
                tagRect.offset(0f, tagHeight + (bottom - top))
            }
            canvas.drawRoundRect(tagRect, dpToPx(4f), dpToPx(4f), textBgPaint)
            canvas.drawText(labelText, tagRect.left + dpToPx(6f), tagRect.bottom - dpToPx(5f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val touchY = event.y

            // Find if user tapped inside any bounding box
            var clickedIdx = -1
            for (idx in detectedObjects.indices) {
                val rect = detectedObjects[idx].boundingBox
                val left = rect.left * scaleFactor + offsetX
                val top = rect.top * scaleFactor + offsetY
                val right = rect.right * scaleFactor + offsetX
                val bottom = rect.bottom * scaleFactor + offsetY

                val mappedRect = RectF(left, top, right, bottom)
                // Add some touch padding
                mappedRect.inset(-dpToPx(12f), -dpToPx(12f))

                if (mappedRect.contains(touchX, touchY)) {
                    clickedIdx = idx
                    break
                }
            }

            if (clickedIdx != -1) {
                selectedIndex = clickedIdx
                onObjectSelectedListener?.invoke(detectedObjects[clickedIdx])
                return true
            }
        }
        return true
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
