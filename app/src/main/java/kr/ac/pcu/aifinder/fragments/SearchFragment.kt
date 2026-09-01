package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.adapters.ItemAdapter
import kr.ac.pcu.aifinder.databinding.FragmentSearchBinding
import kotlinx.coroutines.launch

class SearchFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private lateinit var recommender: AiFindRecommender
    private lateinit var adapter: ItemAdapter
    private var allItems = listOf<ItemRecord>()
    private var sortBy = "latest" // "latest" or "name"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))
        recommender = AiFindRecommender()
        
        val user = itemStorage.getCurrentUser()
        val displayName = user?.displayName ?: (user?.username ?: "")
        if (displayName.isNotEmpty()) {
            binding.tvWelcome.text = "${displayName}님, 환영합니다 👋"
        }

        setupRecyclerView()
        loadData()
        setupSearch()
        
        binding.cardAiAssistant.setOnClickListener {
            AiAssistantDialogFragment().show(parentFragmentManager, "ai_assistant")
        }

        binding.tvWelcome.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnSort.setOnClickListener {
            sortBy = if (sortBy == "latest") "name" else "latest"
            binding.tvSortLabel.text = if (sortBy == "latest") "최신순" else "이름순"
            filterItems(binding.etSearch.text?.toString().orEmpty())
        }
    }

    private fun showEditNameDialog() {
        val user = itemStorage.getCurrentUser() ?: return
        val currentName = user.displayName.takeIf { it.isNotBlank() } ?: user.username
        val editText = android.widget.EditText(requireContext()).apply {
            setText(currentName)
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("이름 변경")
            .setMessage("새로운 이름을 입력해주세요.")
            .setView(editText)
            .setPositiveButton("변경") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedUser = user.copy(displayName = newName)
                    itemStorage.saveOrUpdateUser(updatedUser)
                    binding.tvWelcome.text = "${newName}님, 환영합니다 👋"
                    if (user.id != "offline_user") {
                        viewLifecycleOwner.lifecycleScope.launch {
                            itemStorage.updateUserProfileRemote(updatedUser)
                        }
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(emptyList(), 
            lifecycleOwner = viewLifecycleOwner,
            onFavoriteClick = { item ->
                itemStorage.toggleFavorite(item.id)
                refreshData()
            },
            onItemClick = { item ->
                val detailFragment = ItemDetailFragment.newInstance(item.id)
                parentFragmentManager.beginTransaction()
                    .replace(kr.ac.pcu.aifinder.R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { item ->
                itemStorage.deleteItem(item.id)
                refreshData()
            }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(context)
        binding.rvItems.adapter = adapter
    }

    private fun loadData() {
        allItems = itemStorage.getItems()
        filterItems(binding.etSearch.text.toString())
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterItems(query: String) {
        val filtered = allItems.filter { 
            it.name.contains(query, ignoreCase = true) || it.areaName.contains(query, ignoreCase = true)
        }.let { list ->
            if (sortBy == "latest") list.sortedByDescending { it.timestamp }
            else list.sortedBy { it.name }
        }
        adapter.updateItems(filtered)

        // Recommendation logic
        if (query.isNotBlank()) {
            val recommendation = recommender.recommend(query, allItems, itemStorage.getRoomAreas())
            if (recommendation != null) {
                binding.cardRecommendation.visibility = View.VISIBLE
                binding.tvRecommendedArea.text = recommendation.recommendedArea.name
                binding.tvRecommendationConfidence.text = "${recommendation.confidence}% 일치"
                binding.tvRecommendationReason.text = recommendation.matchReason
            } else {
                binding.cardRecommendation.visibility = View.GONE
            }
        } else {
            binding.cardRecommendation.visibility = View.GONE
        }
    }

    override fun refreshData() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
