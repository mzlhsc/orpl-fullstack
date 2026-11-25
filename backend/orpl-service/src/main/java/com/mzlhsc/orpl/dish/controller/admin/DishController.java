package com.mzlhsc.orpl.dish.controller.admin;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.dish.domain.dto.DishDTO;
import com.mzlhsc.orpl.dish.domain.dto.DishPageQueryDTO;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.dish.domain.vo.DishVO;
import com.mzlhsc.orpl.dish.service.IDishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理（管理端）
 */
@RequestMapping("/admin/dish")
@RestController("adminDishController")
@RequiredArgsConstructor
@Tag(name = "菜品相关接口", description = "菜品的增删改查接口")
@Slf4j
public class DishController {

    private final IDishService dishService;

    /**
     * 新增菜品
     */
    @PostMapping
    @Operation(summary = "新增菜品", description = "传入菜品信息保存一条记录")
    @CachePut(cacheNames = "dishCache" ,key = "#dishDTO.categoryId")
    public void save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
    }

    /**
     * 分页查询菜品
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询菜品", description = "有条件查询")
    public PageDTO<DishVO> page(@ParameterObject DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品 : {}", dishPageQueryDTO);
        return dishService.pageQuery(dishPageQueryDTO);
    }

    /**
     * 根据id查询菜品
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询菜品")
    public DishVO getId(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        if (dishVO == null) {
            throw new NotFoundException("菜品不存在");
        }
        return dishVO;
    }

    /**
     * 根据分类id查询菜品
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public List<Dish> list(Long categoryId) {
        log.info("根据分类id查询菜品: {}", categoryId);
        if (categoryId == null) {
            throw new BadRequestException("分类id不能为空");
        }
        return dishService.lambdaQuery()
                .eq(Dish::getCategoryId, categoryId)
                .list();
    }

    /**
     * 修改菜品
     */
    @PutMapping
    @Operation(summary = "修改菜品")
    @CacheEvict(cacheNames = "dishCache" ,allEntries = true)
    public void update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品: {}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
    }

    /**
     * 批量删除菜品
     */
    @DeleteMapping
    @Operation(summary = "批量删除菜品")
    @CacheEvict(cacheNames = "dishCache" ,allEntries = true)
    public void deleteWithFlavor(@RequestParam List<Long> ids) {
        log.info("批量删除菜品: {}", ids);
        dishService.deleteWithFlavor(ids);
    }

    /**
     * 菜品停起售
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "菜品停起售")
    @CacheEvict(cacheNames = "dishCache" ,allEntries = true)
    public void status(@PathVariable Integer status, @RequestParam Long id) {
        log.info("菜品停起售: {} {}", status, id);
        if (id == null) {
            throw new BadRequestException("菜品id不能为空");
        }
        if (status == null || status != 0 && status != 1) {
            throw new BadRequestException("菜品状态不合法");
        }
        if (dishService.getById(id) == null) {
            throw new NotFoundException("菜品不存在");
        }
        dishService.lambdaUpdate()
                .set(Dish::getStatus, status)
                .eq(Dish::getId, id)
                .update();
    }
}

