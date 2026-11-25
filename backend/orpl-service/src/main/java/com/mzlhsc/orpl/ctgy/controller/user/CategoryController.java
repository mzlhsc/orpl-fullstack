package com.mzlhsc.orpl.ctgy.controller.user;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.ctgy.domain.po.Category;
import com.mzlhsc.orpl.ctgy.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 菜品及套餐分类 前端控制器（用户端）
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-06
 */
@RestController("userCategoryController")
@RequestMapping("/user/category")
@RequiredArgsConstructor
@Tag(name = "菜品及套餐分类")
@Slf4j
public class CategoryController {

    /** 分类状态：1 启用，0 禁用 */
    private static final int STATUS_ENABLE = 1;

    private final ICategoryService categoryService;

    /**
     * 根据类型查询分类（用户端只返回启用状态的分类）
     *
     * @param type 1 菜品分类，2 套餐分类
     */
    @GetMapping("/list")
    @Operation(summary = "根据类型查询分类")
    public List<Category> list(Integer type) {
        log.info("根据类型查询分类: {}", type);
        if (type != null && type != 1 && type != 2) {
            throw new BadRequestException("分类类型不合法");
        }
        return categoryService.lambdaQuery()
                .eq(type != null, Category::getType, type)
                .eq(Category::getStatus, STATUS_ENABLE)
                .orderByAsc(Category::getSort)
                .list();
    }

}

