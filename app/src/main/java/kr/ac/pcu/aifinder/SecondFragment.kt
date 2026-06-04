package kr.ac.pcu.aifinder

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.pcu.aifinder.databinding.FragmentSecondBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemStorage: ItemStorage
    private val checklistPrefs by lazy { requireContext().getSharedPreferences("outing_checklist", Context.MODE_PRIVATE) }

    companion object {
        private const val KEY_CHECKLIST = "checklist_items"
        private const val KEY_ALARM_HOUR = "alarm_hour"
        private const val KEY_ALARM_MINUTE = "alarm_minute"
        private const val KEY_ALARM_ENABLED = "alarm_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemStorage = ItemStorage(requireContext())

        // Hide floating action button on detail screens to focus on the feature details
        activity?.findViewById<View>(R.id.fab)?.visibility = View.GONE

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        renderFeature(arguments?.getString(FirstFragment.ARG_FEATURE).orEmpty())
    }

    private fun renderFeature(feature: String) {
        binding.detailContainer.removeAllViews()
        when (feature) {
            FirstFragment.FEATURE_ROOM_MAP -> renderRoomMap()
            FirstFragment.FEATURE_PHOTO_LIST -> renderPhotoList()
            FirstFragment.FEATURE_FAVORITES -> renderFavorites()
            FirstFragment.FEATURE_OUTING_CHECKLIST -> renderOutingChecklist()
            FirstFragment.FEATURE_WEEK_STATS -> renderWeekStats()
            else -> renderFavorites()
        }
    }

    // --- 1. ROOM MAP VIEW ---
    private fun renderRoomMap() {
        binding.detailTitle.setText(R.string.room_map_view)
        binding.detailSubtitle.setText(R.string.room_map_subtitle)

        val ctx = requireContext()

        // 1. Add description instructions
        binding.detailContainer.addView(TextView(ctx).apply {
            text = "구역을 선택하여 등록된 물품을 확인하거나, 이름을 변경할 수 있습니다."
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        // 2. Add RoomMapView card
        val mapCard = MaterialCardView(ctx).apply {
            radius = dp(12).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.white, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(280)
            ).apply { bottomMargin = dp(16) }
        }

        val roomMapView = RoomMapView(ctx).apply {
            id = View.generateViewId()
        }
        mapCard.addView(roomMapView)
        binding.detailContainer.addView(mapCard)

        // 3. Area detail container (Header + Edit button + Items list)
        val areaDetailsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        binding.detailContainer.addView(areaDetailsContainer)

        // Refresh helper
        fun refreshMapDetails(selectedAreaId: Int) {
            val areas = itemStorage.getRoomAreas()
            val allItems = itemStorage.getItems()
            val counts = allItems.groupBy { it.areaId }.mapValues { it.value.size }
            
            roomMapView.setAreas(areas, counts)
            roomMapView.selectedAreaId = selectedAreaId

            areaDetailsContainer.removeAllViews()

            val selectedArea = areas.firstOrNull { it.id == selectedAreaId } ?: return

            // Area Header Row
            val headerRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
            }

            headerRow.addView(TextView(ctx).apply {
                text = "${selectedArea.name} 물품 목록"
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            headerRow.addView(Button(ctx).apply {
                text = "이름 수정"
                setOnClickListener {
                    showRenameAreaDialog(selectedArea.id, selectedArea.name) {
                        refreshMapDetails(selectedAreaId)
                    }
                }
            })
            areaDetailsContainer.addView(headerRow)

            // Items List in selected area
            val areaItems = itemStorage.getItemsInArea(selectedAreaId)
            if (areaItems.isEmpty()) {
                areaDetailsContainer.addView(featureCard("등록된 물품 없음", "이 구역에 등록된 물건이 없습니다."))
            } else {
                areaItems.forEach { item ->
                    val subtitle = if (item.photoUri != null) "카메라 이미지 연결됨" else "메모 등록"
                    areaDetailsContainer.addView(rowCard(item.name, subtitle, "삭제") {
                        itemStorage.deleteItem(item.id)
                        Toast.makeText(ctx, "${item.name}이(가) 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        refreshMapDetails(selectedAreaId)
                    })
                }
            }
        }

        // Setup listener
        roomMapView.setOnAreaClickListener { area ->
            refreshMapDetails(area.id)
        }

        // Default to first area selection
        refreshMapDetails(1)
    }

    private fun showRenameAreaDialog(areaId: Int, currentName: String, onRenamed: () -> Unit) {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = "구역 이름"
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val input = TextInputEditText(inputLayout.context).apply {
            setText(currentName)
            setSelectAllOnFocus(true)
            maxLines = 1
        }
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("구역 이름 변경")
            .setView(inputLayout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("저장") { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty()) {
                    itemStorage.renameArea(areaId, newName)
                    Toast.makeText(requireContext(), "구역 이름이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    onRenamed()
                }
            }
            .show()
    }


    // --- 2. PHOTO LIST VIEW (Legacy compatibility) ---
    private fun renderPhotoList() {
        binding.detailTitle.setText(R.string.photo_list_view)
        binding.detailSubtitle.setText(R.string.photo_list_subtitle)

        val items = itemStorage.getItems().filter { it.photoUri != null }
        if (items.isEmpty()) {
            binding.detailContainer.addView(featureCard(
                "기록된 사진이 없습니다", 
                "홈 화면 우측 하단의 카메라 플로팅 버튼을 터치하여 AI 물체 감지 사진 기록을 시작해 보세요."
            ))
            return
        }

        binding.detailContainer.addView(TextView(requireContext()).apply {
            text = "AI 카메라를 통해 등록된 사진 목록입니다."
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        items.forEachIndexed { index, item ->
            binding.detailContainer.addView(photoCard(index + 1, item))
        }
    }

    private fun photoCard(number: Int, item: ItemRecord): View =
        MaterialCardView(requireContext()).apply {
            radius = dp(10).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }

            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))

                addView(ImageView(requireContext()).apply {
                    setImageURI(Uri.parse(item.photoUri))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(180)
                    )
                })
                
                addView(TextView(requireContext()).apply {
                    text = "${item.name}"
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(12), 0, 0)
                })
                
                addView(TextView(requireContext()).apply {
                    text = "위치: ${item.areaName}"
                    setTextColor(resources.getColor(R.color.accent_teal, null))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(4), 0, 0)
                })

                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                addView(TextView(requireContext()).apply {
                    text = "등록일시: $dateStr"
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    textSize = 12f
                    setPadding(0, dp(4), 0, 0)
                })
            })
        }


    // --- 3. FAVORITES VIEW ---
    private fun renderFavorites() {
        binding.detailTitle.setText(R.string.favorites_list)
        binding.detailSubtitle.setText(R.string.favorites_subtitle)

        fun refreshFavorites() {
            binding.detailContainer.removeAllViews()
            val favorites = itemStorage.getFavorites()
            if (favorites.isEmpty()) {
                binding.detailContainer.addView(featureCard(
                    "즐겨찾기가 비어있습니다", 
                    "홈 화면에서 등록된 물건 목록 카드를 선택한 뒤 '즐겨찾기 추가'를 눌러 자주 찾는 물건을 등록해 보세요."
                ))
                return
            }

            favorites.forEach { item ->
                val subtitle = "위치: ${item.areaName} | ${if (item.photoUri != null) "사진 기록 있음" else "메모 기록"}"
                binding.detailContainer.addView(rowCard(item.name, subtitle, "해제") {
                    itemStorage.toggleFavorite(item.id)
                    Toast.makeText(requireContext(), "즐겨찾기에서 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    refreshFavorites()
                })
            }
        }

        refreshFavorites()
    }


    // --- 4. WEEK STATISTICS VIEW ---
    private fun renderWeekStats() {
        binding.detailTitle.setText(R.string.recent_week_stats)
        binding.detailSubtitle.setText(R.string.week_stats_subtitle)

        val stats = itemStorage.getRecent7DaysStats()
        if (stats.isEmpty()) {
            binding.detailContainer.addView(featureCard(
                "통계 데이터 없음", 
                "최근 7일 동안 새로 등록된 소지품 데이터가 없어 통계를 계산할 수 없습니다."
            ))
            return
        }

        val totalRegistered = itemStorage.getItems().filter { 
            (System.currentTimeMillis() - it.timestamp) < (7 * 24 * 60 * 60 * 1000L) 
        }.size

        // Display summary card
        binding.detailContainer.addView(featureCard(
            "최근 7일간 총 ${totalRegistered}개 등록됨", 
            "가장 물품 등록이 활발한 구역: ${stats.maxByOrNull { it.value }?.key ?: "없음"}"
        ))

        binding.detailContainer.addView(TextView(requireContext()).apply {
            text = "구역별 물품 등록 비율"
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(8))
        })

        // List statistics per area
        stats.forEach { (areaName, count) ->
            val percentage = if (totalRegistered > 0) (count.toFloat() / totalRegistered * 100).toInt() else 0
            binding.detailContainer.addView(rowCard(
                areaName, 
                "등록된 물품 수: ${count}개", 
                "${percentage}%"
            ) {})
        }
    }


    // --- 5. OUTING CHECKLIST AND ALARM SCHEDULING ---
    private fun renderOutingChecklist() {
        binding.detailContainer.removeAllViews()
        binding.detailTitle.setText(R.string.outing_checklist)
        binding.detailSubtitle.setText(R.string.outing_checklist_subtitle)

        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.checklist_item_hint)
        }
        val input = TextInputEditText(inputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        inputLayout.addView(input)
        binding.detailContainer.addView(inputLayout)

        binding.detailContainer.addView(Button(requireContext()).apply {
            text = getString(R.string.add_checklist_item)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(14)
            }
            setOnClickListener {
                val label = input.text?.toString()?.trim().orEmpty()
                if (label.isNotEmpty()) {
                    val items = savedChecklistItems().toMutableList()
                    items.add(ChecklistItem(label, false))
                    saveChecklistItems(items)
                    renderOutingChecklist()
                }
            }
        })

        // Draw checklist rows
        val items = savedChecklistItems()
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                binding.detailContainer.addView(checklistRow(index, item))
            }
        } else {
            binding.detailContainer.addView(featureCard("체크리스트가 비어있습니다", "외출할 때 챙겨갈 물건을 등록해 보세요."))
        }

        // Draw alarm scheduler card
        binding.detailContainer.addView(TextView(requireContext()).apply {
            text = "외출 리마인드 알림 설정"
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(16), 0, dp(8))
        })

        binding.detailContainer.addView(alarmSchedulerCard())
    }

    private fun alarmSchedulerCard(): View {
        val isEnabled = checklistPrefs.getBoolean(KEY_ALARM_ENABLED, false)
        val hour = checklistPrefs.getInt(KEY_ALARM_HOUR, 8)
        val minute = checklistPrefs.getInt(KEY_ALARM_MINUTE, 30)

        val card = MaterialCardView(requireContext()).apply {
            radius = dp(10).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val statusText = if (isEnabled) {
            "매일 오전 ${String.format("%02d:%02d", hour, minute)} 리마인드 알림 활성화됨"
        } else {
            "리마인드 알림 설정이 꺼져 있습니다."
        }

        content.addView(TextView(requireContext()).apply {
            text = statusText
            setTextColor(resources.getColor(if (isEnabled) R.color.accent_green else R.color.text_secondary, null))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        })

        val btnRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        btnRow.addView(Button(requireContext()).apply {
            text = if (isEnabled) "알림 변경" else "알림 설정"
            setOnClickListener {
                showTimePickerDialog(hour, minute)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        })

        if (isEnabled) {
            btnRow.addView(Button(requireContext()).apply {
                text = "알림 끄기"
                setOnClickListener {
                    disableOutingAlarm()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        content.addView(btnRow)

        card.addView(content)
        return card
    }

    private fun showTimePickerDialog(currentHour: Int, currentMinute: Int) {
        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            enableOutingAlarm(hourOfDay, minute)
        }, currentHour, currentMinute, false).show()
    }

    private fun enableOutingAlarm(hour: Int, minute: Int) {
        // Save alarm values
        checklistPrefs.edit()
            .putInt(KEY_ALARM_HOUR, hour)
            .putInt(KEY_ALARM_MINUTE, minute)
            .putBoolean(KEY_ALARM_ENABLED, true)
            .apply()

        // Schedule daily AlarmManager alarm
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), OutingAlarmReceiver::class.java).apply {
            action = OutingAlarmReceiver.ACTION_OUTING_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
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
                requireContext(),
                "외출 전 리마인드 알림이 오전 ${String.format("%02d:%02d", hour, minute)}에 설정되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "알람 권한 허용이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

        renderOutingChecklist()
    }

    private fun disableOutingAlarm() {
        checklistPrefs.edit().putBoolean(KEY_ALARM_ENABLED, false).apply()

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), OutingAlarmReceiver::class.java).apply {
            action = OutingAlarmReceiver.ACTION_OUTING_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        Toast.makeText(requireContext(), "외출 리마인드 알림을 해제했습니다.", Toast.LENGTH_SHORT).show()
        renderOutingChecklist()
    }

    private fun checklistRow(index: Int, item: ChecklistItem): View =
        MaterialCardView(requireContext()).apply {
            radius = dp(8).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }

            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(8), dp(8))

                addView(CheckBox(requireContext()).apply {
                    text = item.label
                    isChecked = item.checked
                    textSize = 17f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnCheckedChangeListener { _, checked ->
                        updateChecklistChecked(index, checked)
                    }
                })
                addView(Button(requireContext()).apply {
                    text = getString(R.string.delete)
                    setOnClickListener { deleteChecklistItem(index) }
                })
            })
        }

    private fun savedChecklistItems(): List<ChecklistItem> {
        val stored = checklistPrefs.getString(KEY_CHECKLIST, null)
        if (stored.isNullOrBlank()) {
            return listOf("휴대폰", "지갑", "현관 열쇠", "우산", "보조 배터리")
                .map { ChecklistItem(it, false) }
        }
        return stored.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { encoded ->
                val parts = encoded.split("^")
                val label = Uri.decode(parts.getOrNull(0).orEmpty())
                if (label.isBlank()) null else ChecklistItem(label, parts.getOrNull(1) == "1")
            }
    }

    private fun saveChecklistItems(items: List<ChecklistItem>) {
        checklistPrefs.edit()
            .putString(KEY_CHECKLIST, items.joinToString("|") { item ->
                "${Uri.encode(item.label)}^${if (item.checked) "1" else "0"}"
            })
            .apply()
    }

    private fun updateChecklistChecked(index: Int, checked: Boolean) {
        val items = savedChecklistItems().toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(checked = checked)
            saveChecklistItems(items)
        }
    }

    private fun deleteChecklistItem(index: Int) {
        val items = savedChecklistItems().toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            saveChecklistItems(items)
            renderOutingChecklist()
        }
    }

    private fun rowCard(title: String, subtitle: String, actionText: String, action: () -> Unit): View =
        MaterialCardView(requireContext()).apply {
            radius = dp(8).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(10), dp(12))
                addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(requireContext()).apply {
                        text = title
                        setTextColor(resources.getColor(R.color.text_primary, null))
                        textSize = 18f
                        typeface = Typeface.DEFAULT_BOLD
                    })
                    addView(TextView(requireContext()).apply {
                        text = subtitle
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        textSize = 14f
                        setPadding(0, dp(6), 0, 0)
                    })
                })
                if (actionText.isNotEmpty()) {
                    addView(Button(requireContext()).apply {
                        text = actionText
                        setOnClickListener { action() }
                    })
                }
            })
        }

    private fun featureCard(title: String, subtitle: String): View =
        MaterialCardView(requireContext()).apply {
            radius = dp(8).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }

            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addView(TextView(requireContext()).apply {
                    text = title
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(requireContext()).apply {
                    text = subtitle
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    textSize = 14f
                    setPadding(0, dp(6), 0, 0)
                })
            })
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class ChecklistItem(val label: String, val checked: Boolean)
}
