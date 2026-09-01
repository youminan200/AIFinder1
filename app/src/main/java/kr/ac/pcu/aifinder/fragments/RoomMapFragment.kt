package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.adapters.RoomMapAdapter
import kr.ac.pcu.aifinder.databinding.FragmentRoomMapBinding

class RoomMapFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentRoomMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private lateinit var adapter: RoomMapAdapter
    private var selectedAreaId: Int = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoomMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))
        
        setupRoomMapView()
        setupRecyclerView()
        loadData()
    }

    private fun setupRoomMapView() {
        val areas = itemStorage.getRoomAreas()
        val items = itemStorage.getItems()
        val counts = items.groupBy { it.areaId }.mapValues { it.value.size }
        
        binding.roomMapView.setAreas(areas, counts)
        binding.roomMapView.selectedAreaId = selectedAreaId
        binding.roomMapView.setOnAreaClickListener { area ->
            selectedAreaId = area.id
            binding.tvAreaHeader.text = "${area.name}의 물건들"
            loadData()
        }
        
        binding.btnRenameArea.setOnClickListener {
            showRenameDialog()
        }
    }

    private fun showRenameDialog() {
        val currentArea = itemStorage.getRoomAreas().find { it.id == selectedAreaId } ?: return
        val editText = android.widget.EditText(requireContext()).apply {
            setText(currentArea.name)
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("구역 이름 변경")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    itemStorage.renameArea(selectedAreaId, newName)
                    binding.tvAreaHeader.text = "${newName}의 물건들"
                    loadData()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = RoomMapAdapter(emptyList(),
            lifecycleOwner = viewLifecycleOwner,
            onFavoriteClick = { item ->
                itemStorage.toggleFavorite(item.id)
                loadData()
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
                loadData()
            }
        )
        binding.rvAreaItems.layoutManager = LinearLayoutManager(context)
        binding.rvAreaItems.adapter = adapter
    }

    private fun loadData() {
        val areaItems = itemStorage.getItems().filter { it.areaId == selectedAreaId }
        adapter.updateItems(areaItems)
        
        // Update room map view counts
        val items = itemStorage.getItems()
        val counts = items.groupBy { it.areaId }.mapValues { it.value.size }
        binding.roomMapView.setAreas(itemStorage.getRoomAreas(), counts)
    }

    override fun refreshData() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
