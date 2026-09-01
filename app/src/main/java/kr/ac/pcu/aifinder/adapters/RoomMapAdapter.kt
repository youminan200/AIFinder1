package kr.ac.pcu.aifinder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kr.ac.pcu.aifinder.ItemRecord
import kr.ac.pcu.aifinder.databinding.ItemRoomMapRowBinding
import kr.ac.pcu.aifinder.databinding.ItemRecordRowBinding
import kr.ac.pcu.aifinder.utils.getItemBadgeColor
import kr.ac.pcu.aifinder.utils.getItemIconRes
import kr.ac.pcu.aifinder.utils.loadItemImage
import kotlinx.coroutines.launch

class RoomMapAdapter(
    private var items: List<ItemRecord>,
    private val lifecycleOwner: LifecycleOwner,
    private val onFavoriteClick: (ItemRecord) -> Unit,
    private val onItemClick: (ItemRecord) -> Unit,
    private val onDeleteClick: (ItemRecord) -> Unit
) : RecyclerView.Adapter<RoomMapAdapter.RoomMapViewHolder>() {

    inner class RoomMapViewHolder(val binding: ItemRoomMapRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomMapViewHolder {
        val binding = ItemRoomMapRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomMapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomMapViewHolder, position: Int) {
        val item = items[position]
        val itemBinding = ItemRecordRowBinding.bind(holder.binding.itemCard.root)
        
        // Bind item data to the included layout
        itemBinding.tvItemName.text = item.name
        itemBinding.tvAreaName.text = item.areaName
        
        val starIcon = if (item.isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        itemBinding.btnFavorite.setImageResource(starIcon)
        itemBinding.btnFavorite.setColorFilter(if (item.isFavorite) android.graphics.Color.parseColor("#FF2D55") else android.graphics.Color.parseColor("#CBD5E1"))

        itemBinding.btnFavorite.setOnClickListener { onFavoriteClick(item) }
        itemBinding.btnDelete.setOnClickListener { onDeleteClick(item) }
        itemBinding.root.setOnClickListener { onItemClick(item) }

        // Timeline visibility
        holder.binding.timelineTopLine.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        holder.binding.timelineBottomLine.visibility = if (position == items.size - 1) View.INVISIBLE else View.VISIBLE

        // Photo/Badge
        if (!item.photoUri.isNullOrEmpty()) {
            itemBinding.ivItemPhoto.visibility = View.VISIBLE
            itemBinding.ivFallbackIcon.visibility = View.GONE
            itemBinding.cardImage.setCardBackgroundColor(android.graphics.Color.parseColor("#F1F5F9"))
            
            lifecycleOwner.lifecycleScope.launch {
                loadItemImage(holder.itemView.context, item.photoUri, itemBinding.ivItemPhoto)
            }
        } else {
            itemBinding.ivItemPhoto.visibility = View.GONE
            itemBinding.ivFallbackIcon.visibility = View.VISIBLE
            
            val badge = getItemBadgeColor(item.name)
            itemBinding.cardImage.setCardBackgroundColor(badge.background)
            itemBinding.ivFallbackIcon.setImageResource(getItemIconRes(item.name))
            itemBinding.ivFallbackIcon.setColorFilter(badge.tint)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ItemRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}
