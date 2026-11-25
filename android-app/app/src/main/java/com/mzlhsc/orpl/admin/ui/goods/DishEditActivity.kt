package com.mzlhsc.orpl.admin.ui.goods

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mzlhsc.orpl.admin.R
import com.mzlhsc.orpl.admin.data.AdminApi
import com.mzlhsc.orpl.admin.data.model.Category
import com.mzlhsc.orpl.admin.data.model.DishFlavor
import com.mzlhsc.orpl.admin.data.model.Dish
import com.mzlhsc.orpl.admin.databinding.ActivityDishEditBinding
import com.mzlhsc.orpl.admin.databinding.ItemDishFlavorBinding
import com.mzlhsc.orpl.admin.util.safeLaunch
import com.mzlhsc.orpl.admin.util.toast
import com.mzlhsc.orpl.admin.util.visibleIf

/** 菜品新增 / 编辑。字段与 Web 端新增菜品表单一致：图片、名称、分类、价格、库存、描述、口味、起售状态 */
class DishEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDishEditBinding

    private var dishId: Long = 0L
    private var dish: Dish? = null
    private var categories: List<Category> = emptyList()
    private var categoryId: Long? = null
    private var imageUrl: String? = null
    private var uploading = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDishEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dishId = intent.getLongExtra(EXTRA_ID, 0L)
        val editing = dishId > 0

        binding.toolbar.toolbarTitle.text = if (editing) "编辑菜品" else "新增菜品"
        binding.toolbar.toolbarBack.setOnClickListener { finish() }

        binding.etImage.setOnFocusChangeListener { _, _ -> applyPreview(binding.etImage.text.toString().trim()) }
        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnClearImage.setOnClickListener {
            imageUrl = null
            binding.etImage.setText("")
            applyPreview("")
        }
        binding.tvCategory.setOnClickListener { chooseCategory() }
        binding.btnAddFlavor.setOnClickListener { addFlavorRow(DishFlavor()) }
        binding.btnSave.setOnClickListener { save() }

        loadCategories()
        if (editing) loadDish() else binding.swStatus.isChecked = true
    }

    private fun loadCategories() {
        safeLaunch {
            categories = runCatching { AdminApi.categoryList(1) }.getOrDefault(emptyList())
            dish?.categoryId?.let { id -> bindCategory(categories.firstOrNull { it.id == id }) }
        }
    }

    private fun loadDish() {
        safeLaunch {
            val data = AdminApi.dishGet(dishId) ?: run {
                toast("菜品不存在或已被删除")
                finish()
                return@safeLaunch
            }
            dish = data
            binding.etName.setText(data.name.orEmpty())
            binding.etPrice.setText(if (data.price == null) "" else data.price.toString())
            binding.etStock.setText((data.stock ?: 100).toString())
            binding.etDesc.setText(data.description.orEmpty())
            binding.etImage.setText(data.image.orEmpty())
            binding.swStatus.isChecked = data.status == 1
            imageUrl = data.image
            categoryId = data.categoryId
            bindCategory(categories.firstOrNull { it.id == data.categoryId })
            applyPreview(data.image.orEmpty())
            data.flavors.orEmpty().forEach { addFlavorRow(it) }
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
            toast("暂无菜品分类，请先到分类管理里新增")
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
                val name = "dish_${System.currentTimeMillis()}." + when {
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

    private fun addFlavorRow(flavor: DishFlavor) {
        val row = ItemDishFlavorBinding.inflate(LayoutInflater.from(this), binding.flavorBox, false)
        row.etFlavorName.setText(flavor.name)
        row.etFlavorValue.setText(flavor.value)
        row.btnRemoveFlavor.setOnClickListener { binding.flavorBox.removeView(row.root) }
        binding.flavorBox.addView(row.root)
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val category = categoryId
        if (name.isEmpty()) {
            toast("请填写菜品名称")
            return
        }
        if (category == null) {
            toast("请选择所属分类")
            return
        }
        val price = priceText.toDoubleOrNull()
        if (price == null || price < 0) {
            toast("请填写正确的价格")
            return
        }
        val stock = binding.etStock.text.toString().trim().toIntOrNull() ?: 100

        // 只收集两个字段都填了的口味行
        val flavors = mutableListOf<Map<String, Any?>>()
        for (i in 0 until binding.flavorBox.childCount) {
            val row = ItemDishFlavorBinding.bind(binding.flavorBox.getChildAt(i))
            val fName = row.etFlavorName.text.toString().trim()
            val fValue = row.etFlavorValue.text.toString().trim()
            if (fName.isNotEmpty() && fValue.isNotEmpty()) {
                flavors += mapOf(
                    "name" to fName,
                    "value" to fValue,
                    "dishId" to dishId.takeIf { it > 0 },
                )
            }
        }

        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "categoryId" to category,
            "price" to price,
            "image" to (imageUrl ?: binding.etImage.text.toString().trim()),
            "description" to binding.etDesc.text.toString().trim(),
            "status" to if (binding.swStatus.isChecked) 1 else 0,
            "stock" to stock,
            "flavors" to flavors,
        )
        if (dishId > 0) body["id"] = dishId

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "保存中…"
        safeLaunch {
            try {
                if (dishId > 0) AdminApi.dishUpdate(body) else AdminApi.dishSave(body)
                toast(if (dishId > 0) "菜品已更新" else "菜品已创建")
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
        const val EXTRA_ID = "dishId"

        fun intent(activity: Activity, dishId: Long? = null): Intent =
            Intent(activity, DishEditActivity::class.java).apply {
                putExtra(EXTRA_ID, dishId ?: 0L)
            }
    }
}
