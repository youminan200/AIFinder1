package kr.ac.pcu.aifinder

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class RoomMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var areas: List<RoomArea> = listOf(
        RoomArea(1, "침대 구역"),
        RoomArea(2, "책상 구역"),
        RoomArea(3, "옷장 구역"),
        RoomArea(4, "현관 구역"),
        RoomArea(5, "선반 구역"),
        RoomArea(6, "창가 구역")
    )
    private var itemCounts: Map<Int, Int> = emptyMap()
    
    var selectedAreaId: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    private var onAreaClickListener: ((RoomArea) -> Unit)? = null

    // Paint objects
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Color definitions
    private val areaColors = listOf(
        Color.parseColor("#EFEAFE"), // Purple-ish
        Color.parseColor("#E8F0FF"), // Blue-ish
        Color.parseColor("#E7F5EF"), // Green-ish
        Color.parseColor("#FFF4DA"), // Amber-ish
        Color.parseColor("#DFF7F4"), // Teal-ish
        Color.parseColor("#FFE8ED")  // Rose-ish
    )

    private val areaBorderColors = listOf(
        Color.parseColor("#6D5BD0"),
        Color.parseColor("#2563EB"),
        Color.parseColor("#167A5A"),
        Color.parseColor("#B7791F"),
        Color.parseColor("#0F766E"),
        Color.parseColor("#C2415B")
    )

    fun setAreas(newAreas: List<RoomArea>, counts: Map<Int, Int> = emptyMap()) {
        this.areas = newAreas
        this.itemCounts = counts
        invalidate()
    }

    fun setOnAreaClickListener(listener: (RoomArea) -> Unit) {
        this.onAreaClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w == 0f || h == 0f) return

        val padding = dpToPx(8f)
        val cellW = w / 2f
        val cellH = h / 3f
        val cardRadius = dpToPx(12f)

        for (row in 0 until 3) {
            for (col in 0 until 2) {
                val index = row * 2 + col
                if (index >= areas.size) continue

                val area = areas[index]
                val colorIndex = index % areaColors.size

                val left = col * cellW + padding
                val top = row * cellH + padding
                val right = (col + 1) * cellW - padding
                val bottom = (row + 1) * cellH - padding

                val cardRect = RectF(left, top, right, bottom)
                val isSelected = area.id == selectedAreaId

                // Draw background card
                if (isSelected) {
                    // Selected state overlay style
                    cardPaint.color = Color.parseColor("#E6F0FA") // Highlighted cyan-blue
                    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

                    borderPaint.color = Color.parseColor("#2563EB") // Highlighted stroke blue
                    borderPaint.strokeWidth = dpToPx(3.5f)
                    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)
                } else {
                    // Normal state card style
                    cardPaint.color = areaColors[colorIndex]
                    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

                    borderPaint.color = areaBorderColors[colorIndex]
                    borderPaint.strokeWidth = dpToPx(1.5f)
                    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)
                }

                // Draw Text label
                textPaint.color = Color.parseColor("#1C2633") // text_primary
                textPaint.textSize = dpToPx(15f)
                val textY = top + (bottom - top) / 2f - dpToPx(2f)
                canvas.drawText(area.name, left + (right - left) / 2f, textY, textPaint)

                // Draw item count
                val count = itemCounts[area.id] ?: 0
                countPaint.color = Color.parseColor("#5E6B7A") // text_secondary
                countPaint.textSize = dpToPx(12f)
                val countY = textY + dpToPx(18f)
                canvas.drawText("보관 중: ${count}개", left + (right - left) / 2f, countY, countPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cellW = width.toFloat() / 2f
            val cellH = height.toFloat() / 3f

            val col = (event.x / cellW).toInt().coerceIn(0, 1)
            val row = (event.y / cellH).toInt().coerceIn(0, 2)

            val index = row * 2 + col
            if (index in areas.indices) {
                val area = areas[index]
                selectedAreaId = area.id
                onAreaClickListener?.invoke(area)
                return true
            }
        }
        return true
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
