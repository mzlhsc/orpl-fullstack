package com.mzlhsc.orpl.admin.ui.goods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.model.Setmeal
import com.mzlhsc.orpl.admin.databinding.ItemSetmealBinding
import com.mzlhsc.orpl.admin.util.Fmt

class SetmealAdapter(
    private val onToggle: (Setmeal, Boolean) -> Unit,
    private val onEdit: (Setmeal) -> Unit,
    private val onDelete: (Setmeal) -> Unit,
) : RecyclerView.Adapter<SetmealAdapter.VH>() {

    private val items = mutableListOf<Setmeal>()

    fun submit(list: List<Setmeal>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun replace(setmeal: Setmeal) {
        val index = items.indexOfFirst { it.id == setmeal.id }
        if (index >= 0) {
            items[index] = setmeal
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSetmealBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemSetmealBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setmeal: Setmeal) {
            binding.tvName.text = setmeal.name ?: "未命名套餐"
            binding.tvMeta.text = listOfNotNull(
                setmeal.categoryName?.takeIf { it.isNotBlank() },
                "¥" + Fmt.moneyPlain(setmeal.price),
            ).joinToString(" · ")
            binding.tvDesc.text = setmeal.description?.takeIf { it.isNotBlank() } ?: "暂无描述"

            if (setmeal.image.isNullOrBlank()) {
                binding.ivThumb.setImageDrawable(null)
            } else {
                Glide.with(binding.ivThumb)
                    .load(setmeal.image)
                    .centerCrop()
                    .placeholder(R.drawable.bg_tag_neutral)
                    .error(R.drawable.bg_tag_neutral)
                    .into(binding.ivThumb)
            }

            binding.swStatus.setOnCheckedChangeListener(null)
            binding.swStatus.isChecked = setmeal.status == 1
            binding.swStatus.setOnCheckedChangeListener { _, isChecked ->
                onToggle(setmeal, isChecked)
            }

            binding.btnEdit.setOnClickListener { onEdit(setmeal) }
            binding.btnDelete.setOnClickListener { onDelete(setmeal) }
            binding.root.setOnClickListener { onEdit(setmeal) }
        }
    }
}
