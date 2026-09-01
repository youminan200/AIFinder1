package kr.ac.pcu.aifinder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kr.ac.pcu.aifinder.ItemRecord
import kr.ac.pcu.aifinder.databinding.ItemRecordRowBinding
import kr.ac.pcu.aifinder.utils.getItemBadgeColor
import kr.ac.pcu.aifinder.utils.getItemIconRes
import kr.ac.pcu.aifinder.utils.loadItemImage
import kotlinx.coroutines.launch

class ItemAdapter(
    private var items: List<ItemRecord>,
    private val lifecycleOwner: LifecycleOwner,
    private val onFavoriteClick: (ItemRecord) -> Unit,
    private val onItemClick: (ItemRecord) -> Unit,
    private val onDeleteClick: (ItemRecord) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvItemName.text = item.name
        holder.binding.tvAreaName.text = item.areaName
        
        val starIcon = if (item.isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        holder.binding.btnFavorite.setImageResource(starIcon)
        holder.binding.btnFavorite.setColorFilter(if (item.isFavorite) android.graphics.Color.parseColor("#FF2D55") else android.graphics.Color.parseColor("#CBD5E1"))

        holder.binding.btnFavorite.setOnClickListener { onFavoriteClick(item) }
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
        
        // Photo loading logic
        if (!item.photoUri.isNullOrEmpty()) {
            holder.binding.ivItemPhoto.visibility = View.VISIBLE
            holder.binding.ivFallbackIcon.visibility = View.GONE
            holder.binding.cardImage.setCardBackgroundColor(android.graphics.Color.parseColor("#F1F5F9"))
            
            lifecycleOwner.lifecycleScope.launch {
                loadItemImage(holder.itemView.context, item.photoUri, holder.binding.ivItemPhoto)
            }
        } else {
            holder.binding.ivItemPhoto.visibility = View.GONE
            holder.binding.ivFallbackIcon.visibility = View.VISIBLE
            
            val badge = getItemBadgeColor(item.name)
            holder.binding.cardImage.setCardBackgroundColor(badge.background)
            holder.binding.ivFallbackIcon.setImageResource(getItemIconRes(item.name))
            holder.binding.ivFallbackIcon.setColorFilter(badge.tint)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ItemRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}
