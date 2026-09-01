package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.adapters.StatsAdapter
import kr.ac.pcu.aifinder.databinding.FragmentStatsBinding

class StatsFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private lateinit var adapter: StatsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))
        
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = StatsAdapter(emptyList(), 0)
        binding.rvStats.layoutManager = LinearLayoutManager(context)
        binding.rvStats.adapter = adapter
    }

    private fun loadData() {
        val stats = itemStorage.getRecent7DaysStats()
        val sortedStats = stats.toList().sortedByDescending { it.second }
        val totalCount = stats.values.sum()
        
        binding.tvTotalCount.text = "${totalCount}개"
        adapter.updateStats(sortedStats, totalCount)
    }

    override fun refreshData() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
