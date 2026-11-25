package com.mzlhsc.orpl.ctgy.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.ctgy.domain.dto.CategoryDTO;
import com.mzlhsc.orpl.ctgy.domain.dto.CategoryPageQueryDTO;
import com.mzlhsc.orpl.ctgy.domain.po.Category;
import com.mzlhsc.orpl.ctgy.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 菜品及套餐分类 前端控制器（管理端）
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-06
 */
@RestController("adminCategoryController")
@RequestMapping("/admin/category")
@RequiredArgsConstructor
@Tag(name = "菜品及套餐分类", description = "照片")
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;

    /**
     * 添加菜品分类
     */
    @PostMapping
    @Operation(summary = "添加菜品分类")
    public void save(@RequestBody CategoryDTO categoryDTO) {
        log.info("添加菜品分类: {}", categoryDTO);
        validate(categoryDTO);
        categoryService.save(BeanUtil.copyProperties(categoryDTO, Category.class)
                .setStatus(1));
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public PageDTO<Category> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分页查询: {}", categoryPageQueryDTO);
        return categoryService.pageQuery(categoryPageQueryDTO);
    }

    /**
     * 根据类型查询分类（管理端返回启用 + 禁用的全部分类）
     */
    @GetMapping("/list")
    @Operation(summary = "根据类型查询分类")
    public List<Category> list(Integer type) {
        log.info("根据类型查询分类: {}", type);
        return categoryService.lambdaQuery()
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort)
                .list();
    }

    /**
     * 修改菜品分类
     */
    @PutMapping
    @Operation(summary = "修改菜品分类")
    public void update(@RequestBody CategoryDTO categoryDTO) {
        log.info("修改菜品分类: {}", categoryDTO);
        if (categoryDTO == null || categoryDTO.getId() == null) {
            throw new BadRequestException("分类id不能为空");
        }
        if (categoryDTO.getName() != null && !StringUtils.hasText(categoryDTO.getName())) {
            throw new BadRequestException("分类名称不能为空");
        }
        categoryService.updateById(BeanUtil.copyProperties(categoryDTO, Category.class));
    }

    /**
     * 根据id删除
     */
    @DeleteMapping
    @Operation(summary = "根据id删除")
    public void deletes(@RequestParam Long id) {
        log.info("根据id删除: {}", id);
        if (id == null) {
            throw new BadRequestException("分类id不能为空");
        }
        //删除没有菜品的菜品分类
        categoryService.removeNoDishByIds(id);
    }

    /**
     * 启用、禁用分类
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用、禁用分类")
    public void status(@PathVariable Integer status, @RequestParam Long id) {
        log.info("启用、禁用分类: {} {}", status, id);
        if (id == null) {
            throw new BadRequestException("分类id不能为空");
        }
        if (status == null || status != 0 && status != 1) {
            throw new BadRequestException("分类状态不合法");
        }
        if (categoryService.getById(id) == null) {
            throw new BadRequestException("分类不存在");
        }
        categoryService.lambdaUpdate()
                .set(Category::getStatus, status)
                .eq(Category::getId, id)
                .update();
    }

    private void validate(CategoryDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new BadRequestException("分类名称不能为空");
        }
        if (dto.getType() == null || dto.getType() != 1 && dto.getType() != 2) {
            throw new BadRequestException("分类类型不合法");
        }
    }

}

