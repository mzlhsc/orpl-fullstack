package com.mzlhsc.orpl.admin.ui.goods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.model.Dish
import com.mzlhsc.orpl.admin.databinding.ItemDishBinding
import com.mzlhsc.orpl.admin.util.Fmt

class DishAdapter(
    private val onToggle: (Dish, Boolean) -> Unit,
    private val onStock: (Dish) -> Unit,
    private val onEdit: (Dish) -> Unit,
    private val onDelete: (Dish) -> Unit,
) : RecyclerView.Adapter<DishAdapter.VH>() {

    private val items = mutableListOf<Dish>()

    fun submit(list: List<Dish>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun replace(dish: Dish) {
        val index = items.indexOfFirst { it.id == dish.id }
        if (index >= 0) {
            items[index] = dish
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDishBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemDishBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dish: Dish) {
            binding.tvName.text = dish.name ?: "未命名菜品"
            val price = "¥" + Fmt.moneyPlain(dish.price)
            binding.tvMeta.text = listOfNotNull(
                dish.categoryName?.takeIf { it.isNotBlank() },
                price,
            ).joinToString(" · ")
            binding.tvStock.text = "库存 ${dish.stock ?: 0}"

            if (dish.image.isNullOrBlank()) {
                binding.ivThumb.setImageDrawable(null)
            } else {
                Glide.with(binding.ivThumb)
                    .load(dish.image)
                    .centerCrop()
                    .placeholder(R.drawable.bg_tag_neutral)
                    .error(R.drawable.bg_tag_neutral)
                    .into(binding.ivThumb)
            }

            binding.swStatus.setOnCheckedChangeListener(null)
            binding.swStatus.isChecked = dish.status == 1
            binding.swStatus.setOnCheckedChangeListener { _, isChecked ->
                onToggle(dish, isChecked)
            }

            binding.btnStock.setOnClickListener { onStock(dish) }
            binding.btnEdit.setOnClickListener { onEdit(dish) }
            binding.btnDelete.setOnClickListener { onDelete(dish) }
            binding.root.setOnClickListener { onEdit(dish) }
        }
    }
}
