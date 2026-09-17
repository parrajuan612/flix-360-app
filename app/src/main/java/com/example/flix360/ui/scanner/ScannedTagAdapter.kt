package com.example.flix360.ui.scanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.flix360.databinding.ItemScannedTagBinding

data class ScannedTag(val epc: String, val rssi: Int)

class ScannedTagAdapter : RecyclerView.Adapter<ScannedTagAdapter.VH>() {

    private val items = mutableListOf<ScannedTag>()

    fun submit(list: List<ScannedTag>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScannedTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemScannedTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScannedTag) {
            binding.txtEpc.text = item.epc
            binding.txtRssi.text = "${item.rssi} dBm"
        }
    }
}
