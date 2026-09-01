package kr.ac.pcu.aifinder.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ac.pcu.aifinder.databinding.ItemStatsRowBinding

class StatsAdapter(
    private var stats: List<Pair<String, Int>>,
    private var totalCount: Int
) : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {

    inner class StatsViewHolder(val binding: ItemStatsRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val binding = ItemStatsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val (areaName, count) = stats[position]
        val ratio = if (totalCount > 0) count.toFloat() / totalCount else 0f
        
        holder.binding.tvAreaName.text = areaName
        holder.binding.tvCountPercent.text = "${count}개 (${(ratio * 100).toInt()}%)"
        holder.binding.pbRatio.progress = (ratio * 100).toInt()
    }

    override fun getItemCount(): Int = stats.size

    fun updateStats(newStats: List<Pair<String, Int>>, newTotal: Int) {
        stats = newStats
        totalCount = newTotal
        notifyDataSetChanged()
    }
}
