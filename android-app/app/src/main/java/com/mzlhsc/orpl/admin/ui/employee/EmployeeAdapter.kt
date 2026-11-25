package com.mzlhsc.orpl.admin.ui.employee

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mzlhsc.orpl.admin.data.model.Employee
import com.mzlhsc.orpl.admin.databinding.ItemEmployeeBinding
import com.mzlhsc.orpl.admin.util.Tags

class EmployeeAdapter(
    private val onToggle: (Employee, Boolean) -> Unit,
    private val onEdit: (Employee) -> Unit,
) : RecyclerView.Adapter<EmployeeAdapter.VH>() {

    private val items = mutableListOf<Employee>()

    fun submit(list: List<Employee>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun replace(employee: Employee) {
        val index = items.indexOfFirst { it.id == employee.id }
        if (index >= 0) {
            items[index] = employee
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(employee: Employee) {
            val name = employee.name ?: "未命名"
            binding.tvName.text = name
            binding.tvAvatar.text = name.trim().take(1).ifEmpty { "员" }
            binding.tvMeta.text = listOfNotNull(
                employee.username?.takeIf { it.isNotBlank() }?.let { "账号 $it" },
                employee.phone?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { "—" }

            val (text, tone) = Tags.enable(employee.status)
            Tags.apply(binding.tvTag, text, tone)

            binding.swStatus.setOnCheckedChangeListener(null)
            binding.swStatus.isChecked = employee.status == 1
            binding.swStatus.setOnCheckedChangeListener { _, checked -> onToggle(employee, checked) }
            binding.root.setOnClickListener { onEdit(employee) }
        }
    }
}