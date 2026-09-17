package com.example.flix360.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.flix360.databinding.ItemCategorySummaryBinding
import java.text.NumberFormat
import java.util.Locale

data class CategorySummary(val name: String, val quantity: Double)

class CategorySummaryAdapter : RecyclerView.Adapter<CategorySummaryAdapter.VH>() {

    private val items = mutableListOf<CategorySummary>()

    fun submit(list: List<CategorySummary>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
    fun submitList(list: List<CategorySummary>) = submit(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCategorySummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemCategorySummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: CategorySummary) {
            binding.txtName.text = model.name.uppercase(Locale.getDefault())
            binding.txtQuantity.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(model.quantity)
        }
    }
}
