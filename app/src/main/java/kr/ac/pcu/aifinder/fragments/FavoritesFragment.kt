package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.adapters.ItemAdapter
import kr.ac.pcu.aifinder.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))
        
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(emptyList(), 
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
        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.adapter = adapter
    }

    private fun loadData() {
        val favorites = itemStorage.getItems().filter { it.isFavorite }
        adapter.updateItems(favorites)
    }

    override fun refreshData() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
