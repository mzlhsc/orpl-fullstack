package com.mzlhsc.orpl.admin.ui.goods

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.data.model.Dish
import com.mzlhsc.orpl.admin.data.model.Setmeal
import com.mzlhsc.orpl.admin.data.model.SetmealDish
import com.mzlhsc.orpl.admin.databinding.ActivitySetmealEditBinding
import com.mzlhsc.orpl.admin.databinding.ItemSetmealDishBinding
import com.mzlhsc.orpl.admin.util.Fmt
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast

/** 套餐新增 / 编辑：与 Web 端套餐表单一致，含图片、名称、分类、价格、描述、起售状态与菜品明细 */
class SetmealEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetmealEditBinding

    private var setmealId: Long = 0L
    private var categories: List<Category> = emptyList()
    private var categoryId: Long? = null
    private var imageUrl: String? = null
    private var uploading = false

    private val chosen = mutableListOf<SetmealDish>()
    private var allDishes: List<Dish> = emptyList()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetmealEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setmealId = intent.getLongExtra(EXTRA_ID, 0L)
        val editing = setmealId > 0
        binding.toolbar.toolbarTitle.text = if (editing) "编辑套餐" else "新增套餐"
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        binding.etImage.setOnFocusChangeListener { _, _ -> applyPreview(binding.etImage.text.toString().trim()) }
        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnClearImage.setOnClickListener {
            imageUrl = null
            binding.etImage.setText("")
            applyPreview("")
        }
        binding.tvCategory.setOnClickListener { chooseCategory() }
        binding.btnAddDish.setOnClickListener { pickDishes() }
        binding.btnSave.setOnClickListener { save() }

        if (!editing) binding.swStatus.isChecked = true
        loadCategories()
        if (editing) loadSetmeal()
    }

    private fun loadCategories() {
        safeLaunch {
            categories = runCatching { AdminApi.categoryList(2) }.getOrDefault(emptyList())
            categoryId?.let { id -> bindCategory(categories.firstOrNull { it.id == id }) }
        }
    }

    private fun loadSetmeal() {
        safeLaunch {
            val data = AdminApi.setmealGet(setmealId) ?: run {
                toast("套餐不存在或已被删除")
                finish()
                return@safeLaunch
            }
            binding.etName.setText(data.name.orEmpty())
            binding.etPrice.setText(if (data.price == null) "" else data.price.toString())
            binding.etDesc.setText(data.description.orEmpty())
            binding.etImage.setText(data.image.orEmpty())
            binding.swStatus.isChecked = data.status == 1
            imageUrl = data.image
            categoryId = data.categoryId
            bindCategory(categories.firstOrNull { it.id == data.categoryId })
            applyPreview(data.image.orEmpty())

            chosen.clear()
            data.setmealDishes.orEmpty().forEach {
                chosen += it.copy(copies = it.copies ?: 1)
            }
            renderChosen()
        }
    }

    private fun bindCategory(category: Category?) {
        if (category == null) return
        categoryId = category.id
        binding.tvCategory.text = category.name.orEmpty()
        binding.tvCategory.setTextColor(getColor(R.color.ink))
    }

    private fun chooseCategory() {
        if (categories.isEmpty()) {
            toast("暂无套餐分类，请先到分类管理里新增")
            return
        }
        val names = categories.map { it.name.orEmpty() }.toTypedArray()
        val checked = categories.indexOfFirst { it.id == categoryId }
        MaterialAlertDialogBuilder(this)
            .setTitle("选择分类")
            .setSingleChoiceItems(names, checked) { dialog, which ->
                bindCategory(categories[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun applyPreview(url: String) {
        if (url.isBlank()) {
            binding.ivImage.setImageDrawable(null)
            return
        }
        Glide.with(binding.ivImage)
            .load(url)
            .centerCrop()
            .placeholder(R.drawable.bg_tag_neutral)
            .error(R.drawable.bg_tag_neutral)
            .into(binding.ivImage)
    }

    private fun uploadImage(uri: Uri) {
        if (uploading) return
        uploading = true
        binding.btnPickImage.isEnabled = false
        binding.btnPickImage.text = "上传中…"
        safeLaunch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("读取图片失败")
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val name = "setmeal_${System.currentTimeMillis()}." + when {
                    mime.contains("png") -> "png"
                    mime.contains("webp") -> "webp"
                    else -> "jpg"
                }
                val url = AdminApi.uploadImage(name, mime, bytes)
                if (url.isNullOrBlank()) {
                    toast("上传失败：后端未返回图片地址")
                } else {
                    imageUrl = url
                    binding.etImage.setText(url)
                    applyPreview(url)
                    toast("图片已上传")
                }
            } catch (e: Exception) {
                toast("图片上传失败：${e.message ?: "请确认后端已配置 OSS"}")
            } finally {
                uploading = false
                binding.btnPickImage.isEnabled = true
                binding.btnPickImage.text = "上传图片"
            }
        }
    }

    /** 从全部菜品里多选，已选的会被排除 */
    private fun pickDishes() {
        safeLaunch {
            if (allDishes.isEmpty()) {
                binding.btnAddDish.text = "加载中…"
                allDishes = runCatching { AdminApi.dishAll() }.getOrDefault(emptyList())
                binding.btnAddDish.text = "＋ 添加菜品"
            }
            val candidates = allDishes.filter { dish -> chosen.none { it.dishId == dish.id } }
            if (candidates.isEmpty()) {
                toast(if (allDishes.isEmpty()) "暂无可选菜品" else "所有菜品都已加入套餐")
                return@safeLaunch
            }
            val names = candidates.map { "${it.name.orEmpty()}  ¥${Fmt.moneyPlain(it.price)}" }.toTypedArray()
            val checked = BooleanArray(candidates.size)
            MaterialAlertDialogBuilder(this)
                .setTitle("选择菜品")
                .setMultiChoiceItems(names, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setNegativeButton("取消", null)
                .setPositiveButton("添加") { _, _ ->
                    candidates.forEachIndexed { index, dish ->
                        if (checked[index]) {
                            chosen += SetmealDish(
                                dishId = dish.id,
                                name = dish.name,
                                price = dish.price,
                                copies = 1,
                            )
                        }
                    }
                    renderChosen()
                }
                .show()
        }
    }

    private fun renderChosen() {
        binding.dishBox.removeAllViews()
        binding.tvDishHint.text = if (chosen.isEmpty()) {
            "至少添加一个菜品，份数可调整"
        } else {
            "已选 ${chosen.size} 种菜品，合计 ${chosen.sumOf { (it.copies ?: 1) }} 份"
        }
        chosen.forEach { item ->
            val row = ItemSetmealDishBinding.inflate(LayoutInflater.from(this), binding.dishBox, false)
            row.tvDishName.text = item.name ?: "菜品"
            row.tvDishPrice.text = "¥" + Fmt.moneyPlain(item.price)
            row.tvCopies.text = (item.copies ?: 1).toString()
            row.btnMinus.setOnClickListener {
                val next = ((item.copies ?: 1) - 1).coerceAtLeast(1)
                item.copies = next
                row.tvCopies.text = next.toString()
            }
            row.btnPlus.setOnClickListener {
                val next = (item.copies ?: 1) + 1
                item.copies = next
                row.tvCopies.text = next.toString()
            }
            row.btnRemove.setOnClickListener {
                chosen.remove(item)
                renderChosen()
            }
            binding.dishBox.addView(row.root)
        }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val category = categoryId
        val price = binding.etPrice.text.toString().trim().toDoubleOrNull()
        if (name.isEmpty()) {
            toast("请填写套餐名称")
            return
        }
        if (category == null) {
            toast("请选择所属分类")
            return
        }
        if (price == null || price < 0) {
            toast("请填写正确的价格")
            return
        }
        if (chosen.isEmpty()) {
            toast("请至少添加一个菜品")
            return
        }

        val dishes = chosen.map {
            mapOf(
                "dishId" to it.dishId,
                "name" to it.name,
                "price" to it.price,
                "copies" to (it.copies ?: 1),
            )
        }
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "categoryId" to category,
            "price" to price,
            "image" to (imageUrl ?: binding.etImage.text.toString().trim()),
            "description" to binding.etDesc.text.toString().trim(),
            "status" to if (binding.swStatus.isChecked) 1 else 0,
            "setmealDishes" to dishes,
        )
        if (setmealId > 0) body["id"] = setmealId

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "保存中…"
        safeLaunch {
            try {
                if (setmealId > 0) AdminApi.setmealUpdate(body) else AdminApi.setmealSave(body)
                toast(if (setmealId > 0) "套餐已更新" else "套餐已创建")
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                toast(e.message ?: "保存失败")
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "保存"
            }
        }
    }

    companion object {
        const val EXTRA_ID = "setmealId"

        fun intent(activity: Activity, setmealId: Long? = null): Intent =
            Intent(activity, SetmealEditActivity::class.java).apply {
                putExtra(EXTRA_ID, setmealId ?: 0L)
            }
    }
}
