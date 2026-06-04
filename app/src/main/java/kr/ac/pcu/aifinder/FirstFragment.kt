package kr.ac.pcu.aifinder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.pcu.aifinder.databinding.FragmentFirstBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemStorage: ItemStorage
    private val recommender = AiFindRecommender()

    private val roomPrefs by lazy {
        requireContext().getSharedPreferences("room_settings", 0)
    }

    // Speech-to-Text launcher
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                binding.searchEditText.setText(spokenText)
            }
        }
    }

    // Register item activity launcher
    private val registerItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            renderResults()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemStorage = ItemStorage(requireContext())

        binding.titleText.text = currentRoomName()
        binding.editRoomNameButton.setOnClickListener { showRoomNameDialog() }
        binding.roomMapCard.setOnClickListener { openFeature(FEATURE_ROOM_MAP) }
        binding.photoListCard.setOnClickListener { openFeature(FEATURE_PHOTO_LIST) }
        binding.favoritesCard.setOnClickListener { openFeature(FEATURE_FAVORITES) }
        binding.outingChecklistCard.setOnClickListener { openFeature(FEATURE_OUTING_CHECKLIST) }
        binding.weekStatsCard.setOnClickListener { openFeature(FEATURE_WEEK_STATS) }

        // Configure speech search mic icon
        binding.searchInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
        binding.searchInputLayout.setEndIconDrawable(android.R.drawable.ic_btn_speak_now)
        binding.searchInputLayout.setEndIconOnClickListener {
            startSpeechToText()
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderResults()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.filterGroup.setOnCheckedStateChangeListener { _, _ -> renderResults() }

        // Connect main activity FAB to ObjectDetectionActivity
        activity?.findViewById<View>(R.id.fab)?.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                val intent = Intent(requireContext(), ObjectDetectionActivity::class.java)
                registerItemLauncher.launch(intent)
            }
        }

        renderResults()
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "찾고 싶은 물건의 이름을 말해주세요.")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "음성 인식을 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderResults() {
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        val selectedFilter = binding.filterGroup.checkedChipId
            .takeIf { it != View.NO_ID && it != R.id.chip_all }
            ?.let { binding.filterGroup.findViewById<Chip>(it).text.toString() }

        val dbItems = itemStorage.getItems()
        val areas = itemStorage.getRoomAreas()

        binding.resultsContainer.removeAllViews()

        // 1. Show AI Search recommendation card at the top if there is a query
        if (query.isNotEmpty()) {
            val recommendation = recommender.recommend(query, dbItems, areas)
            if (recommendation != null) {
                binding.resultsContainer.addView(recommendationCard(recommendation))
            }
        }

        // 2. Filter list of items
        val filtered = dbItems.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.name.contains(query, ignoreCase = true) ||
                item.areaName.contains(query, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "Docs" -> item.name.contains("메모") || item.name.contains("서류") || item.name.contains("노트")
                "Images" -> item.photoUri != null
                "Notes" -> item.photoUri == null && !item.name.contains("메모")
                else -> true
            }
            matchesQuery && matchesFilter
        }

        if (filtered.isEmpty() && query.isEmpty()) {
            binding.resultsContainer.addView(emptyStateView("아직 등록된 물건이 없습니다.\n우측 하단 카메라 버튼을 눌러 첫 물건을 등록해 보세요."))
            return
        } else if (filtered.isEmpty()) {
            // Even if list filter is empty, the recommendation card could still show cold-start suggestions.
            if (query.isEmpty()) {
                binding.resultsContainer.addView(emptyStateView("일치하는 물건이 없습니다."))
            }
            return
        }

        filtered.forEach { binding.resultsContainer.addView(resultCard(it)) }
    }

    private fun recommendationCard(rec: AiFindRecommender.RecommendationResult): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(12).toFloat()
            strokeWidth = dp(2)
            setCardBackgroundColor(resources.getColor(R.color.icon_chip_blue, null))
            setStrokeColor(resources.getColor(R.color.accent_blue, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // AI Chip Tag
        val tagContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tagContainer.addView(TextView(requireContext()).apply {
            text = "★ AI 최적 추천 위치"
            setTextColor(resources.getColor(R.color.accent_blue, null))
            textSize = 12f
            paint.isFakeBoldText = true
        })
        tagContainer.addView(TextView(requireContext()).apply {
            text = " (신뢰도 ${rec.confidence}%)"
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 11f
        })
        content.addView(tagContainer)

        // Recommended Area Title
        content.addView(TextView(requireContext()).apply {
            text = rec.recommendedArea.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 22f
            paint.isFakeBoldText = true
            setPadding(0, dp(6), 0, dp(6))
        })

        // Match description / Reason
        content.addView(TextView(requireContext()).apply {
            text = rec.matchReason
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            setLineSpacing(2f, 1f)
        })

        card.addView(content)
        return card
    }

    private fun resultCard(item: ItemRecord): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(10).toFloat()
            strokeWidth = dp(1)
            setCardBackgroundColor(resources.getColor(R.color.surface_card, null))
            setStrokeColor(resources.getColor(R.color.outline_soft, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            
            setOnClickListener {
                // Tapping item details opens dialog showing metadata and picture if available
                showItemDetailsDialog(item)
            }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        // Header info (Area and date)
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        header.addView(TextView(requireContext()).apply {
            text = item.areaName
            setTextColor(resources.getColor(R.color.accent_green, null))
            textSize = 12f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
        header.addView(TextView(requireContext()).apply {
            text = dateStr
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 11f
        })
        content.addView(header)

        // Item title name
        content.addView(TextView(requireContext()).apply {
            text = item.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 18f
            paint.isFakeBoldText = true
            setPadding(0, dp(4), 0, dp(4))
        })

        // Bounding box status info if exists
        val summary = if (item.photoUri != null) "카메라 이미지 기록 연결됨" else "메모로 등록됨"
        content.addView(TextView(requireContext()).apply {
            text = summary
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 13f
        })

        card.addView(content)
        return card
    }

    private fun showItemDetailsDialog(item: ItemRecord) {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setMessage("보관 위치: ${item.areaName}\n등록 일시: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))}")
            .setPositiveButton("확인", null)
            .setNeutralButton(if (item.isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가") { _, _ ->
                val newStatus = itemStorage.toggleFavorite(item.id)
                Toast.makeText(requireContext(), if (newStatus) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 해제되었습니다.", Toast.LENGTH_SHORT).show()
                renderResults()
            }
            .setNegativeButton("삭제") { _, _ ->
                itemStorage.deleteItem(item.id)
                Toast.makeText(requireContext(), "물품이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                renderResults()
            }

        // If photo exists, show thumbnail in dialog
        if (item.photoUri != null) {
            val padding = dp(20)
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }
            val imgView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(200)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                try {
                    setImageURI(android.net.Uri.parse(item.photoUri))
                } catch (e: Exception) {
                    visibility = View.GONE
                }
            }
            container.addView(imgView)
            builder.setView(container)
        }

        builder.show()
    }

    private fun emptyStateView(message: String): View =
        TextView(requireContext()).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(dp(16), dp(28), dp(16), dp(28))
            textSize = 14f
        }

    private fun currentRoomName(): String =
        roomPrefs.getString(KEY_ROOM_NAME, getString(R.string.home_title)).orEmpty()

    private fun showRoomNameDialog() {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.room_name_hint)
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val input = TextInputEditText(inputLayout.context).apply {
            setText(currentRoomName())
            setSelectAllOnFocus(true)
            maxLines = 1
        }
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_room_name)
            .setView(inputLayout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val roomName = input.text?.toString()?.trim().orEmpty()
                    .ifEmpty { getString(R.string.home_title) }
                roomPrefs.edit().putString(KEY_ROOM_NAME, roomName).apply()
                binding.titleText.text = roomName
            }
            .show()
    }

    private fun openFeature(feature: String) {
        findNavController().navigate(
            R.id.action_FirstFragment_to_SecondFragment,
            Bundle().apply { putString(ARG_FEATURE, feature) }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_FEATURE = "feature"
        const val FEATURE_ROOM_MAP = "room_map"
        const val FEATURE_PHOTO_LIST = "photo_list"
        const val FEATURE_FAVORITES = "favorites"
        const val FEATURE_OUTING_CHECKLIST = "outing_checklist"
        const val FEATURE_WEEK_STATS = "week_stats"
        private const val KEY_ROOM_NAME = "room_name"
    }
}
