package kr.ac.pcu.aifinder.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ac.pcu.aifinder.ChecklistItem
import kr.ac.pcu.aifinder.databinding.ItemChecklistBinding

class ChecklistAdapter(
    private var items: List<ChecklistItem>,
    private val onCheckedChange: (Int, Boolean) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(val binding: ItemChecklistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvItemName.text = item.name
        holder.binding.cbItem.isChecked = item.checked
        
        holder.binding.cbItem.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(position, isChecked)
        }
        
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ChecklistItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
