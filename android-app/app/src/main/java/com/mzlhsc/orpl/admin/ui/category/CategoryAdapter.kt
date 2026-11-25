package com.mzlhsc.orpl.admin.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onToggle: (Category, Boolean) -> Unit,
    private val onEdit: (Category) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private val items = mutableListOf<Category>()

    fun submit(list: List<Category>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun currentList(): List<Category> = items.toList()

    fun replace(category: Category) {
        val index = items.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            items[index] = category
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvName.text = category.name ?: "未命名分类"
            binding.tvMeta.text = "排序 ${category.sort ?: 0} · 编号 ${category.id ?: "-"}"

            binding.swStatus.setOnCheckedChangeListener(null)
            binding.swStatus.isChecked = category.status == 1
            binding.swStatus.setOnCheckedChangeListener { _, checked -> onToggle(category, checked) }

            binding.btnEdit.setOnClickListener { onEdit(category) }
            binding.root.setOnClickListener { onEdit(category) }
        }
    }
}
