package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.adapters.ChecklistAdapter
import kr.ac.pcu.aifinder.databinding.FragmentChecklistBinding

class ChecklistFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: PlatformStorage
    private lateinit var adapter: ChecklistAdapter
    private var checklistItems = mutableListOf<ChecklistItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = PlatformStorage(requireContext())
        
        setupRecyclerView()
        loadData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = ChecklistAdapter(checklistItems, 
            onCheckedChange = { position, isChecked ->
                checklistItems[position] = checklistItems[position].copy(checked = isChecked)
                saveData()
            },
            onDeleteClick = { position ->
                checklistItems.removeAt(position)
                adapter.updateItems(checklistItems)
                saveData()
            }
        )
        binding.rvChecklist.layoutManager = LinearLayoutManager(context)
        binding.rvChecklist.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            val name = binding.etNewItem.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) {
                checklistItems.add(ChecklistItem(name = name, checked = false))
                adapter.updateItems(checklistItems)
                binding.etNewItem.text?.clear()
                saveData()
            }
        }

        binding.btnTimePicker.setOnClickListener {
            val hour = storage.getString("outing_alarm_hour", "8")?.toIntOrNull() ?: 8
            val minute = storage.getString("outing_alarm_minute", "0")?.toIntOrNull() ?: 0
            
            showTimePickerDialog(requireContext(), hour, minute) { h, m ->
                storage.putString("outing_alarm_hour", h.toString())
                storage.putString("outing_alarm_minute", m.toString())
                updateTimeButtonText(h, m)
                if (binding.switchAlarm.isChecked) {
                    scheduleOutingAlarm(requireContext(), h, m)
                }
            }
        }

        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked: Boolean ->
            storage.putString("outing_alarm_enabled", if (isChecked) "1" else "0")
            binding.layoutTimePicker.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                val h = storage.getString("outing_alarm_hour", "8")?.toIntOrNull() ?: 8
                val m = storage.getString("outing_alarm_minute", "0")?.toIntOrNull() ?: 0
                checkAndRequestNotificationPermission(requireActivity())
                scheduleOutingAlarm(requireContext(), h, m)
            } else {
                cancelOutingAlarm(requireContext())
            }
        }
    }

    private fun loadData() {
        val listText = storage.getString("checklist_items", "") ?: ""
        checklistItems.clear()
        if (listText.isBlank()) {
            val defaults = listOf("휴대폰", "지갑", "현관 열쇠", "우산", "보조 배터리")
            checklistItems.addAll(defaults.map { ChecklistItem(name = it, checked = false) })
        } else {
            val loaded = listText.split("|").filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split("^")
                val name = parts.getOrNull(0).orEmpty()
                if (name.isBlank()) null else ChecklistItem(name = name, checked = parts.getOrNull(1) == "1")
            }
            checklistItems.addAll(loaded)
        }
        adapter.updateItems(checklistItems)
        
        // Setup Alarm states
        val alarmEnabled = storage.getString("outing_alarm_enabled", "0") == "1"
        binding.switchAlarm.isChecked = alarmEnabled
        binding.layoutTimePicker.visibility = if (alarmEnabled) View.VISIBLE else View.GONE
        
        val hour = storage.getString("outing_alarm_hour", "8")?.toIntOrNull() ?: 8
        val minute = storage.getString("outing_alarm_minute", "0")?.toIntOrNull() ?: 0
        updateTimeButtonText(hour, minute)
    }

    private fun saveData() {
        val serialized = checklistItems.joinToString("|") { "${it.name}^${if (it.checked) "1" else "0"}" }
        storage.putString("checklist_items", serialized)
    }

    private fun updateTimeButtonText(hour: Int, minute: Int) {
        val displayTime = String.format("오전 %02d:%02d", if (hour > 12) hour - 12 else hour, minute)
            .replace("오전 00", "오전 12")
            .let { if (hour >= 12) it.replace("오전", "오후") else it }
        binding.btnTimePicker.text = displayTime
    }

    override fun refreshData() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
