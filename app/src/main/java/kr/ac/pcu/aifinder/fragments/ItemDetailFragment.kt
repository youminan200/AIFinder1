package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.databinding.FragmentItemDetailBinding
import kr.ac.pcu.aifinder.utils.loadItemImage
import kotlinx.coroutines.launch

class ItemDetailFragment : Fragment() {
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private var itemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getString("item_id")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))
        
        val item = itemStorage.getItems().find { it.id == itemId }
        if (item != null) {
            binding.tvItemName.text = item.name
            binding.tvAreaName.text = "위치: ${item.areaName}"
            
            // Load photo
            if (!item.photoUri.isNullOrEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    loadItemImage(requireContext(), item.photoUri, binding.ivItemPhoto)
                }
            }
            
            binding.btnDelete.setOnClickListener {
                itemStorage.deleteItem(item.id)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(itemId: String) = ItemDetailFragment().apply {
            arguments = Bundle().apply {
                putString("item_id", itemId)
            }
        }
    }
}
